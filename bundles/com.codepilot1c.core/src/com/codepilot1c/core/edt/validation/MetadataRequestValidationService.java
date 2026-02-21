package com.codepilot1c.core.edt.validation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;

import com.codepilot1c.core.edt.forms.CreateFormRequest;
import com.codepilot1c.core.edt.forms.FormUsage;
import com.codepilot1c.core.edt.forms.UpdateFormModelRequest;
import com.codepilot1c.core.edt.external.ExternalCreateProcessingRequest;
import com.codepilot1c.core.edt.external.ExternalCreateReportRequest;
import com.codepilot1c.core.edt.dcs.DcsCreateMainSchemaRequest;
import com.codepilot1c.core.edt.dcs.DcsUpsertCalculatedFieldRequest;
import com.codepilot1c.core.edt.dcs.DcsUpsertParameterRequest;
import com.codepilot1c.core.edt.dcs.DcsUpsertQueryDatasetRequest;
import com.codepilot1c.core.edt.metadata.AddMetadataChildRequest;
import com.codepilot1c.core.edt.metadata.CreateMetadataRequest;
import com.codepilot1c.core.edt.metadata.DeleteMetadataRequest;
import com.codepilot1c.core.edt.metadata.EdtMetadataGateway;
import com.codepilot1c.core.edt.metadata.MetadataChildKind;
import com.codepilot1c.core.edt.metadata.MetadataKind;
import com.codepilot1c.core.edt.metadata.MetadataOperationCode;
import com.codepilot1c.core.edt.metadata.MetadataOperationException;
import com.codepilot1c.core.edt.metadata.MetadataProjectReadinessChecker;
import com.codepilot1c.core.edt.metadata.UpdateMetadataRequest;
import com.codepilot1c.core.logging.LogSanitizer;
import com.codepilot1c.core.logging.VibeLogger;

/**
 * Pre-validates metadata mutation requests and issues one-time validation tokens.
 */
public class MetadataRequestValidationService {

    private static final VibeLogger.CategoryLogger LOG = VibeLogger.forClass(MetadataRequestValidationService.class);

    private final EdtMetadataGateway gateway;
    private final MetadataProjectReadinessChecker readinessChecker;
    private final ValidationTokenStore tokenStore;

    public MetadataRequestValidationService() {
        this(new EdtMetadataGateway(), ValidationTokenStore.getInstance());
    }

    MetadataRequestValidationService(EdtMetadataGateway gateway, ValidationTokenStore tokenStore) {
        this.gateway = gateway;
        this.tokenStore = tokenStore;
        this.readinessChecker = new MetadataProjectReadinessChecker(gateway);
    }

    public boolean isEdtAvailable() {
        return gateway.isEdtAvailable();
    }

    public ValidationResult validateAndIssueToken(ValidationRequest request) {
        String opId = LogSanitizer.newId("validate-md"); //$NON-NLS-1$
        long startedAt = System.currentTimeMillis();
        LOG.info("[%s] validateAndIssueToken START operation=%s project=%s", // $NON-NLS-1$
                opId, request.operation(), request.projectName());
        LOG.debug("[%s] Raw validation payload: %s", opId, // $NON-NLS-1$
                LogSanitizer.truncate(LogSanitizer.redactSecrets(String.valueOf(request.payload())), 4000));
        request.validate();
        ensureRuntimeReady(request.projectName());

        List<String> checks = new ArrayList<>();
        Map<String, Object> normalizedPayload = normalizePayload(request, checks);
        LOG.debug("[%s] Normalized validation payload: %s", opId, // $NON-NLS-1$
                LogSanitizer.truncate(LogSanitizer.redactSecrets(String.valueOf(normalizedPayload)), 4000));
        ValidationTokenStore.TokenIssue tokenIssue = tokenStore.issueToken(
                request.operation(), request.projectName(), normalizedPayload);
        LOG.info("[%s] Token issued in %s, expiresAt=%d", opId, // $NON-NLS-1$
                LogSanitizer.formatDuration(System.currentTimeMillis() - startedAt),
                tokenIssue.expiresAtEpochMs());

        return new ValidationResult(
                true,
                request.projectName(),
                request.operation().getToolName(),
                checks,
                normalizedPayload,
                tokenIssue.token(),
                tokenIssue.expiresAtEpochMs());
    }

