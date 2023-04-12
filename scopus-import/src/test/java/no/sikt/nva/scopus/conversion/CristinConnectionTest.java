package no.sikt.nva.scopus.conversion;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static no.sikt.nva.scopus.conversion.CristinConnection.CRISTIN_ORGANIZATION_RESPONSE_ERROR;
import static no.sikt.nva.scopus.conversion.CristinConnection.CRISTIN_PERSON_RESPONSE_ERROR;
import static no.sikt.nva.scopus.conversion.PiaConnection.HTTPS_SCHEME;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.net.HttpURLConnection;
import java.net.URI;
import no.sikt.nva.scopus.conversion.model.cristin.Organization;
import no.sikt.nva.scopus.conversion.model.cristin.Person;
import no.unit.nva.stubs.WiremockHttpClient;
import nva.commons.logutils.LogUtils;
import org.apache.http.client.utils.URIBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@WireMockTest(httpsEnabled = true)
public class CristinConnectionTest {

    private CristinConnection cristinConnection;

    @BeforeEach
    void init() {
        var httpClient = WiremockHttpClient.create();
        cristinConnection = new CristinConnection(httpClient);
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
        var actualOrganization = cristinConnection.getCristinOrganizationByCristinId(randomOrganizationUri);
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
    void shouldReturnOrganizationIfCristinProxyRespondsWithOrganization(WireMockRuntimeInfo wireMockRuntimeInfo) {
        var randomOrganizationId = getRandomOrganizationUri(wireMockRuntimeInfo);
        var expectedOrganization = createExpectedOrganization(randomOrganizationId);
        mockCristinOrganization(randomOrganizationId, expectedOrganization.toJsonString());
        var actualOrganization = cristinConnection.getCristinOrganizationByCristinId(randomOrganizationId);
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
        var actualOrganization = cristinConnection.getCristinOrganizationByCristinId(cristinId);
        assertThat(actualOrganization, is(nullValue()));
    }

    private static URI getRandomPersonUri(WireMockRuntimeInfo wireMockRuntimeInfo) {
        return attempt(() -> new URIBuilder()
                                 .setHost(wireMockRuntimeInfo.getHttpsBaseUrl().replace("https://", ""))
                                 .setPath("cristin/person/" + randomString())
                                 .setScheme(HTTPS_SCHEME)
                                 .build())
                   .orElseThrow();
    }

    private Organization createExpectedOrganization(URI organizationId) {
        return new Organization(organizationId, null);
    }

    private Person createExpectedPerson(URI personId) {
        return new Person.Builder().withId(personId).build();
    }

    private URI getRandomOrganizationUri(WireMockRuntimeInfo wireMockRuntimeInfo) {
        return attempt(() -> new URIBuilder()
                                 .setHost(wireMockRuntimeInfo.getHttpsBaseUrl().replace("https://", ""))
                                 .setPath("cristin/organization/" + randomString())
                                 .setScheme(HTTPS_SCHEME)
                                 .build())
                   .orElseThrow();
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
