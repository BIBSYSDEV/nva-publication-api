package no.sikt.nva.brage.migration.merger;

import no.unit.nva.model.Publication;
import no.unit.nva.model.contexttypes.Degree;
import nva.commons.core.JacocoGenerated;

public final class PreMergeValidator {

    @JacocoGenerated
    private PreMergeValidator() {

    }

    public static boolean shouldNotMergePublications(Publication existingPublication) {
        return isDegree(existingPublication) && hasBrageHandle(existingPublication);
    }

    private static boolean hasBrageHandle(Publication existingPublication) {
        return existingPublication.getAdditionalIdentifiers()
                   .stream()
                   .anyMatch(additionalIdentifier -> "handle".equalsIgnoreCase(additionalIdentifier.getSourceName()));
    }

    private static boolean isDegree(Publication existingPublication) {
        return existingPublication.getEntityDescription().getReference().getPublicationContext() instanceof Degree;
    }
}
