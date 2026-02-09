/*
 * Copyright (c) 2024 Example
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3.
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.codepilot1c.rag.indexer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;

import com.codepilot1c.core.embedding.EmbeddingProviderRegistry;
import com.codepilot1c.core.embedding.EmbeddingResult;
import com.codepilot1c.core.embedding.IEmbeddingProvider;
import com.codepilot1c.core.index.CodeChunk;
import com.codepilot1c.core.index.ICodebaseIndex;
import com.codepilot1c.core.logging.LogSanitizer;
import com.codepilot1c.core.logging.VibeLogger;
import com.codepilot1c.rag.lucene.LuceneVectorStore;

/**
 * Background job for indexing the codebase.
 *
 * <p>This job scans all projects in the workspace, chunks the code files,
 * generates embeddings, and stores them in the vector index.</p>
 */
public class CodebaseIndexer extends Job {

    private static final VibeLogger.CategoryLogger LOG = VibeLogger.forClass(CodebaseIndexer.class);

    private static final String JOB_NAME = "Indexing Codebase"; //$NON-NLS-1$
    private static final int BATCH_SIZE = 20;
    private static final int MAX_CHUNKS_PER_FILE = 100;
    private static final int MAX_FILE_SIZE_BYTES = 500_000; // 500KB max file size

    private final ICodebaseIndex index;
    private final IEmbeddingProvider embeddingProvider;
    private volatile boolean cancelled = false;

    /**
     * Creates a new codebase indexer job.
     *
     * @param index the codebase index
     * @param embeddingProvider the embedding provider
     */
    public CodebaseIndexer(ICodebaseIndex index, IEmbeddingProvider embeddingProvider) {
        super(JOB_NAME);
        this.index = index;
        this.embeddingProvider = embeddingProvider;
        setUser(false);
        setPriority(Job.LONG);
    }

