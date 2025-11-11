package no.sikt.nva.scopus.conversion;

import static java.util.Objects.nonNull;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
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
import no.unit.nva.commons.json.JsonUtils;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;
import nva.commons.secrets.SecretsReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PiaConnection {
    public static final String CRISTIN_PATH = "cristin";
    public static final String PERSON_PATH = "person";
    public static final int FALSE_IN_PIA_INTEGER = 0;
    public static final String PIA_REST_API_ENV_KEY = "PIA_REST_API";
    public static final String API_HOST = "API_HOST";
    public static final String PIA_USERNAME_KEY = "PIA_USERNAME_KEY";
    public static final String PIA_PASSWORD_KEY = "PIA_PASSWORD_KEY";
    public static final String PIA_SECRETS_NAME_ENV_KEY = "PIA_SECRETS_NAME";
    public static final String PIA_PATH = "sentralimport";
    public static final String PIA_AUTHORS_PATH = "authors";
    public static final String PIA_ORGS_PATH = "orgs";
    public static final String PIA_MATCHES_PATH = "matches";
    public static final String PIA_AUTHOR_ID_QUERY_PARAM = "author_id";
    public static final String PIA_AFFILIATION_ID_QUERY_PARAM = "affiliation_id";
    public static final String SCOPUS = "SCOPUS:";
    public static final String PIA_RESPONSE_ERROR = "Pia responded with status code";
    private static final String COULD_NOT_GET_ERROR_MESSAGE = "Could not get response from Pia for scopus id ";
    private static final String USERNAME_PASSWORD_DELIMITER = ":";
    private static final String AUTHORIZATION = "Authorization";
    private static final String BASIC_AUTHORIZATION = "Basic %s";
    private static final Logger logger = LoggerFactory.getLogger(PiaConnection.class);
    public static final String HTTPS_SCHEME = "https://";
    private static final String ORGANIZATION_PATH = "organization";
    public static final ObjectMapper DTO_OBJECT_MAPPER = JsonUtils.dtoObjectMapper;
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

    public Optional<URI> getCristinPersonIdentifier(String scopusAuthorIdentifier) {
        return attempt(() -> getPiaAuthorResponse(scopusAuthorIdentifier))
                   .map(this::getCristinNumber)
                   .map(Optional::orElseThrow)
                   .map(this::createCristinUriFromCristinNumber)
                   .toOptional();
    }

    public Optional<URI> fetchCristinOrganizationIdentifier(String scopusAffiliationIdentifier) {
        return attempt(() -> fetchAffiliationList(scopusAffiliationIdentifier))
                   .map(this::selectOneAffiliation)
                   .map(this::createCristinUriFromCristinOrganization)
                   .map(Optional::get)
                   .toOptional();
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

    private Optional<URI> createCristinUriFromCristinOrganization(Affiliation affiliation) {
        return Optional.ofNullable(affiliation)
            .filter(input -> nonNull(input.getUnitIdentifier()))
            .map(this::constructUri);
    }

    private URI constructUri(Affiliation affiliation) {
        return createUriWrapper(cristinProxyHost)
                   .addChild(CRISTIN_PATH)
                   .addChild(ORGANIZATION_PATH)
                   .addChild(affiliation.getUnitIdentifier())
                   .getUri();
    }

    private Affiliation selectOneAffiliation(List<Affiliation> affiliations) {
        return Optional.ofNullable(affiliations)
                   .map(this::selectTopLevelOrg)
                   .orElse(null);
    }

    @JacocoGenerated
    private Affiliation selectTopLevelOrg(List<Affiliation> affiliations) {
        return affiliations.stream()
                   .filter(this::allValuesArePresent)
                   .max(Comparator.comparingInt(aff -> Integer.parseInt(aff.getCount())))
                   .orElse(affiliations.getFirst());
    }

    private boolean allValuesArePresent(Affiliation affiliation) {
        return nonNull(affiliation.getInstitutionIdentifier())
               && nonNull(affiliation.getUnitIdentifier())
               && nonNull(affiliation.getCount());
    }

    private List<Affiliation> fetchAffiliationList(String scopusAffiliationIdentifier) {
        return attempt(() -> constructAffiliationUri(scopusAffiliationIdentifier))
                   .map(this::getResponse)
                   .map(this::getBodyFromResponse)
                   .map(this::convertToAffiliations)
                   .orElseThrow();
    }

    private List<Affiliation> convertToAffiliations(String body) throws JsonProcessingException {
        return Arrays.asList(DTO_OBJECT_MAPPER.readValue(body, Affiliation[].class));
    }

    private URI createCristinUriFromCristinNumber(Integer cristinNumber) {
        return createUriWrapper(cristinProxyHost)
                      .addChild(CRISTIN_PATH)
                      .addChild(PERSON_PATH)
                      .addChild(String.valueOf(cristinNumber))
                      .getUri();
    }

    private UriWrapper createUriWrapper(String hostString) {
        return UriWrapper.fromUri(PiaConnection.HTTPS_SCHEME + hostString);
    }

    private HttpRequest createRequest(URI uri) {
        return HttpRequest.newBuilder()
                   .uri(uri)
                   .setHeader(AUTHORIZATION, piaAuthorization)
                   .GET()
                   .build();
    }

    private String getPiaJsonAsString(String scopusAUId) {
        var uri = constructAuthorUri(scopusAUId);
        return attempt(() -> getResponse(uri))
                   .map(this::getBodyFromResponse)
                   .orElseThrow(fail ->
                                    logExceptionAndThrowRuntimeError(fail.getException(),
                                                                     COULD_NOT_GET_ERROR_MESSAGE + scopusAUId));
    }

    private URI constructAuthorUri(String scopusAuthorId) {
        return createUriWrapper(piaHost)
                   .addChild(PIA_PATH)
                   .addChild(PIA_AUTHORS_PATH)
                   .addQueryParameter(PIA_AUTHOR_ID_QUERY_PARAM, SCOPUS + scopusAuthorId)
                   .getUri();
    }

    private URI constructAffiliationUri(String scopusAffiliationId) {
        return createUriWrapper(piaHost)
                    .addChild(PIA_PATH)
                    .addChild(PIA_ORGS_PATH)
                    .addChild(PIA_MATCHES_PATH)
                    .addQueryParameter(PIA_AFFILIATION_ID_QUERY_PARAM, SCOPUS + scopusAffiliationId)
                    .getUri();
    }

    private RuntimeException logExceptionAndThrowRuntimeError(Exception exception, String message) {
        logger.info(message);
        return new RuntimeException(exception);
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

    private List<Author> getPiaAuthorResponse(String scopusAuid) {
        var piaResponse = getPiaJsonAsString(scopusAuid);
        var type = new TypeReference<ArrayList<Author>>(){};
        return attempt(() -> DTO_OBJECT_MAPPER.readValue(piaResponse, type)).orElseThrow();
    }

    private Optional<Integer> getCristinNumber(List<Author> authors) {
        return authors.stream()
                   .filter(this::hasCristinId)
                   .findFirst()
                   .map(Author::getCristinId);
    }

    private boolean hasCristinId(Author author) {
        return author.getCristinId() != FALSE_IN_PIA_INTEGER;
    }
}
