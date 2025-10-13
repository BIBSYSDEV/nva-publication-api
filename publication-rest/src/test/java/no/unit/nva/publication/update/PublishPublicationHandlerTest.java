package no.unit.nva.publication.update;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static java.net.HttpURLConnection.HTTP_BAD_GATEWAY;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static no.unit.nva.model.testing.PublicationGenerator.randomNonDegreePublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomOpenFile;
import static no.unit.nva.publication.PublicationServiceConfig.PUBLICATION_IDENTIFIER_PATH_PARAMETER_NAME;
import static no.unit.nva.publication.PublicationServiceConfig.dtoObjectMapper;
import static no.unit.nva.publication.model.business.PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_ONLY;
import static no.unit.nva.testutils.RandomDataGenerator.randomBoolean;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import no.unit.nva.clients.CustomerDto;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.PublishingService;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import no.unit.nva.testutils.RandomDataGenerator;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Problem;

class PublishPublicationHandlerTest extends ResourcesLocalTest {

    private Context context;
    private ByteArrayOutputStream output;
    private ResourceService resourceService;
    private TicketService ticketService;
    private PublishPublicationHandler handler;

    @BeforeEach
    void setUp() throws NotFoundException {
        super.init();
        context = new FakeContext();
        output = new ByteArrayOutputStream();
        resourceService = getResourceService(client);
        ticketService = getTicketService();
        var identityServiceClient = mock(IdentityServiceClient.class);
        when(identityServiceClient.getCustomerById(any())).thenReturn(
            customerWithWorkflow(REGISTRATOR_PUBLISHES_METADATA_ONLY.getValue()));
        var publishingService = new PublishingService(resourceService, ticketService,
                                                      identityServiceClient);
        handler = new PublishPublicationHandler(publishingService, new Environment());
    }

