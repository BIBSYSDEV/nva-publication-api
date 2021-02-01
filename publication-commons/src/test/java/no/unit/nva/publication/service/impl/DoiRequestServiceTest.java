package no.unit.nva.publication.service.impl;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.dynamodbv2.model.TransactionCanceledException;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.PublicationGenerator;
import no.unit.nva.publication.service.ResourcesDynamoDbLocalTest;
import no.unit.nva.publication.service.impl.exceptions.BadRequestException;
import no.unit.nva.publication.storage.model.DoiRequest;
import no.unit.nva.publication.storage.model.UserInstance;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

public class DoiRequestServiceTest extends ResourcesDynamoDbLocalTest {

    public static final String NOT_THE_RESOURCE_OWNER = "someOther@owner.org";
    public static final String SOME_USER = "some@user.com";
    public static final URI SOME_PUBLISHER = URI.create("https://some-publisher.example.org");
    private static final Instant PUBLICATION_CREATION_TIME = Instant.parse("2010-01-01T10:15:30.00Z");
    private static final Instant PUBLICATION_UPDATE_TIME = Instant.parse("2011-02-02T10:15:30.00Z");
    private static final Instant DOI_REQUEST_CREATION_TIME = Instant.parse("2012-02-02T10:15:30.00Z");
    private static final Instant DOI_REQUEST_UPDATE_TIME = Instant.parse("2013-02-02T10:15:30.00Z");
    public static final String SOME_CURATOR = "some@curator";
    private static final URI SOME_OTHER_PUBLISHER = URI.create("https://some-other-publisher.com");

    private Clock mockClock;
    private ResourceService resourceService;
    private DoiRequestService doiRequestService;

    @BeforeEach
    public void initialize() {
        super.init();
        this.mockClock = mock(Clock.class);
        when(mockClock.instant())
            .thenReturn(PUBLICATION_CREATION_TIME)
            .thenReturn(PUBLICATION_UPDATE_TIME)
            .thenReturn(DOI_REQUEST_CREATION_TIME)
            .thenReturn(DOI_REQUEST_UPDATE_TIME);
        this.resourceService = new ResourceService(client, mockClock);
        this.doiRequestService = new DoiRequestService(client, mockClock);
    }

    @Test
    public void createDoiRequestStoresNewDoiRequestForPublishedResource()
        throws ApiGatewayException {
        Publication publication = createPublishedPublication();

        createDoiRequest(publication);
        DoiRequest doiRequest = getDoiRequest(publication);
        assertThat(doiRequest.getCreatedDate(), is(equalTo(DOI_REQUEST_CREATION_TIME)));
        assertThat(doiRequest, is(not(nullValue())));
    }

    public Publication createPublishedPublication()
        throws ApiGatewayException {
        Publication publication = createPublication();
        publishPublication(publication);
        return publication;
    }

    @Test
    public void createDoiRequestThrowsExceptionWhenTheUserIsNotTheResourceOwner()
        throws ApiGatewayException {
        Publication publication = createPublishedPublication();

        Executable action = () -> createDoiRequest(publication, NOT_THE_RESOURCE_OWNER);
        BadRequestException exception = assertThrows(BadRequestException.class, action);
        assertThat(exception.getCause(), is(instanceOf(NotFoundException.class)));
    }

    @Test
    public void createDoiRequestThrowsBadRequestExceptionWhenPublicationIdIsEmpty() {
        UserInstance userInstance = new UserInstance(SOME_USER, SOME_PUBLISHER);
        Executable action = () -> doiRequestService.createDoiRequest(userInstance, null);
        BadRequestException exception = assertThrows(BadRequestException.class, action);
        assertThat(exception.getMessage(), containsString(ResourceService.EMPTY_RESOURCE_IDENTIFIER_ERROR));
    }

    @Test
    public void createDoiRequestThrowsConflictExceptionWhenDoiRequestAlreadyExists()
        throws ApiGatewayException {
        Publication publication = createPublishedPublication();

        createDoiRequest(publication);

        Executable action = () -> createDoiRequest(publication);
        ConflictException exception = assertThrows(ConflictException.class, action);

        assertThat(exception.getCause(), is(instanceOf(TransactionCanceledException.class)));
    }

