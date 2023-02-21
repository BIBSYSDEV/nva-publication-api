package no.sikt.nva.scopus.conversion;

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

    public static final String CRISTIN_RESPONDED_WITH_BAD_STATUS_CODE_ERROR_MESSAGE = "cristin responded with status "
                                                                                      + "code: ";
    private static final Logger logger = LoggerFactory.getLogger(CristinConnection.class);
    private final HttpClient httpClient;

    public CristinConnection(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @JacocoGenerated
    public CristinConnection() {
        this(HttpClient.newBuilder().build());
    }

    public Optional<Person> getCristinPersonByCristinId(URI cristinPersonId) {

        return Optional.ofNullable(attempt(() -> createRequest(cristinPersonId))
                                       .map(this::getCristinResponse)
                                       .map(this::getBodyFromResponse)
                                       .map(this::getCristinPersonResponse)
                                       .orElse(null));
    }

    public Organization getCristinOrganizationByCristinId(URI cristinOrgId) {
        return attempt(() -> createRequest(cristinOrgId))
                   .map(this::getCristinResponse)
                   .map(this::getBodyFromResponse)
                   .map(this::convertToOrganization)
                   .orElse(null);
    }

    private Organization convertToOrganization(String body) throws JsonProcessingException {
        return new ObjectMapper().readValue(body, Organization.class);
    }

    private Person getCristinPersonResponse(String json) throws JsonProcessingException {
        return JsonUtils.singleLineObjectMapper.readValue(json, Person.class);
    }

    private String getBodyFromResponse(HttpResponse<String> response) {
        if (response.statusCode() != HttpURLConnection.HTTP_OK) {
            logger.info(CRISTIN_RESPONDED_WITH_BAD_STATUS_CODE_ERROR_MESSAGE + response.statusCode());
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
}
