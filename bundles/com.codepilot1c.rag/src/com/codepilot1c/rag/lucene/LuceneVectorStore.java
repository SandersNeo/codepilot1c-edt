/*
 * Copyright (c) 2024 Example
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3.
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.codepilot1c.rag.lucene;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import com.codepilot1c.core.index.CodeChunk;
import com.codepilot1c.core.index.CodeSearchHit;
import com.codepilot1c.core.index.ICodebaseIndex;
import com.codepilot1c.core.index.IndexStats;
import com.codepilot1c.core.logging.LogSanitizer;
import com.codepilot1c.core.logging.VibeLogger;

/**
 * Lucene-based implementation of the codebase index with vector search support.
 *
 * <p>Uses Apache Lucene for indexing code chunks. For KNN vector search,
 * embeddings are stored as binary fields and cosine similarity is computed
 * at search time.</p>
 *
 * <p>Note: For production use with large codebases, consider using Lucene 9.x+
 * with native KNN vector search or an external vector database.</p>
 */
public class LuceneVectorStore implements ICodebaseIndex {

    private static final VibeLogger.CategoryLogger LOG = VibeLogger.forClass(LuceneVectorStore.class);

    private static final String FIELD_ID = "id"; //$NON-NLS-1$
    private static final String FIELD_FILE_PATH = "filePath"; //$NON-NLS-1$
    private static final String FIELD_PROJECT_NAME = "projectName"; //$NON-NLS-1$
    private static final String FIELD_CONTENT = "content"; //$NON-NLS-1$
    private static final String FIELD_SYMBOL_NAME = "symbolName"; //$NON-NLS-1$
    private static final String FIELD_CHUNK_TYPE = "chunkType"; //$NON-NLS-1$
    private static final String FIELD_START_LINE = "startLine"; //$NON-NLS-1$
    private static final String FIELD_END_LINE = "endLine"; //$NON-NLS-1$
    private static final String FIELD_METADATA_PATH = "metadataPath"; //$NON-NLS-1$
    private static final String FIELD_EMBEDDING = "embedding"; //$NON-NLS-1$
    private static final String FIELD_EMBEDDING_DIMENSION = "embeddingDimension"; //$NON-NLS-1$

    private final Path indexPath;
    private final int embeddingDimension;
    private final String embeddingModel;

    private Directory directory;
    private IndexWriter writer;
    private DirectoryReader reader;
    private IndexSearcher searcher;
    private StandardAnalyzer analyzer;

    private boolean initialized = false;
    private Instant lastUpdated;

    /**
     * Creates a new Lucene vector store.
     *
     * @param indexPath the path to store the index
     * @param embeddingDimension the embedding vector dimension
     * @param embeddingModel the embedding model identifier
     */
    public LuceneVectorStore(Path indexPath, int embeddingDimension, String embeddingModel) {
        this.indexPath = indexPath;
        this.embeddingDimension = embeddingDimension;
        this.embeddingModel = embeddingModel;
    }

    @Override
    public synchronized void initialize() throws IndexException {
        if (initialized) {
            LOG.debug("Индекс уже инициализирован"); //$NON-NLS-1$
            return;
        }

        long startTime = System.currentTimeMillis();
        LOG.info("Инициализация Lucene индекса: %s (dimension=%d, model=%s)", //$NON-NLS-1$
                indexPath, embeddingDimension, embeddingModel);

        try {
            Files.createDirectories(indexPath);
            directory = FSDirectory.open(indexPath);
            analyzer = new StandardAnalyzer();

            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
            writer = new IndexWriter(directory, config);
            writer.commit();

            refreshReader();
            initialized = true;
            lastUpdated = Instant.now();

            long duration = System.currentTimeMillis() - startTime;
            LOG.info("Lucene индекс инициализирован за %s, документов: %d", //$NON-NLS-1$
                    LogSanitizer.formatDuration(duration), reader.numDocs());
        } catch (IOException e) {
            LOG.error("Ошибка инициализации Lucene индекса: %s", e.getMessage()); //$NON-NLS-1$
            throw new IndexException("Failed to initialize Lucene index", e); //$NON-NLS-1$
        }
    }

