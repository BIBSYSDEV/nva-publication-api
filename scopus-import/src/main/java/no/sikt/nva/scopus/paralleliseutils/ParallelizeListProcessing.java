package no.sikt.nva.scopus.paralleliseutils;

import static nva.commons.core.attempt.Try.attempt;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.function.Function;

public final class ParallelizeListProcessing {

  // Upper bound on simultaneous calls so we parallelise the work without bombing the
  // external HTTP APIs (PIA, Cristin) with hundreds of concurrent requests.
  public static final int DEFAULT_MAX_CONCURRENT_NETWORKING_OPERATIONS = 20;

  private ParallelizeListProcessing() {}

  public static <I, R> List<R> runAsVirtualNetworkingCallingThreads(
      List<I> inputList, Function<I, R> job) {
    return runAsVirtualThreads(inputList, job, DEFAULT_MAX_CONCURRENT_NETWORKING_OPERATIONS);
  }

  @SuppressWarnings("PMD.DoNotUseThreads")
  public static <I, R> List<R> runAsVirtualThreads(
      List<I> inputList, Function<I, R> job, int maxConcurrency) {
    var concurrencyLimiter = new Semaphore(maxConcurrency);
    var tasks =
        inputList.stream()
            .map(item -> (Callable<R>) () -> runWithLimit(concurrencyLimiter, item, job))
            .toList();
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      return executor.invokeAll(tasks).stream()
          .map(ParallelizeListProcessing::waitForFuture)
          .toList();
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(exception);
    }
  }

  private static <I, R> R runWithLimit(Semaphore concurrencyLimiter, I item, Function<I, R> job) {
    concurrencyLimiter.acquireUninterruptibly();
    try {
      return job.apply(item);
    } finally {
      concurrencyLimiter.release();
    }
  }

  private static <R> R waitForFuture(Future<R> future) {
    return attempt(future::get).orElseThrow();
  }
}
