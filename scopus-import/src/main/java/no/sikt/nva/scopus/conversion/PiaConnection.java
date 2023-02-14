package no.sikt.nva.scopus.conversion;

import static java.util.Objects.nonNull;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import no.sikt.nva.scopus.conversion.model.pia.Affiliation;
import no.sikt.nva.scopus.conversion.model.pia.Author;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Failure;
import nva.commons.core.paths.UriWrapper;
import nva.commons.secrets.SecretsReader;
import org.apache.http.client.utils.URIBuilder;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PiaConnection {

    public static final String CRISTIN_PERSON_PATH = "/cristin/person/";
    public static final String ERROR_MESSAGE_EXTRACT_CRISTINID_ERROR = "Could not extract cristin id";
    public static final int FALSE_IN_PIA_INTEGER = 0;
    public static final String PIA_REST_API_ENV_KEY = "PIA_REST_API";
    public static final String API_HOST = "API_HOST";
    public static final String PIA_USERNAME_KEY = "PIA_USERNAME_KEY";
    public static final String PIA_PASSWORD_KEY = "PIA_PASSWORD_KEY";
    public static final String PIA_SECRETS_NAME_ENV_KEY = "PIA_SECRETS_NAME";
    public static final String PIA_AUTHORS_PATH = "/sentralimport/authors";
    public static final String PIA_ORGS_PATH = "/sentralimport/orgs/matches";
    public static final String PIA_AUTHOR_ID_QUERY_PARAM = "author_id";
    public static final String PIA_AFFILIATION_ID_QUERY_PARAM = "affiliation_id";
    public static final String SCOPUS = "SCOPUS:";
    public static final String HTTPS_SCHEME = "https";
    public static final String ORGANIZATION = "organization";
    public static final String CRISTIN = "cristin";
    public static final String ERROR_MESSAGE_EXTRACTING_CRISTIN_ORG_ID = "Could not extract cristin id";
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
        this.cristinProxyHost = environment.readEnv(API_HOST);
    }

    @JacocoGenerated
    public PiaConnection() {
        this(getDefaultHttpClient(),
             new SecretsReader(),
             new Environment());
    }

    public URI getCristinPersonIdentifier(String scopusAuthorIdentifier) {
        return attempt(() -> getPiaAuthorResponse(scopusAuthorIdentifier))
                   .map(this::getCristinNumber)
                   .map(Optional::orElseThrow)
                   .map(this::createCristinUriFromCristinNumber)
                   .orElse(this::logFailureFetchingCristinPersonAndReturnNull);
    }

    public URI getCristinOrganizationIdentifier(String scopusAffiliationIdentifier) {
        return attempt(() -> fetchAffiliationList(scopusAffiliationIdentifier))
                   .map(this::selectOneAffiliation)
                   .map(this::createCristinUriFromCristinOrganization)
                   .orElse(this::logFailureFetchingCristinOrganizationAndReturnNull);
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

    private URI createCristinUriFromCristinOrganization(Affiliation affiliation) {
        return UriWrapper.fromUri(cristinProxyHost)
                   .addChild(CRISTIN)
                   .addChild(ORGANIZATION)
                   .addChild(affiliation.getUnitIdentifier())
                   .getUri();
    }

    private Affiliation selectOneAffiliation(List<Affiliation> affiliations) {
        return Optional.ofNullable(affiliations)
                   .map(this::selectTopLevelOrg)
                   .orElse(null);
    }

    private Affiliation selectTopLevelOrg(List<Affiliation> affiliations) {
        return affiliations.stream()
                   .filter(this::allValuesArePresent)
                   .max(Comparator.comparingInt(aff -> Integer.parseInt(aff.getCount())))
                   .orElse(affiliations.get(0));
    }

    private boolean allValuesArePresent(Affiliation affiliation) {
        return nonNull(affiliation.getInstitutionIdentifier())
               && nonNull(affiliation.getUnitIdentifier())
               && nonNull(affiliation.getCount());
    }

    private List<Affiliation> fetchAffiliationList(String scopusAffiliationIdentifier) {
        return attempt(() -> cosntructAffiliationUri(scopusAffiliationIdentifier))
                   .map(this::getResponse)
                   .map(this::getBodyFromResponse)
                   .map(this::convertToAffiliations)
                   .orElseThrow();
    }

    private List<Affiliation> convertToAffiliations(String body) throws JsonProcessingException {
        return Arrays.asList(new ObjectMapper().readValue(body, Affiliation[].class));
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
        var uri = cosntructAuthorUri(scopusId);
        return attempt(() -> getResponse(uri))
                   .map(this::getBodyFromResponse)
                   .orElseThrow(fail ->
                                    logExceptionAndThrowRuntimeError(fail.getException(),
                                                                     COULD_NOT_GET_ERROR_MESSAGE + scopusId));
    }

    private URI cosntructAuthorUri(String scopusAuthorId) {
        return attempt(() -> new URIBuilder()
                                 .setHost(piaHost)
                                 .setPath(PIA_AUTHORS_PATH)
                                 .setParameter(PIA_AUTHOR_ID_QUERY_PARAM, SCOPUS + scopusAuthorId)
                                 .setScheme(HTTPS_SCHEME)
                                 .build())
                   .orElseThrow();
    }

    private URI cosntructAffiliationUri(String scopusAffiliationId) {
        return attempt(() -> new URIBuilder()
                                 .setHost(piaHost)
                                 .setPath(PIA_ORGS_PATH)
                                 .setParameter(PIA_AFFILIATION_ID_QUERY_PARAM, SCOPUS + scopusAffiliationId)
                                 .setScheme(HTTPS_SCHEME)
                                 .build())
                   .orElseThrow();
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

    private HttpResponse<String> getResponse(URI uri) throws IOException, InterruptedException {
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
    private URI logFailureFetchingCristinPersonAndReturnNull(Failure<URI> failure) {
        logger.info(ERROR_MESSAGE_EXTRACT_CRISTINID_ERROR, failure.getException());
        return null;
    }

    @Nullable
    private URI logFailureFetchingCristinOrganizationAndReturnNull(Failure<URI> failure) {
        logger.info(ERROR_MESSAGE_EXTRACTING_CRISTIN_ORG_ID, failure.getException());
        return null;
    }
}
