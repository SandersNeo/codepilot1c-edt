package com.codepilot1c.core.edt.external;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.common.util.EMap;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;

import com._1c.g5.v8.dt.core.platform.IExternalObjectProject;
import com._1c.g5.v8.dt.core.platform.IV8Project;
import com._1c.g5.v8.dt.core.platform.IV8ProjectManager;
import com._1c.g5.v8.dt.metadata.mdclass.MdClassFactory;
import com._1c.g5.v8.dt.metadata.mdclass.MdObject;
import com._1c.g5.v8.dt.platform.version.Version;
import com.codepilot1c.core.edt.metadata.EdtMetadataGateway;
import com.codepilot1c.core.edt.metadata.MetadataOperationCode;
import com.codepilot1c.core.edt.metadata.MetadataOperationException;
import com.codepilot1c.core.logging.VibeLogger;

/**
 * Service for external reports/processors in EDT external-object projects.
 */
public class EdtExternalObjectService {

    private static final VibeLogger.CategoryLogger LOG = VibeLogger.forClass(EdtExternalObjectService.class);
    private static final String RU_LANGUAGE = "ru"; //$NON-NLS-1$
    private static final int REGISTRATION_WAIT_ATTEMPTS = 8;
    private static final long REGISTRATION_WAIT_SLEEP_MS = 250L;

    private final EdtMetadataGateway gateway;

    public EdtExternalObjectService() {
        this(new EdtMetadataGateway());
    }

    EdtExternalObjectService(EdtMetadataGateway gateway) {
        this.gateway = gateway;
    }

    public ExternalCreateObjectResult createReport(ExternalCreateReportRequest request) {
        request.validate();
        gateway.ensureExternalObjectRuntimeAvailable();

        IProject baseProject = resolveBaseProject(request.normalizedProjectName());
        ensureExternalProjectDoesNotExist(request.normalizedExternalProjectName());

        IV8Project baseV8Project = gateway.getV8ProjectManager().getProject(baseProject);
        if (baseV8Project == null) {
            throw new MetadataOperationException(
                    MetadataOperationCode.PROJECT_NOT_FOUND,
                    "Base project is not a V8 project: " + baseProject.getName(),
                    false); //$NON-NLS-1$
        }

        Version version = request.effectiveVersion(baseV8Project.getVersion());
        Path defaultContainer = Path.of(baseProject.getLocation().toOSString()).getParent();
        if (defaultContainer == null) {
            defaultContainer = Path.of(baseProject.getWorkspace().getRoot().getLocation().toOSString());
        }
        Path projectPath = request.effectiveProjectPath(defaultContainer);

        MdObject externalObject = MdClassFactory.eINSTANCE.createExternalReport();
        setCommonProperties(externalObject, request.normalizedObjectName(), request.normalizedSynonym(), request.normalizedComment());

        IProject createdProject;
        try {
            createdProject = gateway.getExternalObjectProjectManager().create(
                    request.normalizedExternalProjectName(),
                    projectPath,
                    version,
                    externalObject,
                    baseProject,
                    new NullProgressMonitor());
        } catch (CoreException e) {
            throw new MetadataOperationException(
                    MetadataOperationCode.EDT_TRANSACTION_FAILED,
                    "Failed to create external report project: " + e.getMessage(),
                    false,
                    e); //$NON-NLS-1$
        }
        if (createdProject == null) {
            throw new MetadataOperationException(
                    MetadataOperationCode.EXTERNAL_OBJECT_API_UNAVAILABLE,
                    "IExternalObjectProjectManager returned null project",
                    false); //$NON-NLS-1$
        }
        awaitRegistration(baseProject, createdProject.getName());

        return new ExternalCreateObjectResult(
                baseProject.getName(),
                createdProject.getName(),
                syntheticFqn(externalObject),
                externalObject.eClass().getName(),
                safe(externalObject.getName()),
                createdProject.getLocation() != null ? createdProject.getLocation().toOSString() : "", //$NON-NLS-1$
                version != null ? version.toString() : ""); //$NON-NLS-1$
    }

