package no.unit.nva.publication.service.impl;

import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.hasSize;
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
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.DoiRequestStatus;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Organization.Builder;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.exception.BadRequestException;
import no.unit.nva.publication.exception.TransactionFailedException;
import no.unit.nva.publication.service.ResourcesDynamoDbLocalTest;
import no.unit.nva.publication.storage.model.DoiRequest;
import no.unit.nva.publication.storage.model.UserInstance;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.attempt.Try;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

public class DoiRequestServiceTest extends ResourcesDynamoDbLocalTest {

    public static final String NOT_THE_RESOURCE_OWNER = "someOther@owner.org";
    public static final String SOME_USER = "some@user.com";

    public static final URI SOME_PUBLISHER = URI.create("https://some-publisher.example.org");
    public static final String SOME_CURATOR = "some@curator";

    private static final Instant PUBLICATION_CREATION_TIME = Instant.parse("2010-01-01T10:15:30.00Z");
    private static final Instant PUBLICATION_UPDATE_TIME = Instant.parse("2011-02-02T10:15:30.00Z");
    private static final Instant DOI_REQUEST_CREATION_TIME = Instant.parse("2012-02-02T10:15:30.00Z");
    private static final Instant DOI_REQUEST_UPDATE_TIME = Instant.parse("2013-02-02T10:15:30.00Z");
    private static final URI SOME_OTHER_PUBLISHER = URI.create("https://some-other-publisher.example.org");

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

