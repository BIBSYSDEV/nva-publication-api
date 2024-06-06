package no.sikt.nva.brage.migration.merger;

import java.util.List;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.contexttypes.Degree;
import nva.commons.core.JacocoGenerated;

public final class PreMergeValidator {

    public static final String HANDLE_ALREADY_EXIST_ERROR_MESSAGE = "Cannot merge student degree with nva publication"
                                                                    + " with handle in additional identifiers: %s";

    @JacocoGenerated
    private PreMergeValidator() {

    }

    public static void validate(Publication existingPublication) {
        var handles = handleInAdditionalIdentifier(existingPublication);
        if (isDegree(existingPublication) && !handles.isEmpty()) {
            throw new DegreeMergingException(String.format(HANDLE_ALREADY_EXIST_ERROR_MESSAGE, handles));
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
