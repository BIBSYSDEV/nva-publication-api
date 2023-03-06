package no.unit.nva.doi;

import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.doi.model.DoiResponse;
import no.unit.nva.doi.model.ReserveDoiRequest;
import no.unit.nva.model.Publication;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;

public class DataCiteReserveDoiClient implements ReserveDoiClient {

    public static final String DOI_REGISTRAR = "doi-registrar";
    public static final String API_HOST = "API_HOST";
    private final HttpClient httpClient;
    private final Environment environment;

    public DataCiteReserveDoiClient(HttpClient httpClient, Environment environment) {
        this.httpClient = httpClient;
        this.environment = environment;
    }

    @Override
    public URI generateDoi(Publication publication) {
        return attempt(this::constructUri)
                   .map(uri -> sendRequest(uri, publication))
                   .map(this::convertResponseToDoi)
                   .orElseThrow();
    }

    private URI constructUri() {
        return UriWrapper.fromUri(environment.readEnv(API_HOST))
                   .addChild(DOI_REGISTRAR)
                   .getUri();
    }

    private HttpResponse<String> sendRequest(URI uri, Publication publication) throws IOException,
                                                                                      InterruptedException {
        var requestBody = new ReserveDoiRequest(publication.getPublisher().getId());
        var request = HttpRequest.newBuilder()
                          .POST(BodyPublishers.ofString(JsonUtils.dtoObjectMapper.writeValueAsString(requestBody)))
                          .uri(uri)
                          .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public URI convertResponseToDoi(HttpResponse<String> response) throws JsonProcessingException {
        var doiResponse = JsonUtils.dtoObjectMapper.readValue(response.body(), DoiResponse.class);
        return doiResponse.getDoi();
    }
}
