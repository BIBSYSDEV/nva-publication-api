package no.unit.nva.publication.model.business;

import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.model.testing.PublicationGenerator.publicationWithIdentifier;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.publication.model.business.StorageModelConfig.dynamoDbObjectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Year;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.publication.model.business.publicationstate.DoiAssignedEvent;
import no.unit.nva.publication.model.business.publicationstate.DoiRejectedEvent;
import no.unit.nva.publication.storage.model.exceptions.IllegalDoiRequestUpdate;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import org.junit.jupiter.api.Test;

class DoiRequestTest {

    public static final String TYPE_FIELD = "type";
    private final DoiRequest sampleDoiRequest = sampleDoiRequestFromResource();

    @Test
    void doiRequestHasTypeDoiRequest() {
        JsonNode json = dynamoDbObjectMapper.convertValue(sampleDoiRequest, JsonNode.class);
        assertThat(json.get(TYPE_FIELD), is(not(nullValue())));
        assertThat(json.get(TYPE_FIELD).textValue(), is(equalTo(DoiRequest.TYPE)));
    }

    @Test
    void doiRequestHasReferenceToResource() {
        assertThat(sampleDoiRequest.getResourceIdentifier(), is(notNullValue()));
    }

    @Test
    void updateThrowsExceptionWhenResourceIdentifierIsDifferent() {
        Resource resource = Resource.fromPublication(randomPublishedPublicationWithIdentifier());
        DoiRequest doiRequest = DoiRequest.create(resource, UserInstance.fromPublication(resource.toPublication()));

        Resource updatedResource = Resource.fromPublication(publicationWithIdentifier());

        assertThrows(IllegalDoiRequestUpdate.class, () -> doiRequest.update(updatedResource));
    }

    @Test
    void shouldSetDoiAssignedEventWhenCompletingDoi() {
        var doiRequest = sampleDoiRequestFromResource();
        var publication = randomPublication();
        publication.setStatus(PUBLISHED);
        publication.getEntityDescription().setPublicationDate(new PublicationDate.Builder().withYear(Year.now().toString()).build());
        var completedDoiRequest = doiRequest.complete(publication, UserInstance.create(randomString(), randomUri()));

        assertInstanceOf(DoiAssignedEvent.class, completedDoiRequest.getTicketEvent());
    }

    @Test
    void shouldSetDoiRejectedEventWhenClosingDoi() throws ApiGatewayException {
        var doiRequest = sampleDoiRequestFromResource();
        var publication = randomPublication();
        publication.setStatus(PUBLISHED);
        publication.getEntityDescription().setPublicationDate(new PublicationDate.Builder().withYear(Year.now().toString()).build());
        var rejectedDoiRequest = doiRequest.close(UserInstance.create(randomString(), randomUri()));

        assertInstanceOf(DoiRejectedEvent.class, rejectedDoiRequest.getTicketEvent());
    }

    @Test
    void shouldThrowInvalidTicketStatusTransitionExceptionWhenCompletingDoiRequestWhichDoesNotSatisfyRequirements() {
        var doiRequest = sampleDoiRequestFromResource();
        var publication = randomPublication();
        publication.getEntityDescription().setPublicationDate(new PublicationDate.Builder().withYear(randomString()).build());

        assertThrows(InvalidTicketStatusTransitionException.class, () ->
            doiRequest.complete(publication, UserInstance.create(randomString(), randomUri())));
    }

    private DoiRequest sampleDoiRequestFromResource() {
        var publication = randomPublishedPublicationWithIdentifier();
        var resource = Resource.fromPublication(publication);
        return DoiRequest.create(resource, UserInstance.fromPublication(resource.toPublication()));
    }

    private static Publication randomPublishedPublicationWithIdentifier() {
        return publicationWithIdentifier().copy().withStatus(PUBLISHED).build();
    }
}