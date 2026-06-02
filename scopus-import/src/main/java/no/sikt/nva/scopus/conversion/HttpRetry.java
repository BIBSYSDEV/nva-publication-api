package no.sikt.nva.scopus.conversion;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class HttpRetry {

  private static final int MAX_ATTEMPTS = 3;
  private static final Duration WAIT_DURATION = Duration.ofMillis(100);
  private static final int TOO_MANY_REQUESTS = 429;
  private static final int FIRST_SERVER_ERROR = 500;

  private HttpRetry() {}

  public static HttpResponse<String> sendWithRetry(
      String name, Supplier<HttpResponse<String>> send) {
    return Retry.of(name, retryConfig()).executeSupplier(send);
  }

  private static RetryConfig retryConfig() {
    return RetryConfig.<HttpResponse<String>>custom()
        .maxAttempts(MAX_ATTEMPTS)
        .waitDuration(WAIT_DURATION)
        .retryOnResult(isTransientFailure())
        .retryExceptions(Exception.class)
        .build();
  }

  private static Predicate<HttpResponse<String>> isTransientFailure() {
    return response ->
        response.statusCode() == TOO_MANY_REQUESTS || response.statusCode() >= FIRST_SERVER_ERROR;
  }
}
