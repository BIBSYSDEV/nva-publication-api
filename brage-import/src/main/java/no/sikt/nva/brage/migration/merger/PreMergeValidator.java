package no.sikt.nva.brage.migration.merger;

import no.unit.nva.model.Publication;
import no.unit.nva.model.contexttypes.Degree;
import nva.commons.core.JacocoGenerated;

public final class PreMergeValidator {

    public static final String HANDLE_ALREADY_EXIST_ERROR_MESSAGE = "Cannot merge student degree with nva publication"
                                                                    + " with handle in additional identifiers";

    @JacocoGenerated
    private PreMergeValidator() {

    }

    public static void validate(Publication existingPublication) {
        if (isDegree(existingPublication)) {
            if (hasHandlePresentAlready(existingPublication)) {
                throw new DegreeMergingException(HANDLE_ALREADY_EXIST_ERROR_MESSAGE);
            }
        }
    }

    private static boolean hasHandlePresentAlready(Publication existingPublication) {
        return existingPublication.getAdditionalIdentifiers()
                   .stream()
                   .anyMatch(additionalIdentifier -> "handle".equalsIgnoreCase(additionalIdentifier.getSourceName()));
    }

    private static boolean isDegree(Publication existingPublicaiton) {
        return existingPublicaiton.getEntityDescription().getReference().getPublicationContext() instanceof Degree;
    }
}
