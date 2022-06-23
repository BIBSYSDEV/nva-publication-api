package no.unit.nva.publication.storage.model;

import com.fasterxml.jackson.databind.JsonNode;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.testing.PublicationGenerator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static no.unit.nva.publication.storage.model.StorageModelConfig.dynamoDbObjectMapper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;

class PublishingRequestTest {

    public static final String TYPE_FIELD = "type";
    private final PublishingRequest sampleRequest = samplePublishingRequest();
    static Publication randomPublication;
    static SortableIdentifier randomIdentifier;

    @BeforeAll
    static void beforeAll() {
        randomPublication = PublicationGenerator.randomPublication();
        randomIdentifier = SortableIdentifier.next();
    }

    @Test
    public void publishingRequestHasTypePublishingRequest() {
        var json = dynamoDbObjectMapper.convertValue(sampleRequest, JsonNode.class);
        assertThat(json.get(TYPE_FIELD), is(not(nullValue())));
        assertThat(json.get(TYPE_FIELD).textValue(), is(equalTo(PublishingRequest.TYPE)));
    }

    @Test
    public void publishingRequestHasReferenceToResource() {
        assertThat(sampleRequest.getResourceIdentifier(), is(notNullValue()));
    }

    @Test
    public void publishingRequestHasPublication() {
        assertThat(sampleRequest.toPublication(), is(notNullValue()));
    }

    @Test
    public void publishingRequestReturnsDAO() {
        assertThat(sampleRequest.toDao(), is(notNullValue()));
    }

    @Test
    public void publishingRequestHasStatus() {
        assertThat(sampleRequest.getStatusString(), is(notNullValue()));
    }

    private PublishingRequest samplePublishingRequest() {
        return PublishingRequest.fromPublication(randomPublication, randomIdentifier);
    }
}
