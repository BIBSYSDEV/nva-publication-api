package no.sikt.nva.scopus.conversion;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.or;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static no.sikt.nva.scopus.conversion.CristinConnection.CRISTIN_ORGANIZATION_RESPONSE_ERROR;
import static no.sikt.nva.scopus.conversion.CristinConnection.CRISTIN_PERSON_RESPONSE_ERROR;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.net.HttpURLConnection;
import java.net.URI;
import no.sikt.nva.scopus.conversion.model.cristin.CristinOrganization;
import no.sikt.nva.scopus.conversion.model.cristin.CristinPerson;
import no.sikt.nva.scopus.utils.CristinGenerator;
import no.unit.nva.stubs.WiremockHttpClient;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;
import nva.commons.logutils.LogUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@WireMockTest(httpsEnabled = true)
class CristinConnectionTest {

    private CristinConnection cristinConnection;

    @BeforeEach
    void init(WireMockRuntimeInfo wireMockRuntimeInfo) {
        var httpClient = WiremockHttpClient.create();
        var environment = mock(Environment.class);
        when(environment.readEnv("API_HOST")).thenReturn(wireMockRuntimeInfo.getHttpsBaseUrl().replace("https://", ""));
        cristinConnection = new CristinConnection(httpClient, environment);
    }

    @Test
    void shouldLogErrorIfCristinProxyRespondsWithErrorCodeForPerson(WireMockRuntimeInfo wireMockRuntimeInfo) {
        var appender = LogUtils.getTestingAppenderForRootLogger();
        var randomPersonUri = getRandomPersonUri(wireMockRuntimeInfo);
        mockCristinPersonBadRequest();
        var actualPerson = cristinConnection.getCristinPersonByCristinId(randomPersonUri);
        assertThat(actualPerson.isEmpty(), is((true)));
        assertThat(appender.getMessages(), containsString(CRISTIN_PERSON_RESPONSE_ERROR));
    }

    @Test
    void shouldLogErrorIfCristinProxyRespondsWithErrorCodeForOrganization(WireMockRuntimeInfo wireMockRuntimeInfo) {
        var appender = LogUtils.getTestingAppenderForRootLogger();
        var randomOrganizationUri = getRandomOrganizationUri(wireMockRuntimeInfo);
        mockCristinOrganizationBadRequest();
        var actualOrganization = cristinConnection.fetchCristinOrganizationByCristinId(randomOrganizationUri);
        assertThat(actualOrganization, is(nullValue()));
        assertThat(appender.getMessages(), containsString(CRISTIN_ORGANIZATION_RESPONSE_ERROR));
    }

    @Test
    void shouldReturnPersonIfCristinProxyRespondsWithPersonResponse(WireMockRuntimeInfo wireMockRuntimeInfo) {
        var randomPersonUri = getRandomPersonUri(wireMockRuntimeInfo);
        var expectedPerson = createExpectedPerson(randomPersonUri);
        mockCristinPerson(randomPersonUri, expectedPerson.toJsonString());
        var actualPerson = cristinConnection.getCristinPersonByCristinId(randomPersonUri);
        assertThat(actualPerson.isPresent(), is(equalTo(true)));
        assertThat(actualPerson.get(), is(equalTo(expectedPerson)));
    }

    @Test
    void shouldReturnPeronFetchedByOrcId() {
        var orcId = randomString();
        mockCristinPersonByOrcId(orcId);
        var actualPerson = cristinConnection.getCristinPersonByOrcId(orcId);
        assertThat(actualPerson.isPresent(), is(equalTo(true)));
    }

    private void mockCristinPersonByOrcId(String orcId) {
        stubFor(
            WireMock.get(urlPathEqualTo("/cristin/person/" + orcId))
                .willReturn(aResponse()
                                .withBody(CristinGenerator.generateCristinPerson(randomUri(), randomString(),
                                                                                 randomString()).toString())
                                .withStatus(HttpURLConnection.HTTP_OK)));
    }

    @Test
    void shouldReturnOrganizationIfCristinProxyRespondsWithOrganization(WireMockRuntimeInfo wireMockRuntimeInfo) {
        var randomOrganizationId = getRandomOrganizationUri(wireMockRuntimeInfo);
        var expectedOrganization = createExpectedOrganization(randomOrganizationId);
        mockCristinOrganization(randomOrganizationId, expectedOrganization.toJsonString());
        var actualOrganization = cristinConnection.fetchCristinOrganizationByCristinId(randomOrganizationId);
        assertThat(actualOrganization, is(equalTo(expectedOrganization)));
    }

    @Test
    void shouldReturnOptionalEmptyWhenCristinIdIsNull() {
        URI cristinId = null;
        var actualPerson = cristinConnection.getCristinPersonByCristinId(cristinId);
        assertThat(actualPerson.isEmpty(), is(equalTo(true)));
    }

    @Test
    void shouldReturnNullWhenCristinIdIsNull() {
        URI cristinId = null;
        var actualOrganization = cristinConnection.fetchCristinOrganizationByCristinId(cristinId);
        assertThat(actualOrganization, is(nullValue()));
    }

    private static URI getRandomPersonUri(WireMockRuntimeInfo wireMockRuntimeInfo) {
        var baseUri = wireMockRuntimeInfo.getHttpsBaseUrl();
        return UriWrapper.fromUri(baseUri)
                   .addChild("cristin")
                   .addChild("person")
                   .addChild(randomString())
                   .getUri();
    }

    private CristinOrganization createExpectedOrganization(URI organizationId) {
        return new CristinOrganization(organizationId, null, randomString());
    }

    private CristinPerson createExpectedPerson(URI personId) {
        return new CristinPerson.Builder().withId(personId).build();
    }

    private URI getRandomOrganizationUri(WireMockRuntimeInfo wireMockRuntimeInfo) {
        var baseUri = wireMockRuntimeInfo.getHttpsBaseUrl();

        return UriWrapper.fromUri(baseUri)
                   .addChild("cristin")
                   .addChild("organization")
                   .addChild(randomString())
                   .getUri();
    }

    private void mockCristinPerson(URI cristinPersonId, String response) {
        stubFor(
            WireMock.get(urlPathEqualTo(cristinPersonId.getPath()))
                .willReturn(aResponse().withBody(response).withStatus(HttpURLConnection.HTTP_OK)));
    }

    private void mockCristinOrganization(URI cristinId, String organization) {
        stubFor(WireMock.get(urlPathEqualTo(cristinId.getPath()))
                    .willReturn(aResponse().withBody(organization).withStatus(HttpURLConnection.HTTP_OK)));
    }

    private void mockCristinPersonBadRequest() {
        stubFor(WireMock.get(urlMatching("/cristin/person/.*"))
                    .willReturn(aResponse().withStatus(HttpURLConnection.HTTP_BAD_REQUEST)));
    }

    private void mockCristinOrganizationBadRequest() {
        stubFor(WireMock.get(urlMatching("/cristin/organization/.*"))
                    .willReturn(aResponse().withStatus(HttpURLConnection.HTTP_BAD_REQUEST)));
    }
}