    @Test
    void shouldReturnUnauthorizedWhenUserIsNotAuthorized() throws IOException {
        var publicationIdentifier = SortableIdentifier.next();
        var request = createUnauthorizedRequest(publicationIdentifier);
        handler.handleRequest(request, output, context);

        var response = GatewayResponse.fromOutputStream(output, Problem.class);

        assertEquals(HTTP_UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void shouldReturnNotFoundWhenPublicationDoesNotExist() throws IOException {
        var publicationIdentifier = SortableIdentifier.next();
        var request = createRequestWithUserWithoutPermissions(publicationIdentifier);
        handler.handleRequest(request, output, context);

        var response = GatewayResponse.fromOutputStream(output, Problem.class);

        assertEquals(HTTP_NOT_FOUND, response.getStatusCode());
    }

    @Test
    void shouldReturnForbiddenWhenUserHasNoPermissionsToPublishPublication() throws IOException, BadRequestException {
        var publication = createPublication();
        var request = createRequestWithUserWithoutPermissions(publication.getIdentifier());
        handler.handleRequest(request, output, context);

        var response = GatewayResponse.fromOutputStream(output, Problem.class);

        assertEquals(HTTP_FORBIDDEN, response.getStatusCode());
    }

    @Test
    void shouldReturnBadRequestWhenPublishingNotPublishablePublication()
        throws IOException, BadRequestException, NotFoundException {
        var publication = createUnpublishablePublication();
        var request = createRequestWithUserWithPermissionsToPublishPublication(publication);
        handler.handleRequest(request, output, context);

        var response = GatewayResponse.fromOutputStream(output, Problem.class);

        assertEquals(HTTP_BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldAllowMoreThanMaxDynamodbTransactionsOnUpdate()
        throws IOException, BadRequestException {
        var pub = randomNonDegreePublication().copy().withStatus(PublicationStatus.DRAFT).build();
        var publication = Resource.fromPublication(pub)
                              .persistNew(resourceService, UserInstance.fromPublication(pub));

        for (int i = 0; i < 101; i++) {
            var userInstance = UserInstance.create(randomString(), randomUri());
            FileEntry.create(randomOpenFile(),
                             publication.getIdentifier(),
                             userInstance).persist(resourceService, userInstance);
        }

        var request = createRequestWithUserWithPermissionsToPublishPublication(publication);
        handler.handleRequest(request, output, context);

        var response = GatewayResponse.fromOutputStream(output, Void.class);

        assertEquals(HTTP_ACCEPTED, response.getStatusCode());
    }

    @Test
    void shouldReturnBadGatewayWhenUnexpectedExceptionOccurs()
        throws IOException, BadRequestException, NotFoundException {
        var publication = createPublication();
        var request = createRequestWithUserWithPermissionsToPublishPublication(publication);

        resourceService = mock(ResourceService.class);
        when(resourceService.getResourceByIdentifier(publication.getIdentifier())).thenReturn(
            Resource.fromPublication(publication));
        doThrow(new RuntimeException()).when(resourceService).updateResource(any(Resource.class), any());
        var publishPublicationHandler = new PublishPublicationHandler(
            new PublishingService(resourceService, ticketService, mock(IdentityServiceClient.class)),
            new Environment());
        publishPublicationHandler.handleRequest(request, output, context);

        var response = GatewayResponse.fromOutputStream(output, Problem.class);

        assertEquals(HTTP_BAD_GATEWAY, response.getStatusCode());
    }

    @Test
    void shouldReturnOkWhenPublishingPublication() throws IOException, BadRequestException {
        var publication = createPublication();
        var request = createRequestWithUserWithPermissionsToPublishPublication(publication);

        handler.handleRequest(request, output, context);

        var response = GatewayResponse.fromOutputStream(output, Void.class);

        assertEquals(HTTP_ACCEPTED, response.getStatusCode());
    }

    private static Map<String, String> publicationIdentifierPathParam(SortableIdentifier publicationIdentifier) {
        return Map.of(PUBLICATION_IDENTIFIER_PATH_PARAMETER_NAME, publicationIdentifier.toString());
    }

    private Publication createUnpublishablePublication() throws BadRequestException, NotFoundException {
        var publication = createPublication();
        var userInstance = UserInstance.fromPublication(publication);
        var resource = Resource.fromPublication(publication);
        resource.publish(resourceService, userInstance);
        resourceService.unpublishPublication(publication, userInstance);
        resourceService.terminateResource(resource.fetch(resourceService).orElseThrow(), userInstance);
        return publication;
    }

    private Publication createPublication() throws BadRequestException {
        Publication publication = randomNonDegreePublication();
        return Resource.fromPublication(publication)
                   .persistNew(resourceService, UserInstance.fromPublication(publication));
    }

    private InputStream createRequestWithUserWithoutPermissions(SortableIdentifier publicationIdentifier)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(dtoObjectMapper).withPathParameters(
                publicationIdentifierPathParam(publicationIdentifier))
                   .withCurrentCustomer(randomUri())
                   .withUserName(randomString())
                   .withPersonCristinId(randomUri())
                   .withTopLevelCristinOrgId(randomUri())
                   .build();
    }

    private InputStream createRequestWithUserWithPermissionsToPublishPublication(Publication publication)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(dtoObjectMapper).withPathParameters(
                publicationIdentifierPathParam(publication.getIdentifier()))
                   .withCurrentCustomer(publication.getPublisher().getId())
                   .withUserName(publication.getResourceOwner().getOwner().getValue())
                   .withPersonCristinId(randomUri())
                   .withTopLevelCristinOrgId(publication.getResourceOwner().getOwnerAffiliation())
                   .withAccessRights(publication.getPublisher().getId(), AccessRight.MANAGE_RESOURCES_STANDARD)
                   .build();
    }

    private InputStream createUnauthorizedRequest(SortableIdentifier publicationIdentifier)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(dtoObjectMapper).withPathParameters(
            publicationIdentifierPathParam(publicationIdentifier)).build();
    }

    private CustomerDto customerWithWorkflow(String workflow) {
        return new CustomerDto(RandomDataGenerator.randomUri(), UUID.randomUUID(), randomString(), randomString(),
                               randomString(), RandomDataGenerator.randomUri(),
                               workflow, randomBoolean(),
                               randomBoolean(), randomBoolean(), Collections.emptyList(),
                               new CustomerDto.RightsRetentionStrategy(randomString(),
                                                                       RandomDataGenerator.randomUri()), randomBoolean());
    }
}