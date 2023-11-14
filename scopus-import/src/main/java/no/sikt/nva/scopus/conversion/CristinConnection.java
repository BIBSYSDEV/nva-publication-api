package no.sikt.nva.scopus.conversion;

import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.Objects.isNull;
import static nva.commons.apigateway.MediaTypes.APPLICATION_JSON_LD;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Optional;
import no.sikt.nva.scopus.conversion.model.cristin.CristinOrganization;
import no.sikt.nva.scopus.conversion.model.cristin.CristinPerson;
import no.unit.nva.commons.json.JsonUtils;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Failure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CristinConnection {

    public static final String CRISTIN_PERSON_RESPONSE_ERROR = "Could not fetch cristin person: ";
    public static final String CRISTIN_ORGANIZATION_RESPONSE_ERROR = "Could not fetch cristin organization: ";
    public static final String QUERY_PARAM_DEPTH_NONE = "?depth=none";
    public static final String COULD_NOT_FETCH_ORGANIZATION = "Could not fetch organization: {}";
    public static final String ORGANIZATION_SUCCESSFULLY_FETCHED = "Organization successfully fetched: {}";
    public static final String ACCEPT = "Accept";
    public static final String CRISTIN_VERSION = "; version=2023-05-26";
    private static final Logger logger = LoggerFactory.getLogger(CristinConnection.class);
    private final HttpClient httpClient;

    public CristinConnection(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @JacocoGenerated
    public CristinConnection() {
        this(HttpClient.newBuilder().build());
    }

    public Optional<CristinPerson> getCristinPersonByCristinId(URI cristinPersonId) {
        return attempt(() -> createRequest(cristinPersonId))
                                             .map(this::getCristinResponse)
                                             .map(this::getBodyFromPersonResponse)
                                             .map(this::getCristinPersonResponse)
                                             .toOptional();
    }

    public CristinOrganization fetchCristinOrganizationByCristinId(URI cristinOrgId) {
        return isNull(cristinOrgId)
                   ? null
                   : attempt(() -> createOrganizationRequest(cristinOrgId))
                         .map(this::getCristinResponse)
                         .map(this::getBodyFromOrganizationResponse)
                         .map(this::convertToOrganization)
                         .orElse(this::loggExceptionAndReturnNull);
    }

    private CristinOrganization loggExceptionAndReturnNull(Failure<CristinOrganization> failure) {
        logger.info(COULD_NOT_FETCH_ORGANIZATION, failure.getException().toString());
        return null;
    }

    private String getBodyFromOrganizationResponse(HttpResponse<String> response) {
        if (response.statusCode() != HTTP_OK) {
            logger.info(CRISTIN_ORGANIZATION_RESPONSE_ERROR + response.statusCode());
            throw new RuntimeException();
        }
        return response.body();
    }

    private CristinOrganization convertToOrganization(String body) throws JsonProcessingException {
        var organization = JsonUtils.dtoObjectMapper.readValue(body, CristinOrganization.class);
        logger.info(ORGANIZATION_SUCCESSFULLY_FETCHED, organization.toJsonString());
        return organization;
    }

    private CristinPerson getCristinPersonResponse(String json) throws JsonProcessingException {
        return JsonUtils.singleLineObjectMapper.readValue(json, CristinPerson.class);
    }

    private String getBodyFromPersonResponse(HttpResponse<String> response) {
        if (response.statusCode() != HTTP_OK) {
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
                .header(ACCEPT, APPLICATION_JSON_LD.toString() + CRISTIN_VERSION)
                .GET()
                .build();
    }
}