    @Override
    public boolean isReady() {
        return initialized && writer != null && !writer.isOpen() == false;
    }

    @Override
    public synchronized void upsertChunk(CodeChunk chunk, float[] embedding) {
        checkInitialized();
        validateEmbedding(embedding);

        try {
            // Delete existing document with same ID
            writer.deleteDocuments(new Term(FIELD_ID, chunk.getId()));

            // Create new document
            Document doc = createDocument(chunk, embedding);
            writer.addDocument(doc);

            lastUpdated = Instant.now();
        } catch (IOException e) {
            LOG.error("Ошибка upsert чанка %s: %s", chunk.getId(), e.getMessage()); //$NON-NLS-1$
        }
    }

    @Override
    public synchronized void upsertChunks(List<CodeChunk> chunks, List<float[]> embeddings) {
        checkInitialized();

        if (chunks.size() != embeddings.size()) {
            throw new IllegalArgumentException("Chunks and embeddings must have same size"); //$NON-NLS-1$
        }

        try {
            for (int i = 0; i < chunks.size(); i++) {
                CodeChunk chunk = chunks.get(i);
                float[] embedding = embeddings.get(i);
                validateEmbedding(embedding);

                writer.deleteDocuments(new Term(FIELD_ID, chunk.getId()));
                Document doc = createDocument(chunk, embedding);
                writer.addDocument(doc);
            }

            lastUpdated = Instant.now();
        } catch (IOException e) {
            LOG.error("Ошибка upsert %d чанков: %s", chunks.size(), e.getMessage()); //$NON-NLS-1$
        }
    }

    @Override
    public synchronized int deleteByFile(String filePath) {
        checkInitialized();

        try {
            // Count before delete
            refreshReader();
            int countBefore = countByField(FIELD_FILE_PATH, filePath);

            writer.deleteDocuments(new Term(FIELD_FILE_PATH, filePath));
            lastUpdated = Instant.now();

            return countBefore;
        } catch (IOException e) {
            LOG.error("Failed to delete by file: " + filePath, e); //$NON-NLS-1$
            return 0;
        }
    }

    @Override
    public synchronized int deleteByProject(String projectName) {
        checkInitialized();

        try {
            refreshReader();
            int countBefore = countByField(FIELD_PROJECT_NAME, projectName);

            writer.deleteDocuments(new Term(FIELD_PROJECT_NAME, projectName));
            lastUpdated = Instant.now();

            return countBefore;
        } catch (IOException e) {
            LOG.error("Failed to delete by project: " + projectName, e); //$NON-NLS-1$
            return 0;
        }
    }

    @Override
    public synchronized boolean deleteChunk(String chunkId) {
        checkInitialized();

        try {
            long seqNo = writer.deleteDocuments(new Term(FIELD_ID, chunkId));
            lastUpdated = Instant.now();
            return seqNo >= 0;
        } catch (IOException e) {
            LOG.error("Failed to delete chunk: " + chunkId, e); //$NON-NLS-1$
            return false;
        }
    }

    @Override
    public List<CodeSearchHit> searchKnn(float[] queryEmbedding, int k) {
        return searchKnn(queryEmbedding, k, null);
    }