    public Map<String, Object> consumeToken(
            String token,
            ValidationOperation operation,
            String projectName
    ) {
        String opId = LogSanitizer.newId("consume-md"); //$NON-NLS-1$
        long startedAt = System.currentTimeMillis();
        LOG.info("[%s] consumeToken START operation=%s project=%s token=%s", // $NON-NLS-1$
                opId, operation, projectName, LogSanitizer.truncate(token, 80));
        ensureRuntimeReady(projectName);
        Map<String, Object> validatedPayload = tokenStore.consumeToken(token, operation, projectName);
        LOG.debug("[%s] consumeToken validated payload: %s", opId, // $NON-NLS-1$
                LogSanitizer.truncate(LogSanitizer.redactSecrets(String.valueOf(validatedPayload)), 4000));
        LOG.info("[%s] consumeToken SUCCESS in %s", opId, // $NON-NLS-1$
                LogSanitizer.formatDuration(System.currentTimeMillis() - startedAt));
        return validatedPayload;
    }

    public Map<String, Object> normalizeCreatePayload(
            String projectName,
            String kindValue,
            String name,
            String synonym,
            String comment,
            Map<String, Object> properties
    ) {
        MetadataKind kind = MetadataKind.fromString(kindValue);
        CreateMetadataRequest request = new CreateMetadataRequest(projectName, kind, name, synonym, comment, properties);
        request.validate();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("project", projectName); //$NON-NLS-1$
        payload.put("kind", kind.name()); //$NON-NLS-1$
        payload.put("name", name); //$NON-NLS-1$
        if (synonym != null && !synonym.isBlank()) {
            payload.put("synonym", synonym); //$NON-NLS-1$
        }
        if (comment != null && !comment.isBlank()) {
            payload.put("comment", comment); //$NON-NLS-1$
        }
        if (properties != null && !properties.isEmpty()) {
            payload.put("properties", properties); //$NON-NLS-1$
        }
        return payload;
    }

    public Map<String, Object> normalizeAddChildPayload(
            String projectName,
            String parentFqn,
            String childKindValue,
            String name,
            String synonym,
            String comment,
            Map<String, Object> properties
    ) {
        MetadataChildKind kind = MetadataChildKind.fromString(childKindValue);
        AddMetadataChildRequest request = new AddMetadataChildRequest(
                projectName, parentFqn, kind, name, synonym, comment, properties);
        request.validate();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("project", projectName); //$NON-NLS-1$
        payload.put("parent_fqn", parentFqn); //$NON-NLS-1$
        payload.put("child_kind", kind.name()); //$NON-NLS-1$
        payload.put("name", name); //$NON-NLS-1$
        if (synonym != null && !synonym.isBlank()) {
            payload.put("synonym", synonym); //$NON-NLS-1$
        }
        if (comment != null && !comment.isBlank()) {
            payload.put("comment", comment); //$NON-NLS-1$
        }
        if (properties != null && !properties.isEmpty()) {
            payload.put("properties", properties); //$NON-NLS-1$
        }
        return payload;
    }

    public Map<String, Object> normalizeCreateFormPayload(
            String projectName,
            String ownerFqn,
            String name,
            String usageValue,
            Boolean managed,
            Boolean setAsDefault,
            String synonym,
            String comment,
            Long waitMs
    ) {
        FormUsage usage = FormUsage.fromOptionalString(usageValue);
        CreateFormRequest request = new CreateFormRequest(
                projectName,
                ownerFqn,
                name,
                usage,
                managed,
                setAsDefault,
                synonym,
                comment,
                waitMs);
        request.validate();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("project", projectName); //$NON-NLS-1$
        payload.put("owner_fqn", ownerFqn); //$NON-NLS-1$
        payload.put("name", name); //$NON-NLS-1$
        if (usage != null) {
            payload.put("usage", usage.name()); //$NON-NLS-1$
        }
        if (managed != null) {
            payload.put("managed", managed); //$NON-NLS-1$
        }
        if (setAsDefault != null) {
            payload.put("set_as_default", setAsDefault); //$NON-NLS-1$
        }
        if (synonym != null && !synonym.isBlank()) {
            payload.put("synonym", synonym); //$NON-NLS-1$
        }
        if (comment != null && !comment.isBlank()) {
            payload.put("comment", comment); //$NON-NLS-1$
        }
        if (waitMs != null) {
            payload.put("wait_ms", waitMs); //$NON-NLS-1$
        }
        return payload;
    }

