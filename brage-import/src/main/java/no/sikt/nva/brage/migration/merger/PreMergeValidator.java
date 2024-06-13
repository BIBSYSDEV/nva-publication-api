package no.sikt.nva.brage.migration.merger;

import no.sikt.nva.brage.migration.lambda.HandleDuplicateException;
import no.sikt.nva.brage.migration.model.PublicationRepresentation;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.contexttypes.Degree;
import nva.commons.core.JacocoGenerated;

public final class PreMergeValidator {

    public static final String POST_ALREADY_MERGED = "Publication with handle %s in additional identifiers: %s";
    public static final String HANDLE = "handle";

    @JacocoGenerated
    private PreMergeValidator() {

    }

    public static boolean shouldNotMergeMetadata(PublicationRepresentation publicationRepresentation,
                                                 Publication existingPublication) {
        hasBeenMigratedBefore(publicationRepresentation, existingPublication);
        return isDegree(existingPublication) && hasBrageHandle(existingPublication);
    }

    private static void hasBeenMigratedBefore(PublicationRepresentation publicationRepresentation,
                                              Publication publication) {
        var handle = getHandle(publicationRepresentation);
        if (containsHandle(publication, handle)) {
            throw new HandleDuplicateException(String.format(POST_ALREADY_MERGED,
                                                             publicationRepresentation.brageRecord().getId().toString(),
                                                             publication.getIdentifier().toString()));
        }
    }

    private static String getHandle(PublicationRepresentation publicationRepresentation) {
        return publicationRepresentation.brageRecord().getId().toString();
    }

    private static boolean hasBrageHandle(Publication publication) {
        return publication.getAdditionalIdentifiers().stream()
                   .map(AdditionalIdentifier::getSourceName)
                   .anyMatch(HANDLE::equals);
    }

    private static boolean containsHandle(Publication publication, String handle) {
        return publication.getAdditionalIdentifiers().stream()
                   .filter(PreMergeValidator::isHandle)
                   .map(AdditionalIdentifier::getValue)
                   .anyMatch(handle::equals);
    }

    private static boolean isHandle(AdditionalIdentifier additionalIdentifier) {
        return HANDLE.equals(additionalIdentifier.getSourceName());
    }

    private static boolean isDegree(Publication publication) {
        return publication.getEntityDescription().getReference().getPublicationContext() instanceof Degree;
    }
}
