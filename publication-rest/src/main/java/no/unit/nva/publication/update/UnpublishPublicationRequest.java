package no.unit.nva.publication.update;

import static java.util.Objects.isNull;
import static no.unit.nva.publication.validation.PublicationUriValidator.isValid;
import java.net.URI;
import java.util.Optional;
import nva.commons.apigateway.exceptions.BadRequestException;

public class UnpublishPublicationRequest implements PublicationRequest {

    public static final String UNPUBLISH_REQUEST_REQUIRES_A_COMMENT = "Unpublish request requires a comment";
    public static final String DUPLICATE_OF_MUST_BE_A_PUBLICATION_API_URI =
        "The duplicateOf field must be a valid publication API URI";

    private URI duplicateOf;
    private String comment;

    public Optional<URI> getDuplicateOf() {
        return Optional.ofNullable(duplicateOf);
    }

    public void setDuplicateOf(URI duplicateOf) {
        this.duplicateOf = duplicateOf;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void validate(String host) throws BadRequestException {
        if (isNull(comment)) {
            throw new BadRequestException(UNPUBLISH_REQUEST_REQUIRES_A_COMMENT);
        }
        if (getDuplicateOf().isPresent()) {
            getDuplicateOf().filter(uri -> isValid(duplicateOf, host))
                .orElseThrow(() -> new BadRequestException(DUPLICATE_OF_MUST_BE_A_PUBLICATION_API_URI));
        }
    }
}