    @Override
    public synchronized List<CodeSearchHit> searchKnn(float[] queryEmbedding, int k, String projectName) {
        checkInitialized();
        validateEmbedding(queryEmbedding);

        long startTime = System.currentTimeMillis();
        LOG.debug("KNN поиск: k=%d, project=%s", k, projectName != null ? projectName : "все"); //$NON-NLS-1$ //$NON-NLS-2$

        List<CodeSearchHit> results = new ArrayList<>();

        try {
            refreshReader();

            int totalDocs = reader.numDocs();
            if (totalDocs == 0) {
                LOG.debug("Индекс пуст, поиск не выполняется"); //$NON-NLS-1$
                return results;
            }

            // For each document, compute cosine similarity and collect top-k
            List<ScoredDocument> scoredDocs = new ArrayList<>();
            int docsProcessed = 0;

            for (int i = 0; i < reader.maxDoc(); i++) {
                Document doc = reader.storedFields().document(i);

                // Filter by project if specified
                if (projectName != null) {
                    String docProject = doc.get(FIELD_PROJECT_NAME);
                    if (!projectName.equals(docProject)) {
                        continue;
                    }
                }
                docsProcessed++;

                // Get embedding
                byte[] embeddingBytes = doc.getBinaryValue(FIELD_EMBEDDING).bytes;
                float[] embedding = bytesToFloats(embeddingBytes);

                // Compute cosine similarity
                float similarity = cosineSimilarity(queryEmbedding, embedding);
                scoredDocs.add(new ScoredDocument(doc, similarity));
            }

            // Sort by score descending
            scoredDocs.sort((a, b) -> Float.compare(b.score, a.score));

            // Take top-k
            int limit = Math.min(k, scoredDocs.size());
            for (int i = 0; i < limit; i++) {
                ScoredDocument sd = scoredDocs.get(i);
                results.add(documentToSearchHit(sd.doc, sd.score));
            }

            long duration = System.currentTimeMillis() - startTime;
            LOG.debug("KNN поиск завершён: %d результатов из %d документов за %s", //$NON-NLS-1$
                    results.size(), docsProcessed, LogSanitizer.formatDuration(duration));

        } catch (IOException e) {
            LOG.error("Ошибка KNN поиска: %s", e.getMessage()); //$NON-NLS-1$
        }

        return results;
    }

    @Override
    public synchronized CodeChunk getChunk(String chunkId) {
        checkInitialized();

        try {
            refreshReader();

            TermQuery query = new TermQuery(new Term(FIELD_ID, chunkId));
            TopDocs topDocs = searcher.search(query, 1);

            if (topDocs.totalHits.value > 0) {
                Document doc = reader.storedFields().document(topDocs.scoreDocs[0].doc);
                return documentToChunk(doc);
            }
        } catch (IOException e) {
            LOG.error("Failed to get chunk: " + chunkId, e); //$NON-NLS-1$
        }

        return null;
    }

    @Override
    public synchronized List<CodeChunk> getChunksByFile(String filePath) {
        checkInitialized();

        List<CodeChunk> chunks = new ArrayList<>();

        try {
            refreshReader();

            TermQuery query = new TermQuery(new Term(FIELD_FILE_PATH, filePath));
            TopDocs topDocs = searcher.search(query, Integer.MAX_VALUE);

            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc = reader.storedFields().document(scoreDoc.doc);
                chunks.add(documentToChunk(doc));
            }
        } catch (IOException e) {
            LOG.error("Failed to get chunks by file: " + filePath, e); //$NON-NLS-1$
        }

