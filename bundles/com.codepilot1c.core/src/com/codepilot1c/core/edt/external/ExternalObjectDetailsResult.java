package com.codepilot1c.core.edt.external;

import java.util.List;

/**
 * Detailed external object projection for agent reasoning.
 */
public record ExternalObjectDetailsResult(
        String project,
        String externalProject,
        String fqn,
        String name,
        String kind,
        String synonym,
        String objectBelonging,
        int attributesCount,
        int tabularSectionsCount,
        int formsCount,
        int templatesCount,
        int containedObjectsCount,
        boolean hasObjectModule,
        boolean hasDefaultForm,
        boolean hasAuxiliaryForm,
        List<String> containedKinds
) {
}
