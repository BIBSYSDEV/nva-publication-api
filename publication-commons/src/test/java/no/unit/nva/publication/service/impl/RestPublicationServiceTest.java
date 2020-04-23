package no.unit.nva.publication.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.ObjectMapperConfig;
import no.unit.nva.publication.exception.InputException;
import no.unit.nva.publication.exception.NoResponseException;
import no.unit.nva.publication.exception.NotImplementedException;
import no.unit.nva.publication.service.PublicationService;
import nva.commons.exceptions.ApiGatewayException;
import nva.commons.utils.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RestPublicationServiceTest {

    public static final String PUBLICATION_JSON = "src/test/resources/publication.json";
    public static final String RESOURCE_RESPONSE = "src/test/resources/resource_response.json";

    public static final String SOME_API_KEY = "some api key";
    public static final String API_HOST = "example.org";
    public static final String API_SCHEME = "http";
    public static final String NO_ITEMS = "{ \"Items\": [] }";

    private HttpClient client;
    private HttpResponse<String> response;
    private Environment environment;
    private ObjectMapper objectMapper = ObjectMapperConfig.objectMapper;

    /**
     * Set up environment.
     */
    @BeforeEach
    public void setUp() {
        client = mock(HttpClient.class);
        response = mock(HttpResponse.class);
        environment = mock(Environment.class);
    }

    @Test
    @DisplayName("calling Constructor With All Env")
    public void callingConstructorWithAllEnv() {
        Environment environment = mock(Environment.class);
        when(environment.readEnv(RestPublicationService.API_SCHEME_ENV)).thenReturn(API_SCHEME);
        when(environment.readEnv(RestPublicationService.API_HOST_ENV)).thenReturn(API_HOST);
        new RestPublicationService(client, objectMapper, environment);
    }

    @Test
    @DisplayName("update Publication Returns Json Object")
    public void updatePublicationReturnsJsonObject() throws IOException, InterruptedException, ApiGatewayException {

        HttpResponse<String> putResponse = mock(HttpResponse.class);
        when((putResponse.body())).thenReturn(getResponse(PUBLICATION_JSON));
        when((putResponse.statusCode())).thenReturn(200);
        HttpResponse<String> getResponse = mock(HttpResponse.class);
        when((getResponse.body())).thenReturn(getResponse(RESOURCE_RESPONSE));
        when((getResponse.statusCode())).thenReturn(200);
        Publication publication = getPublication();

        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(putResponse)
                .thenReturn(getResponse);

        PublicationService publicationService = new RestPublicationService(client, objectMapper, API_SCHEME, API_HOST);

        publicationService.updatePublication(
                publication.getIdentifier(),
                publication,
                SOME_API_KEY);
    }

    @Test
    @DisplayName("when updatePublication receives Forbidden it throws NoResponseException")
    public void updatePublicationThrowsNoResponseExceptionOnNotFound() throws IOException, InterruptedException {

        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        when((response.body())).thenReturn("{\"message\": \"Forbidden\"}");
        when((response.statusCode())).thenReturn(403);

        PublicationService publicationService = new RestPublicationService(client, objectMapper, API_SCHEME, API_HOST);

        Publication publication = getPublication();
        assertThrows(NoResponseException.class, () -> publicationService.updatePublication(
                publication.getIdentifier(),
                publication,
                SOME_API_KEY));
    }

    @Test
    @DisplayName("when client get an error the error is propagated to the response")
    public void getPublicationClientError() throws IOException, InterruptedException {
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenThrow(IOException.class);

        PublicationService publicationService = new RestPublicationService(client, objectMapper, API_SCHEME, API_HOST);

        assertThrows(NoResponseException.class, () -> publicationService.getPublication(
                UUID.randomUUID(),
                SOME_API_KEY
        ));
    }

    @Test
    public void updatePublicationWithDifferentIdentifiersThrowsException() throws IOException, InterruptedException {
        Publication publication = getPublication();

        PublicationService publicationService = new RestPublicationService(client, objectMapper, API_SCHEME, API_HOST);

        assertThrows(InputException.class, () -> publicationService.updatePublication(
                UUID.randomUUID(),
                publication,
                SOME_API_KEY
        ));
    }

    @Test
    public void updatePublicationWithInvalidJsonPublicationThrowsException() throws IOException {
        Publication publication = getPublication();
        ObjectMapper failingObjectMapper = mock(ObjectMapper.class);
        when(failingObjectMapper.writeValueAsString(publication)).thenThrow(JsonProcessingException.class);
        PublicationService publicationService = new RestPublicationService(
                client, failingObjectMapper, API_SCHEME, API_HOST);

        assertThrows(InputException.class, () -> publicationService.updatePublication(
                publication.getIdentifier(),
                publication,
                SOME_API_KEY
        ));
    }

    @Test
    @DisplayName("when client get an error the error is propagated to the response")
    public void updatePublicationClientError() throws IOException, InterruptedException {
        Publication publication = getPublication();
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenThrow(IOException.class);

        PublicationService publicationService = new RestPublicationService(client, objectMapper, API_SCHEME, API_HOST);

        assertThrows(NoResponseException.class, () -> publicationService.updatePublication(
                publication.getIdentifier(),
                publication,
                SOME_API_KEY
        ));
    }

    @Test
    @DisplayName("when client receives a non empty json object it sends it to the response body")
    public void getPublicationReturnsJsonObject() throws IOException, InterruptedException, ApiGatewayException {
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        when((response.body())).thenReturn(getResponse(RESOURCE_RESPONSE));

        PublicationService publicationService = new RestPublicationService(client, objectMapper, API_SCHEME, API_HOST);

        Publication publication = publicationService.getPublication(
                UUID.randomUUID(),
                SOME_API_KEY
        );

        assertNotNull(publication);
    }

    @Test
    @DisplayName("when publication has no items it returns an empty response")
    public void getPublicationNoItems() throws IOException, InterruptedException, ApiGatewayException {
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        when((response.body())).thenReturn(NO_ITEMS);

        PublicationService publicationService = new RestPublicationService(client, objectMapper, API_SCHEME, API_HOST);

        assertThrows(NoResponseException.class, () -> publicationService.getPublication(
                UUID.randomUUID(),
                SOME_API_KEY
        ));
    }

    @Test
    @DisplayName("notImplemented Methods Throws Run Time Exception")
    public void notImplementedMethodsThrowsRunTimeException() {
        PublicationService publicationService = new RestPublicationService(client, objectMapper, API_SCHEME, API_HOST);
        assertThrows(NotImplementedException.class, () ->  {
            publicationService.getPublicationsByOwner(null, null, null);
        });
        assertThrows(NotImplementedException.class, () ->  {
            publicationService.getPublicationsByPublisher(null, null);
        });
    }

    private String getResponse(String path) throws IOException {
        return Files.readString(Path.of(path));
    }

    private Publication getPublication() throws IOException {
        return objectMapper.readValue(new File(PUBLICATION_JSON), Publication.class);
    }
}
