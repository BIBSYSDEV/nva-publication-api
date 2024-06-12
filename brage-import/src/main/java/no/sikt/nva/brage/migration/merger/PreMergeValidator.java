package no.sikt.nva.brage.migration.merger;

import java.util.List;
import no.sikt.nva.brage.migration.lambda.HandleDuplicateException;
import no.sikt.nva.brage.migration.model.PublicationRepresentation;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.contexttypes.Degree;
import nva.commons.core.JacocoGenerated;

public final class PreMergeValidator {

    public static final String HANDLE_ALREADY_EXIST_ERROR_MESSAGE = "Cannot merge student degree with nva publication"
                                                                    + " with handle in additional identifiers: %s";

    public static final String POST_ALREADY_MERGED = "Publication with handle %s in additional identifiers: %s";

    @JacocoGenerated
    private PreMergeValidator() {

    }

    public static void validate(Publication existingPublication, PublicationRepresentation publicationRepresentation) {
        var handles = handleInAdditionalIdentifier(existingPublication);
        hasBeenMigratedBefore(publicationRepresentation, handles, existingPublication);
        degreeThatHasBeenMergedPreviously(existingPublication, handles);
    }

    private static void degreeThatHasBeenMergedPreviously(Publication existingPublication, List<String> handles) {
        if (isDegree(existingPublication) && !handles.isEmpty()) {
            throw new DegreeMergingException(String.format(HANDLE_ALREADY_EXIST_ERROR_MESSAGE, handles));
        }
    }

    private static void hasBeenMigratedBefore(PublicationRepresentation publicationRepresentation,
                                              List<String> handles, Publication existingPublication) {
        if (handles.contains(publicationRepresentation.brageRecord().getId().toString())) {
            throw new HandleDuplicateException(String.format(POST_ALREADY_MERGED,
                                                             publicationRepresentation.brageRecord().getId().toString(),
                                                             existingPublication.getIdentifier().toString()));
        }
    }

    private static List<String> handleInAdditionalIdentifier(Publication existingPublication) {
        return existingPublication.getAdditionalIdentifiers()
                   .stream()
                   .filter(additionalIdentifier -> "handle".equalsIgnoreCase(additionalIdentifier.getSourceName()))
                   .map(AdditionalIdentifier::getValue)
                   .toList();
    }

    private static boolean isDegree(Publication existingPublicaiton) {
        return existingPublicaiton.getEntityDescription().getReference().getPublicationContext() instanceof Degree;
    }
}