    public Map<String, Object> normalizeExtensionCreateProjectPayload(
            String projectName,
            String extensionProject,
            String baseProject,
            String projectPath,
            String version,
            String configurationName,
            String purpose,
            String compatibilityMode
    ) {
        String effectiveBaseProject = baseProject == null || baseProject.isBlank() ? projectName : baseProject.trim();
        if (!projectName.equals(effectiveBaseProject)) {
            throw new MetadataOperationException(
                    MetadataOperationCode.KNOWLEDGE_REQUIRED,
                    "payload.base_project must match project", false); //$NON-NLS-1$
        }

        com.codepilot1c.core.edt.extension.ExtensionCreateProjectRequest request =
                new com.codepilot1c.core.edt.extension.ExtensionCreateProjectRequest(
                        effectiveBaseProject,
                        extensionProject,
                        projectPath,
                        version,
                        configurationName,
                        purpose,
                        compatibilityMode);
        request.validate();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("project", projectName); //$NON-NLS-1$
        payload.put("base_project", request.normalizedBaseProjectName()); //$NON-NLS-1$
        payload.put("extension_project", request.normalizedExtensionProjectName()); //$NON-NLS-1$
        if (projectPath != null && !projectPath.isBlank()) {
            payload.put("project_path", projectPath.trim()); //$NON-NLS-1$
        }
        if (version != null && !version.isBlank()) {
            payload.put("version", version.trim()); //$NON-NLS-1$
        }
        if (configurationName != null && !configurationName.isBlank()) {
            payload.put("configuration_name", configurationName.trim()); //$NON-NLS-1$
        }
        payload.put("purpose", request.effectivePurpose().name()); //$NON-NLS-1$
        if (request.effectiveCompatibilityMode() != null) {
            payload.put("compatibility_mode", request.effectiveCompatibilityMode().name()); //$NON-NLS-1$
        }
        return payload;
    }

    public Map<String, Object> normalizeExternalCreateReportPayload(
            String projectName,
            String externalProject,
            String name,
            String projectPath,
            String version,
            String synonym,
            String comment
    ) {
        ExternalCreateReportRequest request = new ExternalCreateReportRequest(
                projectName,
                externalProject,
                name,
                projectPath,
                version,
                synonym,
                comment);
        request.validate();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("project", request.normalizedProjectName()); //$NON-NLS-1$
        payload.put("external_project", request.normalizedExternalProjectName()); //$NON-NLS-1$
        payload.put("name", request.normalizedObjectName()); //$NON-NLS-1$
        if (projectPath != null && !projectPath.isBlank()) {
            payload.put("project_path", projectPath.trim()); //$NON-NLS-1$
        }
        if (version != null && !version.isBlank()) {
            payload.put("version", version.trim()); //$NON-NLS-1$
        }
        if (request.normalizedSynonym() != null) {
            payload.put("synonym", request.normalizedSynonym()); //$NON-NLS-1$
        }
        if (request.normalizedComment() != null) {
            payload.put("comment", request.normalizedComment()); //$NON-NLS-1$
        }
        return payload;
    }

    public Map<String, Object> normalizeExternalCreateProcessingPayload(
            String projectName,
            String externalProject,
            String name,
            String projectPath,
            String version,
            String synonym,
            String comment
    ) {
        ExternalCreateProcessingRequest request = new ExternalCreateProcessingRequest(
                projectName,
                externalProject,
                name,
                projectPath,
                version,
                synonym,
                comment);
        request.validate();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("project", request.normalizedProjectName()); //$NON-NLS-1$
        payload.put("external_project", request.normalizedExternalProjectName()); //$NON-NLS-1$
        payload.put("name", request.normalizedObjectName()); //$NON-NLS-1$
        if (projectPath != null && !projectPath.isBlank()) {
            payload.put("project_path", projectPath.trim()); //$NON-NLS-1$
        }
        if (version != null && !version.isBlank()) {
            payload.put("version", version.trim()); //$NON-NLS-1$
        }
        if (request.normalizedSynonym() != null) {
            payload.put("synonym", request.normalizedSynonym()); //$NON-NLS-1$
        }
        if (request.normalizedComment() != null) {
            payload.put("comment", request.normalizedComment()); //$NON-NLS-1$
        }
        return payload;
    }