    public ExternalCreateObjectResult createProcessing(ExternalCreateProcessingRequest request) {
        request.validate();
        gateway.ensureExternalObjectRuntimeAvailable();

        IProject baseProject = resolveBaseProject(request.normalizedProjectName());
        ensureExternalProjectDoesNotExist(request.normalizedExternalProjectName());

        IV8Project baseV8Project = gateway.getV8ProjectManager().getProject(baseProject);
        if (baseV8Project == null) {
            throw new MetadataOperationException(
                    MetadataOperationCode.PROJECT_NOT_FOUND,
                    "Base project is not a V8 project: " + baseProject.getName(),
                    false); //$NON-NLS-1$
        }

        Version version = request.effectiveVersion(baseV8Project.getVersion());
        Path defaultContainer = Path.of(baseProject.getLocation().toOSString()).getParent();
        if (defaultContainer == null) {
            defaultContainer = Path.of(baseProject.getWorkspace().getRoot().getLocation().toOSString());
        }
        Path projectPath = request.effectiveProjectPath(defaultContainer);

        MdObject externalObject = MdClassFactory.eINSTANCE.createExternalDataProcessor();
        setCommonProperties(externalObject, request.normalizedObjectName(), request.normalizedSynonym(), request.normalizedComment());

        IProject createdProject;
        try {
            createdProject = gateway.getExternalObjectProjectManager().create(
                    request.normalizedExternalProjectName(),
                    projectPath,
                    version,
                    externalObject,
                    baseProject,
                    new NullProgressMonitor());
        } catch (CoreException e) {
            throw new MetadataOperationException(
                    MetadataOperationCode.EDT_TRANSACTION_FAILED,
                    "Failed to create external data processor project: " + e.getMessage(),
                    false,
                    e); //$NON-NLS-1$
        }
        if (createdProject == null) {
            throw new MetadataOperationException(
                    MetadataOperationCode.EXTERNAL_OBJECT_API_UNAVAILABLE,
                    "IExternalObjectProjectManager returned null project",
                    false); //$NON-NLS-1$
        }
        awaitRegistration(baseProject, createdProject.getName());

        return new ExternalCreateObjectResult(
                baseProject.getName(),
                createdProject.getName(),
                syntheticFqn(externalObject),
                externalObject.eClass().getName(),
                safe(externalObject.getName()),
                createdProject.getLocation() != null ? createdProject.getLocation().toOSString() : "", //$NON-NLS-1$
                version != null ? version.toString() : ""); //$NON-NLS-1$
    }

