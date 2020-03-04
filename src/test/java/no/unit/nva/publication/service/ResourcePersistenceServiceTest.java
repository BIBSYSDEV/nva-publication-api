package no.unit.nva.publication.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ResourcePersistenceServiceTest {

    @Test
    public void test() throws IOException, InterruptedException {
        HttpClient client = mock(HttpClient.class);
        HttpResponse<String> httpResponse = mock(HttpResponse.class);
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);
        when((httpResponse.body())).thenReturn("{ \"status\": \"success\" }");

        ResourcePersistenceService resourcePersistenceService = new ResourcePersistenceService(client);

        JsonNode jsonNode = resourcePersistenceService.fetchResource(
                UUID.randomUUID(),
                "http",
                "example.org",
                "some api key"
        );

        assertNotNull(jsonNode);
    }

}
