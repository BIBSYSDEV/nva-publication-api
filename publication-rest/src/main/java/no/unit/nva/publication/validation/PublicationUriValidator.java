package no.unit.nva.publication.validation;

import java.net.URI;
import java.util.Arrays;
import no.unit.nva.identifiers.SortableIdentifier;

public final class PublicationUriValidator {

    public static final String PATH_DELIMITER = "/";
    public static final int TWO_ELEMENTS = 2;

    private static final String PUBLICATION = "publication";
    public static final int REMOVE_FIRST_SLASH = 1;

    private PublicationUriValidator() {
        // NO-OP
    }

    public static boolean isValid(URI duplicateOf, String expectedHost) {
        return expectedHost.equals(duplicateOf.getHost()) && validatePath(duplicateOf);
    }

    private static boolean validatePath(URI duplicateOf) {
        var pathElements = Arrays.asList(duplicateOf.getPath()
                                             .substring(REMOVE_FIRST_SLASH)
                                             .split(PATH_DELIMITER));
        return pathElements.size() == TWO_ELEMENTS
               && PUBLICATION.equals(pathElements.getFirst())
               && isSortableIdentifier(pathElements.getLast());
    }

    private static boolean isSortableIdentifier(String identifier) {
        try {
            new SortableIdentifier(identifier);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