    @Test
    public void createDoiRequestThrowsExceptionWhenDuplicateIdentifierIsCreated()
        throws ApiGatewayException {
        Publication publication = createPublishedPublication();

        SortableIdentifier duplicateIdentifier = SortableIdentifier.next();
        Supplier<SortableIdentifier> identifierSupplier = () -> duplicateIdentifier;
        DoiRequestService doiRequestServiceProducingDuplicates = new DoiRequestService(client, mockClock,
            identifierSupplier);

        UserInstance userInstance = createUserInstance(publication);

        Executable action = () -> doiRequestServiceProducingDuplicates.createDoiRequest(userInstance,
            publication.getIdentifier());
        assertDoesNotThrow(action);
        assertThrows(ConflictException.class, action);
    }

    @Test
    public void createDoiRequestDoesNotThrowExceptionWhenResourceIsNotPublished() throws ConflictException {
        Publication publication = createPublication();
        UserInstance userInstance = createUserInstance(publication);
        Executable action = () -> doiRequestService.createDoiRequest(userInstance, publication.getIdentifier());
        assertDoesNotThrow(action);
    }

    @Test
    public void listDoiRequestsForPublishedResourcesReturnsAllDoiRequestsWithStatusRequestedForPublishedResources()
        throws ApiGatewayException {
        Publication publishedPublication = createPublication();
        publishPublication(publishedPublication);
        Publication draftPublication = createPublication();

        createDoiRequest(publishedPublication);
        createDoiRequest(draftPublication);

        UserInstance curator = new UserInstance(SOME_CURATOR, publishedPublication.getPublisher().getId());
        List<DoiRequest> doiRequests = doiRequestService.listDoiRequestsForPublishedPublications(curator);

        DoiRequest expectedDoiRequest = getDoiRequest(publishedPublication);
        DoiRequest unexpectedDoiRequest = getDoiRequest(draftPublication);

        assertThat(doiRequests, hasItem(expectedDoiRequest));
        assertThat(doiRequests, not(hasItem(unexpectedDoiRequest)));
    }

    @Test
    public void listDoiRequestsForPublishedResourcesDoesNotReturnDoiRequestsFromDifferentOrganization()
        throws ApiGatewayException {
        Publication publishedPublication = createPublication();
        publishPublication(publishedPublication);

        createDoiRequest(publishedPublication);

        UserInstance irrelevantUser = new UserInstance(SOME_CURATOR, SOME_OTHER_PUBLISHER);
        List<DoiRequest> resultForIrrelevantUser =
            doiRequestService.listDoiRequestsForPublishedPublications(irrelevantUser);
        assertThat(resultForIrrelevantUser, is(emptyCollectionOf(DoiRequest.class)));

        UserInstance relevantUser = new UserInstance(SOME_CURATOR, publishedPublication.getPublisher().getId());
        List<DoiRequest> resultForRelevantUser =
            doiRequestService.listDoiRequestsForPublishedPublications(relevantUser);

        DoiRequest expectedDoiRequest = getDoiRequest(publishedPublication);
        assertThat(resultForRelevantUser, hasItem(expectedDoiRequest));
    }

    public DoiRequest getDoiRequest(Publication publishedPublication) {
        return doiRequestService
            .getDoiRequestByResourceIdentifier(
                createUserInstance(publishedPublication),
                publishedPublication.getIdentifier()
            );
    }

    public void publishPublication(Publication publishedPublication) throws ApiGatewayException {
        resourceService.publishPublication(
            createUserInstance(publishedPublication),
            publishedPublication.getIdentifier());
    }

    private void createDoiRequest(Publication publishedPublication)
        throws BadRequestException, ConflictException {
        doiRequestService.createDoiRequest(
            createUserInstance(publishedPublication), publishedPublication.getIdentifier());
    }

    private void createDoiRequest(Publication publication, String owner)
        throws BadRequestException, ConflictException {
        UserInstance userInstance = new UserInstance(owner, publication.getPublisher().getId());
        doiRequestService.createDoiRequest(userInstance, publication.getIdentifier());
    }

    private UserInstance createUserInstance(Publication publication) {
        return new UserInstance(publication.getOwner(), publication.getPublisher().getId());
    }

    private Publication createPublication() throws ConflictException {
        Publication publication = PublicationGenerator.publicationWithoutIdentifier();
        return resourceService.createPublication(publication);
    }
}