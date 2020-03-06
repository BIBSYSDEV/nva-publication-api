package no.unit.nva.publication.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.ModifyPublicationHandler;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import java.io.File;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ModifyResourceServiceTest {

    public static final String PUBLICATION_JSON = "src/test/resources/publication.json";
    public static final String SOME_API_KEY = "some api key";
    public static final String API_HOST = "example.org";
    public static final String API_SCHEME = "http";

    private ObjectMapper objectMapper = ModifyPublicationHandler.createObjectMapper();

    @Test
    public void test() throws IOException, InterruptedException {

        Publication publication = getPublication();
        HttpClient client = mock(HttpClient.class);
        HttpResponse<String> httpResponse = mock(HttpResponse.class);
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);
        when((httpResponse.body())).thenReturn(objectMapper.writeValueAsString(publication));
        when((httpResponse.statusCode())).thenReturn(200);


        ModifyResourceService modifyResourceService = new ModifyResourceService(client);

        JsonNode jsonNode = modifyResourceService.modifyResource(
                publication.getIdentifier(),
                publication,
                API_SCHEME,
                API_HOST,
                SOME_API_KEY
        );

        assertNotNull(jsonNode);
    }

    @Test
    public void testClientError() throws IOException, InterruptedException {

        Publication publication = getPublication();
        HttpClient client = mock(HttpClient.class);
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenThrow(IOException.class);

        ModifyResourceService modifyResourceService = new ModifyResourceService(client);

        Assertions.assertThrows(IOException.class, () -> {
            modifyResourceService.modifyResource(
                    publication.getIdentifier(),
                    publication,
                    API_SCHEME,
                    API_HOST,
                    SOME_API_KEY
            );
        });
    }

    private Publication getPublication() throws IOException {
        return objectMapper.readValue(new File(PUBLICATION_JSON), Publication.class);
    }

}