    public Map<String, Object> normalizeExtensionAdoptPayload(
            String projectName,
            String extensionProject,
            String baseProject,
            String sourceObjectFqn,
            Boolean updateIfExists
    ) {
        String effectiveBaseProject = baseProject == null || baseProject.isBlank() ? projectName : baseProject.trim();
        if (!projectName.equals(effectiveBaseProject)) {
            throw new MetadataOperationException(
                    MetadataOperationCode.KNOWLEDGE_REQUIRED,
                    "payload.base_project must match project", false); //$NON-NLS-1$
        }
        com.codepilot1c.core.edt.extension.ExtensionAdoptObjectRequest request =
                new com.codepilot1c.core.edt.extension.ExtensionAdoptObjectRequest(
                        effectiveBaseProject,
                        extensionProject,
                        sourceObjectFqn,
                        updateIfExists);
        request.validate();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("project", projectName); //$NON-NLS-1$
        payload.put("base_project", effectiveBaseProject); //$NON-NLS-1$
        payload.put("extension_project", request.normalizedExtensionProjectName()); //$NON-NLS-1$
        payload.put("source_object_fqn", request.normalizedSourceObjectFqn()); //$NON-NLS-1$
        payload.put("update_if_exists", Boolean.valueOf(request.shouldUpdateIfExists())); //$NON-NLS-1$
        return payload;
    }

    public Map<String, Object> normalizeExtensionSetPropertyStatePayload(
            String projectName,
            String extensionProject,
            String baseProject,
            String sourceObjectFqn,
            String propertyName,
            String state
    ) {
        String effectiveBaseProject = baseProject == null || baseProject.isBlank() ? projectName : baseProject.trim();
        if (!projectName.equals(effectiveBaseProject)) {
            throw new MetadataOperationException(
                    MetadataOperationCode.KNOWLEDGE_REQUIRED,
                    "payload.base_project must match project", false); //$NON-NLS-1$
        }

        com.codepilot1c.core.edt.extension.ExtensionSetPropertyStateRequest request =
                new com.codepilot1c.core.edt.extension.ExtensionSetPropertyStateRequest(
                        extensionProject,
                        effectiveBaseProject,
                        sourceObjectFqn,
                        propertyName,
                        state);
        request.validate();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("project", projectName); //$NON-NLS-1$
        payload.put("base_project", effectiveBaseProject); //$NON-NLS-1$
        payload.put("extension_project", request.normalizedExtensionProjectName()); //$NON-NLS-1$
        payload.put("source_object_fqn", request.normalizedSourceObjectFqn()); //$NON-NLS-1$
        payload.put("property_name", request.normalizedPropertyName()); //$NON-NLS-1$
        payload.put("state", request.parseState().name()); //$NON-NLS-1$
        return payload;
    }

    public Map<String, Object> normalizeDcsCreateMainSchemaPayload(
            String projectName,
            String ownerFqn,
            String templateName,
            Boolean forceReplace
    ) {
        DcsCreateMainSchemaRequest request = new DcsCreateMainSchemaRequest(
                projectName,
                ownerFqn,
                templateName,
                forceReplace);
        request.validate();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("project", request.normalizedProjectName()); //$NON-NLS-1$
        payload.put("owner_fqn", request.normalizedOwnerFqn()); //$NON-NLS-1$
        payload.put("template_name", request.effectiveTemplateName()); //$NON-NLS-1$
        payload.put("force_replace", Boolean.valueOf(request.shouldForceReplace())); //$NON-NLS-1$
        return payload;
    }

    public Map<String, Object> normalizeDcsUpsertQueryDatasetPayload(
            String projectName,
            String ownerFqn,
            String datasetName,
            String query,
            String dataSource,
            Boolean autoFillAvailableFields,
            Boolean useQueryGroupIfPossible
    ) {
        DcsUpsertQueryDatasetRequest request = new DcsUpsertQueryDatasetRequest(
                projectName,
                ownerFqn,
                datasetName,
                query,
                dataSource,
                autoFillAvailableFields,
                useQueryGroupIfPossible);
        request.validate();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("project", request.normalizedProjectName()); //$NON-NLS-1$
        payload.put("owner_fqn", request.normalizedOwnerFqn()); //$NON-NLS-1$
        payload.put("dataset_name", request.normalizedDatasetName()); //$NON-NLS-1$
        if (request.normalizedQuery() != null) {
            payload.put("query", request.normalizedQuery()); //$NON-NLS-1$
        }
        if (request.normalizedDataSource() != null) {
            payload.put("data_source", request.normalizedDataSource()); //$NON-NLS-1$
        }
        if (request.autoFillAvailableFields() != null) {
            payload.put("auto_fill_available_fields", request.autoFillAvailableFields()); //$NON-NLS-1$
        }
        if (request.useQueryGroupIfPossible() != null) {
            payload.put("use_query_group_if_possible", request.useQueryGroupIfPossible()); //$NON-NLS-1$
        }
        return payload;
    }

