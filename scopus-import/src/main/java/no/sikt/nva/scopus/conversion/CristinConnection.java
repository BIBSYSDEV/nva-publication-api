package no.sikt.nva.scopus.conversion;

import static java.util.Objects.isNull;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Optional;
import no.sikt.nva.scopus.conversion.model.cristin.Organization;
import no.sikt.nva.scopus.conversion.model.cristin.Person;
import no.unit.nva.commons.json.JsonUtils;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CristinConnection {

    public static final String CRISTIN_PERSON_RESPONSE_ERROR = "Could not fetch cristin person: ";
    public static final String CRISTIN_ORGANIZATION_RESPONSE_ERROR = "Could not fetch cristin organization: ";
    private static final Logger logger = LoggerFactory.getLogger(CristinConnection.class);
    public static final String QUERY_PARAM_DEPTH_NONE = "?depth=none";
    private final HttpClient httpClient;

    public CristinConnection(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @JacocoGenerated
    public CristinConnection() {
        this(HttpClient.newBuilder().build());
    }

    public Optional<Person> getCristinPersonByCristinId(URI cristinPersonId) {
        return isNull(cristinPersonId)
                   ? Optional.empty()
                   : Optional.ofNullable(attempt(() -> createRequest(cristinPersonId))
                                             .map(this::getCristinResponse)
                                             .map(this::getBodyFromPersonResponse)
                                             .map(this::getCristinPersonResponse)
                                             .orElse(failure -> null));
    }

    public Organization getCristinOrganizationByCristinId(URI cristinOrgId) {
        return isNull(cristinOrgId)
                   ? null
                   : attempt(() -> createOrganizationRequest(cristinOrgId))
                         .map(this::getCristinResponse)
                         .map(this::getBodyFromOrganizationResponse)
                         .map(this::convertToOrganization)
                         .orElse(failure -> null);
    }

    private String getBodyFromOrganizationResponse(HttpResponse<String> response) {
        if (response.statusCode() != HttpURLConnection.HTTP_OK) {
            logger.info(CRISTIN_ORGANIZATION_RESPONSE_ERROR + response.statusCode());
            throw new RuntimeException();
        }
        return response.body();
    }

    private Organization convertToOrganization(String body) throws JsonProcessingException {
        return new ObjectMapper().readValue(body, Organization.class);
    }

    private Person getCristinPersonResponse(String json) throws JsonProcessingException {
        return JsonUtils.singleLineObjectMapper.readValue(json, Person.class);
    }

    private String getBodyFromPersonResponse(HttpResponse<String> response) {
        if (response.statusCode() != HttpURLConnection.HTTP_OK) {
            logger.info(CRISTIN_PERSON_RESPONSE_ERROR + response.statusCode());
            throw new RuntimeException();
        }
        return response.body();
    }

    private HttpResponse<String> getCristinResponse(HttpRequest httpRequest) throws IOException, InterruptedException {
        return httpClient.send(httpRequest, BodyHandlers.ofString());
    }

    private HttpRequest createRequest(URI uri) {
        return HttpRequest.newBuilder()
                   .uri(uri)
                   .GET()
                   .build();
    }

    private HttpRequest createOrganizationRequest(URI uri) {
        var organizationUri = URI.create(uri + QUERY_PARAM_DEPTH_NONE);
        return HttpRequest.newBuilder()
                   .uri(organizationUri)
                   .GET()
                   .build();
    }
}
