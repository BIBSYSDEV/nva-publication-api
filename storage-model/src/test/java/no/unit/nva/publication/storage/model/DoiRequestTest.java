package no.unit.nva.publication.storage.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.PublicationGenerator;
import nva.commons.core.JsonUtils;
import org.junit.jupiter.api.Test;

public class DoiRequestTest {

    public static final String TYPE_FIELD = "type";
    private static final Instant NOW = Instant.now();
    public static final Clock CLOCK = fixedClock();
    private final DoiRequest sampleDoiRequest = sampleDoiRequest();

    @Test
    public void doiRequestHasTypeDoiRequest() {

        JsonNode json = JsonUtils.objectMapper.convertValue(sampleDoiRequest, JsonNode.class);
        assertThat(json.get(TYPE_FIELD), is(not(nullValue())));
        assertThat(json.get(TYPE_FIELD).textValue(), is(equalTo(DoiRequest.TYPE)));
    }

    @Test
    public void doiRequestHasReferenceToResource() {
        assertThat(sampleDoiRequest.getResourceIdentifier(), is(notNullValue()));
    }

    @Test
    public void doiRequestCannotBeCreatedWithoutReferenceToResource() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            this::doiRequestWithoutResourceReference);
        assertThat(exception.getMessage(), is(equalTo(DoiRequest.MISSING_RESOURCE_REFERENCE_ERROR)));
    }

    @Test
    public void doiRequestContainsResourcesMainTitle() {
        Publication publication = PublicationGenerator.publicationWithIdentifier();
        Resource resource = Resource.fromPublication(publication);
        DoiRequest doiRequest = DoiRequest.fromResource(resource, fixedClock());
        assertThat(doiRequest.getResourceTitle(), is(equalTo(publication.getEntityDescription().getMainTitle())));
    }

    private static Clock fixedClock() {
        return Clock.fixed(NOW, ZoneId.systemDefault());
    }

    private DoiRequest doiRequestWithoutResourceReference() {
        return new DoiRequest(
            sampleDoiRequest.getIdentifier(),
            null,
            sampleDoiRequest.getResourceTitle(),
            sampleDoiRequest.getOwner(),
            sampleDoiRequest.getCustomerId(),
            sampleDoiRequest.getStatus(),
            PublicationStatus.DRAFT,
            sampleDoiRequest.getCreatedDate(),
            sampleDoiRequest.getModifiedDate());
    }

    private DoiRequest sampleDoiRequest() {
        Resource resource = Resource.fromPublication(PublicationGenerator.publicationWithIdentifier());
        return DoiRequest.fromResource(resource, CLOCK);
    }
}