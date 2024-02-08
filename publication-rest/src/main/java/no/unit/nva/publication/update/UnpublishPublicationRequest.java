package no.unit.nva.publication.update;

import java.net.URI;
import java.util.Optional;

public class UnpublishPublicationRequest implements PublicationRequest {
    private Optional<URI> duplicateOf = Optional.empty();
    private String comment;

    public Optional<URI> getDuplicateOf() {
        return duplicateOf;
    }

    public void setDuplicateOf(URI duplicateOf) {
        this.duplicateOf = Optional.of(duplicateOf);
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
