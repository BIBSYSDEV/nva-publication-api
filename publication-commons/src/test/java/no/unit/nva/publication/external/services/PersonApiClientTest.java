package no.unit.nva.publication.external.services;

import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
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
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadGatewayException;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.logutils.LogUtils;
import nva.commons.logutils.TestAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Status;

class PersonApiClientTest {

    private static final String SUCCESSFUL_NON_EMPTY_RESPONSE =
        IoUtils.stringFromResources(Path.of("personApiClient", "successful_non_empty_response.json"));
    private static final String SUCCESSFUL_EMPTY_RESPONSE =
        IoUtils.stringFromResources(Path.of("personApiClient", "successful_empty_response.json"));
    private PersonApiClient personApiClient;
    private String errorMessage;

    private String inputFeideId;
    private int randomFailureCode;

    @BeforeEach
    public void init() {
        inputFeideId = randomString();
        errorMessage = randomString();
        randomFailureCode= randomNonSuccessfulStatusCode();
    }

    @Test
    void shouldReturnPersonsOrgUnitIdsWhenInputIsUserExistingInPersonService()
        throws IOException, InterruptedException, ApiGatewayException {
        personApiClient = new PersonApiClient(createMockHttpClientReturningResponse(successfulNonEmptyResponse()));

        List<URI> userAffiliations = personApiClient.fetchAffiliationsForUser(inputFeideId);
        assertThat(userAffiliations, is(hasItems(URI.create("https://api.cristin.no/v2/units/194.63.10.0"))));
    }

    @Test
    void shouldReturnEmptyListIfUserWasNotFoundInPersonService()
        throws IOException, InterruptedException, ApiGatewayException {
        personApiClient = new PersonApiClient(createMockHttpClientReturningResponse(successfulEmptyResponse()));
        List<URI> userAffiliations = personApiClient.fetchAffiliationsForUser(inputFeideId);
        assertThat(userAffiliations, is(empty()));
    }

    @Test
    void shouldReturnBadGatewayWhenResponseIsNotSuccessful() throws IOException, InterruptedException {
        TestAppender logger = LogUtils.getTestingAppenderForRootLogger();
        personApiClient = new PersonApiClient(createMockHttpClientReturningResponse(badRequestResponse()));
        assertThrows(BadGatewayException.class, () -> personApiClient.fetchAffiliationsForUser(inputFeideId));
        assertThat(logger.getMessages(), containsString(errorMessage));
    }

    private HttpResponse<String> badRequestResponse() {
        HttpResponse<String> response = mock(HttpResponse.class);
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
        HttpClient httpClient = mock(HttpClient.class);
        when(httpClient.send(any(HttpRequest.class), any())).thenAnswer((ignored -> response));
        return httpClient;
    }

    private HttpResponse<String> successfulNonEmptyResponse() {
        return mockSuccessfulResponse(SUCCESSFUL_NON_EMPTY_RESPONSE);
    }

    private HttpResponse<String> successfulEmptyResponse() {
        return mockSuccessfulResponse(SUCCESSFUL_EMPTY_RESPONSE);
    }

    private HttpResponse<String> mockSuccessfulResponse(String successfulEmptyResponse) {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.body()).thenReturn(successfulEmptyResponse);
        when(response.statusCode()).thenReturn(HttpURLConnection.HTTP_OK);
        return response;
    }
}