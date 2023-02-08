package no.sikt.nva.scopus.conversion;

import static nva.commons.core.attempt.Try.attempt;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import no.sikt.nva.scopus.conversion.model.pia.Author;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Failure;
import nva.commons.core.paths.UriWrapper;
import nva.commons.secrets.SecretsReader;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PiaConnection {

    public static final String CRISTIN_PERSON_PATH = "/cristin/person/";
    public static final String ERROR_MESSAGE_EXTRACT_CRISTINID_ERROR = "Could not extract cristin id";
    public static final int FALSE_IN_PIA_INTEGER = 0;
    public static final String PIA_REST_API_ENV_KEY = "PIA_REST_API";
    public static final String API_HOST_ENV_KEY = "API_HOST";
    public static final String PIA_USERNAME_KEY = "PIA_USERNAME_KEY";
    public static final String PIA_PASSWORD_KEY = "PIA_PASSWORD_KEY";
    public static final String PIA_SECRETS_NAME_ENV_KEY = "PIA_SECRETS_NAME";
    private static final String PIA_RESPONSE_ERROR = "Pia responded with status code";
    private static final String COULD_NOT_GET_ERROR_MESSAGE = "Could not get response from Pia for scopus id ";
    private static final String USERNAME_PASSWORD_DELIMITER = ":";
    private static final String AUTHORIZATION = "Authorization";
    private static final String BASIC_AUTHORIZATION = "Basic %s";
    private static final Logger logger = LoggerFactory.getLogger(PiaConnection.class);
    private final HttpClient httpClient;
    private final transient String piaAuthorization;
    private final String piaHost;

    private final String cristinProxyHost;

    public PiaConnection(HttpClient httpClient,
                         SecretsReader secretsReader,
                         Environment environment) {
        this.httpClient = httpClient;
        this.piaHost = environment.readEnv(PIA_REST_API_ENV_KEY);
        this.piaAuthorization = createAuthorization(secretsReader, environment);
        this.cristinProxyHost = environment.readEnv(API_HOST_ENV_KEY);
    }

    @JacocoGenerated
    public PiaConnection() {
        this(getDefaultHttpClient(),
             new SecretsReader(),
             new Environment());
    }

    public URI getCristinID(String scopusId) {
        return attempt(() -> getPiaAuthorResponse(scopusId))
                   .map(this::getCristinNumber)
                   .map(Optional::orElseThrow)
                   .map(this::createCristinUriFromCristinNumber)
                   .orElse(this::logFailureAndReturnNull);
    }

    @JacocoGenerated
    private static HttpClient getDefaultHttpClient() {
        return HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build();
    }

    private static String createAuthorization(SecretsReader secretsReader, Environment environment) {
        var piaUsernameKeyName = environment.readEnv(PIA_USERNAME_KEY);
        var piaPasswordKeyName = environment.readEnv(PIA_PASSWORD_KEY);
        var piaSecretsName = environment.readEnv(PIA_SECRETS_NAME_ENV_KEY);

        var piaUserName = secretsReader.fetchSecret(piaSecretsName, piaUsernameKeyName);
        var piaPassword = secretsReader.fetchSecret(piaSecretsName, piaPasswordKeyName);
        var loginPassword = piaUserName + USERNAME_PASSWORD_DELIMITER + piaPassword;
        return String.format(BASIC_AUTHORIZATION, Base64.getEncoder().encodeToString(loginPassword.getBytes()));
    }

    private URI createCristinUriFromCristinNumber(int cristinNumber) {
        return UriWrapper.fromUri(cristinProxyHost + CRISTIN_PERSON_PATH + cristinNumber)
                   .getUri();
    }

    private HttpRequest createRequest(URI uri) {
        return HttpRequest.newBuilder()
                   .uri(uri)
                   .setHeader(AUTHORIZATION, piaAuthorization)
                   .GET()
                   .build();
    }

    private String getPiaJsonAsString(String scopusId) {
        var uri = constructUri(scopusId);
        return attempt(() -> getPiaResponse(uri))
                   .map(this::getBodyFromResponse)
                   .orElseThrow(fail ->
                                    logExceptionAndThrowRuntimeError(fail.getException(),
                                                                     COULD_NOT_GET_ERROR_MESSAGE + scopusId));
    }

    private URI constructUri(String scopusId) {
        return attempt(() -> new URI(piaHost + "/sentralimport/authors?author_id=SCOPUS:" + scopusId)).orElseThrow();
    }

    private RuntimeException logExceptionAndThrowRuntimeError(Exception exception, String message) {
        logger.info(message);
        return exception instanceof RuntimeException
                   ? (RuntimeException) exception
                   : new RuntimeException(exception);
    }

    private String getBodyFromResponse(HttpResponse<String> response) {
        if (response.statusCode() != HttpURLConnection.HTTP_OK) {
            logger.info(PIA_RESPONSE_ERROR + response.statusCode());
            throw new RuntimeException();
        }
        return response.body();
    }

    private HttpResponse<String> getPiaResponse(URI uri) throws IOException, InterruptedException {
        return httpClient.send(createRequest(uri), BodyHandlers.ofString());
    }

    private List<Author> getPiaAuthorResponse(String scopusID) {
        var piaResponse = getPiaJsonAsString(scopusID);
        Type listType = new TypeToken<ArrayList<Author>>() {
        }.getType();
        var gson = new Gson();
        return gson.fromJson(piaResponse, listType);
    }

    private Optional<Integer> getCristinNumber(List<Author> authors) {
        var optionalAuthWithCristinId = authors.stream().filter(this::hasCristinId).findFirst();
        return optionalAuthWithCristinId.map(Author::getCristinId);
    }

    private boolean hasCristinId(Author author) {
        return author.getCristinId() != FALSE_IN_PIA_INTEGER;
    }

    @Nullable
    private URI logFailureAndReturnNull(Failure<URI> failure) {
        logger.info(ERROR_MESSAGE_EXTRACT_CRISTINID_ERROR, failure.getException());
        return null;
    }
}
