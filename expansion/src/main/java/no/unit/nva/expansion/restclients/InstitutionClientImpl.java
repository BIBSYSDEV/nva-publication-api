package no.unit.nva.expansion.restclients;

import no.unit.nva.expansion.model.InstitutionResponse;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashSet;
import java.util.Set;

import static com.google.common.net.HttpHeaders.ACCEPT;
import static com.google.common.net.MediaType.JSON_UTF_8;
import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.expansion.ExpansionConstants.API_HOST;
import static no.unit.nva.expansion.ExpansionConstants.API_SCHEME;
import static no.unit.nva.expansion.ExpansionConstants.INSTITUTION_SERVICE_PATH;

public class InstitutionClientImpl implements InstitutionClient {

    private static final String GET_INSTITUTION_ERROR = "Error getting departments for institution";
    public static final String URI_QUERY = "?uri=";
    public static final String RESPONSE_STATUS_BODY = "Response status=%s, body=%s";

    private final Logger logger = LoggerFactory.getLogger(InstitutionClientImpl.class);
    private final HttpClient httpClient;

    @JacocoGenerated
    public InstitutionClientImpl() {
        this(HttpClient.newHttpClient());
    }

    public InstitutionClientImpl(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public Set<URI> getOrganizationIds(URI organizationId) {
        Set<URI> organizationIds = new HashSet<>();
        try {
            HttpRequest request = createGetInstitutionHierarchyHttpRequest(organizationId);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == HTTP_OK) {
                InstitutionResponse institutionResponse = InstitutionResponse.fromJson(response.body());
                organizationIds.addAll(institutionResponse.getOrganizationIds());
            } else {
                logWarning(response);
            }
        } catch (IOException | InterruptedException e) {
            logger.error(GET_INSTITUTION_ERROR, e);
        }
        return organizationIds;
    }

    private void logWarning(HttpResponse<String> response) {
        logger.warn(String.format(RESPONSE_STATUS_BODY, response.statusCode(), response.body()));
    }

    private HttpRequest createGetInstitutionHierarchyHttpRequest(URI organizationId) {
        return HttpRequest.newBuilder()
                .uri(createGetInstitutionUri(organizationId))
                .headers(ACCEPT, JSON_UTF_8.toString())
                .GET()
                .build();
    }

    private URI createGetInstitutionUri(URI organizationId) {
        String query = URI_QUERY + organizationId;
        return new UriWrapper(API_SCHEME, API_HOST)
                .addChild(INSTITUTION_SERVICE_PATH + query)
                .getUri();
    }
}
