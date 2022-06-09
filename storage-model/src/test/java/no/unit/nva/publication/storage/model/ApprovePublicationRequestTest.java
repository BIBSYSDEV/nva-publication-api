package no.unit.nva.publication.storage.model;

import com.fasterxml.jackson.databind.JsonNode;
import no.unit.nva.identifiers.SortableIdentifier;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

import static no.unit.nva.publication.storage.model.StorageModelConfig.dynamoDbObjectMapper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.jupiter.api.Assertions.*;

class ApprovePublicationRequestTest {

    public static final String TYPE_FIELD = "type";
    private static final Instant NOW = Instant.now();
    public static final Clock CLOCK = fixedClock();
    private final ApprovePublicationRequest sampleRequest = sampleApprovePublicationRequest();


    @Test
    public void approvePublicationRequestHasTypeApprovePublicationRequest() {

        JsonNode json = dynamoDbObjectMapper.convertValue(sampleRequest, JsonNode.class);
        assertThat(json.get(TYPE_FIELD), is(not(nullValue())));
        assertThat(json.get(TYPE_FIELD).textValue(), is(equalTo(ApprovePublicationRequest.TYPE)));
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

    private ApprovePublicationRequest sampleApprovePublicationRequest() {
        ApprovePublicationRequest approvePublicationRequest = new ApprovePublicationRequest(SortableIdentifier.next());
        approvePublicationRequest.setCreatedDate(NOW);
        approvePublicationRequest.setModifiedDate(NOW);
        approvePublicationRequest.setResourceIdentifier(SortableIdentifier.next());
        approvePublicationRequest.setRowVersion(UUID.randomUUID().toString());
        return  approvePublicationRequest;
    }


}
