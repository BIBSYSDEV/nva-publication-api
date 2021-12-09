package no.unit.nva.publication.external.services;

import static com.google.common.net.HttpHeaders.ACCEPT;
import static java.util.Objects.nonNull;
import static no.unit.nva.publication.PublicationServiceConfig.API_HOST;
import static no.unit.nva.publication.PublicationServiceConfig.dtoObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.net.MediaType;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadGatewayException;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;

public class PersonApiClient {

    public static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().build();
    public static final String ERROR_WITH_PERSON_SERVICE_COMMINUCATION = "Error communicating with Person service";
    public static final String PERSON_SERVICE_RESPONSE = "Person Service response:";
    public static final String USER_AFFILIATIONS_FIELD = "/orgunitids";
    public static final String PATH_TO_PESON_SERVICE_PROXY = "person";
    private static final Logger logger = LoggerFactory.getLogger(PersonApiClient.class);
    private final HttpClient httpClient;

    public PersonApiClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @JacocoGenerated
    public PersonApiClient() {
        this.httpClient = HTTP_CLIENT;
    }

    public List<URI> fetchAffiliationsForUser(String feideId)
        throws IOException, InterruptedException, ApiGatewayException {
        var queryUri = createPersonServiceQuery(feideId);
        var response = sendQuery(queryUri);
        checkResponse(response);
        return extractUserAffiliations(response);
    }

    private void checkResponse(HttpResponse<String> response) throws BadGatewayException {
        if (!Objects.equals(HttpURLConnection.HTTP_OK, response.statusCode())) {
            var errorReport = generateErrorReport(response);
            logger.warn(PERSON_SERVICE_RESPONSE + errorReport);
            throw new BadGatewayException(ERROR_WITH_PERSON_SERVICE_COMMINUCATION);
        }
    }

    private Problem generateErrorReport(HttpResponse<String> response) {
        return Problem.builder()
            .withStatus(Status.valueOf(response.statusCode()))
            .withDetail(response.body())
            .build();
    }

    private HttpResponse<String> sendQuery(URI queryUri) throws IOException, InterruptedException {
        var httpRequest = HttpRequest.newBuilder(queryUri)
            .header(ACCEPT, MediaType.JSON_UTF_8.toString())
            .GET()
            .build();
        return httpClient.send(httpRequest, BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private URI createPersonServiceQuery(String feideId) {
        return new UriWrapper("https", API_HOST)
            .addChild(PATH_TO_PESON_SERVICE_PROXY)
            .addQueryParameter("feideid", feideId)
            .getUri();
    }

    private List<URI> extractUserAffiliations(HttpResponse<String> response) throws JsonProcessingException {
        return extractUserInformation(response)
            .map(details -> (ArrayNode) details.at(USER_AFFILIATIONS_FIELD))
            .map(this::toUriList)
            .orElse(Collections.emptyList());
    }

    private Optional<ObjectNode> extractUserInformation(HttpResponse<String> response) throws JsonProcessingException {
        var result = (ArrayNode) dtoObjectMapper.readTree(response.body());
        return nonNull(result) && !result.isEmpty()
                   ? Optional.of((ObjectNode) result.get(0))
                   : Optional.empty();
    }

    private List<URI> toUriList(ArrayNode affiliations) {
        List<URI> uris = new ArrayList<>();
        affiliations.forEach(uriString -> uris.add(URI.create(uriString.textValue())));
        return uris;
    }
}
