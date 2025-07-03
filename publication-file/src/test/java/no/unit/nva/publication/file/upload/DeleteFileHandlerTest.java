package no.unit.nva.publication.file.upload;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomOpenFile;
import static no.unit.nva.publication.PublicationServiceConfig.dtoObjectMapper;
import static no.unit.nva.publication.model.business.UserInstanceFixture.getDegreeAndFileCuratorFromPublication;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.instancetypes.journal.JournalArticle;
import no.unit.nva.publication.commons.customer.CustomerApiClient;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.testutils.HandlerRequestBuilder;
import no.unit.nva.testutils.RandomDataGenerator;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DeleteFileHandlerTest extends ResourcesLocalTest {

    private static final Context CONTEXT = mock(Context.class);
    private ResourceService resourceService;
    private ByteArrayOutputStream output;
    private DeleteFileHandler handler;

    @BeforeEach
    void setUp() {
        super.init();
        resourceService = getResourceService(client);
        var fileService = new FileService(mock(AmazonS3.class), mock(CustomerApiClient.class), resourceService);
        handler = new DeleteFileHandler(fileService, new Environment());
        output = new ByteArrayOutputStream();
    }

    @Test
    void shouldReturnUnauthorizedWhenUserIsNotAuthorized() throws IOException {
        var request = createUnauthorizedRequest(UUID.randomUUID(), SortableIdentifier.next());

        handler.handleRequest(request, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Void.class);

        assertEquals(HTTP_UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void shouldReturnAcceptedWhenPublicationDoesNotExist() throws IOException {
        var request = createAuthorizedRequestWithRandomUser(UUID.randomUUID(), SortableIdentifier.next());

        handler.handleRequest(request, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Void.class);

        assertEquals(HTTP_ACCEPTED, response.getStatusCode());
    }

    @Test
    void shouldReturnAcceptedWhenFileDoesNotExist() throws IOException, BadRequestException {
        var publication = randomPublication(JournalArticle.class);
        var userInstance = UserInstance.fromPublication(publication);
        var resource = Resource.fromPublication(publication).persistNew(resourceService, userInstance);
        var file = randomOpenFile();

        var request = createRequestForUserWithPermissions(file.getIdentifier(), resource.getIdentifier(), userInstance);

        handler.handleRequest(request, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Void.class);

        assertEquals(HTTP_ACCEPTED, response.getStatusCode());
    }

    @Test
    void shouldReturnForbiddenWhenUserHaNoPermissionToDeleteFile() throws IOException, BadRequestException {
        var publication = randomPublication(JournalArticle.class);
        var userInstance = UserInstance.fromPublication(publication);
        var resource = Resource.fromPublication(publication).persistNew(resourceService, userInstance);
        var file = randomOpenFile();
        FileEntry.create(file, resource.getIdentifier(), userInstance).persist(resourceService);

        var request = createAuthorizedRequestWithRandomUser(file.getIdentifier(), resource.getIdentifier());

        handler.handleRequest(request, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Void.class);

        assertEquals(HTTP_FORBIDDEN, response.getStatusCode());
    }

    @Test
    void shouldReturnInternalServerErrorWhenUnexpectedException() throws IOException, NotFoundException {
        var publication = randomPublication(JournalArticle.class);
        var curator = getDegreeAndFileCuratorFromPublication(publication);
        var request = createRequestForUserWithPermissions(UUID.randomUUID(), publication.getIdentifier(), curator);

        var handlerThrowingException = handlerThrowingExceptionOnFileUpdate(publication, curator);
        handlerThrowingException.handleRequest(request, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Void.class);

        assertEquals(HTTP_INTERNAL_ERROR, response.getStatusCode());
    }

    @Test
    void shouldReturnAcceptedWhenDeletingFile() throws IOException, BadRequestException {
        var publication = randomPublication(JournalArticle.class);
        var curator = getDegreeAndFileCuratorFromPublication(publication);
        var resource = Resource.fromPublication(publication).persistNew(resourceService, curator);
        var file = randomOpenFile();
        FileEntry.create(file, resource.getIdentifier(), curator).persist(resourceService);

        var request = createRequestForUserWithPermissions(file.getIdentifier(), resource.getIdentifier(), curator);

        handler.handleRequest(request, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Void.class);

        assertEquals(HTTP_ACCEPTED, response.getStatusCode());
    }

    private static Map<String, String> getPathParameters(UUID fileIdentifier,
                                                         SortableIdentifier publicationIdentifier) {
        return Map.of("publicationIdentifier", publicationIdentifier.toString(), "fileIdentifier",
                      fileIdentifier.toString());
    }

    private InputStream createRequestForUserWithPermissions(UUID fileIdentifier, SortableIdentifier resourceIdentifier,
                                                            UserInstance userInstance) throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(dtoObjectMapper).withPathParameters(
                getPathParameters(fileIdentifier, resourceIdentifier))
                   .withUserName(userInstance.getUsername())
                   .withCurrentCustomer(userInstance.getCustomerId())
                   .withTopLevelCristinOrgId(userInstance.getTopLevelOrgCristinId())
                   .withAccessRights(userInstance.getCustomerId(),
                                     userInstance.getAccessRights().toArray(AccessRight[]::new))
                   .withPersonCristinId(randomUri())
                   .build();
    }

    private InputStream createAuthorizedRequestWithRandomUser(UUID fileIdentifier,
                                                              SortableIdentifier publicationIdentifier)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(dtoObjectMapper).withPathParameters(
                getPathParameters(fileIdentifier, publicationIdentifier))
                   .withUserName(randomString())
                   .withCurrentCustomer(randomUri())
                   .withTopLevelCristinOrgId(RandomDataGenerator.randomUri())
                   .withPersonCristinId(randomUri())
                   .build();
    }

    private InputStream createUnauthorizedRequest(UUID fileIdentifier, SortableIdentifier publicationIdentifier)
        throws JsonProcessingException {

        return new HandlerRequestBuilder<Void>(dtoObjectMapper).withPathParameters(
            getPathParameters(fileIdentifier, publicationIdentifier)).build();
    }

    private DeleteFileHandler handlerThrowingExceptionOnFileUpdate(Publication publication, UserInstance userInstance)
        throws NotFoundException {
        resourceService = mock(ResourceService.class);
        when(resourceService.getResourceByIdentifier(publication.getIdentifier())).thenReturn(
            Resource.fromPublication(publication));
        when(resourceService.fetchFile(any())).thenReturn(
            Optional.of(FileEntry.create(randomOpenFile(), publication.getIdentifier(), userInstance)));
        doThrow(new RuntimeException()).when(resourceService).updateFile(any());
        return new DeleteFileHandler(
            new FileService(mock(AmazonS3.class), mock(CustomerApiClient.class), resourceService), new Environment());
    }
}