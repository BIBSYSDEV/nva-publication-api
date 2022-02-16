package no.unit.nva.expansion.model;

import static no.unit.nva.expansion.ExpansionConfig.objectMapper;
import static no.unit.nva.expansion.model.ExpandedResource.fromPublication;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import no.unit.nva.expansion.FakeResourceExpansionService;
import no.unit.nva.expansion.ResourceExpansionService;
import no.unit.nva.model.Publication;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.service.impl.MessageService;
import no.unit.nva.publication.storage.model.DoiRequest;
import no.unit.nva.publication.storage.model.Resource;
import nva.commons.apigateway.exceptions.NotFoundException;
import org.junit.jupiter.api.Test;

class ExpandedDataEntryTest  {

    public static final String TYPE = "type";
    public static final String EXPECTED_TYPE_OF_EXPANDED_RESOURCE_ENTRY = "Publication";
    private static final MessageService messageService = new FakeMessageService();
    private final ResourceExpansionService resourceExpansionService = new FakeResourceExpansionService();

    @Test
    void shouldReturnExpandedResourceWithoutLossOfInformation() throws JsonProcessingException {
        var publication = PublicationGenerator.randomPublication();
        var expandedResource = fromPublication(publication);
        var regeneratedPublication = objectMapper.readValue(expandedResource.toJsonString(), Publication.class);
        assertThat(regeneratedPublication, is(equalTo(publication)));
    }

    @Test
    void shouldReturnExpandedDoiRequestWithoutLossOfInformation() throws NotFoundException {
        var publication = PublicationGenerator.randomPublication();
        var doiRequest = DoiRequest.newDoiRequestForResource(Resource.fromPublication(publication));
        ExpandedDoiRequest expandedDoiRequest =
            ExpandedDoiRequest.create(doiRequest, resourceExpansionService, messageService);
        assertThat(expandedDoiRequest.toDoiRequest(), is(equalTo(doiRequest)));
    }

    @Test
    void expandedResourceShouldHaveTypePublicationInheritingTheTypeFromThePublicationWhenItIsSerialized()
        throws JsonProcessingException {
        var publication = PublicationGenerator.randomPublication();
        var expandedResource = fromPublication(publication);
        var json = objectMapper.readTree(expandedResource.toJsonString());
        assertThat(json.get(TYPE).textValue(), is(equalTo(EXPECTED_TYPE_OF_EXPANDED_RESOURCE_ENTRY)));
    }

    @Test
    void expandedDoiRequestShouldHaveTypeDoiRequest() throws NotFoundException {
        var publication = PublicationGenerator.randomPublication();
        var doiRequest = DoiRequest.newDoiRequestForResource(Resource.fromPublication(publication));
        var expandedResource =
            ExpandedDoiRequest.create(doiRequest, resourceExpansionService, messageService);
        var json = objectMapper.convertValue(expandedResource, ObjectNode.class);
        assertThat(json.get(TYPE).textValue(), is(equalTo(ExpandedDoiRequest.TYPE)));
    }

}