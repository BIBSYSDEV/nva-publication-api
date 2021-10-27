package no.unit.nva.expansion.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.expansion.Constants;
import no.unit.nva.expansion.InstitutionClient;
import no.unit.nva.expansion.model.InstitutionResponse;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.JsonUtils;
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

public class InstitutionClientImpl implements InstitutionClient {

    private static final String GET_INSTITUTION_ERROR = "Error getting departments for institution";
    public static final String URI_QUERY = "?uri=";

    private final Logger logger = LoggerFactory.getLogger(InstitutionClientImpl.class);
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = JsonUtils.dtoObjectMapper;

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
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(createGetInstitutionUri(organizationId))
                    .headers(ACCEPT, JSON_UTF_8.toString())
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == HTTP_OK) {
                InstitutionResponse institutionResponse = objectMapper
                        .readValue(response.body(), InstitutionResponse.class);
                organizationIds.addAll(institutionResponse.getOrganizationIds());
            }
        } catch (IOException | InterruptedException e) {
            logger.warn(GET_INSTITUTION_ERROR, e);
        }
        return organizationIds;
    }

    private URI createGetInstitutionUri(URI organizationId) {
        String query = URI_QUERY + organizationId;
        return new UriWrapper(Constants.API_SCHEME, Constants.API_HOST)
                .addChild(Constants.INSTITUTION_SERVICE_PATH + query)
                .getUri();
    }
}
