package no.unit.nva.publication.storage.model;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.DoiRequestStatus;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.PublicationGenerator;
import nva.commons.core.JsonUtils;
import org.junit.jupiter.api.Test;

public class DoiRequestTest {

    public static final String TYPE_FIELD = "type";
    public static final SortableIdentifier DOI_REQUEST_IDENTIFIER = SortableIdentifier.next();
    public static final SortableIdentifier RESOURCE_IDENTIFIER = SortableIdentifier.next();
    public static final String RESOURCE_TITLE = "resourceTitle";
    public static final String SOME_OWNER = "someOwner";
    public static final URI SOME_CUSTOMER = URI.create("https://some-customer.com");
    public static final DoiRequestStatus SOME_DOI_REQUEST_STATUS = DoiRequestStatus.REJECTED;
    public static final PublicationStatus SOME_PUBLICATION_STATUS = PublicationStatus.DRAFT;
    public static final Instant DOI_REQUEST_CREATION_TIME = Instant.parse("1000-01-01T10:15:30.00Z");
    public static final Instant DOI_REQUEST_UPDATE_TIME = Instant.parse("2000-01-01T10:15:30.00Z");
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

    @Test
    public void toPublicationReturnsPublicationInstanceWithoutLossOfInformation() {
        DoiRequest doiRequest = new DoiRequest(
            DOI_REQUEST_IDENTIFIER,
            RESOURCE_IDENTIFIER,
            RESOURCE_TITLE,
            SOME_OWNER,
            SOME_CUSTOMER,
            SOME_DOI_REQUEST_STATUS,
            SOME_PUBLICATION_STATUS,
            DOI_REQUEST_CREATION_TIME,
            DOI_REQUEST_UPDATE_TIME
        );
        assertThat(doiRequest, doesNotHaveEmptyValues());
        Publication generatedPublication = doiRequest.toPublication();

        assertThat(generatedPublication.getIdentifier(), is(equalTo(RESOURCE_IDENTIFIER)));
        assertThat(generatedPublication.getEntityDescription().getMainTitle(), is(equalTo(RESOURCE_TITLE)));
        assertThat(generatedPublication.getOwner(), is(equalTo(SOME_OWNER)));
        assertThat(generatedPublication.getPublisher().getId(), is(equalTo(SOME_CUSTOMER)));

        no.unit.nva.model.DoiRequest doiRequestDto = generatedPublication.getDoiRequest();
        assertThat(doiRequestDto.getStatus(), is(equalTo(SOME_DOI_REQUEST_STATUS)));
        assertThat(doiRequestDto.getCreatedDate(), is(equalTo(DOI_REQUEST_CREATION_TIME)));
        assertThat(doiRequestDto.getModifiedDate(), is(equalTo(DOI_REQUEST_UPDATE_TIME)));
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