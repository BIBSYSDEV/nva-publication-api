package no.sikt.nva.scopus.conversion;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.util.Objects.nonNull;
import static no.sikt.nva.scopus.conversion.PiaConnection.API_HOST;
import static no.sikt.nva.scopus.conversion.PiaConnection.PIA_PASSWORD_KEY;
import static no.sikt.nva.scopus.conversion.PiaConnection.PIA_REST_API_ENV_KEY;
import static no.sikt.nva.scopus.conversion.PiaConnection.PIA_SECRETS_NAME_ENV_KEY;
import static no.sikt.nva.scopus.conversion.PiaConnection.PIA_USERNAME_KEY;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.net.HttpURLConnection;
import java.util.Comparator;
import java.util.List;
import no.sikt.nva.scopus.conversion.model.pia.Affiliation;
import no.sikt.nva.scopus.utils.PiaResponseGenerator;
import no.unit.nva.stubs.FakeSecretsManagerClient;
import no.unit.nva.stubs.WiremockHttpClient;
import nva.commons.core.Environment;
import nva.commons.secrets.SecretsReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@WireMockTest(httpsEnabled = true)
public class PiaConnectionTest {

    private static final String PIA_SECRET_NAME = "someSecretName";
    private static final String PIA_USERNAME_SECRET_KEY = "someUserNameKey";
    private static final String PIA_PASSWORD_SECRET_KEY = "somePasswordNameKey";
    private PiaConnection piaConnection;

    @BeforeEach
    void init(WireMockRuntimeInfo wireMockRuntimeInfo) {
        var fakeSecretsManagerClient = new FakeSecretsManagerClient();
        fakeSecretsManagerClient.putSecret(PIA_SECRET_NAME, PIA_USERNAME_SECRET_KEY, randomString());
        fakeSecretsManagerClient.putSecret(PIA_SECRET_NAME, PIA_PASSWORD_SECRET_KEY, randomString());
        var secretsReader = new SecretsReader(fakeSecretsManagerClient);
        piaConnection = new PiaConnection(WiremockHttpClient.create(), secretsReader,
                                              createPiaConnectionEnvironment(wireMockRuntimeInfo));
    }

    @Test
    void shouldSelectTopLevelOrgWithHighestCountNumber() {
        var affiliationId = randomString();
        var affiliationList = new PiaResponseGenerator().generateAffiliations(affiliationId);
        var affiliationUnitWithHighestCount = affiliationList.stream()
                                                  .filter(this::allValuesArePresent)
                                                  .max(Comparator.comparingInt(aff -> Integer.parseInt(aff.getCount())))
                                                  .orElse(affiliationList.get(0));
        var response = PiaResponseGenerator.convertAffiliationsToJson(affiliationList);
        mockedPiaAffiliationIdSearch(affiliationId, response);
        var affiliationUri = piaConnection.getCristinOrganizationIdentifier(affiliationId);
        assertThat(affiliationUri.toString(), containsString(affiliationUnitWithHighestCount.getUnitIdentifier()));
    }

    @Test
    void shouldReturnNullWhenAffiliationListFromPiaIsEmpty() {
        var affiliationId = randomString();
        var response = PiaResponseGenerator.convertAffiliationsToJson(List.of());
        mockedPiaAffiliationIdSearch(affiliationId, response);
        var affiliationUri = piaConnection.getCristinOrganizationIdentifier(affiliationId);
        assertThat(affiliationUri, is(nullValue()));
    }

    private void mockedPiaAffiliationIdSearch(String affiliationId, String response) {

        stubFor(WireMock.get(urlPathEqualTo("/sentralimport/orgs/matches"))
                    .withQueryParam("affiliation_id", WireMock.equalTo("SCOPUS:" + affiliationId))
                    .willReturn(aResponse().withBody(response).withStatus(HttpURLConnection.HTTP_OK)));
    }

    private Environment createPiaConnectionEnvironment(WireMockRuntimeInfo wireMockRuntimeInfo) {
        var environment = mock(Environment.class);
        when(environment.readEnv(PIA_REST_API_ENV_KEY)).thenReturn(wireMockRuntimeInfo.getHttpsBaseUrl().replace(
            "https://", ""));
        when(environment.readEnv(API_HOST)).thenReturn(wireMockRuntimeInfo.getHttpsBaseUrl().replace(
            "https://", ""));
        when(environment.readEnv(PIA_USERNAME_KEY)).thenReturn(PIA_USERNAME_SECRET_KEY);
        when(environment.readEnv(PIA_PASSWORD_KEY)).thenReturn(PIA_USERNAME_SECRET_KEY);
        when(environment.readEnv(PIA_SECRETS_NAME_ENV_KEY)).thenReturn(PIA_SECRET_NAME);
        return environment;
    }

    private boolean allValuesArePresent(Affiliation affiliation) {
        return nonNull(affiliation.getInstitutionIdentifier())
               && nonNull(affiliation.getUnitIdentifier())
               && nonNull(affiliation.getCount());
    }
}