    public ExternalObjectsResult listObjects(ExternalListObjectsRequest request) {
        request.validate();
        gateway.ensureExternalObjectRuntimeAvailable();

        IProject baseProject = resolveBaseProject(request.normalizedProjectName());
        String externalProjectFilter = request.normalizedExternalProjectName();
        String typeFilter = request.normalizedTypeFilter();
        String nameFilter = request.normalizedNameContains();
        int offset = request.effectiveOffset();
        int limit = request.effectiveLimit();

        List<ExternalObjectSummary> all = new ArrayList<>();
        for (IExternalObjectProject externalProject : listExternalProjects(baseProject, externalProjectFilter)) {
            all.addAll(collectObjectSummaries(externalProject, baseProject, typeFilter, nameFilter));
        }

        all.sort(Comparator
                .comparing(ExternalObjectSummary::kind, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(ExternalObjectSummary::name, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(ExternalObjectSummary::externalProject, String.CASE_INSENSITIVE_ORDER));

        int total = all.size();
        int start = Math.min(offset, total);
        int end = Math.min(start + limit, total);
        List<ExternalObjectSummary> page = start >= end
                ? List.of()
                : new ArrayList<>(all.subList(start, end));

        return new ExternalObjectsResult(
                baseProject.getName(),
                total,
                page.size(),
                start,
                limit,
                end < total,
                page);
    }

    public ExternalProjectsResult listProjects(ExternalListProjectsRequest request) {
        request.validate();
        gateway.ensureExternalObjectRuntimeAvailable();

        IProject baseProject = resolveBaseProject(request.normalizedProjectName());
        String nameFilter = request.normalizedNameContains();
        int offset = request.effectiveOffset();
        int limit = request.effectiveLimit();

        List<ExternalProjectSummary> all = new ArrayList<>();
        for (IExternalObjectProject externalProject : listExternalProjects(baseProject, null)) {
            String externalName = externalProject.getProject().getName();
            if (nameFilter != null && !externalName.toLowerCase(Locale.ROOT).contains(nameFilter)) {
                continue;
            }
            String path = externalProject.getProject().getLocation() != null
                    ? externalProject.getProject().getLocation().toOSString()
                    : ""; //$NON-NLS-1$
            String version = externalProject.getVersion() != null ? externalProject.getVersion().toString() : ""; //$NON-NLS-1$
            all.add(new ExternalProjectSummary(baseProject.getName(), externalName, path, version));
        }
        all.sort(Comparator.comparing(ExternalProjectSummary::externalProject, String.CASE_INSENSITIVE_ORDER));

        int total = all.size();
        int start = Math.min(offset, total);
        int end = Math.min(start + limit, total);
        List<ExternalProjectSummary> page = start >= end ? List.of() : new ArrayList<>(all.subList(start, end));

        return new ExternalProjectsResult(
                baseProject.getName(),
                total,
                page.size(),
                start,
                limit,
                end < total,
                page);
    }

    public ExternalObjectDetailsResult getDetails(ExternalGetDetailsRequest request) {
        request.validate();
        gateway.ensureExternalObjectRuntimeAvailable();

        IProject baseProject = resolveBaseProject(request.normalizedProjectName());
        String externalProjectFilter = request.normalizedExternalProjectName();
        String requestedRef = request.normalizedObjectFqn();
        String requestedRefNormalized = normalize(requestedRef);

        for (IExternalObjectProject externalProject : listExternalProjects(baseProject, externalProjectFilter)) {
            for (MdObject object : safeExternalObjects(externalProject)) {
                if (object == null) {
                    continue;
                }
                String syntheticFqn = syntheticFqn(object);
                if (!matchesRequestedObject(requestedRef, requestedRefNormalized, syntheticFqn, object.getName())) {
                    continue;
                }
                return buildDetails(baseProject, externalProject, object, syntheticFqn);
            }
        }

        throw new MetadataOperationException(
                MetadataOperationCode.EXTERNAL_OBJECT_NOT_FOUND,
                "External object not found: " + requestedRef,
                false); //$NON-NLS-1$
    }

    private IProject resolveBaseProject(String projectName) {
        IProject project = gateway.resolveProject(projectName);
        if (project == null || !project.exists()) {
            throw new MetadataOperationException(
                    MetadataOperationCode.PROJECT_NOT_FOUND,
                    "Project not found: " + projectName,
                    false); //$NON-NLS-1$
        }
        return project;
    }

    private void ensureExternalProjectDoesNotExist(String externalProjectName) {
        IProject existingProject = gateway.resolveProject(externalProjectName);
        if (existingProject != null && existingProject.exists()) {
            throw new MetadataOperationException(
                    MetadataOperationCode.EXTERNAL_OBJECT_ALREADY_EXISTS,
                    "Project already exists: " + externalProjectName,
                    false); //$NON-NLS-1$
        }
    }

    private List<IExternalObjectProject> listExternalProjects(IProject baseProject, String externalProjectFilter) {
        IV8ProjectManager v8ProjectManager = gateway.getV8ProjectManager();
        List<IExternalObjectProject> projects = new ArrayList<>();
        for (IExternalObjectProject externalProject : v8ProjectManager.getProjects(IExternalObjectProject.class)) {
            if (externalProject == null || externalProject.getProject() == null) {
                continue;
            }
            if (!belongsToBaseProject(externalProject, baseProject.getName())) {
                continue;
            }
            if (externalProjectFilter != null
                    && !externalProject.getProject().getName().equalsIgnoreCase(externalProjectFilter)) {
                continue;
            }
            projects.add(externalProject);
        }
        projects.sort(Comparator.comparing(p -> p.getProject().getName(), String.CASE_INSENSITIVE_ORDER));
        return projects;
    }

    private void awaitRegistration(IProject baseProject, String externalProjectName) {
        for (int attempt = 1; attempt <= REGISTRATION_WAIT_ATTEMPTS; attempt++) {
            if (isRegisteredForBaseProject(baseProject.getName(), externalProjectName)) {
                if (attempt > 1) {
                    LOG.debug("External project registration confirmed: %s (attempt=%d)", //$NON-NLS-1$
                            externalProjectName, Integer.valueOf(attempt));
                }
                return;
            }
            refreshProject(baseProject);
            sleep(REGISTRATION_WAIT_SLEEP_MS);
        }
        LOG.warn("External project registration delayed: %s base=%s", //$NON-NLS-1$
                externalProjectName, baseProject.getName());
    }

    private boolean isRegisteredForBaseProject(String baseProjectName, String externalProjectName) {
        for (IExternalObjectProject project : gateway.getV8ProjectManager().getProjects(IExternalObjectProject.class)) {
            if (project == null || project.getProject() == null) {
                continue;
            }
            if (!project.getProject().getName().equalsIgnoreCase(externalProjectName)) {
                continue;
            }
            return belongsToBaseProject(project, baseProjectName);
        }
        return false;
    }

    private boolean belongsToBaseProject(IExternalObjectProject externalProject, String baseProjectName) {
        if (externalProject == null || externalProject.getProject() == null || baseProjectName == null) {
            return false;
        }
        String normalizedBase = baseProjectName.trim();
        IProject parentProject = externalProject.getParentProject();
        if (parentProject != null && parentProject.exists()
                && parentProject.getName().equalsIgnoreCase(normalizedBase)) {
            return true;
        }
        try {
            IV8Project parentV8 = gateway.getExternalObjectProjectManager().getParentProject(externalProject.getProject());
            if (parentV8 != null && parentV8.getProject() != null
                    && parentV8.getProject().getName().equalsIgnoreCase(normalizedBase)) {
                return true;
            }
        } catch (RuntimeException e) {
            LOG.debug("Cannot resolve external parent via manager for project=%s: %s", //$NON-NLS-1$
                    externalProject.getProject().getName(), e.getMessage());
        }
        return false;
    }

    private void refreshProject(IProject project) {
        if (project == null || !project.exists()) {
            return;
        }
        try {
            project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
        } catch (CoreException e) {
            LOG.debug("refreshLocal failed for project=%s: %s", project.getName(), e.getMessage()); //$NON-NLS-1$
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private List<ExternalObjectSummary> collectObjectSummaries(
            IExternalObjectProject externalProject,
            IProject baseProject,
            String typeFilter,
            String nameFilter
    ) {
        List<ExternalObjectSummary> result = new ArrayList<>();
        Predicate<MdObject> filter = object -> {
            if (object == null) {
                return false;
            }
            if (typeFilter != null) {
                String kindToken = normalize(object.eClass().getName());
                if (!kindToken.contains(typeFilter)) {
                    return false;
                }
            }
            if (nameFilter != null) {
                String candidate = safe(object.getName()).toLowerCase(Locale.ROOT);
                if (!candidate.contains(nameFilter)) {
                    return false;
                }
            }
            return true;
        };

        for (MdObject object : safeExternalObjects(externalProject, filter)) {
            if (object == null) {
                continue;
            }
            String kind = object.eClass().getName();
            String name = safe(object.getName());
            result.add(new ExternalObjectSummary(
                    baseProject.getName(),
                    externalProject.getProject().getName(),
                    syntheticFqn(object),
                    name,
                    kind,
                    resolveSynonym(object.getSynonym()),
                    object.getObjectBelonging() != null ? object.getObjectBelonging().name() : "")); //$NON-NLS-1$
        }
        return result;
    }

    private ExternalObjectDetailsResult buildDetails(
            IProject baseProject,
            IExternalObjectProject externalProject,
            MdObject object,
            String syntheticFqn
    ) {
        int attributesCount = sizeOfFeatureList(object, "attributes"); //$NON-NLS-1$
        int tabularSectionsCount = sizeOfFeatureList(object, "tabularSections"); //$NON-NLS-1$
        int formsCount = sizeOfFeatureList(object, "forms"); //$NON-NLS-1$
        int templatesCount = sizeOfFeatureList(object, "templates"); //$NON-NLS-1$
        int containedObjectsCount = sizeOfFeatureList(object, "containedObjects"); //$NON-NLS-1$

        boolean hasObjectModule = hasFeatureValue(object, "objectModule"); //$NON-NLS-1$
        boolean hasDefaultForm = hasFeatureValue(object, "defaultForm"); //$NON-NLS-1$
        boolean hasAuxiliaryForm = hasFeatureValue(object, "auxiliaryForm"); //$NON-NLS-1$

        List<String> containedKinds = containedKinds(object, 20);

        return new ExternalObjectDetailsResult(
                baseProject.getName(),
                externalProject.getProject().getName(),
                syntheticFqn,
                safe(object.getName()),
                object.eClass().getName(),
                resolveSynonym(object.getSynonym()),
                object.getObjectBelonging() != null ? object.getObjectBelonging().name() : "", //$NON-NLS-1$
                attributesCount,
                tabularSectionsCount,
                formsCount,
                templatesCount,
                containedObjectsCount,
                hasObjectModule,
                hasDefaultForm,
                hasAuxiliaryForm,
                containedKinds);
    }

    private Collection<MdObject> safeExternalObjects(IExternalObjectProject project) {
        return safeExternalObjects(project, obj -> true);
    }

    private Collection<MdObject> safeExternalObjects(IExternalObjectProject project, Predicate<MdObject> filter) {
        try {
            Collection<MdObject> objects = project.getExternalObjects(filter);
            return objects == null ? List.of() : objects;
        } catch (RuntimeException e) {
            throw new MetadataOperationException(
                    MetadataOperationCode.EXTERNAL_OBJECT_API_UNAVAILABLE,
                    "Failed to read external objects from project " + project.getProject().getName() + ": " + e.getMessage(),
                    false,
                    e); //$NON-NLS-1$
        }
    }

    private boolean matchesRequestedObject(String rawRef, String normalizedRef, String syntheticFqn, String name) {
        if (normalize(syntheticFqn).equals(normalizedRef)) {
            return true;
        }
        if (name != null && normalize(name).equals(normalizedRef)) {
            return true;
        }
        return normalize(rawRef).equals(normalize(syntheticFqn));
    }

    private void setCommonProperties(MdObject object, String name, String synonym, String comment) {
        if (object.getUuid() == null) {
            object.setUuid(UUID.randomUUID());
        }
        object.setName(name);
        if (comment != null && !comment.isBlank()) {
            object.setComment(comment);
        }
        if (synonym != null && !synonym.isBlank() && object.getSynonym() != null) {
            object.getSynonym().put(RU_LANGUAGE, synonym);
        }
    }

    private String syntheticFqn(MdObject object) {
        String kind = object.eClass().getName();
        String name = safe(object.getName());
        return kind + "." + name; //$NON-NLS-1$
    }

    private int sizeOfFeatureList(EObject object, String featureName) {
        EStructuralFeature feature = object.eClass().getEStructuralFeature(featureName);
        if (feature == null) {
            return 0;
        }
        Object value = object.eGet(feature);
        if (value instanceof List<?> list) {
            return list.size();
        }
        return value == null ? 0 : 1;
    }

    private boolean hasFeatureValue(EObject object, String featureName) {
        EStructuralFeature feature = object.eClass().getEStructuralFeature(featureName);
        if (feature == null) {
            return false;
        }
        Object value = object.eGet(feature);
        if (value instanceof List<?> list) {
            return !list.isEmpty();
        }
        return value != null;
    }

    private List<String> containedKinds(EObject object, int limit) {
        EStructuralFeature feature = object.eClass().getEStructuralFeature("containedObjects"); //$NON-NLS-1$
        if (feature == null) {
            return List.of();
        }
        Object value = object.eGet(feature);
        if (!(value instanceof List<?> list)) {
            return List.of();
        }

        Map<String, Integer> counters = new LinkedHashMap<>();
        for (Object item : list) {
            if (!(item instanceof EObject eObject)) {
                continue;
            }
            String kind = eObject.eClass().getName();
            counters.put(kind, Integer.valueOf(counters.getOrDefault(kind, Integer.valueOf(0)).intValue() + 1));
        }

        List<String> result = new ArrayList<>();
        counters.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER))
                .limit(limit)
                .forEach(entry -> result.add(entry.getKey() + ":" + entry.getValue())); //$NON-NLS-1$
        return result;
    }

    private String resolveSynonym(EMap<String, String> map) {
        if (map == null || map.isEmpty()) {
            return ""; //$NON-NLS-1$
        }
        String ru = map.get(RU_LANGUAGE);
        if (ru != null && !ru.isBlank()) {
            return ru;
        }
        for (String value : map.values()) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return ""; //$NON-NLS-1$
    }

    private String safe(String value) {
        return value == null ? "" : value; //$NON-NLS-1$
    }

    private String normalize(String value) {
        if (value == null) {
            return ""; //$NON-NLS-1$
        }
        String lowered = value.trim().toLowerCase(Locale.ROOT).replace('ё', 'е');
        StringBuilder sb = new StringBuilder(lowered.length());
        for (int i = 0; i < lowered.length(); i++) {
            char ch = lowered.charAt(i);
            if (ch == '_' || ch == '-' || ch == '.' || Character.isWhitespace(ch)) {
                continue;
            }
            sb.append(ch);
        }
        return sb.toString();
    }
}
