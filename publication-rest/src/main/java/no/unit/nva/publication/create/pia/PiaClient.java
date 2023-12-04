package no.unit.nva.publication.create.pia;

import static java.net.HttpURLConnection.HTTP_CREATED;
import static no.unit.nva.publication.create.pia.PiaUpdateRequest.toPiaRequest;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Base64;
import java.util.List;
import no.unit.nva.model.Contributor;
import nva.commons.core.paths.UriWrapper;
import nva.commons.secrets.SecretsReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PiaClient {

    private static final Logger logger = LoggerFactory.getLogger(PiaClient.class);

    private static final String USERNAME_PASSWORD_DELIMITER = ":";

    private static final String BASIC_AUTHORIZATION = "Basic %s";

    private static final String AUTHORIZATION = "Authorization";
    private final URI piaUri;
    private final HttpClient httpClient;
    private final String authorization;

    public PiaClient(PiaClientConfig config) {
        this.piaUri = UriWrapper.fromUri(config.piaHost())
                          .addChild("sentralimport")
                          .addChild("authors")
                          .getUri();
        this.httpClient = config.client();
        this.authorization = createAuthorization(config.secretsReader(),
                                                 config.piaUsernameKey(),
                                                 config.piaPasswordKey(),
                                                 config.piaSecretsNameEnvKey());
    }

    public void updateContributor(List<Contributor> contributors, String scopusId) {
        if (!contributors.isEmpty()) {
            var payload = createPayload(contributors, scopusId);
            var request = createRequest(payload.toString());
            sendRequest(request);
        }
    }

    private static String createAuthorization(SecretsReader secretsReader,
                                              String piaUsernameKeyName,
                                              String piaPasswordKeyName,
                                              String piaSecretsName) {
        var piaUserName = secretsReader.fetchSecret(piaSecretsName, piaUsernameKeyName);
        var piaPassword = secretsReader.fetchSecret(piaSecretsName, piaPasswordKeyName);
        var loginPassword = piaUserName + USERNAME_PASSWORD_DELIMITER + piaPassword;
        return String.format(BASIC_AUTHORIZATION, Base64.getEncoder().encodeToString(loginPassword.getBytes()));
    }

    private void sendRequest(HttpRequest request) {
        try {
            var response = httpClient.send(request, BodyHandlers.ofString());
            if (response.statusCode() != HTTP_CREATED) {
                logger.error("Updating PIA failed: " + response);
            }
        } catch (Exception e) {
            logger.error("Updating PIA failed with exception:", e);
        }
    }

    private HttpRequest createRequest(String jsonPayload) {
        return HttpRequest
                   .newBuilder()
                   .header(AUTHORIZATION, authorization)
                   .POST(BodyPublishers.ofString(jsonPayload))
                   .uri(piaUri)
                   .build();
    }

    private List<PiaUpdateRequest> createPayload(List<Contributor> contributors, String scopusId) {
        return contributors
                   .stream()
                   .map(contributor -> toPiaRequest(contributor, scopusId))
                   .toList();
    }
}
