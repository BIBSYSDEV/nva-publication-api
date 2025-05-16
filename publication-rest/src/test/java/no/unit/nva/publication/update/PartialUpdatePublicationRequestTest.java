package no.unit.nva.publication.update;

import static no.unit.nva.model.associatedartifacts.AssociatedArtifactList.empty;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.List;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.Resource;
import org.junit.jupiter.api.Test;

class PartialUpdatePublicationRequestTest {

    @Test
    void shouldThrowIllegalArgumentExceptionWhenUpdatingResourceWithDifferentIdentifier() {
        var resource = Resource.fromPublication(randomPublication());
        var request = requestWithIdentifier(SortableIdentifier.next());

        assertThrows(IllegalArgumentException.class, () -> request.generateUpdate(resource));
    }

    private static PartialUpdatePublicationRequest requestWithIdentifier(SortableIdentifier identifier) {
        return new PartialUpdatePublicationRequest(identifier, List.of(), List.of(), empty());
    }
}
