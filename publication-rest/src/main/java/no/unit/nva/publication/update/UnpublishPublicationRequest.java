package no.unit.nva.publication.update;

import java.net.URI;
import java.util.Optional;

public class UnpublishPublicationRequest implements PublicationRequest {

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
}
