package no.unit.nva.publication.delete;

import static java.util.Collections.singletonMap;
import static java.util.Objects.nonNull;
import static no.unit.nva.model.testing.PublicationGenerator.randomEntityDescription;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.publication.PublicationRestHandlersTestConfig.restApiMapper;
import static no.unit.nva.publication.PublicationServiceConfig.ENVIRONMENT;
import static no.unit.nva.publication.RequestUtil.PUBLICATION_IDENTIFIER;
import static no.unit.nva.publication.testing.http.RandomPersonServiceResponse.randomUri;
import static no.unit.nva.testutils.HandlerRequestBuilder.CLIENT_ID_CLAIM;
import static no.unit.nva.testutils.HandlerRequestBuilder.ISS_CLAIM;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.apigateway.AccessRight.EDIT_OWN_INSTITUTION_RESOURCES;
import static nva.commons.apigateway.AccessRight.PUBLISH_DEGREE;
import static nva.commons.apigateway.ApiGatewayHandler.ALLOWED_ORIGIN_ENV;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Set;
import no.unit.nva.clients.GetExternalClientResponse;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.Reference;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
import no.unit.nva.model.instancetypes.degree.DegreePhd;
import no.unit.nva.model.instancetypes.degree.UnconfirmedDocument;
import no.unit.nva.model.instancetypes.journal.JournalArticle;
import no.unit.nva.model.pages.MonographPages;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UnpublishRequest;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import no.unit.nva.testutils.TestHeaders;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.zalando.problem.Problem;

class DeletePublicationHandlerTest extends ResourcesLocalTest {

    public static final String WILDCARD = "*";
    public static final String SOME_USER = "some_other_user";
    private static final String EXTERNAL_CLIENT_ID = "external-client-id";
    private static final String EXTERNAL_ISSUER = ENVIRONMENT.readEnv("EXTERNAL_USER_POOL_URI");
    private final Context context = new FakeContext();
    private DeletePublicationHandler handler;
    private ResourceService publicationService;
    private IdentityServiceClient identityServiceClient;
    private Environment environment;
    private ByteArrayOutputStream outputStream;
    private GetExternalClientResponse getExternalClientResponse;
    private TicketService ticketService;

