package no.unit.nva.publication.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mikael.urlbuilder.UrlBuilder;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.exception.ErrorResponseException;
import no.unit.nva.publication.exception.InputException;
import no.unit.nva.publication.exception.NoResponseException;
import no.unit.nva.publication.exception.NotFoundException;
import no.unit.nva.publication.exception.NotImplementedException;
import no.unit.nva.publication.model.PublicationSummary;
import no.unit.nva.publication.service.PublicationService;
import nva.commons.exceptions.ApiGatewayException;
import nva.commons.utils.Environment;
import org.apache.http.HttpStatus;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class RestPublicationService implements PublicationService {

    public static final String PATH = "/resource/";
    public static final String ACCEPT = "Accept";
    public static final String APPLICATION_JSON = "application/json";
    public static final String AUTHORIZATION = "Authorization";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String API_HOST_ENV = "API_HOST";
    public static final String API_SCHEME_ENV = "API_SCHEME";
    public static final String ITEMS_0 = "/Items/0";
    public static final String ERROR_COMMUNICATING_WITH_REMOTE_SERVICE = "Error communicating with remote service: ";
    public static final String ERROR_RESPONSE_FROM_REMOTE_SERVICE = "Error response from remote service: ";
    public static final String ERROR_MAPPING_PUBLICATION_TO_JSON = "Error mapping Publication to JSON";
    public static final String IDENTIFIERS_NOT_EQUAL = "Identifier in request parameters '%s' "
            + "is not equal to identifier in customer object '%s'";

    private final ObjectMapper objectMapper;
    private final HttpClient client;
    private final String apiScheme;
    private final String apiHost;

    /**
     * Constructor for RestPublicationService.
     *
     * @param objectMapper objectMapper
     * @param client    client
     * @param apiScheme apiScheme
     * @param apiHost   apiHost
     */
    public RestPublicationService(HttpClient client, ObjectMapper objectMapper, String apiScheme, String apiHost) {
        this.objectMapper = objectMapper;
        this.client = client;
        this.apiScheme = apiScheme;
        this.apiHost = apiHost;
    }

    /**
     * Constructor for RestPublicationService.
     *
     * @param client    client
     * @param objectMapper objectMapper
     * @param environment   environment
     */
    public RestPublicationService(HttpClient client, ObjectMapper objectMapper, Environment environment) {
        this(client, objectMapper, environment.readEnv(API_SCHEME_ENV), environment.readEnv(API_HOST_ENV));
    }

    @Override
    public Publication getPublication(UUID identifier, String authorization)
            throws ApiGatewayException {
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
                throw new NotFoundException("Publication not found for identifier: " + identifier);
            }

            return objectMapper.readValue(objectMapper.writeValueAsString(item0), Publication.class);
        } catch (Exception e) {
            throw new NoResponseException(ERROR_COMMUNICATING_WITH_REMOTE_SERVICE + uri.toString(), e);
        }
    }

    @Override
    public Publication updatePublication(UUID identifier, Publication publication, String authorization)
            throws ApiGatewayException {
        validateIdentifier(identifier, publication);
        System.out.println("Sending request to modify resource " + identifier.toString());
        publication.setModifiedDate(Instant.now());

        String body;
        try {
            body = objectMapper.writeValueAsString(publication);
        } catch (JsonProcessingException e) {
            throw new InputException(ERROR_MAPPING_PUBLICATION_TO_JSON, e);
        }

        System.out.println("Request body " + body);
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
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .build();

        try {
            HttpResponse<String> httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            System.out.println("Received response for modify resource request on " + identifier.toString());
            if (httpResponse.statusCode() == HttpStatus.SC_OK) {
                // resource API returns DynamoDB response and updated Publication
                return getPublication(identifier, authorization);
            } else {
                System.out.println(ERROR_RESPONSE_FROM_REMOTE_SERVICE + uri.toString());
                throw new ErrorResponseException(httpResponse.body());
            }
        } catch (Exception e) {
            throw new NoResponseException(ERROR_COMMUNICATING_WITH_REMOTE_SERVICE + uri.toString(), e);
        }
    }

    private void validateIdentifier(UUID identifier, Publication publication) throws InputException {
        if (!identifier.equals(publication.getIdentifier())) {
            String errorMessage = String.format(IDENTIFIERS_NOT_EQUAL, identifier, publication.getIdentifier());
            throw new InputException(errorMessage, null);
        }
    }

    @Override
    public List<PublicationSummary> getPublicationsByPublisher(URI publisherId, String authorization)
            throws ApiGatewayException {
        throw new NotImplementedException();
    }

    @Override
    public List<PublicationSummary> getPublicationsByOwner(String owner, URI publisherId, String authorization)
            throws ApiGatewayException {
        throw new NotImplementedException();
    }
}
