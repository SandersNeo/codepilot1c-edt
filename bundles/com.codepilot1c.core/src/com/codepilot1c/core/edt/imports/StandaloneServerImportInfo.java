package com.codepilot1c.core.edt.imports;

/**
 * Result details for the standalone-server side of infobase import.
 */
public record StandaloneServerImportInfo(
        boolean created,
        boolean started,
        String statusMessage,
        String serverName,
        String serverVersion,
        String serverLocation,
        String serverDataLocation,
        String infobaseUrl,
        String designerUrl) {
}