    @BeforeEach
    public void setUp() throws NotFoundException {
        init();
        prepareEnvironment();
        prepareIdentityServiceClient();
        publicationService = new ResourceService(client, Clock.systemDefaultZone());
        ticketService = new TicketService(client);
        handler = new DeletePublicationHandler(publicationService, ticketService, environment, identityServiceClient);
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    void handleRequestReturnsAcceptedWhenOnDraftPublication() throws IOException, BadRequestException {

        var publication = createAndPersistPublication();

        InputStream inputStream = new HandlerRequestBuilder<Publication>(restApiMapper)
                                      .withHeaders(TestHeaders.getRequestHeaders())
                                      .withPathParameters(
                                          singletonMap(PUBLICATION_IDENTIFIER, publication.getIdentifier().toString()))
                                      .withUserName(publication.getResourceOwner().getOwner().getValue())
                                      .withCurrentCustomer(publication.getPublisher().getId())
                                      .build();

        handler.handleRequest(inputStream, outputStream, context);

        var gatewayResponse = GatewayResponse.fromOutputStream(outputStream, Void.class);
        assertEquals(SC_ACCEPTED, gatewayResponse.getStatusCode());
    }

    @Test
    void handleRequestReturnsAcceptedWhenOnDraftPublicationAndClientIsExternal() throws IOException,
                                                                                        BadRequestException {
        var createdPublication = createAndPersistPublicationWithExternalOwner();

        InputStream inputStream = new HandlerRequestBuilder<Publication>(restApiMapper)
                                      .withHeaders(TestHeaders.getRequestHeaders())
                                      .withPathParameters(singletonMap(PUBLICATION_IDENTIFIER,
                                                                       createdPublication.getIdentifier().toString()))
                                      .withAuthorizerClaim(ISS_CLAIM, EXTERNAL_ISSUER)
                                      .withAuthorizerClaim(CLIENT_ID_CLAIM, EXTERNAL_CLIENT_ID)
                                      .build();

        handler.handleRequest(inputStream, outputStream, context);

        var gatewayResponse = GatewayResponse.fromOutputStream(outputStream, Void.class);
        assertEquals(SC_ACCEPTED, gatewayResponse.getStatusCode());
    }

    @Test
    void handleRequestReturnsErrorWhenCallerIsNotOwnerOfPublication() throws IOException, ApiGatewayException {
        var createdPublication = createAndPersistPublication();

        InputStream inputStream = new HandlerRequestBuilder<Publication>(restApiMapper)
                                      .withHeaders(TestHeaders.getRequestHeaders())
                                      .withPathParameters(singletonMap(PUBLICATION_IDENTIFIER,
                                                                       createdPublication.getIdentifier().toString()))
                                      .withUserName(SOME_USER)
                                      .withCurrentCustomer(createdPublication.getPublisher().getId())
                                      .build();

        handler.handleRequest(inputStream, outputStream, context);

        var gatewayResponse = GatewayResponse.fromOutputStream(outputStream, Problem.class);
        // Return BadRequest because Dynamo cannot distinguish between the primary key (containing the user info)
        // being wrong or the status of the resource not being "DRAFT"
        assertEquals(SC_BAD_REQUEST, gatewayResponse.getStatusCode());
    }

    @Test
    void handleRequestReturnsErrorWhenCallerIsNotOwnerOfPublicationAndCalledIsExternalClient()
        throws IOException, BadRequestException {
        var createdPublication = createAndPersistPublication();

        InputStream inputStream = new HandlerRequestBuilder<Publication>(restApiMapper)
                                      .withHeaders(TestHeaders.getRequestHeaders())
                                      .withPathParameters(singletonMap(PUBLICATION_IDENTIFIER,
                                                                       createdPublication.getIdentifier().toString()))
                                      .withAuthorizerClaim(ISS_CLAIM, EXTERNAL_ISSUER)
                                      .withAuthorizerClaim(CLIENT_ID_CLAIM, EXTERNAL_CLIENT_ID)
                                      .build();

        handler.handleRequest(inputStream, outputStream, context);

        var gatewayResponse = GatewayResponse.fromOutputStream(outputStream, Problem.class);
        // Return BadRequest because Dynamo cannot distinguish between the primary key (containing the user info)
        // being wrong or the status of the resource not being "DRAFT"
        assertEquals(SC_BAD_REQUEST, gatewayResponse.getStatusCode());
    }

    @Test
    void handleRequestReturnsUnauthorizedWhenCallerIsMissingClientId()
        throws IOException, NotFoundException, BadRequestException {
        prepareIdentityServiceClientForNotFound();
        Publication createdPublication = createAndPersistPublication();

        InputStream inputStream = new HandlerRequestBuilder<Publication>(restApiMapper)
                                      .withHeaders(TestHeaders.getRequestHeaders())
                                      .withPathParameters(singletonMap(PUBLICATION_IDENTIFIER,
                                                                       createdPublication.getIdentifier().toString()))
                                      .withAuthorizerClaim(ISS_CLAIM, EXTERNAL_ISSUER)
                                      .build();

        handler.handleRequest(inputStream, outputStream, context);

        var gatewayResponse = GatewayResponse.fromOutputStream(outputStream, Problem.class);
        assertEquals(SC_UNAUTHORIZED, gatewayResponse.getStatusCode());
    }

    @Test
    void handleRequestReturnsBadRequestWhenAlreadyMarkedForDeletionPublication()
        throws IOException, ApiGatewayException {
        Publication publication = createAndPersistPublication();
        markForDeletion(publication);

        InputStream inputStream = new HandlerRequestBuilder<Publication>(restApiMapper)
                                      .withHeaders(TestHeaders.getRequestHeaders())
                                      .withPathParameters(
                                          singletonMap(PUBLICATION_IDENTIFIER, publication.getIdentifier().toString()))
                                      .withUserName(publication.getResourceOwner().getOwner().getValue())
                                      .withCurrentCustomer(publication.getPublisher().getId())
                                      .build();

        handler.handleRequest(inputStream, outputStream, context);

        var gatewayResponse = GatewayResponse.fromOutputStream(outputStream, Problem.class);
        assertEquals(HttpStatus.SC_BAD_REQUEST, gatewayResponse.getStatusCode());
    }

    @Test
    void shouldReturnUnauthorizedWhenMissingAccessRightsWhenUnpublishingPublication()
        throws IOException, ApiGatewayException {
        var publication = createAndPersistPublicationWithoutDoi(true);

        var inputStream = new HandlerRequestBuilder<Void>(restApiMapper)
                              .withUserName(randomString())
                              .withCurrentCustomer(randomUri())
                              .withAccessRights(null)
                              .withPathParameters(
                                  Map.of(PUBLICATION_IDENTIFIER, publication.getIdentifier().toString()))
                              .build();

        handler.handleRequest(inputStream, outputStream, context);

        var response = GatewayResponse.fromOutputStream(outputStream, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(SC_UNAUTHORIZED)));
    }