    @Test
    public void createDoiRequestCreatesNewDoiRequestForPublicationWithoutMetadata()
        throws BadRequestException, NotFoundException, TransactionFailedException {
        Publication emptyPublication = emptyPublication();
        UserInstance userInstance = createUserInstance(emptyPublication);
        doiRequestService.createDoiRequest(userInstance, emptyPublication.getIdentifier());

        DoiRequest actualDoiRequest = doiRequestService.getDoiRequestByResourceIdentifier(userInstance,
            emptyPublication.getIdentifier());

        DoiRequest expectedDoiRequest = expectedDoiRequestForEmptyPublication(emptyPublication, actualDoiRequest);

        assertThat(actualDoiRequest, is(equalTo(expectedDoiRequest)));
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
    public void createDoiRequestThrowsTransactionFailedExceptionWhenDoiRequestAlreadyExists()
        throws ApiGatewayException {
        Publication publication = createPublishedPublication();

        createDoiRequest(publication);

        Executable action = () -> createDoiRequest(publication);
        TransactionFailedException exception = assertThrows(TransactionFailedException.class, action);

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
        assertThrows(TransactionFailedException.class, action);
    }

    @Test
    public void createDoiRequestDoesNotThrowExceptionWhenResourceIsNotPublished() throws TransactionFailedException {
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

        UserInstance curator = createSampleCurator(publishedPublication);
        List<DoiRequest> doiRequests = doiRequestService.listDoiRequestsForPublishedPublications(curator);

        DoiRequest expectedDoiRequest = getDoiRequest(publishedPublication);
        DoiRequest unexpectedDoiRequest = getDoiRequest(draftPublication);

        assertThat(doiRequests, hasItem(expectedDoiRequest));
        assertThat(doiRequests, not(hasItem(unexpectedDoiRequest)));
    }

    public UserInstance createSampleCurator(Publication publication) {
        return new UserInstance(SOME_CURATOR, publication.getPublisher().getId());
    }

    @Test
    public void listDoiRequestsForPublishedResourcesDoesNotReturnDoiRequestsFromDifferentOrganization()
        throws ApiGatewayException {
        Publication publishedPublication = createPublishedPublication();

        createDoiRequest(publishedPublication);

        UserInstance irrelevantUser = new UserInstance(SOME_CURATOR, SOME_OTHER_PUBLISHER);
        List<DoiRequest> resultForIrrelevantUser =
            doiRequestService.listDoiRequestsForPublishedPublications(irrelevantUser);
        assertThat(resultForIrrelevantUser, is(emptyCollectionOf(DoiRequest.class)));

        UserInstance relevantUser = createSampleCurator(publishedPublication);
        List<DoiRequest> resultForRelevantUser =
            doiRequestService.listDoiRequestsForPublishedPublications(relevantUser);

        DoiRequest expectedDoiRequest = getDoiRequest(publishedPublication);
        assertThat(resultForRelevantUser, hasItem(expectedDoiRequest));
    }

    @Test
    public void listDoiRequestsForUserReturnsDoiRequestsWithStatusRequested() throws ApiGatewayException {
        Publication publication = createPublication();
        createDoiRequest(publication);

        UserInstance userInstance = createUserInstance(publication);
        List<DoiRequest> result = doiRequestService.listDoiRequestsForUser(userInstance);

        var expectedDoiRequest =
            doiRequestService.getDoiRequestByResourceIdentifier(userInstance, publication.getIdentifier());

        assertThat(result, is(equalTo(List.of(expectedDoiRequest))));
    }

    @Test
    public void listDoiRequestsForUserReturnsAllRelevantDoiRequests() throws ApiGatewayException {
        int endExclusive = 10;
        List<Publication> publications = IntStream.range(0, endExclusive).boxed()
            .map(attempt(i -> createPublication()))
            .map(Try::orElseThrow)
            .collect(Collectors.toList());

        for (Publication publication : publications) {
            doiRequestService.createDoiRequest(createUserInstance(publication), publication.getIdentifier());
        }

        UserInstance userInstance = createUserInstance(publications.get(0));
        int resultSizePerQuery = 1;
        List<DoiRequest> result = doiRequestService.listDoiRequestsForUser(userInstance, resultSizePerQuery);

        assertThat(result, hasSize(endExclusive));
    }

    @Test
    public void updateDoiRequestUpdatesDoiRequestStatusInDatabase()
        throws ApiGatewayException {
        var publication = createPublishedPublication();
        UserInstance userInstance = createUserInstance(publication);
        SortableIdentifier doiRequestIdentifier = doiRequestService
            .createDoiRequest(userInstance, publication.getIdentifier());

        DoiRequestStatus expectedNewDoiRequestStatus = DoiRequestStatus.APPROVED;
        doiRequestService.updateDoiRequest(userInstance, publication.getIdentifier(), expectedNewDoiRequestStatus);

        DoiRequest updatedDoiRequest = doiRequestService.getDoiRequest(userInstance, doiRequestIdentifier);
        assertThat(updatedDoiRequest.getStatus(), is(equalTo(expectedNewDoiRequestStatus)));
    }

    @Test
    public void updateDoiRequestUpdatesModifiedDateOfDoiRequest()
        throws ApiGatewayException {
        Publication publication = createPublication();
        UserInstance userInstance = createUserInstance(publication);
        resourceService.publishPublication(userInstance, publication.getIdentifier());
        SortableIdentifier doiRequestIdentifier = doiRequestService
            .createDoiRequest(userInstance, publication.getIdentifier());

        DoiRequestStatus expectedNewDoiRequestStatus = DoiRequestStatus.APPROVED;
        doiRequestService.updateDoiRequest(userInstance, publication.getIdentifier(), expectedNewDoiRequestStatus);

        DoiRequest updatedDoiRequest = doiRequestService.getDoiRequest(userInstance, doiRequestIdentifier);
        assertThat(updatedDoiRequest.getModifiedDate(), is(equalTo(DOI_REQUEST_UPDATE_TIME)));
    }

    @Test
    public void updateDoiRequestUpdatesStatusIndexForDoiRequest()
        throws ApiGatewayException {
        var publication = createPublication();
        UserInstance someUser = createUserInstance(publication);
        resourceService.publishPublication(someUser, publication.getIdentifier());
        SortableIdentifier doiRequestIdentifier =
            doiRequestService.createDoiRequest(someUser, publication.getIdentifier());

        UserInstance sampleCurator = createSampleCurator(publication);

        assertThatDoiRequestIsIncludedInTheCuratorView(sampleCurator, doiRequestIdentifier);

        DoiRequestStatus expectedNewDoiRequestStatus = DoiRequestStatus.APPROVED;
        doiRequestService.updateDoiRequest(someUser, publication.getIdentifier(), expectedNewDoiRequestStatus);

        assertThatDoiRequestHasBeenRemovedFromCuratorsView(sampleCurator, doiRequestIdentifier);
    }

    @Test
    public void getDoiRequestThrowsNotFoundExceptionWhenDoiRequestWasNotFound() {
        UserInstance someUser = new UserInstance(SOME_USER, SOME_PUBLISHER);
        Executable action = () -> doiRequestService.getDoiRequest(someUser, SortableIdentifier.next());
        assertThrows(NotFoundException.class, action);
    }

    @Test
    public void updateDoiRequestThrowsBadRequestExceptionWhenPublicationIsDraft()
        throws TransactionFailedException, BadRequestException {
        Publication publication = createPublication();
        createDoiRequest(publication);
        UserInstance sampleCurator = createSampleCurator(publication);
        Executable action =
            () -> doiRequestService.updateDoiRequest(sampleCurator, publication.getIdentifier(),
                DoiRequestStatus.APPROVED);
        assertThrows(BadRequestException.class, action);
    }

    private void assertThatDoiRequestHasBeenRemovedFromCuratorsView(UserInstance sampleCurator,
                                                                    SortableIdentifier doiRequestIdentifier) {
        List<SortableIdentifier> updatedDoiRequestList = createDoiRequestListForCurator(sampleCurator);

        assertThat(updatedDoiRequestList, not(hasItem(doiRequestIdentifier)));
    }

    private List<SortableIdentifier> createDoiRequestListForCurator(UserInstance sampleCurator) {
        return doiRequestService
            .listDoiRequestsForPublishedPublications(sampleCurator)
            .stream().map(DoiRequest::getIdentifier).collect(Collectors.toList());
    }

    private void assertThatDoiRequestIsIncludedInTheCuratorView(UserInstance sampleCurator,
                                                                SortableIdentifier doiRequestIdentifier) {
        List<SortableIdentifier> doiRequestsList = createDoiRequestListForCurator(sampleCurator);
        assertThat(doiRequestsList, hasItem(doiRequestIdentifier));
    }

    private DoiRequest getDoiRequest(Publication publishedPublication) throws NotFoundException {
        return doiRequestService
            .getDoiRequestByResourceIdentifier(
                createUserInstance(publishedPublication),
                publishedPublication.getIdentifier()
            );
    }

    private void publishPublication(Publication publishedPublication) throws ApiGatewayException {
        resourceService.publishPublication(
            createUserInstance(publishedPublication),
            publishedPublication.getIdentifier());
    }

    private DoiRequest expectedDoiRequestForEmptyPublication(Publication emptyPublication,
                                                             DoiRequest actualDoiRequest) {
        return DoiRequest.builder()
            .withIdentifier(actualDoiRequest.getIdentifier())
            .withResourceIdentifier(emptyPublication.getIdentifier())
            .withOwner(emptyPublication.getOwner())
            .withCustomerId(emptyPublication.getPublisher().getId())
            .withStatus(DoiRequestStatus.REQUESTED)
            .withResourceStatus(PublicationStatus.DRAFT)
            .withCreatedDate(DOI_REQUEST_CREATION_TIME)
            .withModifiedDate(DOI_REQUEST_CREATION_TIME)
            .withResourceModifiedDate(PUBLICATION_CREATION_TIME)
            .build();
    }

    private void skipPublicationUpdate() {
        mockClock.instant();
    }

    private Publication emptyPublication() throws TransactionFailedException {
        Organization publisher = new Builder().withId(SOME_PUBLISHER).build();
        Publication publication = new Publication.Builder()
            .withOwner(SOME_USER)
            .withPublisher(publisher)
            .withStatus(PublicationStatus.DRAFT)
            .build();

        Publication emptyPublication = resourceService.createPublication(publication);
        skipPublicationUpdate();

        return emptyPublication;
    }

    private Publication createPublishedPublication()
        throws ApiGatewayException {
        Publication publication = createPublication();
        publishPublication(publication);
        return publication;
    }

    private void createDoiRequest(Publication publication)
        throws BadRequestException, TransactionFailedException {
        doiRequestService.createDoiRequest(
            createUserInstance(publication), publication.getIdentifier());
    }

    private void createDoiRequest(Publication publication, String owner)
        throws BadRequestException, TransactionFailedException {
        UserInstance userInstance = new UserInstance(owner, publication.getPublisher().getId());
        doiRequestService.createDoiRequest(userInstance, publication.getIdentifier());
    }

    private UserInstance createUserInstance(Publication publication) {
        return new UserInstance(publication.getOwner(), publication.getPublisher().getId());
    }

    private Publication createPublication() throws TransactionFailedException {
        Publication publication = PublicationGenerator.publicationWithoutIdentifier();
        return resourceService.createPublication(publication);
    }
}