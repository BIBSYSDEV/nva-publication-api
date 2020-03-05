package no.unit.nva.publication.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mikael.urlbuilder.UrlBuilder;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.MainHandler;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.UUID;

public class ResourcePersistenceService {

    public static final String PATH = "/resource";
    public static final String APPLICATION_JSON = "application/json";
    public static final String AUTHORIZATION = "Authorization";
    public static final String CONTENT_TYPE = "Content-Type";
    private final HttpClient client;
    private final ObjectMapper objectMapper = MainHandler.createObjectMapper();

    protected ResourcePersistenceService(HttpClient client) {
        this.client = client;
    }

    public ResourcePersistenceService() {
        this(HttpClient.newHttpClient());
    }

    /**
     * Modify Resource in Database.
     *
     * @param identifier  identifier
     * @param apiScheme  apiScheme
     * @param apiHost  apiHost
     * @param authorization authorization
     * @throws IOException IOException
     * @throws InterruptedException InterruptedException
     */

    public void modifyResource(UUID identifier, Publication publication, String apiScheme, String apiHost,
                               String authorization)
            throws IOException, InterruptedException {

        publication.setModifiedDate(Instant.now());

        URI uri = UrlBuilder.empty()
                .withScheme(apiScheme)
                .withHost(apiHost)
                .withPath(PATH)
                .withPath(identifier.toString())
                .toUri();

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(uri)
                .header(AUTHORIZATION, authorization)
                .header(CONTENT_TYPE, APPLICATION_JSON)
                .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(publication)))
                .build();

        try {
            client.send(httpRequest, HttpResponse.BodyHandlers.discarding());
        } catch (IOException e) {
            System.out.println("Error communicating with remote service: " + uri.toString());
            throw e;
        }
    }

}