package no.sikt.nva.scopus.conversion;

import static java.util.Objects.isNull;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Memoizes lookups so concurrent and repeated requests for the same key trigger a single fetch.
 *
 * <p>The fetch runs outside the map's internal locks, unlike {@code computeIfAbsent}, which would
 * run it while holding a bin lock. On Java 21 a virtual thread that blocks inside such a lock pins
 * its carrier thread, serializing all in-flight lookups onto the few available carriers.
 */
public final class LookupCache<K, V> {

  private final Map<K, CompletableFuture<V>> entries = new ConcurrentHashMap<>();

  public V getOrFetch(K key, Function<K, V> fetcher) {
    var freshEntry = new CompletableFuture<V>();
    var existingEntry = entries.putIfAbsent(key, freshEntry);
    return isNull(existingEntry)
        ? fetchAndComplete(key, fetcher, freshEntry)
        : existingEntry.join();
  }

  public void clear() {
    entries.clear();
  }

  private V fetchAndComplete(K key, Function<K, V> fetcher, CompletableFuture<V> entry) {
    try {
      var value = fetcher.apply(key);
      entry.complete(value);
      return value;
    } catch (RuntimeException exception) {
      entries.remove(key, entry);
      entry.completeExceptionally(exception);
      throw exception;
    }
  }
}
