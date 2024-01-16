package no.unit.nva.publication.validation.config;

import static java.util.Objects.nonNull;
import static org.apache.http.HttpHeaders.ACCEPT;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.publication.validation.ConfigNotAvailableException;
import no.unit.nva.publication.validation.FilesAllowedForTypesSupplier;
import nva.commons.core.JacocoGenerated;

public class CustomerApiFilesAllowedForTypesConfigSupplier implements FilesAllowedForTypesSupplier {

    public static final String ALLOW_FILE_UPLOAD_FOR_TYPES_FIELD_NAME = "allowFileUploadForTypes";
    private final HttpClient httpClient;
    private static final String APPLICATION_JSON = "application/json";

    public CustomerApiFilesAllowedForTypesConfigSupplier(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @JacocoGenerated
    public CustomerApiFilesAllowedForTypesConfigSupplier() {
        this(HttpClient.newBuilder().build());
    }

    @Override
    public Set<String> get(URI customerUri) {
        var request = HttpRequest.newBuilder(customerUri)
                          .GET()
                          .header(ACCEPT, APPLICATION_JSON)
                          .build();
        try {
            var response = httpClient.send(request, BodyHandlers.ofString());
            if (HttpURLConnection.HTTP_OK == response.statusCode()) {
                return extractFilesAllowedForTypesFromJsonResponse(response, request);
            } else {
                throw new ConfigNotAvailableException(String.format("Got http response code %d",
                                                                    response.statusCode()));
            }
        } catch (IOException e) {
            throw new ConfigNotAvailableException(request.uri().toString(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ConfigNotAvailableException(request.uri().toString(), e);
        }
    }

    private static Set<String> extractFilesAllowedForTypesFromJsonResponse(HttpResponse<String> response,
                                                                           HttpRequest request)
        throws JsonProcessingException {

        var node = parseJson(response.body()).get(ALLOW_FILE_UPLOAD_FOR_TYPES_FIELD_NAME);
        if (nodeIsArray(node)) {
            return extractTypes(node);
        } else {
            throw new ConfigNotAvailableException(String.format("Response from %s did not contain expected "
                                                                + "field %s of type array!",
                                                                request.uri().toString(),
                                                                ALLOW_FILE_UPLOAD_FOR_TYPES_FIELD_NAME));
        }
    }

    private static boolean nodeIsArray(final JsonNode node) {
        return nonNull(node) && node.isArray();
    }

    private static JsonNode parseJson(final String json) throws JsonProcessingException {
        return JsonUtils.dtoObjectMapper.readTree(json);
    }

    private static Set<String> extractTypes(final JsonNode node) {
        return StreamSupport.stream(node.spliterator(), false)
                   .map(JsonNode::asText)
                   .collect(Collectors.toSet());
    }
}
