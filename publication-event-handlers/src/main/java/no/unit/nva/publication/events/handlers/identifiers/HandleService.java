package no.unit.nva.publication.events.handlers.identifiers;

import static io.vavr.control.Try.of;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.decorators.Decorators;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.vavr.control.Try;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.Supplier;
import no.unit.nva.auth.AuthorizedBackendClient;
import no.unit.nva.commons.json.JsonUtils;
import nva.commons.core.useragent.UserAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HandleService {

    public static final URI REPO = URI.create("https://github.com/BIBSYSDEV/nva-publication-api");
    public static final int BAD_REQUEST = 400;
    private final String handleBasePath;
    private final String apiDomain;
    private final AuthorizedBackendClient backendClient;
    private static final Logger logger = LoggerFactory.getLogger(HandleService.class);
    public static final String ACCEPT_VALUE = "application/json";
    public static final String ACCEPT_KEY = "Accept";
    public static final String USER_AGENT_KEY = "User-Agent";
    public static final String SIKT_EMAIL = "support@sikt.no";

    public HandleService(AuthorizedBackendClient backendClient, String apiDomain, String handleBasePath) {
        this.apiDomain = apiDomain;
        this.handleBasePath = handleBasePath;
        this.backendClient = backendClient;
    }

    public URI createHandle(URI uri) {
        return executeWithRetry(() -> executeRequest(uri));
    }

    private static <T> T executeWithRetry(Supplier<T> action) {
        var rateLimiter = createRateLimiter();
        var retryWithDefaultConfig = createRetryConfig();

        Supplier<T> decoratedSupplier = Decorators.ofSupplier(action)
                                              .withRateLimiter(rateLimiter)
                                              .withRetry(retryWithDefaultConfig)
                                              .decorate();

        return Try.ofSupplier(decoratedSupplier).get();
    }

    private static Retry createRetryConfig() {
        var retryRegistry = RetryRegistry.of(RetryConfig.custom()
                                                 .maxAttempts(5)
                                                 .intervalFunction(IntervalFunction.ofExponentialRandomBackoff())
                                                 .build());
        return retryRegistry.retry("executeRequest");
    }

    private static RateLimiter createRateLimiter() {
        var config = RateLimiterConfig.custom()
                         .limitRefreshPeriod(Duration.ofMinutes(1))
                         .limitForPeriod(100)
                         .timeoutDuration(Duration.ofSeconds(10))
                         .build();
        var rateLimiterRegistry = RateLimiterRegistry.of(config);
        return rateLimiterRegistry.rateLimiter("executeRequest");
    }

    private URI executeRequest(URI payloadUri) {
        logger.info("Requesting {} with payloadUri {}", createRequestUri(), payloadUri);
        return of(() -> attempt(() -> sendRequest(payloadUri))
                            .map(HttpResponse::body)
                            .map(HandleService::getHandleFromBody)
                            .map(URI::create)
                            .orElseThrow()).get();
    }

    private static String getHandleFromBody(String body) {
        try {
            return JsonUtils.dtoObjectMapper.readTree(body).get("handle").asText();
        } catch (JsonProcessingException e) {
            logger.error("Error processing JSON: \n\n" + body, e);
            throw new RuntimeException(e);
        }
    }

    private HttpResponse<String> sendRequest(URI payloadUri) throws IOException, InterruptedException {
        HttpResponse<String> response = backendClient.send(httpRequestBuilder(payloadUri), BodyHandlers.ofString(StandardCharsets.UTF_8));
        if(response.statusCode() >= BAD_REQUEST) {
            logger.error("Error response from server: {} \n{}",  response.statusCode(),
                         response.body());
            throw new RuntimeException("Request failed with status code: " + response.statusCode());
        }
        return response;
    }

    private Builder httpRequestBuilder(URI payloadUri) throws JsonProcessingException {
        return HttpRequest.newBuilder()
                   .uri(createRequestUri())
                   .headers(ACCEPT_KEY, ACCEPT_VALUE,
                            USER_AGENT_KEY, getUserAgent())
                   .POST(HttpRequest.BodyPublishers.ofString(JsonUtils.dtoObjectMapper.writeValueAsString(
                       createBody(payloadUri))));
    }

    private URI createRequestUri() {
        return URI.create("https://" + apiDomain + "/" + handleBasePath);
    }

    private static Object createBody(URI requestUri) {
        return new Object() {
            public final URI uri = requestUri;
        };
    }

    private String getUserAgent() {
        return UserAgent.newBuilder()
                   .client(getClass())
                   .environment(apiDomain)
                   .repository(REPO)
                   .email(SIKT_EMAIL)
                   .version("1.0")
                   .build()
                   .toString();
    }
}
