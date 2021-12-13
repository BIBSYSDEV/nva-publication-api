package no.unit.nva.publication.service.impl;

import static no.unit.nva.publication.TestingUtils.randomOrgUnitId;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import no.unit.nva.publication.external.services.PersonApiResponseBodyMock;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AffiliationSelectionServiceTest {

    private String feideId;
    private String responseWithOneUserAffiliation;
    private URI singleOrgUnitId;

    @BeforeEach
    public void init() {
        feideId = randomString();
        singleOrgUnitId = randomOrgUnitId();
        responseWithOneUserAffiliation = PersonApiResponseBodyMock.createResponse(feideId, singleOrgUnitId).toString();
    }

    @Test
    void shouldReturnTheAffiliationUriWhenUserHasOnlyOneAffiliation()
        throws IOException, InterruptedException, ApiGatewayException {
        var affiliationService = newAffiliationService(mockResponseWithOneAffiliationForUser());
        var affiliationUri = affiliationService.fetchAffiliation(feideId).orElseThrow();
        assertThat(affiliationUri, is(equalTo(singleOrgUnitId)));
    }

    @Test
    void shouldReturnAffiliationSuchThatThereIsNoOtherAffiliationThatProvidesMoreInformationForSameInstitution()
        throws IOException, InterruptedException, ApiGatewayException {
        var leastSpecificUnitId = URI.create("https://api.cristin.no/v2/institutions/194");
        var midSpecificUnitId = URI.create("https://api.cristin.no/v2/units/194.63.0.0");
        var mostSpecificUnitId = URI.create("https://api.cristin.no/v2/units/194.63.10.0");
        var affiliationService =
            newAffiliationService(mockResponseWithAffiliations(midSpecificUnitId,
                                                               leastSpecificUnitId,
                                                               mostSpecificUnitId)
            );
        var actualAffiliation = affiliationService.fetchAffiliation(feideId).orElseThrow();
        assertThat(actualAffiliation, is(equalTo(mostSpecificUnitId)));
    }

    @Test
    void shouldReturnInstitutionAffiliationIfThatIsTheOnlyPresentOne()
        throws IOException, InterruptedException, ApiGatewayException {
        var expectedUri = URI.create("https://api.cristin.no/v2/institutions/194");
        var affiliationService =
            newAffiliationService(mockResponseWithAffiliations(expectedUri)
            );
        URI actualAffiliation = affiliationService.fetchAffiliation(feideId).orElseThrow();
        assertThat(actualAffiliation, is(equalTo(expectedUri)));
    }


    private AffiliationSelectionService newAffiliationService(HttpResponse<String> response)
        throws IOException, InterruptedException {
        return  AffiliationSelectionService.create(mockHttpClient(response));
    }

    private HttpClient mockHttpClient(HttpResponse<String> httpResponse) throws IOException, InterruptedException {
        var httpClient = mock(HttpClient.class);
        when(httpClient.send(any(HttpRequest.class), any())).thenAnswer(ignored -> httpResponse);
        return httpClient;
    }

    private HttpResponse<String> mockResponseWithOneAffiliationForUser() {
        return mockResponse(responseWithOneUserAffiliation);
    }

    private HttpResponse<String> mockResponseWithAffiliations(URI... orgUnitIds) {
        var body = PersonApiResponseBodyMock.createResponse(feideId, orgUnitIds).toString();
        return mockResponse(body);
    }

    @SuppressWarnings("unchecked")
    private HttpResponse<String> mockResponse(String body) {
        var response = (HttpResponse<String>) mock(HttpResponse.class);
        when(response.body()).thenReturn(body);
        when(response.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        return response;
    }
}