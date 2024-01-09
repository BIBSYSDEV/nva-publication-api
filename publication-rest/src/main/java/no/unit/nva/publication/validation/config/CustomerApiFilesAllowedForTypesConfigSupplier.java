package no.unit.nva.publication.validation.config;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.HashSet;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.publication.validation.ConfigNotAvailableException;
import no.unit.nva.publication.validation.FilesAllowedForTypesSupplier;
import nva.commons.core.JacocoGenerated;

public class CustomerApiFilesAllowedForTypesConfigSupplier implements FilesAllowedForTypesSupplier {

    public static final String ALLOW_FILE_UPLOAD_FOR_TYPES_FIELD_NAME = "allowFileUploadForTypes";
    private final HttpClient httpClient;

    public CustomerApiFilesAllowedForTypesConfigSupplier(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @JacocoGenerated
    public CustomerApiFilesAllowedForTypesConfigSupplier() {
        this(HttpClient.newBuilder().build());
    }

    @Override
    public HashSet<String> get(URI customerUri) {
        var request = HttpRequest.newBuilder(customerUri).GET().build();
        try {
            var response = httpClient.send(request, BodyHandlers.ofString());
            if (HttpURLConnection.HTTP_OK == response.statusCode()) {
                final var types = new HashSet<String>();
                var rootNode = JsonUtils.dtoObjectMapper.readTree(response.body());
                var allowFileUploadForTypesNode = rootNode.get(ALLOW_FILE_UPLOAD_FOR_TYPES_FIELD_NAME);
                if (allowFileUploadForTypesNode != null && allowFileUploadForTypesNode.isArray()) {
                    allowFileUploadForTypesNode.elements().forEachRemaining(
                        node -> types.add(node.asText())
                    );
                    return types;
                } else {
                    throw new ConfigNotAvailableException(String.format("Response from %s did not contain expected "
                                                                        + "field %s of type array!",
                                                                        request.uri().toString(),
                                                                        ALLOW_FILE_UPLOAD_FOR_TYPES_FIELD_NAME));
                }
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
}
