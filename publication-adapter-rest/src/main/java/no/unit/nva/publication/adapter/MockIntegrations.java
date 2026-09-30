package no.unit.nva.publication.adapter;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MockIntegrations {

    public static final int DEFAULT_PORT = 8090;

    private static final Logger logger = LoggerFactory.getLogger(MockIntegrations.class);
    private static final String TOKEN_PATH = "/oauth2/token";
    private static final String CUSTOMER_PATH = "/customer/<customerId>";
    private static final String CATCH_ALL_PATH = "/<path>";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";
    private static final String EMPTY_JSON = "{}";
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

    public static Javalin start(int port) {
        var server = Javalin.create();
        server.post(TOKEN_PATH, context -> respondWith(context, FAKE_ACCESS_TOKEN));
        server.get(CUSTOMER_PATH, context -> respondWith(context, DEFAULT_CUSTOMER_BODY));
        registerCatchAll(server);
        server.start(port);
        logger.info("Mock integrations listening on port {} (Cognito + Customer API)", server.port());
        return server;
    }

    private static void registerCatchAll(Javalin server) {
        Stream.of(HandlerType.GET, HandlerType.POST, HandlerType.PUT, HandlerType.PATCH,
                  HandlerType.DELETE, HandlerType.HEAD, HandlerType.OPTIONS)
            .forEach(type -> server.addHttpHandler(type, CATCH_ALL_PATH,
                                                   context -> respondWith(context, EMPTY_JSON)));
    }

    private static void respondWith(Context context, String body) {
        context.status(200).header(CONTENT_TYPE, APPLICATION_JSON).result(body);
    }
}
