package no.unit.nva.publication.service;

import com.fasterxml.jackson.databind.JsonNode;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import java.util.UUID;

import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

public class ResourcePersistenceService {

    public static final String PATH = "/resource";
    private final Client client;

    protected ResourcePersistenceService(Client client) {
        this.client = client;
    }

    public ResourcePersistenceService() {
        this(ClientBuilder.newClient());
    }

    /**
     * Fetch Resource from Database.
     *
     * @param identifier  identifier
     * @param apiUrl  apiUrl
     * @param authorization authorization
     */

    public JsonNode fetchResource(UUID identifier, String apiUrl, String authorization) {
        return client.target(apiUrl).path(PATH).path(identifier.toString())
                .request(APPLICATION_JSON)
                .header(AUTHORIZATION, authorization)
                .get(JsonNode.class);
    }

}