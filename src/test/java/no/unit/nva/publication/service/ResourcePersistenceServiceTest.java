package no.unit.nva.publication.service;

import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ResourcePersistenceServiceTest {

    @Test
    public void test() {

        Client client = mock(Client.class);
        WebTarget webTarget = mock(WebTarget.class);
        when(client.target(anyString())).thenReturn(webTarget);
        when(webTarget.path(anyString())).thenReturn(webTarget);
        Invocation.Builder builder = mock(Invocation.Builder.class);
        when(webTarget.request(anyString())).thenReturn(builder);
        when(builder.header(anyString(),anyString())).thenReturn(builder);
        when(builder.post(any(), (Class<Object>) any())).thenReturn(null);

        ResourcePersistenceService resourcePersistenceService = new ResourcePersistenceService(client);

        resourcePersistenceService.fetchResource(UUID.randomUUID(), "http://example.org", "some api key");
    }

}
