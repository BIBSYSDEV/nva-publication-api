package no.unit.nva.publication.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mikael.urlbuilder.UrlBuilder;
import no.unit.nva.publication.MainHandler;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

public class ResourcePersistenceService {

    public static final String PATH = "/resource";
    public static final String ACCEPT = "Accept";
    public static final String APPLICATION_JSON = "application/json";
    public static final String AUTHORIZATION = "Authorization";
    private final HttpClient client;
    private final ObjectMapper objectMapper = MainHandler.createObjectMapper();

    protected ResourcePersistenceService(HttpClient client) {
        this.client = client;
    }

    public ResourcePersistenceService() {
        this(HttpClient.newHttpClient());
    }

    /**
     * Fetch Resource from Database.
     *
     * @param identifier  identifier
     * @param apiScheme  apiScheme
     * @param apiHost  apiHost
     * @param authorization authorization
     * @throws IOException IOException
     * @throws InterruptedException InterruptedException
     */

    public JsonNode fetchResource(UUID identifier, String apiScheme, String apiHost, String authorization)
            throws IOException, InterruptedException {

        URI uri = UrlBuilder.empty()
                .withScheme(apiScheme)
                .withHost(apiHost)
                .withPath(PATH)
                .withPath(identifier.toString())
                .toUri();

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(uri)
                .header(ACCEPT, APPLICATION_JSON)
                .header(AUTHORIZATION, authorization)
                .GET()
                .build();

        try {
            HttpResponse<String> httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            return objectMapper.readTree(httpResponse.body());
        } catch (IOException e) {
            System.out.println("Error communicating with remote service: " + uri.toString());
            throw e;
        }
    }

}