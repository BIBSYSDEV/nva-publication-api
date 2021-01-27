package no.unit.nva.publication.service.impl;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.dynamodbv2.model.TransactionCanceledException;
import java.io.IOException;
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
    public static final URI SOME_PUBLISHER = URI.create("https://some-publicsher.com");
    private static final Instant PUBLICATION_CREATION_TIME = Instant.parse("2010-01-01T10:15:30.00Z");
    private static final Instant PUBLICATION_UPDATE_TIME = Instant.parse("2011-02-02T10:15:30.00Z");
    private static final Instant DOI_REQUEST_CREATION_TIME = Instant.parse("2012-02-02T10:15:30.00Z");
    private static final Instant DOI_REQUEST_UPDATE_TIME = Instant.parse("2013-02-02T10:15:30.00Z");
    public static final String SOME_CURATOR = "some@curator";

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

        SortableIdentifier doiRequestIdentifier = createDoiRequest(publication, publication.getOwner());
        DoiRequest doiRequest = readDoiRequest(publication, doiRequestIdentifier);
        assertThat(doiRequest.getCreatedDate(), is(equalTo(DOI_REQUEST_CREATION_TIME)));
        assertThat(doiRequest, is(not(nullValue())));
    }

    public Publication createPublishedPublication()
        throws ApiGatewayException {
        Publication publication = createPublication();
        resourceService.publishPublication(createUserInstance(publication), publication.getIdentifier());
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
    public void createDoiRequestThrowsBadRequestExceptionWhenPublicationIdIsEmpty() throws IOException {
        UserInstance userInstance = new UserInstance(SOME_USER, SOME_PUBLISHER);
        Executable action = () -> doiRequestService.createDoiRequest(userInstance, null);
        BadRequestException exception = assertThrows(BadRequestException.class, action);
        assertThat(exception.getMessage(), containsString(ResourceService.EMPTY_RESOURCE_IDENTIFIER_ERROR));
    }

    @Test
    public void createDoiRequestThrowsConflictExceptionWhenDoiRequestAlreadyExists()
        throws ApiGatewayException {
        Publication publication = createPublishedPublication();

        createDoiRequest(publication, publication.getOwner());

        Executable action = () -> createDoiRequest(publication, publication.getOwner());
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
        resourceService.publishPublication(
            createUserInstance(publishedPublication),
            publishedPublication.getIdentifier());
        Publication draftPublication = createPublication();

        SortableIdentifier expectedDoiRequestIdentifier = doiRequestService.createDoiRequest(
            createUserInstance(publishedPublication),
            publishedPublication.getIdentifier());
        DoiRequest expectedDoiRequest = doiRequestService
            .getDoiRequest(createUserInstance(publishedPublication), expectedDoiRequestIdentifier);

        doiRequestService.createDoiRequest(
            createUserInstance(draftPublication),
            draftPublication.getIdentifier());

        UserInstance userInstance = new UserInstance(SOME_CURATOR,
            publishedPublication.getPublisher().getId());
        List<DoiRequest> doiRequests = doiRequestService.listDoiRequestsForPublishedPublications(userInstance);

        assertThat(doiRequests, is(not(nullValue())));
        assertThat(doiRequests, is(equalTo(List.of(expectedDoiRequest))));
    }

    private UserInstance createUserInstance(Publication publication) {
        return new UserInstance(publication.getOwner(), publication.getPublisher().getId());
    }

    private DoiRequest readDoiRequest(Publication publication, SortableIdentifier doiRequestIdentifier) {
        UserInstance userInstance = createUserInstance(publication);

        return doiRequestService.getDoiRequest(userInstance, doiRequestIdentifier);
    }

    private SortableIdentifier createDoiRequest(Publication publication, String owner)
        throws BadRequestException, ConflictException {
        UserInstance userInstance = new UserInstance(owner, publication.getPublisher().getId());
        return doiRequestService.createDoiRequest(userInstance, publication.getIdentifier());
    }

    private Publication createPublication() throws ConflictException {
        Publication publication = PublicationGenerator.publicationWithoutIdentifier();
        return resourceService.createPublication(publication);
    }
}