        return chunks;
    }

    @Override
    public synchronized IndexStats getStats() {
        checkInitialized();

        try {
            refreshReader();

            int totalChunks = reader.numDocs();

            // Count unique files
            int totalFiles = countUniqueValues(FIELD_FILE_PATH);

            // Count unique projects
            int totalProjects = countUniqueValues(FIELD_PROJECT_NAME);

            // Get index size
            long indexSizeBytes = getDirectorySize(indexPath);

            return new IndexStats(
                    totalChunks,
                    totalFiles,
                    totalProjects,
                    indexSizeBytes,
                    lastUpdated,
                    embeddingDimension,
                    embeddingModel
            );
        } catch (IOException e) {
            LOG.error("Failed to get stats", e); //$NON-NLS-1$
            return IndexStats.empty();
        }
    }

    @Override
    public synchronized void commit() {
        checkInitialized();

        try {
            writer.commit();
            refreshReader();
        } catch (IOException e) {
            LOG.error("Failed to commit", e); //$NON-NLS-1$
        }
    }

    @Override
    public synchronized void optimize() {
        checkInitialized();

        try {
            writer.forceMerge(1);
            writer.commit();
            refreshReader();
        } catch (IOException e) {
            LOG.error("Failed to optimize", e); //$NON-NLS-1$
        }
    }

    @Override
    public synchronized void clear() {
        checkInitialized();

        try {
            writer.deleteAll();
            writer.commit();
            refreshReader();
            lastUpdated = Instant.now();
        } catch (IOException e) {
            LOG.error("Failed to clear index", e); //$NON-NLS-1$
        }
    }

    @Override
    public synchronized List<CodeChunk> searchByKeyword(String keyword, int maxResults) {
        checkInitialized();

        List<CodeChunk> results = new ArrayList<>();

        try {
            refreshReader();

            if (reader.numDocs() == 0 || keyword == null || keyword.trim().isEmpty()) {
                return results;
            }

            // Parse keywords and create query
            String[] terms = keyword.toLowerCase().trim().split("\\s+"); //$NON-NLS-1$

            BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();
            for (String term : terms) {
                if (term.length() > 1) {
                    // Search in content field
                    queryBuilder.add(
                            new org.apache.lucene.search.WildcardQuery(
                                    new Term(FIELD_CONTENT, "*" + term + "*")), //$NON-NLS-1$ //$NON-NLS-2$
                            BooleanClause.Occur.SHOULD
                    );
                    // Also search in symbol name
                    queryBuilder.add(
                            new org.apache.lucene.search.WildcardQuery(
                                    new Term(FIELD_SYMBOL_NAME, "*" + term + "*")), //$NON-NLS-1$ //$NON-NLS-2$
                            BooleanClause.Occur.SHOULD
                    );
                }
            }

            BooleanQuery query = queryBuilder.build();
            TopDocs topDocs = searcher.search(query, maxResults);

            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc = reader.storedFields().document(scoreDoc.doc);
                results.add(documentToChunk(doc));
            }
        } catch (IOException e) {
            LOG.error("Failed to search by keyword: " + keyword, e); //$NON-NLS-1$
        }

        return results;
    }

    @Override
    public synchronized void close() {
        LOG.info("Закрытие Lucene индекса: %s", indexPath); //$NON-NLS-1$
        try {
            if (reader != null) {
                reader.close();
                reader = null;
            }
            if (writer != null) {
                writer.close();
                writer = null;
            }
            if (directory != null) {
                directory.close();
                directory = null;
            }
            if (analyzer != null) {
                analyzer.close();
                analyzer = null;
            }
            initialized = false;
            LOG.info("Lucene индекс закрыт"); //$NON-NLS-1$
        } catch (IOException e) {
            LOG.error("Ошибка при закрытии индекса: %s", e.getMessage()); //$NON-NLS-1$
        }
    }

    // --- Private helper methods ---

    private void checkInitialized() {
        if (!initialized) {
            throw new IndexException("Index not initialized"); //$NON-NLS-1$
        }
    }

    private void validateEmbedding(float[] embedding) {
        if (embedding == null || embedding.length != embeddingDimension) {
            throw new IllegalArgumentException(
                    "Expected embedding dimension " + embeddingDimension + //$NON-NLS-1$
                            " but got " + (embedding == null ? "null" : embedding.length)); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    private void refreshReader() throws IOException {
        if (reader == null) {
            reader = DirectoryReader.open(writer);
            searcher = new IndexSearcher(reader);
        } else {
            DirectoryReader newReader = DirectoryReader.openIfChanged(reader, writer);
            if (newReader != null) {
                reader.close();
                reader = newReader;
                searcher = new IndexSearcher(reader);
            }
        }
    }

    private Document createDocument(CodeChunk chunk, float[] embedding) {
        Document doc = new Document();

        doc.add(new StringField(FIELD_ID, chunk.getId(), Field.Store.YES));
        doc.add(new StringField(FIELD_FILE_PATH, chunk.getFilePath(), Field.Store.YES));
        doc.add(new StringField(FIELD_PROJECT_NAME, chunk.getProjectName(), Field.Store.YES));
        doc.add(new TextField(FIELD_CONTENT, chunk.getContent(), Field.Store.YES));
        doc.add(new StringField(FIELD_SYMBOL_NAME, chunk.getSymbolName(), Field.Store.YES));
        doc.add(new StringField(FIELD_CHUNK_TYPE, chunk.getChunkType().name(), Field.Store.YES));
        doc.add(new IntPoint(FIELD_START_LINE, chunk.getStartLine()));
        doc.add(new StoredField(FIELD_START_LINE, chunk.getStartLine()));
        doc.add(new IntPoint(FIELD_END_LINE, chunk.getEndLine()));
        doc.add(new StoredField(FIELD_END_LINE, chunk.getEndLine()));
        doc.add(new StringField(FIELD_METADATA_PATH,
                chunk.getMetadataPath() != null ? chunk.getMetadataPath() : "", //$NON-NLS-1$
                Field.Store.YES));

        // Store embedding as binary
        doc.add(new StoredField(FIELD_EMBEDDING, floatsToBytes(embedding)));
        doc.add(new StoredField(FIELD_EMBEDDING_DIMENSION, embedding.length));

        return doc;
    }

    private CodeChunk documentToChunk(Document doc) {
        return CodeChunk.builder()
                .id(doc.get(FIELD_ID))
                .filePath(doc.get(FIELD_FILE_PATH))
                .projectName(doc.get(FIELD_PROJECT_NAME))
                .content(doc.get(FIELD_CONTENT))
                .symbolName(doc.get(FIELD_SYMBOL_NAME))
                .chunkType(CodeChunk.ChunkType.valueOf(doc.get(FIELD_CHUNK_TYPE)))
                .startLine(doc.getField(FIELD_START_LINE).numericValue().intValue())
                .endLine(doc.getField(FIELD_END_LINE).numericValue().intValue())
                .metadataPath(doc.get(FIELD_METADATA_PATH))
                .build();
    }

    private CodeSearchHit documentToSearchHit(Document doc, float score) {
        return new CodeSearchHit(
                doc.get(FIELD_ID),
                doc.get(FIELD_FILE_PATH),
                doc.get(FIELD_PROJECT_NAME),
                doc.get(FIELD_CONTENT),
                doc.get(FIELD_SYMBOL_NAME),
                CodeChunk.ChunkType.valueOf(doc.get(FIELD_CHUNK_TYPE)),
                doc.getField(FIELD_START_LINE).numericValue().intValue(),
                doc.getField(FIELD_END_LINE).numericValue().intValue(),
                doc.get(FIELD_METADATA_PATH),
                score
        );
    }

    private int countByField(String fieldName, String value) throws IOException {
        TermQuery query = new TermQuery(new Term(fieldName, value));
        TopDocs topDocs = searcher.search(query, Integer.MAX_VALUE);
        return (int) topDocs.totalHits.value;
    }

    private int countUniqueValues(String fieldName) throws IOException {
        java.util.Set<String> uniqueValues = new java.util.HashSet<>();
        for (int i = 0; i < reader.maxDoc(); i++) {
            Document doc = reader.storedFields().document(i);
            String value = doc.get(fieldName);
            if (value != null) {
                uniqueValues.add(value);
            }
        }
        return uniqueValues.size();
    }

    private long getDirectorySize(Path path) {
        try {
            return Files.walk(path)
                    .filter(Files::isRegularFile)
                    .mapToLong(p -> {
                        try {
                            return Files.size(p);
                        } catch (IOException e) {
                            return 0;
                        }
                    })
                    .sum();
        } catch (IOException e) {
            return 0;
        }
    }

    /**
     * Computes cosine similarity between two vectors.
     */
    private float cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) {
            return 0;
        }

        float dotProduct = 0;
        float normA = 0;
        float normB = 0;

        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        if (normA == 0 || normB == 0) {
            return 0;
        }

        return dotProduct / (float) (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * Converts float array to bytes for storage.
     */
    private byte[] floatsToBytes(float[] floats) {
        java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocate(floats.length * 4);
        for (float f : floats) {
            buffer.putFloat(f);
        }
        return buffer.array();
    }

    /**
     * Converts bytes back to float array.
     */
    private float[] bytesToFloats(byte[] bytes) {
        java.nio.ByteBuffer buffer = java.nio.ByteBuffer.wrap(bytes);
        float[] floats = new float[bytes.length / 4];
        for (int i = 0; i < floats.length; i++) {
            floats[i] = buffer.getFloat();
        }
        return floats;
    }

    /**
     * Helper class for scored documents during search.
     */
    private static class ScoredDocument {
        final Document doc;
        final float score;

        ScoredDocument(Document doc, float score) {
            this.doc = doc;
            this.score = score;
        }
    }
}
