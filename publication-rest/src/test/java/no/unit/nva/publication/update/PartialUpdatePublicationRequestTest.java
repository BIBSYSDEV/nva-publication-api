package no.unit.nva.publication.update;

import static no.unit.nva.model.associatedartifacts.AssociatedArtifactList.empty;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import java.util.Set;
import no.unit.nva.commons.json.JsonUtils;
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

    @Test
    void shouldNotUpdateFieldsNotSupportedByPartialUpdate() throws JsonProcessingException {
        var doi = randomUri();
        var json = """
                        {
              "type": "PartialUpdatePublicationRequest",
              "identifier": "0196cfeaceef-458c6685-bc99-4218-ac4e-4ff9582b0800",
              "doi": "__DOI__"
            }
            """.replace("__DOI__", doi.toString());

        var resource = Resource.fromPublication(randomPublication());
        resource.setIdentifier(new SortableIdentifier("0196cfeaceef-458c6685-bc99-4218-ac4e-4ff9582b0800"));

        var updatedResource = JsonUtils.dtoObjectMapper.readValue(json, PartialUpdatePublicationRequest.class)
                                  .generateUpdate(resource);

        assertEquals(resource.getDoi(), updatedResource.getDoi());
    }

    private static PartialUpdatePublicationRequest emptyRequestWithIdentifier(SortableIdentifier identifier) {
        return new PartialUpdatePublicationRequest(identifier, Set.of(), List.of(), empty());
    }
}
