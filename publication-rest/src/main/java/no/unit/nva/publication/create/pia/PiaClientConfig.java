package no.unit.nva.publication.create.pia;

import java.net.http.HttpClient;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.secrets.SecretsReader;

public record PiaClientConfig(String piaHost,
                              String piaUsernameKey,
                              String piaPasswordKey,
                              String piaSecretsNameEnvKey,
                              HttpClient client,
                              SecretsReader secretsReader) {

    public static final String PIA_HOST = "PIA_REST_API";

    public static final String PIA_USERNAME_KEY = "PIA_USERNAME_KEY";
    public static final String PIA_PASSWORD_KEY = "PIA_PASSWORD_KEY";
    public static final String PIA_SECRETS_NAME_ENV_KEY = "PIA_SECRETS_NAME";


    @JacocoGenerated
    public static PiaClientConfig getDefaultConfig() {
        var environment = new Environment();
        return new PiaClientConfig(
            environment.readEnv(PIA_HOST),
            environment.readEnv(PIA_USERNAME_KEY),
            environment.readEnv(PIA_PASSWORD_KEY),
            environment.readEnv(PIA_SECRETS_NAME_ENV_KEY),
            getDefaultHttpClient(),
            new SecretsReader()
        );
    }

    @JacocoGenerated
    private static HttpClient getDefaultHttpClient() {
        return HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build();
    }
}
