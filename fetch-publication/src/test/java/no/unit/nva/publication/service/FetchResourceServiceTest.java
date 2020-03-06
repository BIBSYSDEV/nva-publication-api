package no.unit.nva.publication.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FetchResourceServiceTest {

    @Test
    public void test() throws IOException, InterruptedException {
        HttpClient client = mock(HttpClient.class);
        HttpResponse<String> httpResponse = mock(HttpResponse.class);
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);
        when((httpResponse.body())).thenReturn("{ \"status\": \"success\" }");

        FetchResourceService fetchResourceService = new FetchResourceService(client);

        JsonNode jsonNode = fetchResourceService.fetchResource(
                UUID.randomUUID(),
                "http",
                "example.org",
                "some api key"
        );

        assertNotNull(jsonNode);
    }

    @Test
    public void testClientError() throws IOException, InterruptedException {
        HttpClient client = mock(HttpClient.class);
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenThrow(IOException.class);

        FetchResourceService fetchResourceService = new FetchResourceService(client);

        Assertions.assertThrows(IOException.class, () -> {
            fetchResourceService.fetchResource(
                    UUID.randomUUID(),
                    "http",
                    "example.org",
                    "some api key"
            );
        });
    }

}
