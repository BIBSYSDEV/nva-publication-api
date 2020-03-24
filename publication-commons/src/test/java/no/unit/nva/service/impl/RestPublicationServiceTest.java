package no.unit.nva.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.Environment;
import no.unit.nva.PublicationHandler;
import no.unit.nva.model.Publication;
import no.unit.nva.service.PublicationService;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static no.unit.nva.service.impl.RestPublicationService.NOT_IMPLEMENTED;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RestPublicationServiceTest {

    public static final String PUBLICATION_JSON = "src/test/resources/publication.json";
    public static final String EMPTY_RESPONSE = "src/test/resources/empty_response.json";
    public static final String RESOURCE_RESPONSE = "src/test/resources/resource_response.json";

    public static final String SOME_API_KEY = "some api key";
    public static final String API_HOST = "example.org";
    public static final String API_SCHEME = "http";

    private ObjectMapper objectMapper = PublicationHandler.createObjectMapper();

    @Rule
    public final EnvironmentVariables environmentVariables
            = new EnvironmentVariables();

    @Mock
    private HttpClient client;

    @Mock
    private HttpResponse<String> response;

    @Test
    public void testDefaultConstructor() {
        environmentVariables.set("API_SCHEME", API_SCHEME);
        environmentVariables.set("API_HOST", API_HOST);
        PublicationService publicationService = RestPublicationService.create(HttpClient.newHttpClient(),
                new Environment());
        Assert.assertNotNull(publicationService);
    }

    @Test
    public void updatePublicationReturnsJsonObject() throws IOException, InterruptedException {

        Publication publication = getPublication();
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        when((response.body())).thenReturn(objectMapper.writeValueAsString(publication));
        when((response.statusCode())).thenReturn(200);

        PublicationService publicationService = new RestPublicationService(API_SCHEME, API_HOST, client);

        Assertions.assertThrows(NoSuchElementException.class, () -> publicationService.updatePublication(
                publication,
                SOME_API_KEY));
    }

    @Test
    public void updatePublicationReturnsNotFound() throws IOException, InterruptedException {

        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        when((response.body())).thenReturn("{\"message\": \"Forbidden\"}");
        when((response.statusCode())).thenReturn(403);

        PublicationService publicationService = new RestPublicationService(API_SCHEME, API_HOST, client);

        Publication publication = getPublication();
        Assertions.assertThrows(IOException.class, () -> publicationService.updatePublication(
                publication,
                SOME_API_KEY));
    }

    @Test
    public void getPublicationClientError() throws IOException, InterruptedException {
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenThrow(IOException.class);

        PublicationService publicationService = new RestPublicationService(API_SCHEME, API_HOST, client);

        Assertions.assertThrows(IOException.class, () -> publicationService.getPublication(
                UUID.randomUUID(),
                SOME_API_KEY
        ));
    }

    @Test
    public void updatePublicationClientError() throws IOException, InterruptedException {
        Publication publication = getPublication();
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenThrow(IOException.class);

        PublicationService publicationService = new RestPublicationService(API_SCHEME, API_HOST, client);

        Assertions.assertThrows(IOException.class, () -> publicationService.updatePublication(
                publication,
                SOME_API_KEY
        ));
    }

    @Test
    public void getPublicationReturnsJsonObject() throws IOException, InterruptedException {
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        when((response.body())).thenReturn(getResponse(RESOURCE_RESPONSE));

        PublicationService publicationService = new RestPublicationService(API_SCHEME, API_HOST, client);

        Optional<Publication> publication = publicationService.getPublication(
                UUID.randomUUID(),
                SOME_API_KEY
        );

        assertTrue(publication.isPresent());
        assertNotNull(publication.get());
    }

    @Test
    public void notImplementedMethodsThrowsRunTimeException() {
        PublicationService publicationService = new RestPublicationService(API_SCHEME, API_HOST, client);
        Assertions.assertThrows(RuntimeException.class, () ->  {
            publicationService.getPublicationsByOwner(null, null, null);
        }, NOT_IMPLEMENTED);
        Assertions.assertThrows(RuntimeException.class, () ->  {
            publicationService.getPublicationsByPublisher(null, null);
        }, NOT_IMPLEMENTED);
    }

    private String getResponse(String path) throws IOException {
        return Files.readString(Path.of(path));
    }

    private Publication getPublication() throws IOException {
        return objectMapper.readValue(new File(PUBLICATION_JSON), Publication.class);
    }
}
