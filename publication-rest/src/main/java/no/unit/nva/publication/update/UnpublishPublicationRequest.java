package no.unit.nva.publication.update;

import java.util.Optional;
import no.unit.nva.identifiers.SortableIdentifier;

public class UnpublishPublicationRequest implements PublicationRequest {
    private Optional<SortableIdentifier> duplicateOf = Optional.empty();
    private String comment;

    public Optional<SortableIdentifier> getDuplicateOf() {
        return duplicateOf;
    }

    public void setDuplicateOf(SortableIdentifier duplicateOf) {
        this.duplicateOf = Optional.of(duplicateOf);
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
