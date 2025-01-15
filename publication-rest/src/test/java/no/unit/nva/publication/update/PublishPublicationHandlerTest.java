package no.unit.nva.publication.update;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static java.net.HttpURLConnection.HTTP_BAD_GATEWAY;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static no.unit.nva.model.testing.PublicationGenerator.randomNonDegreePublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.publication.PublicationServiceConfig.PUBLICATION_IDENTIFIER_PATH_PARAMETER_NAME;
import static no.unit.nva.publication.PublicationServiceConfig.dtoObjectMapper;
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
import java.util.Map;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Problem;

class PublishPublicationHandlerTest extends ResourcesLocalTest {

    private Context context;
    private ByteArrayOutputStream output;
    private ResourceService resourceService;
    private PublishPublicationHandler handler;

    @BeforeEach
    void setUp() {
        super.init();
        context = new FakeContext();
        output = new ByteArrayOutputStream();
        resourceService = getResourceServiceBuilder().build();
        handler = new PublishPublicationHandler(resourceService);
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
    void shouldReturnBadGatewayWhenUnexpectedExceptionOccurs()
        throws IOException, BadRequestException, NotFoundException {
        var publication = createPublication();
        var request = createRequestWithUserWithPermissionsToPublishPublication(publication);

        resourceService = mock(ResourceService.class);
        when(resourceService.getResourceByIdentifier(publication.getIdentifier())).thenReturn(Resource.fromPublication(publication));
        doThrow(new RuntimeException()).when(resourceService).updateResource(any(Resource.class));
        var publishPublicationHandler = new PublishPublicationHandler(resourceService);
        publishPublicationHandler.handleRequest(request, output, context);

        var response = GatewayResponse.fromOutputStream(output, Problem.class);

        assertEquals(HTTP_BAD_GATEWAY, response.getStatusCode());
    }

    @Test
    void shouldReturnOkWhenPublishingPublication()
        throws IOException, BadRequestException {
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
        UserInstance userInstance = UserInstance.fromPublication(publication);
        Resource.fromPublication(publication).publish(resourceService, userInstance);
        resourceService.unpublishPublication(publication, userInstance);
        resourceService.deletePublication(publication, userInstance);
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

    private InputStream createRequestWithUserWithPermissionsToPublishPublication(Publication publication) throws JsonProcessingException {
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
}