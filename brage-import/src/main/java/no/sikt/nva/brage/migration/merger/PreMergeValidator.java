package no.sikt.nva.brage.migration.merger;

import no.sikt.nva.brage.migration.lambda.HandleDuplicateException;
import no.sikt.nva.brage.migration.model.PublicationRepresentation;
import no.unit.nva.model.additionalidentifiers.HandleIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.additionalidentifiers.SourceName;
import no.unit.nva.model.contexttypes.Degree;
import nva.commons.core.JacocoGenerated;

public final class PreMergeValidator {

    public static final String POST_ALREADY_MERGED = "Publication with handle %s in additional identifiers: %s";

    @JacocoGenerated
    private PreMergeValidator() {

    }

    public static boolean shouldNotMergeMetadata(PublicationRepresentation publicationRepresentation,
                                                 Publication existingPublication) {
        hasBeenMigratedBefore(publicationRepresentation, existingPublication);
        return isDegree(existingPublication) && hasBeenImportedFromBrage(existingPublication);
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

    private static boolean hasBeenImportedFromBrage(Publication publication) {
        return publication.getAdditionalIdentifiers().stream()
                   .filter(HandleIdentifier.class::isInstance)
                   .map(HandleIdentifier.class::cast)
                   .map(HandleIdentifier::source)
                   .anyMatch(SourceName::isFromBrageSystem);
    }

    private static boolean containsHandle(Publication publication, String handle) {
        return publication.getAdditionalIdentifiers().stream()
                   .filter(HandleIdentifier.class::isInstance)
                   .map(HandleIdentifier.class::cast)
                   .anyMatch(handleIdentifier -> handleIdentifier.value().equals(handle));
    }

    private static boolean isDegree(Publication publication) {
        return publication.getEntityDescription().getReference().getPublicationContext() instanceof Degree;
    }
}