    /**
     * Creates an indexer with default index and provider.
     *
     * @param indexPath the path to store the index
     * @param embeddingDimension the embedding dimension
     * @param embeddingModel the embedding model name
     * @return a new indexer, or null if provider not available
     */
    public static CodebaseIndexer create(Path indexPath, int embeddingDimension, String embeddingModel) {
        LOG.info("Создание индексатора: path=%s, dimension=%d, model=%s", //$NON-NLS-1$
                indexPath, embeddingDimension, embeddingModel);

        IEmbeddingProvider provider = EmbeddingProviderRegistry.getInstance().getActiveProvider();
        if (provider == null || !provider.isConfigured()) {
            LOG.warn("Embedding провайдер не настроен"); //$NON-NLS-1$
            return null;
        }
        LOG.debug("Используется embedding провайдер: %s", provider.getClass().getSimpleName()); //$NON-NLS-1$

        LuceneVectorStore store = new LuceneVectorStore(indexPath, embeddingDimension, embeddingModel);
        try {
            store.initialize();
            LOG.info("Векторное хранилище инициализировано"); //$NON-NLS-1$
        } catch (ICodebaseIndex.IndexException e) {
            LOG.error("Ошибка инициализации индекса: %s", e.getMessage()); //$NON-NLS-1$
            return null;
        }

        return new CodebaseIndexer(store, provider);
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        long startTime = System.currentTimeMillis();
        LOG.info("=== Начало индексации кодовой базы ==="); //$NON-NLS-1$

        SubMonitor subMonitor = SubMonitor.convert(monitor, JOB_NAME, 100);

        try {
            // Initialize chunker registry
            CodeChunkerRegistry.getInstance().initialize();
            LOG.debug("Реестр чанкеров инициализирован"); //$NON-NLS-1$

            // Count files to index
            long collectStart = System.currentTimeMillis();
            List<IFile> filesToIndex = collectFilesToIndex(subMonitor.split(10));
            LOG.debug("Сбор файлов завершён за %s", //$NON-NLS-1$
                    LogSanitizer.formatDuration(System.currentTimeMillis() - collectStart));

            if (filesToIndex.isEmpty()) {
                LOG.info("Нет файлов для индексации"); //$NON-NLS-1$
                return Status.OK_STATUS;
            }

            LOG.info("Найдено %d файлов для индексации", filesToIndex.size()); //$NON-NLS-1$

            // Index files in batches
            SubMonitor indexMonitor = subMonitor.split(85);
            indexMonitor.setWorkRemaining(filesToIndex.size());

            List<CodeChunk> batchChunks = new ArrayList<>();
            int filesProcessed = 0;
            int totalChunks = 0;
            int errorCount = 0;

            for (IFile file : filesToIndex) {
                if (cancelled || monitor.isCanceled()) {
                    LOG.warn("Индексация отменена пользователем после %d файлов", filesProcessed); //$NON-NLS-1$
                    break;
                }

                try {
                    List<CodeChunk> chunks = chunkFile(file);
                    if (!chunks.isEmpty()) {
                        batchChunks.addAll(chunks);
                        totalChunks += chunks.size();

                        // Process batch when full (or when it gets too large)
                        if (batchChunks.size() >= BATCH_SIZE) {
                            processBatch(batchChunks);
                            LOG.debug("Обработан batch: %d чанков", batchChunks.size()); //$NON-NLS-1$
                            batchChunks = new ArrayList<>(); // Create new list to free memory
                        }
                    }

                } catch (OutOfMemoryError oom) {
                    // Critical: clear batch and force GC
                    batchChunks.clear();
                    batchChunks = new ArrayList<>();
                    System.gc();
                    errorCount++;
                    LOG.error("OutOfMemory при обработке файла %s - очищаем память", file.getFullPath()); //$NON-NLS-1$
                } catch (Exception e) {
                    errorCount++;
                    LOG.error("Ошибка индексации файла %s: %s", file.getFullPath(), e.getMessage()); //$NON-NLS-1$
                }

                filesProcessed++;
                indexMonitor.worked(1);

                // Log progress every 100 files
                if (filesProcessed % 100 == 0) {
                    LOG.debug("Прогресс: %d/%d файлов (%d чанков)", //$NON-NLS-1$
                            filesProcessed, filesToIndex.size(), totalChunks);
                }

                indexMonitor.subTask("Indexed " + filesProcessed + "/" + filesToIndex.size() + " files"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }

            // Process remaining batch
            if (!batchChunks.isEmpty()) {
                processBatch(batchChunks);
                LOG.debug("Обработан финальный batch: %d чанков", batchChunks.size()); //$NON-NLS-1$
            }

            // Commit and optimize
            subMonitor.subTask("Optimizing index..."); //$NON-NLS-1$
            LOG.debug("Коммит и оптимизация индекса..."); //$NON-NLS-1$

            long optimizeStart = System.currentTimeMillis();
            index.commit();
            index.optimize();
            LOG.debug("Оптимизация завершена за %s", //$NON-NLS-1$
                    LogSanitizer.formatDuration(System.currentTimeMillis() - optimizeStart));

            long duration = System.currentTimeMillis() - startTime;
            LOG.info("=== Индексация завершена: %d файлов, %d чанков, %d ошибок за %s ===", //$NON-NLS-1$
                    filesProcessed, totalChunks, errorCount, LogSanitizer.formatDuration(duration));

            return Status.OK_STATUS;

        } catch (Exception e) {
            LOG.error("Критическая ошибка индексации: %s", e.getMessage()); //$NON-NLS-1$
            return new Status(IStatus.ERROR, "com.codepilot1c.rag", "Indexing failed", e); //$NON-NLS-1$ //$NON-NLS-2$
        } finally {
            monitor.done();
        }
    }

    @Override
    protected void canceling() {
        LOG.info("Получен запрос на отмену индексации"); //$NON-NLS-1$
        cancelled = true;
        if (embeddingProvider != null) {
            embeddingProvider.cancel();
        }
    }

    /**
     * Collects all files to index from the workspace.
     */
    private List<IFile> collectFilesToIndex(IProgressMonitor monitor) {
        List<IFile> files = new ArrayList<>();
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

        IProject[] projects = root.getProjects();
        LOG.debug("Сканирование %d проектов в workspace", projects.length); //$NON-NLS-1$

        SubMonitor subMonitor = SubMonitor.convert(monitor, "Collecting files", projects.length); //$NON-NLS-1$

        for (IProject project : projects) {
            if (!project.isOpen()) {
                LOG.debug("Пропуск закрытого проекта: %s", project.getName()); //$NON-NLS-1$
                subMonitor.worked(1);
                continue;
            }

            int projectFilesBefore = files.size();
            try {
                project.accept(new IResourceVisitor() {
                    @Override
                    public boolean visit(IResource resource) throws CoreException {
                        if (resource instanceof IFile) {
                            IFile file = (IFile) resource;
                            Optional<ICodeChunker> chunker = CodeChunkerRegistry.getInstance()
                                    .getChunkerForFile(file);
                            if (chunker.isPresent()) {
                                files.add(file);
                            }
                        }
                        return true;
                    }
                });
                int projectFiles = files.size() - projectFilesBefore;
                if (projectFiles > 0) {
                    LOG.debug("Проект %s: найдено %d файлов для индексации", //$NON-NLS-1$
                            project.getName(), projectFiles);
                }
            } catch (CoreException e) {
                LOG.error("Ошибка сканирования проекта %s: %s", project.getName(), e.getMessage()); //$NON-NLS-1$
            }

            subMonitor.worked(1);
        }

        return files;
    }

    /**
     * Chunks a file into code chunks.
     */
    private List<CodeChunk> chunkFile(IFile file) throws Exception {
        Optional<ICodeChunker> chunkerOpt = CodeChunkerRegistry.getInstance().getChunkerForFile(file);
        if (!chunkerOpt.isPresent()) {
            return new ArrayList<>();
        }

        // Check file size to avoid OOM on huge files
        long fileSize = file.getLocation().toFile().length();
        if (fileSize > MAX_FILE_SIZE_BYTES) {
            LOG.warn("Пропуск слишком большого файла (%d KB): %s", //$NON-NLS-1$
                    fileSize / 1024, file.getFullPath());
            return new ArrayList<>();
        }

        ICodeChunker chunker = chunkerOpt.get();
        String content;
        try (java.io.InputStream stream = file.getContents()) {
            content = new String(stream.readAllBytes(), file.getCharset());
        }
        String projectName = file.getProject().getName();

        List<CodeChunk> chunks = chunker.chunk(file, content, projectName);

        // Limit chunks per file to prevent memory issues
        if (chunks.size() > MAX_CHUNKS_PER_FILE) {
            LOG.warn("Файл %s: ограничено до %d чанков (было %d)", //$NON-NLS-1$
                    file.getFullPath(), MAX_CHUNKS_PER_FILE, chunks.size());
            return chunks.subList(0, MAX_CHUNKS_PER_FILE);
        }

        return chunks;
    }

    /**
     * Processes a batch of chunks: generates embeddings and stores them.
     */
    private void processBatch(List<CodeChunk> chunks) throws Exception {
        if (chunks.isEmpty()) {
            return;
        }

        long startTime = System.currentTimeMillis();

        // Extract content for embedding
        List<String> texts = new ArrayList<>();
        int totalChars = 0;
        for (CodeChunk chunk : chunks) {
            texts.add(chunk.getContent());
            totalChars += chunk.getContent().length();
        }

        // Generate embeddings
        long embedStart = System.currentTimeMillis();
        CompletableFuture<List<EmbeddingResult>> future = embeddingProvider.embedBatch(texts);
        List<EmbeddingResult> results = future.get();
        LOG.debug("Embeddings для %d чанков получены за %s (%d символов)", //$NON-NLS-1$
                chunks.size(),
                LogSanitizer.formatDuration(System.currentTimeMillis() - embedStart),
                totalChars);

        // Store in index
        long storeStart = System.currentTimeMillis();
        List<float[]> embeddings = new ArrayList<>();
        for (EmbeddingResult result : results) {
            embeddings.add(result.getEmbedding());
        }

        index.upsertChunks(chunks, embeddings);
        LOG.debug("Чанки сохранены в индекс за %s", //$NON-NLS-1$
                LogSanitizer.formatDuration(System.currentTimeMillis() - storeStart));
    }

    /**
     * Returns the codebase index.
     *
     * @return the index
     */
    public ICodebaseIndex getIndex() {
        return index;
    }

    /**
     * Indexes a single file (for incremental updates).
     *
     * @param file the file to index
     * @throws Exception if indexing fails
     */
    public void indexFile(IFile file) throws Exception {
        String filePath = file.getFullPath().toString();
        LOG.debug("Инкрементальная индексация файла: %s", filePath); //$NON-NLS-1$

        long startTime = System.currentTimeMillis();

        // Delete existing chunks for this file
        index.deleteByFile(filePath);

        // Chunk and index
        List<CodeChunk> chunks = chunkFile(file);
        if (!chunks.isEmpty()) {
            processBatch(chunks);
            index.commit();
            LOG.debug("Файл проиндексирован: %d чанков за %s", //$NON-NLS-1$
                    chunks.size(), LogSanitizer.formatDuration(System.currentTimeMillis() - startTime));
        } else {
            LOG.debug("Файл не содержит индексируемого контента: %s", filePath); //$NON-NLS-1$
        }
    }

    /**
     * Removes a file from the index.
     *
     * @param filePath the file path
     */
    public void removeFile(String filePath) {
        LOG.debug("Удаление файла из индекса: %s", filePath); //$NON-NLS-1$
        index.deleteByFile(filePath);
        index.commit();
    }
}
