package no.unit.nva.publication;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import java.net.URI;
import java.nio.file.Path;
import no.unit.nva.testutils.JwtTestToken;
import nva.commons.core.ioutils.IoUtils;

public class CustomerApiStubs {

    private static final String TOKEN = JwtTestToken.randomToken();
    private static final String SUCCESSFUL_TOKEN_RESPONSE = """
        {
            "access_token": "%s"
        }
        """.formatted(TOKEN);

    private CustomerApiStubs() {
    }

    public static void stubSuccessfulTokenResponse() {
        stubFor(post(urlPathEqualTo("/oauth2/token"))
                    .withBasicAuth("id", "secret")
                    .willReturn(
                        aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(SUCCESSFUL_TOKEN_RESPONSE)));
    }

    public static void stubCustomSuccessfulCustomerResponse(URI customerId, String customerResponse) {
        stubFor(get(urlPathEqualTo(customerId.getPath()))
                    .withHeader("Authorization", equalTo("Bearer " + TOKEN))
                    .willReturn(
                        aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(customerResponse)));
    }

    public static void stubCustomerResponseAcceptingFilesForAllTypes(URI customerId) {
        stubCustomSuccessfulCustomerResponse(customerId, customerAcceptingFilesForAllTypes());
    }

    public static void stubCustomerResponseAcceptingFilesForAllTypesAndNotAllowingAutoPublishingFiles(URI customerId) {
        stubCustomSuccessfulCustomerResponse(customerId,
                                             customerAcceptingFilesForAllTypesNotAllowingAutoPublishingFiles());
    }

    public static void stubCustomerResponseAcceptingFilesForAllTypesAndOverridableRrs(URI customerId) {
        stubCustomSuccessfulCustomerResponse(customerId, customerAcceptingFilesForAllTypesAndOverridableRrs());
    }

    public static void stubSuccessfulCustomerResponseAllowingFilesForNoTypes(URI customerId) {
        stubCustomSuccessfulCustomerResponse(customerId, customerAcceptingFilesForNoTypes());
    }

    public static void stubCustomerResponseNotFound(URI customerId) {
        stubFor(get(urlPathEqualTo(customerId.getPath()))
                    .withHeader("Authorization", equalTo("Bearer " + TOKEN))
                    .willReturn(
                        aResponse()
                            .withStatus(404)));
    }

    private static String customerAcceptingFilesForAllTypes() {
        return IoUtils.stringFromResources(
            Path.of("customerWithAllTypesAllowingFileAndAllowingAutoApprovalOfPublishingRequests.json"));
    }

    private static String customerAcceptingFilesForAllTypesNotAllowingAutoPublishingFiles() {
        return IoUtils.stringFromResources(
            Path.of("customerWithAllTypesAllowingFileAndNotAllowingAutoApprovalOfPublishingRequests.json"));
    }

    private static String customerAcceptingFilesForAllTypesAndOverridableRrs() {
        return IoUtils.stringFromResources(
            Path.of("customerWithAllTypesAllowingFileAndAllowingAutoApprovalOfPublishingRequestsOverridableRrs.json"));
    }

    private static String customerAcceptingFilesForNoTypes() {
        return IoUtils.stringFromResources(
            Path.of("customerWithNoTypesAllowingFileAndAllowingAutoApprovalOfPublishingRequests.json"));
    }
}