    public Map<String, Object> normalizeDcsUpsertParameterPayload(
            String projectName,
            String ownerFqn,
            String parameterName,
            String expression,
            Boolean availableAsField,
            Boolean valueListAllowed,
            Boolean denyIncompleteValues,
            Boolean useRestriction
    ) {
        DcsUpsertParameterRequest request = new DcsUpsertParameterRequest(
                projectName,
                ownerFqn,
                parameterName,
                expression,
                availableAsField,
                valueListAllowed,
                denyIncompleteValues,
                useRestriction);
        request.validate();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("project", request.normalizedProjectName()); //$NON-NLS-1$
        payload.put("owner_fqn", request.normalizedOwnerFqn()); //$NON-NLS-1$
        payload.put("parameter_name", request.normalizedParameterName()); //$NON-NLS-1$
        if (request.normalizedExpression() != null) {
            payload.put("expression", request.normalizedExpression()); //$NON-NLS-1$
        }
        if (request.availableAsField() != null) {
            payload.put("available_as_field", request.availableAsField()); //$NON-NLS-1$
        }
        if (request.valueListAllowed() != null) {
            payload.put("value_list_allowed", request.valueListAllowed()); //$NON-NLS-1$
        }
        if (request.denyIncompleteValues() != null) {
            payload.put("deny_incomplete_values", request.denyIncompleteValues()); //$NON-NLS-1$
        }
        if (request.useRestriction() != null) {
            payload.put("use_restriction", request.useRestriction()); //$NON-NLS-1$
        }
        return payload;
    }

    public Map<String, Object> normalizeDcsUpsertCalculatedFieldPayload(
            String projectName,
            String ownerFqn,
            String dataPath,
            String expression,
            String presentationExpression
    ) {
        DcsUpsertCalculatedFieldRequest request = new DcsUpsertCalculatedFieldRequest(
                projectName,
                ownerFqn,
                dataPath,
                expression,
                presentationExpression);
        request.validate();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("project", request.normalizedProjectName()); //$NON-NLS-1$
        payload.put("owner_fqn", request.normalizedOwnerFqn()); //$NON-NLS-1$
        payload.put("data_path", request.normalizedDataPath()); //$NON-NLS-1$
        if (request.normalizedExpression() != null) {
            payload.put("expression", request.normalizedExpression()); //$NON-NLS-1$
        }
        if (request.normalizedPresentationExpression() != null) {
            payload.put("presentation_expression", request.normalizedPresentationExpression()); //$NON-NLS-1$
        }
        return payload;
    }

    public Map<String, Object> normalizeUpdatePayload(
            String projectName,
            String targetFqn,
            Map<String, Object> changes
    ) {
        UpdateMetadataRequest request = new UpdateMetadataRequest(projectName, targetFqn, changes);
        request.validate();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("project", projectName); //$NON-NLS-1$
        payload.put("target_fqn", targetFqn); //$NON-NLS-1$
        if (changes != null && !changes.isEmpty()) {
            payload.put("changes", new LinkedHashMap<>(changes)); //$NON-NLS-1$
        }
        return payload;
    }

    public Map<String, Object> normalizeDeletePayload(
            String projectName,
            String targetFqn,
            boolean recursive
    ) {
        return normalizeDeletePayload(projectName, targetFqn, recursive, false);
    }

    public Map<String, Object> normalizeDeletePayload(
            String projectName,
            String targetFqn,
            boolean recursive,
            boolean force
    ) {
        DeleteMetadataRequest request = new DeleteMetadataRequest(projectName, targetFqn, recursive, force);
        request.validate();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("project", projectName); //$NON-NLS-1$
        payload.put("target_fqn", targetFqn); //$NON-NLS-1$
        payload.put("recursive", Boolean.valueOf(recursive)); //$NON-NLS-1$
        payload.put("force", Boolean.valueOf(force)); //$NON-NLS-1$
        return payload;
    }

    public Map<String, Object> normalizeUpdateFormModelPayload(
            String projectName,
            String formFqn,
            List<Map<String, Object>> operations
    ) {
        UpdateFormModelRequest request = new UpdateFormModelRequest(projectName, formFqn, operations);
        request.validate();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("project", projectName); //$NON-NLS-1$
        payload.put("form_fqn", formFqn); //$NON-NLS-1$
        payload.put("operations", operations == null ? List.of() : new ArrayList<>(operations)); //$NON-NLS-1$
        return payload;
    }

