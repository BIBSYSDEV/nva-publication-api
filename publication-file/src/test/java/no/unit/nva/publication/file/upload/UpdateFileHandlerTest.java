package no.unit.nva.publication.file.upload;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomOpenFile;
import static no.unit.nva.publication.PublicationServiceConfig.dtoObjectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.s3.AmazonS3;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.associatedartifacts.file.PublisherVersion;
import no.unit.nva.model.instancetypes.journal.JournalArticle;
import no.unit.nva.publication.commons.customer.CustomerApiClient;
import no.unit.nva.publication.file.upload.restmodel.UpdateFileRequest;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UpdateFileHandlerTest extends ResourcesLocalTest {

    private static final Context CONTEXT = new FakeContext();
    private ByteArrayOutputStream output;
    private ResourceService resourceService;
    private UpdateFileHandler handler;

    @BeforeEach
    public void setUp() {
        super.init();
        this.output = new ByteArrayOutputStream();
        this.resourceService = getResourceServiceBuilder().build();
        this.handler = new UpdateFileHandler(
            new FileService(mock(AmazonS3.class), mock(CustomerApiClient.class), resourceService));
    }

    @Test
    void shouldReturnBadRequestWhenFileIdentifierInPathDoesNotMatchFileIdentifierInRequestBody() throws IOException {
        var publication = randomPublication(JournalArticle.class);
        var request = createNotSupportedRequest(publication.getIdentifier());

        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);

        assertEquals(HTTP_BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldReturnUnauthorizedWhenUserIsNotAuthorized() throws IOException {
        var publication = randomPublication(JournalArticle.class);
        var file = randomOpenFile();
        var request = createUnauthorizedRequest(file.getIdentifier(), publication.getIdentifier());

        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);

        assertEquals(HTTP_UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void shouldReturnForbiddenWhenUserHasNoPermissionsToUpdateFile() throws IOException, BadRequestException {
        var publication = randomPublication(JournalArticle.class);
        var userInstance = UserInstance.fromPublication(publication);
        var resource = Resource.fromPublication(publication).persistNew(resourceService, userInstance);
        var request = createRandomUserAuthorizedRequest(UUID.randomUUID(), resource.getIdentifier());

        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);

        assertEquals(HTTP_FORBIDDEN, response.getStatusCode());
    }

    @Test
    void shouldReturnNotFoundWhenPublicationDoesNotExist() throws IOException {
        var publication = randomPublication(JournalArticle.class);
        var userInstance = UserInstance.fromPublication(publication);
        var fileIdentifier = UUID.randomUUID();
        var request = createRequestForUserWithPermissions(fileIdentifier, publication.getIdentifier(), userInstance,
                                                          randomUpdateFileRequest(fileIdentifier));

        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);

        assertEquals(HTTP_NOT_FOUND, response.getStatusCode());
    }

    @Test
    void shouldReturnNotFoundWhenFileDoesNotExist() throws IOException, BadRequestException {
        var publication = randomPublication(JournalArticle.class);
        var userInstance = UserInstance.fromPublication(publication);
        var resource = Resource.fromPublication(publication).persistNew(resourceService, userInstance);
        var fileIdentifier = UUID.randomUUID();
        var request = createRequestForUserWithPermissions(fileIdentifier, resource.getIdentifier(), userInstance,
                                                          randomUpdateFileRequest(fileIdentifier));

        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);

        assertEquals(HTTP_NOT_FOUND, response.getStatusCode());
    }

    @Test
    void shouldReturnInternalServerErrorWhenUnexpectedErrorOccurs() throws IOException, NotFoundException {
        var publication = randomPublication(JournalArticle.class);
        var userInstance = UserInstance.fromPublication(publication);
        var handlerThrowingException = handlerThrowingExceptionOnFileUpdate(publication, userInstance);
        var fileIdentifier = UUID.randomUUID();
        var requestBody = randomUpdateFileRequest(fileIdentifier);
        var request = createRequestForUserWithPermissions(fileIdentifier, publication.getIdentifier(), userInstance,
                                                          requestBody);

        handlerThrowingException.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);

        assertEquals(HTTP_INTERNAL_ERROR, response.getStatusCode());
    }

    @Test
    void shouldReturnAcceptedWhenFileHasBeenUpdatedSuccessfully() throws IOException, BadRequestException {
        var publication = randomPublication();
        var userInstance = UserInstance.fromPublication(publication);
        var resource = Resource.fromPublication(publication).persistNew(resourceService, userInstance);
        var file = randomOpenFile();
        FileEntry.create(file, resource.getIdentifier(), userInstance).persist(resourceService);
        var requestBody = randomUpdateFileRequest(file.getIdentifier());
        var request = createRequestForUserWithPermissions(file.getIdentifier(), resource.getIdentifier(), userInstance,
                                                          requestBody);

        handler.handleRequest(request, output, CONTEXT);
        var response = GatewayResponse.fromOutputStream(output, Void.class);

        assertEquals(HTTP_ACCEPTED, response.getStatusCode());
    }

    private static Map<String, String> toPathParameters(UUID fileIdentifier, SortableIdentifier publicationIdentifier) {
        return Map.of("publicationIdentifier", publicationIdentifier.toString(), "fileIdentifier",
                      fileIdentifier.toString());
    }

    private UpdateFileHandler handlerThrowingExceptionOnFileUpdate(Publication publication, UserInstance userInstance)
        throws NotFoundException {
        resourceService = mock(ResourceService.class);
        when(resourceService.getResourceByIdentifier(publication.getIdentifier())).thenReturn(
            Resource.fromPublication(publication));
        when(resourceService.fetchFile(any())).thenReturn(
            Optional.of(FileEntry.create(randomOpenFile(), publication.getIdentifier(), userInstance)));
        doThrow(new RuntimeException()).when(resourceService).updateFile(any());
        return new UpdateFileHandler(
            new FileService(mock(AmazonS3.class), mock(CustomerApiClient.class), resourceService));
    }

    private InputStream createRequestForUserWithPermissions(UUID fileIdentifier,
                                                            SortableIdentifier publicationIdentifier,
                                                            UserInstance userInstance, UpdateFileRequest requestBody)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<UpdateFileRequest>(dtoObjectMapper).withBody(requestBody)
                   .withPathParameters(toPathParameters(fileIdentifier, publicationIdentifier))
                   .withUserName(userInstance.getUsername())
                   .withCurrentCustomer(userInstance.getCustomerId())
                   .withPersonCristinId(randomUri())
                   .withTopLevelCristinOrgId(userInstance.getTopLevelOrgCristinId())
                   .build();
    }

    private UpdateFileRequest randomUpdateFileRequest(UUID fileIdentifier) {
        return new UpdateFileRequest(fileIdentifier, randomUri(), PublisherVersion.ACCEPTED_VERSION, Instant.now(),
                                     randomString());
    }

    private InputStream createRandomUserAuthorizedRequest(UUID fileIdentifier, SortableIdentifier publicationIdentifier)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<UpdateFileRequest>(dtoObjectMapper).withBody(
                randomUpdateFileRequest(fileIdentifier))
                   .withPathParameters(toPathParameters(fileIdentifier, publicationIdentifier))
                   .withUserName(randomString())
                   .withCurrentCustomer(randomUri())
                   .withPersonCristinId(randomUri())
                   .withTopLevelCristinOrgId(randomUri())
                   .build();
    }

    private InputStream createUnauthorizedRequest(UUID fileIdentifier, SortableIdentifier publicationIdentifier)
        throws JsonProcessingException {

        return new HandlerRequestBuilder<UpdateFileRequest>(dtoObjectMapper).withBody(
                randomUpdateFileRequest(fileIdentifier))
                   .withPathParameters(toPathParameters(fileIdentifier, publicationIdentifier))
                   .build();
    }

    private InputStream createNotSupportedRequest(SortableIdentifier publicationIdentifier)
        throws JsonProcessingException {

        return new HandlerRequestBuilder<UpdateFileRequest>(dtoObjectMapper).withBody(
                randomUpdateFileRequest(UUID.randomUUID()))
                   .withPathParameters(toPathParameters(UUID.randomUUID(), publicationIdentifier))
                   .build();
    }
}