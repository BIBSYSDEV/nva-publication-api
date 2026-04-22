package no.unit.nva.publication.adapter;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MockIntegrations {

    public static final int DEFAULT_PORT = 8090;

    private static final Logger logger = LoggerFactory.getLogger(MockIntegrations.class);
    private static final String TOKEN_PATH = "/oauth2/token";
    private static final String CUSTOMER_PATH_PATTERN = "/customer/.*";
    private static final String FAKE_JWT =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"
        + ".eyJzdWIiOiJmYWtlLWJhY2tlbmQiLCJpYXQiOjE3MDAwMDAwMDAsImV4cCI6OTk5OTk5OTk5OX0"
        + ".dGVzdC1zaWduYXR1cmUtaWdub3JlZA";
    private static final String FAKE_ACCESS_TOKEN =
        "{\"access_token\":\"" + FAKE_JWT + "\",\"token_type\":\"Bearer\",\"expires_in\":3600}";
    private static final String DEFAULT_CUSTOMER_BODY = """
        {
          "allowFileUploadForTypes": [],
          "publicationWorkflow": "RegistratorPublishesMetadataAndFiles",
          "rightsRetentionStrategy": {
            "type": "NullRightsRetentionStrategy",
            "id": ""
          }
        }
        """;

    private MockIntegrations() {
    }

    public static WireMockServer start(int port) {
        var server = new WireMockServer(options().port(port));
        server.start();
        stubCognitoToken(server);
        stubCustomerApi(server);
        stubCatchAllOk(server);
        logger.info("Mock integrations listening on port {} (Cognito + Customer API)", port);
        return server;
    }

    private static void stubCognitoToken(WireMockServer server) {
        server.stubFor(post(urlPathEqualTo(TOKEN_PATH))
                           .willReturn(aResponse()
                                           .withStatus(200)
                                           .withHeader("Content-Type", "application/json")
                                           .withBody(FAKE_ACCESS_TOKEN)));
    }

    private static void stubCustomerApi(WireMockServer server) {
        server.stubFor(get(urlMatching(CUSTOMER_PATH_PATTERN))
                           .willReturn(aResponse()
                                           .withStatus(200)
                                           .withHeader("Content-Type", "application/json")
                                           .withBody(DEFAULT_CUSTOMER_BODY)));
    }

    private static void stubCatchAllOk(WireMockServer server) {
        server.stubFor(any(urlMatching(".*"))
                           .atPriority(10)
                           .willReturn(aResponse()
                                           .withStatus(200)
                                           .withHeader("Content-Type", "application/json")
                                           .withBody("{}")));
    }
}
