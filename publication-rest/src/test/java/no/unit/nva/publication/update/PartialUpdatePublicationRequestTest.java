package no.unit.nva.publication.update;

import static no.unit.nva.model.associatedartifacts.AssociatedArtifactList.empty;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.List;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.Resource;
import org.junit.jupiter.api.Test;

class PartialUpdatePublicationRequestTest {

    @Test
    void shouldThrowIllegalArgumentExceptionWhenUpdatingResourceWithDifferentIdentifier() {
        var resource = Resource.fromPublication(randomPublication());
        var request = emptyRequestWithIdentifier(SortableIdentifier.next());

        assertThrows(IllegalArgumentException.class, () -> request.generateUpdate(resource));
    }

    @Test
    void partialUpdateShouldUpdateProjectsFundingsAndAssociatedArtifacts() {
        var resource = Resource.fromPublication(randomPublication());
        var request = emptyRequestWithIdentifier(resource.getIdentifier());

        var updatedResource = request.generateUpdate(resource);

        assertNotEquals(resource.getFundings(), updatedResource.getFundings());
        assertNotEquals(resource.getProjects(), updatedResource.getProjects());
        assertNotEquals(resource.getAssociatedArtifacts(), updatedResource.getAssociatedArtifacts());
    }

    @Test
    void partialUpdateShouldNotUpdateOtherFieldsThanFundingsProjectsAndAssociatedArtifacts() {
        var resource = Resource.fromPublication(randomPublication());
        resource.setFundings(List.of());
        resource.setProjects(List.of());
        resource.setAssociatedArtifacts(empty());

        var request = emptyRequestWithIdentifier(resource.getIdentifier());

        var updatedResource = request.generateUpdate(resource);

        assertEquals(resource, updatedResource);
    }

    private static PartialUpdatePublicationRequest emptyRequestWithIdentifier(SortableIdentifier identifier) {
        return new PartialUpdatePublicationRequest(identifier, List.of(), List.of(), empty());
    }
}
