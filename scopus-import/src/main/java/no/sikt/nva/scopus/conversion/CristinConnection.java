package no.sikt.nva.scopus.conversion;

import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.Objects.isNull;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Optional;
import no.sikt.nva.scopus.conversion.model.cristin.CristinPerson;
import no.sikt.nva.scopus.conversion.model.cristin.SearchOrganizationResponse;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.expansion.model.cristin.CristinOrganization;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Failure;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CristinConnection {

    private static final String CRISTIN_PERSON_RESPONSE_ERROR = "Could not fetch cristin person: ";
    private static final String CRISTIN_ORGANIZATION_RESPONSE_ERROR = "Could not fetch cristin organization: ";
    private static final String COULD_NOT_FETCH_ORGANIZATION = "Could not fetch organization: {}";
    private static final String ORGANIZATION_SUCCESSFULLY_FETCHED = "Organization successfully fetched: {}";
    private static final String CRISTIN = "cristin";
    private static final String PERSON = "person";
    private static final Logger logger = LoggerFactory.getLogger(CristinConnection.class);
    private static final String API_HOST = "API_HOST";
    private static final String ORGANIZATION = "organization";
    private static final String APPLICATION_JSON = "application/json";
    private static final String QUERY = "query";
    private final HttpClient httpClient;
    private final Environment environment;

    public CristinConnection(HttpClient httpClient, Environment environment) {
        this.httpClient = httpClient;
        this.environment = environment;
    }

    @JacocoGenerated
    public CristinConnection() {
        this(HttpClient.newBuilder().build(), new Environment());
    }

    public Optional<CristinPerson> getCristinPersonByCristinId(URI cristinPersonId) {
        return attempt(() -> createRequest(cristinPersonId))
                   .map(this::getCristinResponse)
                   .map(this::getBodyFromPersonResponse)
                   .map(this::getCristinPersonResponse)
                   .toOptional();
    }

    public CristinOrganization fetchCristinOrganizationByCristinId(URI cristinOrgId) {
        return isNull(cristinOrgId) ? null
                   : attempt(() -> createRequest(cristinOrgId))
                         .map(this::getCristinResponse)
                         .map(this::getBodyFromOrganizationResponse)
                         .map(this::convertToOrganization)
                         .orElse(this::loggExceptionAndReturnNull);
    }

    public Optional<CristinPerson> getCristinPersonByOrcId(String orcid) {
        return attempt(() -> createCristinPersonUri(orcid))
                   .map(this::createRequest)
                   .map(this::getCristinResponse)
                   .map(this::getBodyFromPersonResponse)
                   .map(this::getCristinPersonResponse)
                   .toOptional();
    }

    public Optional<SearchOrganizationResponse> searchCristinOrganization(String organization) {
        return attempt(() -> constructSearchCristinOrganizationUri(organization))
                   .map(this::createRequest)
                   .map(this::getCristinResponse)
                   .map(this::getBodyFromOrganizationResponse)
                   .map(this::convertSearchResponseToOrganization)
                   .toOptional();
    }

    private SearchOrganizationResponse convertSearchResponseToOrganization(String body) throws JsonProcessingException {
        return JsonUtils.singleLineObjectMapper.readValue(body, SearchOrganizationResponse.class);
    }

    private URI constructSearchCristinOrganizationUri(String organization) {
        return UriWrapper.fromUri(PiaConnection.HTTPS_SCHEME + environment.readEnv(API_HOST))
                   .addChild(CRISTIN)
                   .addChild(ORGANIZATION)
                   .addQueryParameter(QUERY, organization)
                   .getUri();
    }

    private URI createCristinPersonUri(String orcId) {
        return UriWrapper.fromUri(PiaConnection.HTTPS_SCHEME + environment.readEnv(API_HOST))
                   .addChild(CRISTIN)
                   .addChild(PERSON)
                   .addChild(UriWrapper.fromUri(orcId).getLastPathElement())
                   .getUri();
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
        return HttpRequest.newBuilder().headers(CONTENT_TYPE, APPLICATION_JSON).uri(uri).GET().build();
    }
}