    private Map<String, Object> normalizePayload(ValidationRequest request, List<String> checks) {
        return switch (request.operation()) {
            case CREATE_METADATA -> {
                Map<String, Object> payload = normalizeCreatePayload(
                        coalesceProject(request.projectName(), request.payload()),
                        asString(request.payload().get("kind")), //$NON-NLS-1$
                        asString(request.payload().get("name")), //$NON-NLS-1$
                        asOptionalString(request.payload().get("synonym")), //$NON-NLS-1$
                        asOptionalString(request.payload().get("comment")), //$NON-NLS-1$
                        asMap(request.payload().get("properties"))); //$NON-NLS-1$
                checks.add("Операция create_metadata валидирована по обязательным полям и имени."); //$NON-NLS-1$
                yield payload;
            }
            case CREATE_FORM -> {
                Map<String, Object> payload = normalizeCreateFormPayload(
                        coalesceProject(request.projectName(), request.payload()),
                        asString(request.payload().get("owner_fqn")), //$NON-NLS-1$
                        asString(request.payload().get("name")), //$NON-NLS-1$
                        asOptionalString(request.payload().get("usage")), //$NON-NLS-1$
                        asOptionalBoolean(request.payload().get("managed")), //$NON-NLS-1$
                        asOptionalBoolean(request.payload().get("set_as_default")), //$NON-NLS-1$
                        asOptionalString(request.payload().get("synonym")), //$NON-NLS-1$
                        asOptionalString(request.payload().get("comment")), //$NON-NLS-1$
                        asOptionalLong(request.payload().get("wait_ms"))); //$NON-NLS-1$
                checks.add("Операция create_form валидирована по обязательным полям и имени."); //$NON-NLS-1$
                yield payload;
            }
            case EXTERNAL_CREATE_REPORT -> {
                Map<String, Object> payload = normalizeExternalCreateReportPayload(
                        coalesceProject(request.projectName(), request.payload()),
                        asString(request.payload().get("external_project")), //$NON-NLS-1$
                        asString(request.payload().get("name")), //$NON-NLS-1$
                        asOptionalString(request.payload().get("project_path")), //$NON-NLS-1$
                        asOptionalString(request.payload().get("version")), //$NON-NLS-1$
                        asOptionalString(request.payload().get("synonym")), //$NON-NLS-1$
                        asOptionalString(request.payload().get("comment"))); //$NON-NLS-1$
                checks.add("Операция external_create_report валидирована по обязательным полям."); //$NON-NLS-1$
                yield payload;
            }
            case EXTERNAL_CREATE_PROCESSING -> {
                Map<String, Object> payload = normalizeExternalCreateProcessingPayload(
                        coalesceProject(request.projectName(), request.payload()),
                        asString(request.payload().get("external_project")), //$NON-NLS-1$
                        asString(request.payload().get("name")), //$NON-NLS-1$
                        asOptionalString(request.payload().get("project_path")), //$NON-NLS-1$
                        asOptionalString(request.payload().get("version")), //$NON-NLS-1$
                        asOptionalString(request.payload().get("synonym")), //$NON-NLS-1$
                        asOptionalString(request.payload().get("comment"))); //$NON-NLS-1$
                checks.add("Операция external_create_processing валидирована по обязательным полям."); //$NON-NLS-1$
                yield payload;
            }
            case EXTENSION_CREATE_PROJECT -> {
                Map<String, Object> payload = normalizeExtensionCreateProjectPayload(
                        coalesceProject(request.projectName(), request.payload()),
                        asString(request.payload().get("extension_project")), //$NON-NLS-1$
                        asOptionalString(request.payload().get("base_project")), //$NON-NLS-1$
                        asOptionalString(request.payload().get("project_path")), //$NON-NLS-1$
                        asOptionalString(request.payload().get("version")), //$NON-NLS-1$
                        asOptionalString(request.payload().get("configuration_name")), //$NON-NLS-1$
                        asOptionalString(request.payload().get("purpose")), //$NON-NLS-1$
                        asOptionalString(request.payload().get("compatibility_mode"))); //$NON-NLS-1$
                checks.add("Операция extension_create_project валидирована по обязательным полям."); //$NON-NLS-1$
                yield payload;
            }
            case EXTENSION_ADOPT_OBJECT -> {
                Map<String, Object> payload = normalizeExtensionAdoptPayload(
                        coalesceProject(request.projectName(), request.payload()),
                        asString(request.payload().get("extension_project")), //$NON-NLS-1$
                        asOptionalString(request.payload().get("base_project")), //$NON-NLS-1$
                        asString(request.payload().get("source_object_fqn")), //$NON-NLS-1$
                        asOptionalBoolean(request.payload().get("update_if_exists"))); //$NON-NLS-1$
                checks.add("Операция extension_adopt_object валидирована по обязательным полям."); //$NON-NLS-1$
                yield payload;
            }
            case EXTENSION_SET_PROPERTY_STATE -> {
                Map<String, Object> payload = normalizeExtensionSetPropertyStatePayload(
                        coalesceProject(request.projectName(), request.payload()),
                        asString(request.payload().get("extension_project")), //$NON-NLS-1$
                        asOptionalString(request.payload().get("base_project")), //$NON-NLS-1$
                        asString(request.payload().get("source_object_fqn")), //$NON-NLS-1$
                        asString(request.payload().get("property_name")), //$NON-NLS-1$
                        asString(request.payload().get("state"))); //$NON-NLS-1$
                checks.add("Операция extension_set_property_state валидирована по обязательным полям."); //$NON-NLS-1$
                yield payload;
            }
            case DCS_CREATE_MAIN_SCHEMA -> {
                Map<String, Object> payload = normalizeDcsCreateMainSchemaPayload(
                        coalesceProject(request.projectName(), request.payload()),
                        asString(request.payload().get("owner_fqn")), //$NON-NLS-1$
                        asOptionalString(request.payload().get("template_name")), //$NON-NLS-1$
                        asOptionalBoolean(request.payload().get("force_replace"))); //$NON-NLS-1$
                checks.add("Операция dcs_create_main_schema валидирована по обязательным полям."); //$NON-NLS-1$
                yield payload;
            }
            case DCS_UPSERT_QUERY_DATASET -> {
                Map<String, Object> payload = normalizeDcsUpsertQueryDatasetPayload(
                        coalesceProject(request.projectName(), request.payload()),
                        asString(request.payload().get("owner_fqn")), //$NON-NLS-1$
                        asString(request.payload().get("dataset_name")), //$NON-NLS-1$
                        asOptionalString(request.payload().get("query")), //$NON-NLS-1$
                        asOptionalString(request.payload().get("data_source")), //$NON-NLS-1$
                        asOptionalBoolean(request.payload().get("auto_fill_available_fields")), //$NON-NLS-1$
                        asOptionalBoolean(request.payload().get("use_query_group_if_possible"))); //$NON-NLS-1$
                checks.add("Операция dcs_upsert_query_dataset валидирована по обязательным полям."); //$NON-NLS-1$
                yield payload;
            }
            case DCS_UPSERT_PARAMETER -> {
                Map<String, Object> payload = normalizeDcsUpsertParameterPayload(
                        coalesceProject(request.projectName(), request.payload()),
                        asString(request.payload().get("owner_fqn")), //$NON-NLS-1$
                        asString(request.payload().get("parameter_name")), //$NON-NLS-1$
                        asOptionalString(request.payload().get("expression")), //$NON-NLS-1$
                        asOptionalBoolean(request.payload().get("available_as_field")), //$NON-NLS-1$
                        asOptionalBoolean(request.payload().get("value_list_allowed")), //$NON-NLS-1$
                        asOptionalBoolean(request.payload().get("deny_incomplete_values")), //$NON-NLS-1$
                        asOptionalBoolean(request.payload().get("use_restriction"))); //$NON-NLS-1$
                checks.add("Операция dcs_upsert_parameter валидирована по обязательным полям."); //$NON-NLS-1$
                yield payload;
            }
            case DCS_UPSERT_CALCULATED_FIELD -> {
                Map<String, Object> payload = normalizeDcsUpsertCalculatedFieldPayload(
                        coalesceProject(request.projectName(), request.payload()),
                        asString(request.payload().get("owner_fqn")), //$NON-NLS-1$
                        asString(request.payload().get("data_path")), //$NON-NLS-1$
                        asOptionalString(request.payload().get("expression")), //$NON-NLS-1$
                        asOptionalString(request.payload().get("presentation_expression"))); //$NON-NLS-1$
                checks.add("Операция dcs_upsert_calculated_field валидирована по обязательным полям."); //$NON-NLS-1$
                yield payload;
            }
            case ADD_METADATA_CHILD -> {
                Map<String, Object> payload = normalizeAddChildPayload(
                        coalesceProject(request.projectName(), request.payload()),
                        asString(request.payload().get("parent_fqn")), //$NON-NLS-1$
                        asString(request.payload().get("child_kind")), //$NON-NLS-1$
                        asString(request.payload().get("name")), //$NON-NLS-1$
                        asOptionalString(request.payload().get("synonym")), //$NON-NLS-1$
                        asOptionalString(request.payload().get("comment")), //$NON-NLS-1$
                        asMap(request.payload().get("properties"))); //$NON-NLS-1$
                checks.add("Операция add_metadata_child валидирована по обязательным полям и имени."); //$NON-NLS-1$
                yield payload;
            }
            case UPDATE_METADATA -> {
                Map<String, Object> payload = normalizeUpdatePayload(
                        coalesceProject(request.projectName(), request.payload()),
                        asString(request.payload().get("target_fqn")), //$NON-NLS-1$
                        asMap(request.payload().get("changes"))); //$NON-NLS-1$
                checks.add("Операция update_metadata валидирована по обязательным полям."); //$NON-NLS-1$
                yield payload;
            }
            case DELETE_METADATA -> {
                Object recursiveObj = request.payload().get("recursive"); //$NON-NLS-1$
                boolean recursive = recursiveObj instanceof Boolean b ? b.booleanValue() : Boolean.parseBoolean(String.valueOf(recursiveObj));
                Object forceObj = request.payload().get("force"); //$NON-NLS-1$
                boolean force = forceObj instanceof Boolean b ? b.booleanValue() : Boolean.parseBoolean(String.valueOf(forceObj));
                Map<String, Object> payload = normalizeDeletePayload(
                        coalesceProject(request.projectName(), request.payload()),
                        asString(request.payload().get("target_fqn")), //$NON-NLS-1$
                        recursive,
                        force);
                checks.add("Операция delete_metadata валидирована по обязательным полям."); //$NON-NLS-1$
                yield payload;
            }
            case MUTATE_FORM_MODEL -> {
                Map<String, Object> payload = normalizeUpdateFormModelPayload(
                        coalesceProject(request.projectName(), request.payload()),
                        asString(request.payload().get("form_fqn")), //$NON-NLS-1$
                        asListOfMaps(request.payload().get("operations"))); //$NON-NLS-1$
                checks.add("Операция mutate_form_model валидирована по обязательным полям."); //$NON-NLS-1$
                yield payload;
            }
        };
    }

