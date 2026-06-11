package no.sikt.nva.scopus.conversion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.IntStream;
import no.sikt.nva.scopus.paralleliseutils.ParallelizeListProcessing;
import org.junit.jupiter.api.Test;

class LookupCacheTest {

  private static final String KEY = "key";
  private static final String VALUE = "value";

  @Test
  void shouldFetchOnlyOnceForRepeatedLookupsOfSameKey() {
    var cache = new LookupCache<String, String>();
    var fetchCount = new AtomicInteger();
    Function<String, String> countingFetcher = countingFetcher(fetchCount);

    var firstResult = cache.getOrFetch(KEY, countingFetcher);
    var secondResult = cache.getOrFetch(KEY, countingFetcher);

    assertThat(firstResult, is(equalTo(VALUE)));
    assertThat(secondResult, is(equalTo(VALUE)));
    assertThat(fetchCount.get(), is(equalTo(1)));
  }

  @Test
  void shouldFetchAgainAfterCacheIsCleared() {
    var cache = new LookupCache<String, String>();
    var fetchCount = new AtomicInteger();
    Function<String, String> countingFetcher = countingFetcher(fetchCount);

    cache.getOrFetch(KEY, countingFetcher);
    cache.clear();
    cache.getOrFetch(KEY, countingFetcher);

    assertThat(fetchCount.get(), is(equalTo(2)));
  }

  @Test
  void shouldPropagateFetcherExceptionAndRetryOnNextLookup() {
    var cache = new LookupCache<String, String>();
    var fetchCount = new AtomicInteger();
    Function<String, String> failingOnceFetcher =
        key -> {
          if (fetchCount.incrementAndGet() == 1) {
            throw new IllegalStateException("transient fetch failure");
          }
          return VALUE;
        };

    assertThrows(IllegalStateException.class, () -> cache.getOrFetch(KEY, failingOnceFetcher));

    assertThat(cache.getOrFetch(KEY, failingOnceFetcher), is(equalTo(VALUE)));
    assertThat(fetchCount.get(), is(equalTo(2)));
  }

  @Test
  void shouldFetchOnlyOnceWhenSameKeyIsLookedUpConcurrently() {
    var cache = new LookupCache<String, Integer>();
    var concurrentLookups = 16;
    var fetchCount = new AtomicInteger();
    var allLookupsStarted = new CountDownLatch(concurrentLookups);
    var lookupIndexes = IntStream.range(0, concurrentLookups).boxed().toList();

    Function<Integer, Integer> lookup =
        index -> {
          allLookupsStarted.countDown();
          return cache.getOrFetch(
              KEY,
              ignored -> {
                awaitLatch(allLookupsStarted);
                return fetchCount.incrementAndGet();
              });
        };

    var results =
        ParallelizeListProcessing.runAsVirtualThreads(lookupIndexes, lookup, concurrentLookups);

    assertThat(fetchCount.get(), is(equalTo(1)));
    assertThat(results, everyItem(is(equalTo(1))));
  }

  /**
   * Regression test for virtual-thread carrier pinning. The fetcher blocks until every lookup is in
   * flight at the same time. There are four times as many lookups as carrier threads, so if a fetch
   * blocked while holding a lock that pins its carrier (as {@code
   * ConcurrentHashMap.computeIfAbsent} does on Java 21), the latch could never reach zero, the
   * awaits would time out and the assertion would fail.
   */
  @Test
  void shouldKeepAllFetchesForDistinctKeysConcurrentlyInFlight() {
    var cache = new LookupCache<Integer, Boolean>();
    var lookupCount = Runtime.getRuntime().availableProcessors() * 4;
    var allFetchesInFlight = new CountDownLatch(lookupCount);
    var keys = IntStream.range(0, lookupCount).boxed().toList();

    Function<Integer, Boolean> lookup =
        key ->
            cache.getOrFetch(
                key,
                ignored -> {
                  allFetchesInFlight.countDown();
                  return awaitLatch(allFetchesInFlight);
                });

    var results = ParallelizeListProcessing.runAsVirtualThreads(keys, lookup, lookupCount);

    assertThat(results, everyItem(is(true)));
  }

  private static Function<String, String> countingFetcher(AtomicInteger fetchCount) {
    return key -> {
      fetchCount.incrementAndGet();
      return VALUE;
    };
  }

  private static boolean awaitLatch(CountDownLatch latch) {
    try {
      return latch.await(5, TimeUnit.SECONDS);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      return false;
    }
  }
}
