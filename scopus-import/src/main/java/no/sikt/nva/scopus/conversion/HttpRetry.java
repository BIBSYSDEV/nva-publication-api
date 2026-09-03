package no.sikt.nva.scopus.conversion;

import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class HttpRetry {

  private static final int MAX_ATTEMPTS = 3;
  private static final Duration INITIAL_WAIT_DURATION = Duration.ofMillis(100);
  private static final double BACKOFF_MULTIPLIER = 2.0;
  private static final double JITTER_FACTOR = 0.5;
  private static final int TOO_MANY_REQUESTS = 429;
  private static final int FIRST_SERVER_ERROR = 500;
  private static final RetryRegistry RETRY_REGISTRY = RetryRegistry.of(retryConfig());

  private HttpRetry() {}

  public static HttpResponse<String> sendWithRetry(
      String name, Supplier<HttpResponse<String>> send) {
    return RETRY_REGISTRY.retry(name).executeSupplier(send);
  }

  private static RetryConfig retryConfig() {
    return RetryConfig.<HttpResponse<String>>custom()
        .maxAttempts(MAX_ATTEMPTS)
        .intervalFunction(
            IntervalFunction.ofExponentialRandomBackoff(
                INITIAL_WAIT_DURATION.toMillis(), BACKOFF_MULTIPLIER, JITTER_FACTOR))
        .retryOnResult(isTransientFailure())
        .retryExceptions(Exception.class)
        .build();
  }

  private static Predicate<HttpResponse<String>> isTransientFailure() {
    return response ->
        response.statusCode() == TOO_MANY_REQUESTS || response.statusCode() >= FIRST_SERVER_ERROR;
  }
}