    private void ensureRuntimeReady(String projectName) {
        LOG.debug("ensureRuntimeReady(project=%s): checking EDT availability", projectName); //$NON-NLS-1$
        gateway.ensureValidationRuntimeAvailable();

        IProject project = gateway.resolveProject(projectName);
        LOG.debug("ensureRuntimeReady(project=%s): resolved project=%s", projectName, //$NON-NLS-1$
                project != null ? project.getName() : "null"); //$NON-NLS-1$
        if (project == null || !project.exists()) {
            throw new MetadataOperationException(
                    MetadataOperationCode.PROJECT_NOT_FOUND,
                    "Project not found: " + projectName, false); //$NON-NLS-1$
        }
        LOG.debug("ensureRuntimeReady(project=%s): checking derived-data readiness", projectName); //$NON-NLS-1$
        readinessChecker.ensureReady(project);
        LOG.debug("ensureRuntimeReady(project=%s): READY", projectName); //$NON-NLS-1$
    }

    private String coalesceProject(String topLevelProject, Map<String, Object> payload) {
        String payloadProject = asOptionalString(payload.get("project")); //$NON-NLS-1$
        if (payloadProject != null && !payloadProject.equals(topLevelProject)) {
            throw new MetadataOperationException(
                    MetadataOperationCode.KNOWLEDGE_REQUIRED,
                    "payload.project must match project", false); //$NON-NLS-1$
        }
        return topLevelProject;
    }

    private String asString(Object value) {
        String str = value == null ? null : String.valueOf(value);
        if (str == null || str.isBlank()) {
            return null;
        }
        return str;
    }

    private String asOptionalString(Object value) {
        String str = asString(value);
        return str == null || str.isBlank() ? null : str;
    }

    private Boolean asOptionalBoolean(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        String text = String.valueOf(value).trim();
        if (text.isBlank()) {
            return null;
        }
        if ("1".equals(text)) { //$NON-NLS-1$
            return Boolean.TRUE;
        }
        if ("0".equals(text)) { //$NON-NLS-1$
            return Boolean.FALSE;
        }
        return Boolean.valueOf(Boolean.parseBoolean(text));
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> asListOfMaps(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                result.add((Map<String, Object>) map);
            }
        }
        return result;
    }

    private Long asOptionalLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return Long.valueOf(number.longValue());
        }
        String text = String.valueOf(value).trim();
        if (text.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(text);
        } catch (NumberFormatException e) {
            throw new MetadataOperationException(
                    MetadataOperationCode.INVALID_PROPERTY_VALUE,
                    "wait_ms must be numeric: " + value, false); //$NON-NLS-1$
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }
}
