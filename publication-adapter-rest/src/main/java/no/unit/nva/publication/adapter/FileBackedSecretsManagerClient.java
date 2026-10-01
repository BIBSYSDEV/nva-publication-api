package no.unit.nva.publication.adapter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.ResourceNotFoundException;

/**
 * Reads secrets from a directory, one file per secret, which is how Kubernetes presents a mounted
 * secret and therefore how Vault-provided values reach a pod on Platon. Unlike a stub returning
 * canned credentials, this is deployable: only the directory contents differ between environments.
 */
public final class FileBackedSecretsManagerClient implements SecretsManagerClient {

    private static final String SERVICE_NAME = "secretsmanager";
    private static final String DEFAULT_SECRETS_DIR = "/etc/secrets";
    private static final String SECRETS_DIR_ENV = "SECRETS_DIR";

    private final Path secretsDirectory;

    public FileBackedSecretsManagerClient(Path secretsDirectory) {
        this.secretsDirectory = secretsDirectory;
    }

    public static FileBackedSecretsManagerClient fromEnvironment() {
        var directory = System.getenv().getOrDefault(SECRETS_DIR_ENV, DEFAULT_SECRETS_DIR);
        return new FileBackedSecretsManagerClient(Path.of(directory));
    }

    @Override
    public GetSecretValueResponse getSecretValue(GetSecretValueRequest request) {
        var secretFile = secretsDirectory.resolve(request.secretId());
        if (!Files.isRegularFile(secretFile)) {
            throw ResourceNotFoundException.builder()
                      .message("No secret named %s in %s".formatted(request.secretId(), secretsDirectory))
                      .build();
        }
        return GetSecretValueResponse.builder()
                   .name(request.secretId())
                   .secretString(readSecret(secretFile))
                   .build();
    }

    @Override
    public String serviceName() {
        return SERVICE_NAME;
    }

    @Override
    public void close() {
        // nothing to release
    }

    private static String readSecret(Path secretFile) {
        try {
            return Files.readString(secretFile).trim();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read secret " + secretFile, e);
        }
    }
}
