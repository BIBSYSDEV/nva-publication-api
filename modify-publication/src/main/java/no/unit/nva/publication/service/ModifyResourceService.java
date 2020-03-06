package no.unit.nva.publication.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mikael.urlbuilder.UrlBuilder;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.ModifyPublicationHandler;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.UUID;

import static no.unit.nva.Logger.log;

public class ModifyResourceService {

    public static final String PATH = "/resource";
    public static final String APPLICATION_JSON = "application/json";
    public static final String AUTHORIZATION = "Authorization";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String ACCEPT = "Accept";
    private static final int OK = 200;
    private final HttpClient client;
    private final ObjectMapper objectMapper = ModifyPublicationHandler.createObjectMapper();

    protected ModifyResourceService(HttpClient client) {
        this.client = client;
    }

    public ModifyResourceService() {
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

    public JsonNode modifyResource(UUID identifier, Publication publication, String apiScheme, String apiHost,
                                   String authorization)
            throws IOException, InterruptedException {

        log("Sending request to modify resource " + identifier.toString());

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
                .header(ACCEPT, APPLICATION_JSON)
                .header(CONTENT_TYPE, APPLICATION_JSON)
                .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(publication)))
                .build();

        try {
            HttpResponse<String> httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            log("Received response for modify resource request on " + identifier.toString());
            if (httpResponse.statusCode() == OK) {
                return objectMapper.readTree(httpResponse.body());
            } else {
                log("Error response from remote service: " + uri.toString());
                throw new IOException(httpResponse.body());
            }
        } catch (IOException e) {
            log("Error communicating with remote service: " + uri.toString());
            throw e;
        }
    }

}