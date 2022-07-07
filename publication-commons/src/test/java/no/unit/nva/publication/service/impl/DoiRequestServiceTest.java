package no.unit.nva.publication.service.impl;

import static no.unit.nva.publication.TestingUtils.createOrganization;
import static no.unit.nva.publication.TestingUtils.createPublicationForUser;
import static no.unit.nva.publication.TestingUtils.randomOrgUnitId;
import static no.unit.nva.publication.TestingUtils.randomUserInstance;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
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
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.publication.exception.BadRequestException;

import no.unit.nva.publication.exception.TransactionFailedException;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.storage.model.DoiRequest;
import no.unit.nva.publication.storage.model.UserInstance;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.attempt.Try;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class DoiRequestServiceTest extends ResourcesLocalTest {

    private static final Instant PUBLICATION_CREATION_TIME = Instant.parse("2010-01-01T10:15:30.00Z");
    private static final Instant PUBLICATION_UPDATE_TIME = Instant.parse("2011-02-02T10:15:30.00Z");
    private static final Instant DOI_REQUEST_CREATION_TIME = Instant.parse("2012-02-02T10:15:30.00Z");
    private static final Instant DOI_REQUEST_UPDATE_TIME = Instant.parse("2013-02-02T10:15:30.00Z");
    private static final URI SOME_OTHER_PUBLISHER = URI.create("https://some-other-publisher.example.org");

    private Clock mockClock;
    private ResourceService resourceService;
    private DoiRequestService doiRequestService;
    private UserInstance owner;
    private UserInstance notTheResourceOwner;

    @BeforeEach
    public void initialize() {
        super.init();
        this.mockClock = mock(Clock.class);
        this.owner = randomUserInstance();
        this.notTheResourceOwner = randomUserInstance();
        when(mockClock.instant())
            .thenReturn(PUBLICATION_CREATION_TIME)
            .thenReturn(PUBLICATION_UPDATE_TIME)
            .thenReturn(DOI_REQUEST_CREATION_TIME)
            .thenReturn(DOI_REQUEST_UPDATE_TIME);
        this.resourceService = new ResourceService(client, mockClock);
        this.doiRequestService = new DoiRequestService(client, mockClock);
    }

    @Test
    void createDoiRequestStoresNewDoiRequestForPublishedResource()
        throws ApiGatewayException {
        Publication publication = createPublishedPublication(owner);

        createDoiRequest(publication);
        DoiRequest doiRequest = getDoiRequest(publication);
        assertThat(doiRequest.getCreatedDate(), is(equalTo(DOI_REQUEST_CREATION_TIME)));
        assertThat(doiRequest, is(not(nullValue())));
    }

    @Test
    void createDoiRequestCreatesNewDoiRequestForPublicationWithoutMetadata()
        throws ApiGatewayException {
        Publication emptyPublication = emptyPublication(owner);
        UserInstance userInstance = createUserInstance(emptyPublication);
        doiRequestService.createDoiRequest(userInstance, emptyPublication.getIdentifier());

        DoiRequest actualDoiRequest =
            doiRequestService.getDoiRequestByResourceIdentifier(userInstance, emptyPublication.getIdentifier());

        DoiRequest expectedDoiRequest = expectedDoiRequestForEmptyPublication(emptyPublication, actualDoiRequest);

        assertThat(actualDoiRequest, is(equalTo(expectedDoiRequest)));
    }

    @Test
    void createDoiRequestThrowsExceptionWhenTheUserIsNotTheResourceOwner()
        throws ApiGatewayException {
        Publication publication = createPublishedPublication(owner);

        Executable action = () -> createDoiRequest(publication, notTheResourceOwner);
        BadRequestException exception = assertThrows(BadRequestException.class, action);
        assertThat(exception.getCause(), is(instanceOf(NotFoundException.class)));
    }

    @Test
    void createDoiRequestThrowsBadRequestExceptionWhenPublicationIdIsEmpty() {

        Executable action = () -> doiRequestService.createDoiRequest(owner, null);
        BadRequestException exception = assertThrows(BadRequestException.class, action);
        assertThat(exception.getMessage(), containsString(ResourceService.EMPTY_RESOURCE_IDENTIFIER_ERROR));
    }

    @Test
    void createDoiRequestThrowsTransactionFailedExceptionWhenDoiRequestAlreadyExists()
        throws ApiGatewayException {
        Publication publication = createPublishedPublication(owner);

        createDoiRequest(publication);

        Executable action = () -> createDoiRequest(publication);
        TransactionFailedException exception = assertThrows(TransactionFailedException.class, action);

        assertThat(exception.getCause(), is(instanceOf(TransactionCanceledException.class)));
    }

    @Test
    void createDoiRequestThrowsExceptionWhenDuplicateIdentifierIsCreated()
        throws ApiGatewayException {
        Publication publication = createPublishedPublication(owner);

        SortableIdentifier duplicateIdentifier = SortableIdentifier.next();
        Supplier<SortableIdentifier> identifierSupplier = () -> duplicateIdentifier;
        DoiRequestService doiRequestServiceProducingDuplicates =
            new DoiRequestService(client,
                                  mockClock,
                                  identifierSupplier);

        UserInstance userInstance = createUserInstance(publication);

        Executable action = () -> doiRequestServiceProducingDuplicates.createDoiRequest(userInstance,
                                                                                        publication.getIdentifier());
        assertDoesNotThrow(action);
        assertThrows(TransactionFailedException.class, action);
    }

    @Test
    void createDoiRequestDoesNotThrowExceptionWhenResourceIsNotPublished() throws ApiGatewayException {
        Publication publication = createDraftPublication(owner);
        Executable action = () -> doiRequestService.createDoiRequest(owner, publication.getIdentifier());
        assertDoesNotThrow(action);
    }

    private Publication createDraftPublication(UserInstance owner) throws ApiGatewayException {
        Publication publication = createPublicationForUser(owner);
        return resourceService.createPublication(owner, publication);
    }

    @Test
    void listDoiRequestsForPublishedResourcesReturnsAllDoiRequestsWithStatusRequestedForPublishedResources()
        throws ApiGatewayException {
        Publication publishedPublication = createPublishedPublication(owner);
        Publication draftPublication = createDraftPublication(owner);

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
        return UserInstance.create(randomString(), publication.getPublisher().getId());
    }

    @Test
    void listDoiRequestsForPublishedResourcesDoesNotReturnDoiRequestsFromDifferentOrganization()
        throws ApiGatewayException {
        Publication publishedPublication = createPublishedPublication(owner);

        createDoiRequest(publishedPublication);

        UserInstance irrelevantCurator = UserInstance.create(randomString(), SOME_OTHER_PUBLISHER);
        List<DoiRequest> resultForIrrelevantUser =
            doiRequestService.listDoiRequestsForPublishedPublications(irrelevantCurator);
        assertThat(resultForIrrelevantUser, is(emptyCollectionOf(DoiRequest.class)));

        UserInstance relevantUser = createSampleCurator(publishedPublication);
        List<DoiRequest> resultForRelevantUser =
            doiRequestService.listDoiRequestsForPublishedPublications(relevantUser);

        DoiRequest expectedDoiRequest = getDoiRequest(publishedPublication);
        assertThat(resultForRelevantUser, hasItem(expectedDoiRequest));
    }

    @Test
    void listDoiRequestsForUserReturnsDoiRequestsWithStatusRequested() throws ApiGatewayException {
        Publication publication = createPublishedPublication(owner);
        createDoiRequest(publication);

        List<DoiRequest> result = doiRequestService.listDoiRequestsForUser(owner);

        var expectedDoiRequest =
            doiRequestService.getDoiRequestByResourceIdentifier(owner, publication.getIdentifier());

        assertThat(result, is(equalTo(List.of(expectedDoiRequest))));
    }

    @Test
    void listDoiRequestsForUserReturnsAllRelevantDoiRequests() throws ApiGatewayException {
        int endExclusive = 10;
        List<Publication> publications = IntStream.range(0, endExclusive).boxed()
            .map(attempt(i -> createDraftPublication(owner)))
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
    void updateDoiRequestUpdatesDoiRequestStatusInDatabase()
        throws ApiGatewayException {
        var publication = createPublishedPublication(owner);
        UserInstance userInstance = createUserInstance(publication);
        SortableIdentifier doiRequestIdentifier = doiRequestService
            .createDoiRequest(userInstance, publication.getIdentifier());

        DoiRequestStatus expectedNewDoiRequestStatus = DoiRequestStatus.APPROVED;
        doiRequestService.updateDoiRequest(userInstance, publication.getIdentifier(), expectedNewDoiRequestStatus);

        DoiRequest updatedDoiRequest = doiRequestService.getDoiRequest(userInstance, doiRequestIdentifier);
        assertThat(updatedDoiRequest.getStatus(), is(equalTo(expectedNewDoiRequestStatus)));
    }

    @Test
    void updateDoiRequestUpdatesModifiedDateOfDoiRequest()
        throws ApiGatewayException {
        Publication publication = createPublishedPublication(owner);
        resourceService.publishPublication(owner, publication.getIdentifier());
        SortableIdentifier doiRequestIdentifier = doiRequestService
            .createDoiRequest(owner, publication.getIdentifier());

        DoiRequestStatus expectedNewDoiRequestStatus = DoiRequestStatus.APPROVED;
        doiRequestService.updateDoiRequest(owner, publication.getIdentifier(), expectedNewDoiRequestStatus);

        DoiRequest updatedDoiRequest = doiRequestService.getDoiRequest(owner, doiRequestIdentifier);
        assertThat(updatedDoiRequest.getModifiedDate(), is(equalTo(DOI_REQUEST_UPDATE_TIME)));
    }

    @Test
    void updateDoiRequestUpdatesStatusIndexForDoiRequest()
        throws ApiGatewayException {
        var publication = createPublishedPublication(owner);
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
    void getDoiRequestThrowsNotFoundExceptionWhenDoiRequestWasNotFound() {
        UserInstance someUser = randomUserInstance();
        Executable action = () -> doiRequestService.getDoiRequest(someUser, SortableIdentifier.next());
        assertThrows(NotFoundException.class, action);
    }

    @Test
    void updateDoiRequestThrowsBadRequestExceptionWhenPublicationIsDraft()
        throws ApiGatewayException {
        Publication publication = createDraftPublication(owner);
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

    private DoiRequest expectedDoiRequestForEmptyPublication(Publication emptyPublication,
                                                             DoiRequest actualDoiRequest) {
        return DoiRequest.builder()
            .withIdentifier(actualDoiRequest.getIdentifier())
            .withResourceIdentifier(emptyPublication.getIdentifier())
            .withOwner(emptyPublication.getResourceOwner().getOwner())
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

    private Publication emptyPublication(UserInstance owner) throws ApiGatewayException {
        Publication publication = new Publication.Builder()
            .withResourceOwner(new ResourceOwner(owner.getUserIdentifier(), randomOrgUnitId()))
            .withPublisher(createOrganization(owner.getOrganizationUri()))
            .withStatus(PublicationStatus.DRAFT)
            .build();

        Publication emptyPublication = resourceService.createPublication(owner, publication);
        skipPublicationUpdate();

        return emptyPublication;
    }

    private Publication createPublishedPublication(UserInstance owner)
        throws ApiGatewayException {
        Publication publication = createPublicationForUser(owner);
        publication = resourceService.createPublication(owner, publication);
        resourceService.publishPublication(owner, publication.getIdentifier());
        return publication;
    }

    private void createDoiRequest(Publication publication)
        throws BadRequestException {
        doiRequestService.createDoiRequest(
            createUserInstance(publication), publication.getIdentifier());
    }

    private void createDoiRequest(Publication publication, UserInstance owner)
        throws BadRequestException {
        doiRequestService.createDoiRequest(owner, publication.getIdentifier());
    }

    private UserInstance createUserInstance(Publication publication) {
        return UserInstance.create(publication.getResourceOwner().getOwner(), publication.getPublisher().getId());
    }
}