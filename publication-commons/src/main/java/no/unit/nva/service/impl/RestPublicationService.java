package no.unit.nva.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mikael.urlbuilder.UrlBuilder;
import no.unit.nva.Environment;
import no.unit.nva.PublicationHandler;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationSummary;
import no.unit.nva.service.PublicationService;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static no.unit.nva.Logger.log;
import static no.unit.nva.PublicationHandler.ENVIRONMENT_VARIABLE_NOT_SET;

public class RestPublicationService implements PublicationService {

    public static final String PATH = "/resource/";
    public static final String ACCEPT = "Accept";
    public static final String APPLICATION_JSON = "application/json";
    public static final String AUTHORIZATION = "Authorization";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String API_HOST_ENV = "API_HOST";
    public static final String API_SCHEME_ENV = "API_SCHEME";
    public static final String ITEMS_0 = "/Items/0";
    private static final int OK = 200;
    public static final String ERROR_COMMUNICATING_WITH_REMOTE_SERVICE = "Error communicating with remote service: ";
    public static final String ERROR_RESPONSE_FROM_REMOTE_SERVICE = "Error response from remote service: ";
    public static final String NOT_IMPLEMENTED = "Not implemented";
    private final HttpClient client;
    private final String apiScheme;
    private final String apiHost;
    private final ObjectMapper objectMapper = PublicationHandler.createObjectMapper();

    /**
     * Constructor for RestPublicationService.
     *
     * @param apiScheme apiScheme
     * @param apiHost   apiHost
     * @param client    client
     */
    public RestPublicationService(String apiScheme, String apiHost, HttpClient client) {
        this.client = client;
        this.apiScheme = apiScheme;
        this.apiHost = apiHost;
    }

    /**
     * Creator helper method for RestPublicationService.
     *
     * @param httpClient    httpClient
     * @param environment   environment
     * @return  RestPublicationService
     */
    public static RestPublicationService create(HttpClient httpClient, Environment environment) {
        String apiHost = environment.get(API_HOST_ENV)
                .orElseThrow(() -> new IllegalStateException(ENVIRONMENT_VARIABLE_NOT_SET + API_HOST_ENV));
        String apiScheme = environment.get(API_SCHEME_ENV)
                .orElseThrow(() -> new IllegalStateException(ENVIRONMENT_VARIABLE_NOT_SET + API_SCHEME_ENV));
        return new RestPublicationService(apiScheme, apiHost, httpClient);
    }

    @Override
    public Optional<Publication> getPublication(UUID identifier, String authorization)
            throws IOException, InterruptedException {
        URI uri = UrlBuilder.empty()
                .withScheme(apiScheme)
                .withHost(apiHost)
                .withPath(PATH + identifier.toString())
                .toUri();

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(uri)
                .header(ACCEPT, APPLICATION_JSON)
                .header(AUTHORIZATION, authorization)
                .GET()
                .build();

        try {
            HttpResponse<String> httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            JsonNode jsonNode = objectMapper.readTree(httpResponse.body());
            JsonNode item0 = jsonNode.at(ITEMS_0);
            if (item0.isMissingNode()) {
                return Optional.empty();
            }

            return Optional.of(objectMapper.readValue(objectMapper.writeValueAsString(item0), Publication.class));
        } catch (IOException e) {
            log(ERROR_COMMUNICATING_WITH_REMOTE_SERVICE + uri.toString());
            throw e;
        }
    }

    @Override
    public Publication updatePublication(Publication publication, String authorization)
            throws IOException, InterruptedException {
        UUID identifier = publication.getIdentifier();

        log("Sending request to modify resource " + identifier.toString());

        publication.setModifiedDate(Instant.now());

        log("Request body " + objectMapper.writeValueAsString(publication));

        URI uri = UrlBuilder.empty()
                .withScheme(apiScheme)
                .withHost(apiHost)
                .withPath(PATH + identifier.toString())
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
                // resource API returns DynamoDB response and updated Publication
                return getPublication(identifier, authorization).get();
            } else {
                log(ERROR_RESPONSE_FROM_REMOTE_SERVICE + uri.toString());
                throw new IOException(httpResponse.body());
            }
        } catch (IOException e) {
            log(ERROR_COMMUNICATING_WITH_REMOTE_SERVICE + uri.toString());
            throw e;
        }
    }

    @Override
    public List<PublicationSummary> getPublicationsByPublisher(String publisherId, String authorization) {
        throw new RuntimeException(NOT_IMPLEMENTED);
    }

    @Override
    public List<PublicationSummary> getPublicationsByOwner(String owner, String publisherId, String authorization) {
        throw new RuntimeException(NOT_IMPLEMENTED);
    }
}
