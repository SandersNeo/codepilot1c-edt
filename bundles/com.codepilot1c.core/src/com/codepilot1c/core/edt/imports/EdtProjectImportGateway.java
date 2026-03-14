package com.codepilot1c.core.edt.imports;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;

import com._1c.g5.v8.dt.cli.api.workspace.IImportConfigurationFilesApi;
import com._1c.g5.v8.dt.core.platform.IBmModelManager;
import com._1c.g5.v8.dt.platform.version.Version;
import com.codepilot1c.core.edt.metadata.EdtMetadataGateway;
import com.codepilot1c.core.edt.runtime.EdtToolErrorCode;
import com.codepilot1c.core.edt.runtime.EdtToolException;
import com.codepilot1c.core.internal.VibeCorePlugin;
import com.e1c.g5.v8.dt.platform.standaloneserver.wst.core.IStandaloneServerService;

/**
 * Gateway for EDT project import services.
 */
public class EdtProjectImportGateway {

    private final EdtMetadataGateway metadataGateway;

    public EdtProjectImportGateway() {
        this(new EdtMetadataGateway());
    }

    EdtProjectImportGateway(EdtMetadataGateway metadataGateway) {
        this.metadataGateway = metadataGateway;
    }

    public IProject resolveProject(String projectName) {
        return metadataGateway.resolveProject(projectName);
    }

    public Version resolvePlatformVersion(IProject project) {
        return metadataGateway.resolvePlatformVersion(project);
    }

    public void waitForModelSynchronization(IProject project) {
        if (project == null) {
            return;
        }
        IBmModelManager modelManager = metadataGateway.getBmModelManager();
        modelManager.waitModelSynchronization(project);
    }

    public File getWorkspaceRoot() {
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace() == null ? null : ResourcesPlugin.getWorkspace().getRoot();
        return root == null || root.getLocation() == null ? null : root.getLocation().toFile();
    }

    public IImportConfigurationFilesApi getImportConfigurationFilesApi() {
        VibeCorePlugin plugin = requirePlugin();
        IImportConfigurationFilesApi service = plugin.getImportConfigurationFilesApi();
        if (service == null) {
            throw unavailable(EdtToolErrorCode.EDT_SERVICE_UNAVAILABLE, "IImportConfigurationFilesApi"); //$NON-NLS-1$
        }
        return service;
    }

    public IStandaloneServerService getStandaloneServerService() {
        VibeCorePlugin plugin = requirePlugin();
        IStandaloneServerService service = plugin.getStandaloneServerService();
        if (service == null) {
            throw unavailable(EdtToolErrorCode.STANDALONE_SERVER_UNAVAILABLE, "IStandaloneServerService"); //$NON-NLS-1$
        }
        return service;
    }

    private VibeCorePlugin requirePlugin() {
        VibeCorePlugin plugin = VibeCorePlugin.getDefault();
        if (plugin == null) {
            throw unavailable(EdtToolErrorCode.EDT_SERVICE_UNAVAILABLE, "VibeCorePlugin"); //$NON-NLS-1$
        }
        return plugin;
    }

    private EdtToolException unavailable(EdtToolErrorCode code, String serviceName) {
        return new EdtToolException(code, serviceName + " is unavailable in EDT runtime"); //$NON-NLS-1$
    }
}