    @Test
    void shouldReturnUnauthorizedWhenUserIsNotPublicationOwnerWhenUnpublishingPublication() throws IOException,
                                                                                                   ApiGatewayException {
        var publication = createAndPersistPublicationWithoutDoi(true);

        var inputStream = createHandlerRequest(publication.getIdentifier(), randomString(), randomUri(),
                                               AccessRight.USER);
        handler.handleRequest(inputStream, outputStream, context);

        var response = GatewayResponse.fromOutputStream(outputStream, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(SC_UNAUTHORIZED)));
    }

    @Test
    void shouldReturnUnauthorizedWhenUserIsNotContributorWhenUnpublishingPublication()
        throws IOException, ApiGatewayException {

        var userId = randomUri();
        var userName = randomString();

        var publication = createPublicationWithoutDoiAndWithContributor(userId, userName);
        publicationService.publishPublication(UserInstance.fromPublication(publication), publication.getIdentifier());

        var inputStream = createHandlerRequest(publication.getIdentifier(), randomString(), randomUri(),
                                               AccessRight.USER, randomUri());
        handler.handleRequest(inputStream, outputStream, context);

        var response = GatewayResponse.fromOutputStream(outputStream, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(SC_UNAUTHORIZED)));
    }

    @Test
    void shouldSuccessWhenUserIsContributorWhenUnpublishingPublication()
        throws ApiGatewayException, IOException {

        var userCristinId = randomUri();
        var userName = randomString();

        var publication = createPublicationWithoutDoiAndWithContributor(userCristinId, userName);
        publicationService.publishPublication(UserInstance.fromPublication(publication), publication.getIdentifier());

        var inputStream = createHandlerRequest(publication.getIdentifier(), userName, randomUri(), AccessRight.USER,
                                               userCristinId);
        handler.handleRequest(inputStream, outputStream, context);

        var response = GatewayResponse.fromOutputStream(outputStream, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(SC_ACCEPTED)));
    }

    @Test
    void shouldReturnSuccessWhenUserIsResourceOwnerWhenUnpublishingPublication()
        throws ApiGatewayException, IOException {

        var userName = randomString();
        var institutionId = randomUri();

        var publication = createAndPersistPublicationWithoutDoiAndWithResourceOwner(userName, institutionId);
        publicationService.publishPublication(UserInstance.fromPublication(publication), publication.getIdentifier());

        var inputStream = createHandlerRequest(publication.getIdentifier(), userName, institutionId, AccessRight.USER);
        handler.handleRequest(inputStream, outputStream, context);

        var response = GatewayResponse.fromOutputStream(outputStream, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(SC_ACCEPTED)));
    }

    @Test
    void shouldReturnBadRequestWhenUnpublishingNotPublishedPublication() throws IOException, ApiGatewayException {
        var unpublishedPublication = createAndPersistPublicationWithoutDoi(false);
        var publisherUri = unpublishedPublication.getPublisher().getId();

        var inputStream = createHandlerRequest(unpublishedPublication.getIdentifier(), randomString(), publisherUri,
                                               AccessRight.EDIT_ALL_NON_DEGREE_RESOURCES);
        handler.handleRequest(inputStream, outputStream, context);

        var response = GatewayResponse.fromOutputStream(outputStream, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(SC_BAD_REQUEST)));
    }

    // TODO: Should this return 200 OK?
    @Test
    void shouldReturnNotFoundWhenPublicationDoesNotExist() throws IOException {
        var inputStream = createHandlerRequest(SortableIdentifier.next(), randomString(), randomUri(),
                                               AccessRight.USER);
        handler.handleRequest(inputStream, outputStream, context);

        var response = GatewayResponse.fromOutputStream(outputStream, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(SC_NOT_FOUND)));
    }

    @Test
    void shouldReturnBadRequestWhenDeletingUnsupportedPublicationStatus() throws BadRequestException, IOException {
        var publication = randomPublication().copy().withStatus(PublicationStatus.NEW).build();
        var persistedPublication = Resource.fromPublication(publication)
                                       .persistNew(publicationService, UserInstance.fromPublication(publication));

        var publisherUri = publication.getPublisher().getId();
        var inputStream = createHandlerRequest(persistedPublication.getIdentifier(), randomString(), publisherUri,
                                               AccessRight.EDIT_OWN_INSTITUTION_RESOURCES);
        handler.handleRequest(inputStream, outputStream, context);

        var response = GatewayResponse.fromOutputStream(outputStream, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(SC_BAD_REQUEST)));
    }

    @Test
    void shouldReturnSuccessAndUpdatePublicationStatusToDeletedWhenUserCanEditOwnInstitutionResources()
        throws IOException, ApiGatewayException {

        var publication = createAndPersistPublicationWithoutDoi(true);
        publicationService.publishPublication(UserInstance.fromPublication(publication), publication.getIdentifier());

        var publisherUri = publication.getPublisher().getId();
        var inputStream = createHandlerRequest(publication.getIdentifier(), randomString(), publisherUri,
                                               AccessRight.EDIT_OWN_INSTITUTION_RESOURCES);
        handler.handleRequest(inputStream, outputStream, context);

        var response = GatewayResponse.fromOutputStream(outputStream, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(SC_ACCEPTED)));

        var unpublishedPublication = publicationService.getPublication(publication);
        assertThat(unpublishedPublication.getStatus(), is(equalTo(PublicationStatus.UNPUBLISHED)));
    }

    @Test
    void shouldReturnSuccessWhenEditorUnpublishesDegree() throws ApiGatewayException, IOException {
        var publication = createAndPersistDegreeWithoutDoi();
        publicationService.publishPublication(UserInstance.fromPublication(publication), publication.getIdentifier());

        var publisherUri = publication.getPublisher().getId();
        var inputStream = createHandlerRequest(publication.getIdentifier(), randomString(), publisherUri,
                                               PUBLISH_DEGREE);
        handler.handleRequest(inputStream, outputStream, context);

        var response = GatewayResponse.fromOutputStream(outputStream, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(SC_ACCEPTED)));
    }

    @Test
    void shouldReturnUnauthorizedWhenNonEditorUnpublishesDegree() throws ApiGatewayException, IOException {
        var publication = createAndPersistDegreeWithoutDoi();
        publicationService.publishPublication(UserInstance.fromPublication(publication), publication.getIdentifier());

        var publisherUri = publication.getPublisher().getId();
        var inputStream = createHandlerRequest(publication.getIdentifier(), randomString(), publisherUri,
                                               AccessRight.USER);
        handler.handleRequest(inputStream, outputStream, context);

        var response = GatewayResponse.fromOutputStream(outputStream, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(SC_UNAUTHORIZED)));
    }

    @Test
    void shouldUpdateDeletedResourceWithDuplicateOfValueWhenResourceIsADuplicate()
        throws ApiGatewayException, IOException {
        var publication = createAndPersistDegreeWithoutDoi();
        publicationService.publishPublication(UserInstance.fromPublication(publication), publication.getIdentifier());
        var duplicate = SortableIdentifier.next();
        var request = createRequestWithDuplicateOfValue(publication.getIdentifier(),
                                                        randomString(),
                                                        publication.getPublisher().getId(),
                                                        PUBLISH_DEGREE,
                                                        duplicate);
        handler.handleRequest(request, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, Void.class);
        var updatedPublication = publicationService.getPublication(publication);
        String duplicateIdentifier = UriWrapper.fromUri(updatedPublication.getDuplicateOf()).getLastPathElement();

        assertThat(response.getStatusCode(), is(equalTo(SC_ACCEPTED)));
        assertThat(duplicateIdentifier, is(equalTo(duplicate.toString())));
    }

    @Test
    void shouldReturnSuccessWhenCuratorUnpublishesPublishedPublicationFromOwnInstitution()
        throws ApiGatewayException, IOException {

        var institutionId = randomUri();
        var curatorUsername = randomString();
        var curatorAccessRight = AccessRight.EDIT_OWN_INSTITUTION_RESOURCES;
        var resourceOwnerUsername = randomString();

        var publication = createAndPersistPublicationWithoutDoiAndWithResourceOwner(resourceOwnerUsername,
                                                                                    institutionId);
        publicationService.publishPublication(UserInstance.fromPublication(publication), publication.getIdentifier());

        var inputStream = createHandlerRequest(publication.getIdentifier(), curatorUsername, institutionId,
                                               curatorAccessRight);
        handler.handleRequest(inputStream, outputStream, context);

        var response = GatewayResponse.fromOutputStream(outputStream, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(SC_ACCEPTED)));
    }

    @Test
    void shouldReturnUnauthorizedWhenCuratorUnpublishesPublishedPublicationFromAnotherInstitution()
        throws IOException, ApiGatewayException {

        var curatorUsername = randomString();
        var curatorInstitutionId = randomUri();
        var curatorAccessRight = AccessRight.EDIT_OWN_INSTITUTION_RESOURCES;

        var resourceOwnerUsername = randomString();
        var resourceOwnerInstitutionId = randomUri();

        var publication = createAndPersistPublicationWithoutDoiAndWithResourceOwner(resourceOwnerUsername,
                                                                                    resourceOwnerInstitutionId);
        publicationService.publishPublication(UserInstance.fromPublication(publication), publication.getIdentifier());

        var inputStream = createHandlerRequest(publication.getIdentifier(), curatorUsername, curatorInstitutionId,
                                               curatorAccessRight);
        handler.handleRequest(inputStream, outputStream, context);

        var response = GatewayResponse.fromOutputStream(outputStream, Void.class);
        assertThat(response.getStatusCode(), is(equalTo(SC_UNAUTHORIZED)));
    }

    @Test
    void shouldPersistUnpublishRequestWhenDeletingPublishedPublication()
        throws ApiGatewayException, IOException {
        var publication = createAndPersistDegreeWithoutDoi();
        publicationService.publishPublication(UserInstance.fromPublication(publication), publication.getIdentifier());
        var publisherUri = publication.getPublisher().getId();
        var request = createHandlerRequest(publication.getIdentifier(), randomString(), publisherUri, PUBLISH_DEGREE);
        handler.handleRequest(request, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, Void.class);
        var persistedTicket = ticketService.fetchTicketByResourceIdentifier(publication.getPublisher().getId(),
                                                                            publication.getIdentifier(),
                                                                            UnpublishRequest.class);

        assertTrue(persistedTicket.isPresent());
        assertThat(response.getStatusCode(), is(equalTo(SC_ACCEPTED)));
    }

    @Test
    void shouldDeleteUnpublishedPublicationWhenUserIsEditor()
        throws ApiGatewayException, IOException {
        var publication = createUnpublishedPublication();

        var publisherUri = publication.getPublisher().getId();
        var request = createHandlerRequest(publication.getIdentifier(), randomString(),
                                           publisherUri, EDIT_OWN_INSTITUTION_RESOURCES);
        handler.handleRequest(request, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, Void.class);

        var deletePublication = publicationService.getPublication(publication);

        assertThat(response.getStatusCode(), is(equalTo(SC_ACCEPTED)));
        assertThat(deletePublication.getStatus(), is(equalTo(PublicationStatus.DELETED)));
        assertThat(deletePublication.getAssociatedArtifacts(), is(emptyIterable()));
    }

    private Publication createUnpublishedPublication() throws ApiGatewayException {
        var publication = createAndPersistDegreeWithoutDoi();
        publicationService.publishPublication(UserInstance.fromPublication(publication), publication.getIdentifier());
        publicationService.unpublishPublication(publicationService.getPublication(publication));
        return publicationService.getPublication(publication);
    }

    private InputStream createHandlerRequest(SortableIdentifier publicationIdentifier, String username,
                                            URI institutionId, AccessRight accessRight)
        throws JsonProcessingException {

        return createHandlerRequest(publicationIdentifier, username, institutionId, accessRight, null);
    }

    private InputStream createHandlerRequest(SortableIdentifier publicationIdentifier, String username,
                                             URI institutionId, AccessRight accessRight, URI cristinId)
        throws JsonProcessingException {
        var request = new HandlerRequestBuilder<Void>(restApiMapper)
                   .withUserName(username)
                   .withCurrentCustomer(institutionId)
                   .withAccessRights(institutionId, accessRight.name())
                   .withPathParameters(
                       Map.of(PUBLICATION_IDENTIFIER, publicationIdentifier.toString()));

        if (nonNull(cristinId)) {
            request.withPersonCristinId(cristinId);
        }

        return request.build();
    }

    private InputStream createRequestWithDuplicateOfValue(SortableIdentifier publicationIdentifier, String username,
                                                          URI institutionId, AccessRight accessRight,
                                                          SortableIdentifier duplicateOf)
        throws JsonProcessingException {
        var request = new HandlerRequestBuilder<Void>(restApiMapper)
                          .withUserName(username)
                          .withQueryParameters(Map.of("duplicate", duplicateOf.toString()))
                          .withCurrentCustomer(institutionId)
                          .withAccessRights(institutionId, accessRight.name())
                          .withPathParameters(Map.of(PUBLICATION_IDENTIFIER, publicationIdentifier.toString()));

        return request.build();
    }

    private void prepareIdentityServiceClient() throws NotFoundException {
        identityServiceClient = Mockito.mock(IdentityServiceClient.class);

        getExternalClientResponse = new GetExternalClientResponse(
            EXTERNAL_CLIENT_ID,
            "someone@123",
            randomUri(),
            randomUri()
        );
        when(identityServiceClient.getExternalClient(any())).thenReturn(getExternalClientResponse);
    }

    private void prepareIdentityServiceClientForNotFound() throws NotFoundException {
        identityServiceClient = Mockito.mock(IdentityServiceClient.class);
        when(identityServiceClient.getExternalClient(any())).thenThrow(NotFoundException.class);
    }

    private void prepareEnvironment() {
        environment = Mockito.mock(Environment.class);
        when(environment.readEnv(ALLOWED_ORIGIN_ENV)).thenReturn(WILDCARD);
    }

    private void markForDeletion(Publication publication) throws ApiGatewayException {
        UserInstance userInstance = UserInstance.create(publication.getResourceOwner().getOwner().getValue(),
                                                        publication.getPublisher().getId());
        publicationService.markPublicationForDeletion(userInstance, publication.getIdentifier());
    }

    private Publication createAndPersistPublication() throws BadRequestException {
        var publication = PublicationGenerator.randomPublication();
        var userInstance = UserInstance
                               .create(publication.getResourceOwner().getOwner().getValue(),
                                       publication.getPublisher().getId());
        return Resource.fromPublication(publication).persistNew(publicationService, userInstance);
    }

    private Publication createAndPersistPublicationWithExternalOwner() throws BadRequestException {
        var publication = PublicationGenerator.randomPublication();
        var owner = new ResourceOwner(
            new Username(getExternalClientResponse.getActingUser()),
            getExternalClientResponse.getCristinUrgUri()
        );
        var userInstance =
            UserInstance.create(owner, getExternalClientResponse.getCustomerUri());
        return Resource.fromPublication(publication).persistNew(publicationService, userInstance);
    }

    private Publication createAndPersistPublicationWithoutDoi(boolean shouldBePublished) throws ApiGatewayException {
        var publication = randomPublication().copy()
                              .withEntityDescription(randomEntityDescription(JournalArticle.class))
                              .withDoi(null).build();
        var persistedPublication = Resource.fromPublication(publication)
                                       .persistNew(publicationService, UserInstance.fromPublication(publication));

        if (shouldBePublished) {
            publicationService.publishPublication(UserInstance.fromPublication(persistedPublication),
                                                  persistedPublication.getIdentifier());
        }

        return persistedPublication;
    }

    private Publication createPublicationWithoutDoiAndWithContributor(URI contributorId, String contributorName)
        throws ApiGatewayException {

        var publication = randomPublication().copy()
                              .withEntityDescription(randomEntityDescription(JournalArticle.class))
                              .withDoi(null).build();

        var identity = new Identity.Builder().withName(contributorName).withId(contributorId).build();
        var contributor = new Contributor.Builder().withIdentity(identity).withRole(new RoleType(Role.CREATOR)).build();
        var entityDesc = publication.getEntityDescription().copy().withContributors(List.of(contributor)).build();
        var publicationWithContributor = publication.copy().withEntityDescription(entityDesc).build();

        return Resource.fromPublication(publicationWithContributor)
                   .persistNew(publicationService, UserInstance.fromPublication(publication));
    }

    private Publication createAndPersistDegreeWithoutDoi() throws BadRequestException {
        var publication = randomPublication().copy().withDoi(null).build();

        var degreePhd = new DegreePhd(new MonographPages(), new PublicationDate(),
                                      Set.of(new UnconfirmedDocument(randomString())));
        var reference = new Reference.Builder().withPublicationInstance(degreePhd).build();
        var entityDescription = publication.getEntityDescription().copy().withReference(reference).build();
        var publicationOfTypeDegree = publication.copy().withEntityDescription(entityDescription).build();

        return Resource.fromPublication(publicationOfTypeDegree)
                   .persistNew(publicationService, UserInstance.fromPublication(publication));
    }

    private Publication createAndPersistPublicationWithoutDoiAndWithResourceOwner(String userName, URI institution)
        throws BadRequestException {

        var publication = randomPublication().copy()
                              .withEntityDescription(randomEntityDescription(JournalArticle.class))
                              .withDoi(null)
                              .withResourceOwner(new ResourceOwner(new Username(userName), institution))
                              .withPublisher(new Organization.Builder().withId(institution).build())
                              .build();

        return Resource.fromPublication(publication)
                   .persistNew(publicationService, UserInstance.fromPublication(publication));
    }
}
