package no.unit.nva.publication.external.services;

import static no.unit.nva.publication.TestingUtils.createRandomOrgUnitId;
import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsIterableContaining.hasItems;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.stream.Collectors;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadGatewayException;
import nva.commons.logutils.LogUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Status;

class PersonApiClientTest {

    private PersonApiClient personApiClient;
    private String errorMessage;
    private String inputFeideId;
    private int randomFailureCode;
    private URI mockOrgUnitId;
    private String successfulNonEmptyResponse;
    private String successfulEmptyResponse;

    @BeforeEach
    public void init() {
        inputFeideId = randomString();
        errorMessage = randomString();
        mockOrgUnitId = createRandomOrgUnitId();
        successfulNonEmptyResponse = PersonApiResponseBodyMock.createResponse(inputFeideId, mockOrgUnitId).toString();
        successfulEmptyResponse = PersonApiResponseBodyMock.createResponse(inputFeideId).toString();
        randomFailureCode = randomNonSuccessfulStatusCode();
    }

    @Test
    void shouldReturnPersonsOrgUnitIdsWhenInputIsUserExistingInPersonService()
        throws IOException, InterruptedException, ApiGatewayException {
        personApiClient = new PersonApiClient(createMockHttpClientReturningResponse(successfulNonEmptyResponse()));
        var userAffiliations = personApiClient.fetchAffiliationsForUser(inputFeideId);
        assertThat(userAffiliations, is(hasItems(mockOrgUnitId)));
    }

    @Test
    void shouldReturnEmptyListIfUserWasNotFoundInPersonService()
        throws IOException, InterruptedException, ApiGatewayException {
        personApiClient = new PersonApiClient(createMockHttpClientReturningResponse(successfulEmptyResponse()));
        var userAffiliations = personApiClient.fetchAffiliationsForUser(inputFeideId);
        assertThat(userAffiliations, is(empty()));
    }

    @Test
    void shouldReturnBadGatewayWhenResponseIsNotSuccessful() throws IOException, InterruptedException {
        var logger = LogUtils.getTestingAppenderForRootLogger();
        personApiClient = new PersonApiClient(createMockHttpClientReturningResponse(badRequestResponse()));
        assertThrows(BadGatewayException.class, () -> personApiClient.fetchAffiliationsForUser(inputFeideId));
        assertThat(logger.getMessages(), containsString(errorMessage));
    }


    @SuppressWarnings("unchecked")
    private HttpResponse<String> badRequestResponse() {
        var response = (HttpResponse<String>) mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(randomFailureCode);
        when(response.body()).thenReturn(errorMessage);
        return response;
    }

    private int randomNonSuccessfulStatusCode() {
        var notSuccessfulValues = Arrays.stream(Status.values())
            .filter(status -> !Status.OK.equals(status))
            .map(Status::getStatusCode)
            .collect(Collectors.toList());

        return randomElement(notSuccessfulValues);
    }

    private HttpClient createMockHttpClientReturningResponse(HttpResponse<String> response)
        throws IOException, InterruptedException {
        var httpClient = mock(HttpClient.class);
        when(httpClient.send(any(HttpRequest.class), any())).thenAnswer((ignored -> response));
        return httpClient;
    }

    private HttpResponse<String> successfulNonEmptyResponse() {
        return mockSuccessfulResponse(successfulNonEmptyResponse);
    }

    private HttpResponse<String> successfulEmptyResponse() {
        return mockSuccessfulResponse(successfulEmptyResponse);
    }

    @SuppressWarnings("unchecked")
    private HttpResponse<String> mockSuccessfulResponse(String successfulEmptyResponse) {
        var response = (HttpResponse<String>) mock(HttpResponse.class);
        when(response.body()).thenReturn(successfulEmptyResponse);
        when(response.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        return response;
    }
}