package no.unit.nva.publication.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FetchResourceServiceTest {

    @Mock
    private HttpClient client;

    @Mock
    private HttpResponse<String> response;

    @Test
    public void testServiceReturnsJsonObject() throws IOException, InterruptedException {
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        when((response.body())).thenReturn("{ \"status\": \"success\" }");

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
