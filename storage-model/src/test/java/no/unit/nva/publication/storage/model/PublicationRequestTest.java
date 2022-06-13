package no.unit.nva.publication.storage.model;

import com.fasterxml.jackson.databind.JsonNode;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.testing.PublicationGenerator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static no.unit.nva.publication.storage.model.StorageModelConfig.dynamoDbObjectMapper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;

class PublicationRequestTest {

    public static final String TYPE_FIELD = "type";
    private static final Instant NOW = Instant.now();
    private final PublicationRequest sampleRequest = samplePublicationRequest();
    static Publication randomPublication;
    static SortableIdentifier randomIdentifier;

    @BeforeAll
    static void beforeAll() {
        randomPublication = PublicationGenerator.randomPublication();
        randomIdentifier = SortableIdentifier.next();
    }


    @Test
    public void approvePublicationRequestHasTypeApprovePublicationRequest() {

        JsonNode json = dynamoDbObjectMapper.convertValue(sampleRequest, JsonNode.class);
        assertThat(json.get(TYPE_FIELD), is(not(nullValue())));
        assertThat(json.get(TYPE_FIELD).textValue(), is(equalTo(PublicationRequest.TYPE)));
    }

    @Test
    public void approvePublicationRequestHasReferenceToResource() {
        assertThat(sampleRequest.getResourceIdentifier(), is(notNullValue()));
    }

    @Test
    public void approvePublicationRequestHasPublication() {
        assertThat(sampleRequest.toPublication(), is(notNullValue()));
    }

    @Test
    public void approvePublicationRequestReturnsDAO() {
        assertThat(sampleRequest.toDao(), is(notNullValue()));
    }

    @Test
    public void approvePublicationRequestHasStatus() {
        assertThat(sampleRequest.getStatusString(), is(notNullValue()));
    }

    private static Clock fixedClock() {
        return Clock.fixed(NOW, ZoneId.systemDefault());
    }

    private PublicationRequest samplePublicationRequest() {
        return PublicationRequest.fromPublication(randomPublication, randomIdentifier);
    }


}
