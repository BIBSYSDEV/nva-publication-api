# PR #2533 review notes (NP-51299: Scopus import timeout)

Working notes for the review of [PR #2533](https://github.com/BIBSYSDEV/nva-publication-api/pull/2533) and the follow-up changes applied on the branch.
Intended as preparation for discussing the PR with the tech lead.

## What the PR fixes

The Scopus import timed out (900 s Lambda ceiling) on XML files with thousands of contributors.
Root cause: `ParallelizeListProcessing` kept submitted futures in a lazy `Stream`, so the terminal `toList()` fused submit and join and the work ran serially.
The PR materializes the tasks, uses `invokeAll`, bounds concurrency with a `Semaphore(20)`, adds per-document memoization caches to `PiaConnection` and `CristinConnection`, adds HTTP retry (resilience4j), and removes a silent fallback that reprocessed the entire list serially on any single failure.

## Main finding: virtual-thread pinning in the caches (fixed on branch)

The original cache implementation used `ConcurrentHashMap.computeIfAbsent` with the full HTTP fetch inside the mapping function.
`computeIfAbsent` runs the mapping function while holding a synchronized bin lock.
On Java 21 a virtual thread that blocks inside a synchronized region pins its carrier thread, and the scheduler does not compensate (JEP 444).
The Lambda runs `java21` at 1800 MB (1 vCPU per `template.yaml`), so effective concurrency would have collapsed from 20 to roughly the carrier count (~1-2), close to re-introducing the original bug.

Fix applied on the branch: a new `LookupCache<K, V>` backed by `ConcurrentHashMap<K, CompletableFuture<V>>`.
Only the future insertion happens under the map lock; the fetch runs lock-free and racing threads park on `join()`, which unmounts cleanly.

Evidence:

- New regression test `LookupCacheTest.shouldKeepAllFetchesForDistinctKeysConcurrentlyInFlight` requires `availableProcessors * 4` fetches to be in flight simultaneously.
- The identical scenario run against a `computeIfAbsent`-based cache (temporary scratch test, since deleted) failed as predicted, confirming both the diagnosis and that the regression test detects it.
- `:scopus-import:test`, `:scopus-import:check`, and the 100 % JaCoCo gate all pass with the fix.

## Checklist: act on or decide

### Must do before merge

- [ ] Review and commit the uncommitted `LookupCache` changes on the branch (new `LookupCache.java` and `LookupCacheTest.java`, modified `CristinConnection.java` and `PiaConnection.java`).

### Should fix, small effort (decide: this PR or follow-up ticket)

- [ ] `ParallelizeListProcessing` uses `acquireUninterruptibly`, which makes the interruption/cancellation path mostly moot: all queued tasks still run to completion after an interrupt because they ignore it while waiting for a permit. Switching to `acquire()` with `InterruptedException` handling would make cancellation actually cancel.
- [ ] Retry policy is a fixed 100 ms wait with no backoff or jitter. For 429 responses from PIA, exponential backoff (`IntervalFunction.ofExponentialRandomBackoff`) would be more polite and more effective.
- [ ] `HttpRetry` builds a new `Retry` and `RetryConfig` per request. A `static final RetryConfig` is cheaper and clearer (the instance name is unused).
- [ ] `getCristinPersonByOrcId(null)` and `searchCristinOrganization(null)` now throw NPE where they previously returned empty. All current callers guard, so no live bug, but adding `isNull` guards (mirroring `getCristinPersonByCristinId`) closes the trap.
- [ ] Test polish: the two Cristin retry tests differ only by status code (collapse to `@ParameterizedTest`), and there is no happy-path retry test (e.g. 503 then 200 succeeds in 2 requests).

### Accepted trade-offs (be ready to defend, or change)

- [ ] Negative caching: a lookup that exhausts its 3 retries is cached as empty for the rest of the document, so every contributor sharing that affiliation silently loses it. Mitigated by the retry; alternative is evicting empty results.
- [ ] On a single job failure, `invokeAll` still waits for all remaining tasks before the exception propagates. Acceptable, and strictly better than the old silent serial fallback.
- [ ] Concurrency limit of 20 is a guess. The retry handling gives a feedback signal, so it can be tuned empirically against PIA's rate limits later.

### Bigger levers (discuss with tech lead, separate tickets)

- [ ] Bump `MemorySize` for `NvaScopusFunction` (Globals default is 1800 MB = 1 vCPU). More vCPUs speed up XML parsing and give virtual threads more carriers; near cost-neutral if runtime drops proportionally.
- [ ] Cache across documents instead of `clearCache()` per document (TTL or container lifetime). Affiliations repeat heavily across publications, so warm invocations could skip most lookups. Trade-off: staleness within a container's lifetime.
- [ ] Ask the PIA/Cristin teams about batch endpoints. 15k HTTP round trips is the structural problem; concurrency divides it by 20, a batch API would divide it by hundreds.
- [ ] Partial-progress checkpointing (persist resolved contributors, or fan out author groups). Only worth it if the steps above do not comfortably clear the 900 s ceiling.

### Java 25 migration note

- [ ] JEP 491 (JDK 24+) removes synchronized pinning, so the original `computeIfAbsent` code would have roughly worked on Java 25. The `LookupCache` design is still correct there (bin locks would still serialize same-bin keys behind HTTP calls), so no rework is needed after migration. Blocker to track: AWS must ship a managed `java25` Lambda runtime (`java21` is the newest today).
