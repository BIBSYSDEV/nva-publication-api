package no.unit.nva.publication.create.pia;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static java.net.HttpURLConnection.HTTP_UNSUPPORTED_TYPE;
import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.core.StringUtils.EMPTY_STRING;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.List;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.Identity;
import no.unit.nva.model.additionalidentifiers.AdditionalIdentifier;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;
import no.unit.nva.stubs.FakeSecretsManagerClient;
import no.unit.nva.stubs.WiremockHttpClient;
import nva.commons.core.paths.UriWrapper;
import nva.commons.logutils.LogRecorder;
import nva.commons.secrets.SecretsReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Documents the contract of the PIA per September 2026 <em>sentralimport</em> author endpoint as
 * observed against the real service, so that the expectations encoded in {@link PiaClient} stay
 * traceable.
 *
 * <p>The endpoint is {@code POST https://{piaHost}/sentralimport/authors}. It is protected by HTTP
 * basic authentication, requires {@code Content-Type: application/json} and answers {@code 204
 * No Content} when the authors were accepted; {@link PiaClient} treats any other status as a failure.
 *
 * <p>The request body is a JSON array of author records, one per contributor, each identifying the
 * publication by its Scopus id. The fields exercised here are {@code cristinId}, which is a JSON
 * number, {@code externalId}, which carries the Scopus AUID, and {@code orcid}, which carries the
 * bare ORCID identifier rather than its URI form; see the individual tests for the constraints PIA
 * puts on them.
 *
 * <p>Some documentation can be found in Jira ticket: SMILE-1295
 */
@WireMockTest(httpsEnabled = true)
class PiaClientTest {

  private static final String PIA_AUTHORS_PATH = "/sentralimport/authors";
  private static final String SCOPUS_AUID_SOURCE_NAME = "scopus-auid";
  private static final String CRISTIN_ID_FIELD = "cristinId";
  private static final String EXTERNAL_ID_FIELD = "externalId";
  private static final String ORCID_FIELD = "orcid";
  private static final String CRISTIN_PERSON_URI_TEMPLATE =
      "https://example.com/cristin/person/%s";
  private static final String SECRET_NAME = "pia-secret-name";
  private static final String USERNAME_KEY = "pia-username-key";
  private static final String PASSWORD_KEY = "pia-password-key";
  private static final String HTTPS_SCHEME = "https://";
  private static final String CONTENT_TYPE = "Content-Type";
  private static final String APPLICATION_JSON = "application/json";
  private static final String UPDATE_FAILED_MESSAGE = "Updating PIA failed";

  private PiaClient piaClient;

  @BeforeEach
  void setUp(WireMockRuntimeInfo wireMockRuntimeInfo) {
    piaClient = new PiaClient(piaClientConfig(wireMockRuntimeInfo));
    stubFor(post(urlEqualTo(PIA_AUTHORS_PATH)).willReturn(aResponse().withStatus(HTTP_NO_CONTENT)));
  }

  @Test
  void shouldSendScopusAuidAsExternalIdAndOrcidAsOrcid() {
    var scopusAuid = randomString();

    piaClient.updateContributor(
        List.of(contributorWith(randomInteger(), scopusAuid, randomUri())), randomString());

    var sentRequest = sentRequest();
    assertEquals(scopusAuid, sentRequest.get(EXTERNAL_ID_FIELD).textValue());
  }

  /**
   * PIA consumes {@code orcid} as the bare identifier value, never as a URI. The field is limited
   * to 20 characters, so posting the full URI form {@code https://orcid.org/0000-0002-4029-1960} is
   * rejected with {@code 400 Bad Request} and a message containing "is too long ... maximum
   * allowed: 20
   */
  @Test
  void shouldSendOrcidAsIdentifierValueAndNotAsUri() {
    var orcidIdentifier = randomString();
    var orcidUri = UriWrapper.fromUri(randomUri()).addChild(orcidIdentifier).getUri();
    piaClient.updateContributor(
        List.of(contributorWith(randomInteger(), randomString(), orcidUri)), randomString());

    assertEquals(orcidIdentifier, sentRequest().get(ORCID_FIELD).textValue());
  }

  /**
   * PIA expects {@code cristinId} as a JSON number. The Cristin identifier is the last path element
   * of the contributor identity URI, and must not be sent as a quoted string.
   */
  @Test
  void shouldSendCristinIdentifierAsNumber() {
    var cristinIdentifier = randomInteger();

    piaClient.updateContributor(
        List.of(contributorWith(cristinIdentifier, randomString(), randomUri())), randomString());

    var cristinId = sentRequest().get(CRISTIN_ID_FIELD);
    assertTrue(cristinId.isNumber(), "cristinId must be a json number but was " + cristinId);
    assertEquals(cristinIdentifier, cristinId.intValue());
  }

  /**
   * PIA rejects a request without {@code Content-Type: application/json} with {@code 415
   * Unsupported Media Type}, even when the body itself is valid JSON. This is why {@link PiaClient}
   * sets the header explicitly instead of relying on a default.
   */
  @Test
  void shouldReturnUnsupportedMediaTypeWhenRequestHasNoContentTypeHeader(
      WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
    stubPiaAcceptingOnlyJsonContentType();

    var response =
        WiremockHttpClient.create()
            .send(requestWithoutContentType(wireMockRuntimeInfo), BodyHandlers.ofString());

    assertEquals(HTTP_UNSUPPORTED_TYPE, response.statusCode());
  }

  /**
   * PIA answers {@code 204 No Content} when the authors were accepted. This is the only status
   * {@link PiaClient} accepts as a success, every other one is logged as a failure.
   */
  @Test
  void shouldNotLogErrorWhenPiaAnswersNoContent() {
    var logRecorder = LogRecorder.forClass(PiaClient.class);

    piaClient.updateContributor(
        List.of(contributorWith(randomInteger(), randomString(), randomUri())), randomString());

    assertEquals(1, findAll(postRequestedFor(urlEqualTo(PIA_AUTHORS_PATH))).size());
    assertThat(logRecorder.asString(), not(containsString(UPDATE_FAILED_MESSAGE)));
  }

  @Test
  void shouldLogStatusCodeAndBodyWhenPiaRejectsAuthorsUpdate() {
    var responseBody = randomString();
    stubFor(
        post(urlEqualTo(PIA_AUTHORS_PATH))
            .willReturn(aResponse().withStatus(HTTP_BAD_REQUEST).withBody(responseBody)));
    var logRecorder = LogRecorder.forClass(PiaClient.class);

    piaClient.updateContributor(
        List.of(contributorWith(randomInteger(), randomString(), randomUri())), randomString());

    assertThat(logRecorder.asString(), containsString(UPDATE_FAILED_MESSAGE));
    assertThat(logRecorder.asString(), containsString(String.valueOf(HTTP_BAD_REQUEST)));
    assertThat(logRecorder.asString(), containsString(responseBody));
  }

  private static JsonNode sentRequest() {
    var body = findAll(postRequestedFor(urlEqualTo(PIA_AUTHORS_PATH))).getFirst().getBodyAsString();
    return attempt(() -> dtoObjectMapper.readTree(body)).orElseThrow().get(0);
  }

  private static void stubPiaAcceptingOnlyJsonContentType() {
    stubFor(
        post(urlEqualTo(PIA_AUTHORS_PATH))
            .willReturn(aResponse().withStatus(HTTP_UNSUPPORTED_TYPE)));
    stubFor(
        post(urlEqualTo(PIA_AUTHORS_PATH))
            .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON))
            .willReturn(aResponse().withStatus(HTTP_CREATED)));
  }

  private static HttpRequest requestWithoutContentType(WireMockRuntimeInfo wireMockRuntimeInfo) {
    return HttpRequest.newBuilder()
        .uri(URI.create(wireMockRuntimeInfo.getHttpsBaseUrl() + PIA_AUTHORS_PATH))
        .POST(BodyPublishers.ofString(randomString()))
        .build();
  }

  private static Contributor contributorWith(
      Integer cristinIdentifier, String scopusAuid, URI orcid) {
    var identity =
        new Identity.Builder()
            .withId(URI.create(CRISTIN_PERSON_URI_TEMPLATE.formatted(cristinIdentifier)))
            .withName(randomString())
            .withOrcId(orcid.toString())
            .withAdditionalIdentifiers(
                List.of(new AdditionalIdentifier(SCOPUS_AUID_SOURCE_NAME, scopusAuid)))
            .build();
    return new Contributor(identity, List.of(), new RoleType(Role.CREATOR), 1, false);
  }

  private static PiaClientConfig piaClientConfig(WireMockRuntimeInfo wireMockRuntimeInfo) {
    return new PiaClientConfig(
        wireMockRuntimeInfo.getHttpsBaseUrl().replace(HTTPS_SCHEME, EMPTY_STRING),
        USERNAME_KEY,
        PASSWORD_KEY,
        SECRET_NAME,
        WiremockHttpClient.create(),
        secretsReader());
  }

  private static SecretsReader secretsReader() {
    var fakeSecretsManagerClient = new FakeSecretsManagerClient();
    fakeSecretsManagerClient.putSecret(SECRET_NAME, USERNAME_KEY, randomString());
    fakeSecretsManagerClient.putSecret(SECRET_NAME, PASSWORD_KEY, randomString());
    return new SecretsReader(fakeSecretsManagerClient);
  }
}
