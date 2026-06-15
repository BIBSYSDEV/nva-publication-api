# PR #2533 review notes (NP-51299: Scopus import timeout)

Working notes for the review of [PR #2533](https://github.com/BIBSYSDEV/nva-publication-api/pull/2533) and the follow-up changes applied on the branch.
Intended as preparation for discussing the PR with the tech lead.

## What the PR fixes

The Scopus import timed out (900 s Lambda ceiling) on XML files with thousands of contributors.
Root cause: `ParallelizeListProcessing` kept submitted futures in a lazy `Stream`, so the terminal `toList()` fused submit and join and the work ran serially.
The PR materializes the tasks, uses `invokeAll`, bounds concurrency with a `Semaphore(20)`, adds per-document memoization caches to `PiaConnection` and `CristinConnection`, adds HTTP retry (resilience4j), and removes a silent fallback that reprocessed the entire list serially on any single failure.

## Caching approach: resolved by the Java 25 migration

This was the main review concern, and it is now closed by the move to Java 25 (merged from main, commit `b892b52a4`).

History, for context if it comes up:

- On **Java 21**, putting the blocking HTTP fetch inside `ConcurrentHashMap.computeIfAbsent` was dangerous.
  `computeIfAbsent` runs the mapping function while holding a synchronized bin lock, and on Java 21 a virtual thread that blocks inside a synchronized region pins its carrier thread (JEP 444).
  On this Lambda (1800 MB, 1 vCPU) that would have collapsed effective concurrency from 20 to roughly 1-2, close to re-introducing the original serial bug.
  A `LookupCache` helper (backed by `CompletableFuture`) was written to keep the fetch out of the bin lock.
- On **Java 25** (JEP 491, shipped JDK 24) a virtual thread blocking inside `synchronized` no longer pins its carrier.
  The reason for the `LookupCache` helper is gone, so the branch now uses a plain `ConcurrentHashMap.computeIfAbsent` cache (the original PR approach) and the helper class plus its test have been removed.

Residual nuance to be aware of (minor, defensible):

- `computeIfAbsent` still holds the per-bin lock while the mapping function runs, so two lookups for *distinct* keys that hash to the *same* bin would serialize behind each other's HTTP call.
  With concurrency capped at 20 and the map growing to thousands of bins, same-bin collisions among in-flight computes are rare and the cost is one bounded HTTP wait.
  `get()` (the cache-hit path, which dominates after warm-up) is lock-free and never blocked.
- The `computeIfAbsent` javadoc advises the mapping function be "short and simple".
  We are technically against that guidance, but on Java 25 the practical impact is negligible and bounded by the concurrency cap.
  If we ever want to remove even this nuance, the fetch-then-`putIfAbsent` pattern does so without futures, at the cost of allowing rare duplicate concurrent fetches (acceptable, since single-fetch-per-ID is not a requirement).

## Checklist: act on or decide

### Must do before merge

- [ ] Squash the `wip:` commit (it currently includes the now-deleted `LookupCache` and this notes file) so the final diff shows only the plain-`ConcurrentHashMap` cache.
- [ ] Decide whether `NP-51299-review-notes.md` should be committed at all (it is an untracked/working doc; likely drop it from the PR).

### Should fix, small effort (decide: this PR or follow-up ticket)

- [ ] `ParallelizeListProcessing` uses `acquireUninterruptibly`, which makes the interruption/cancellation path mostly moot: all queued tasks still run to completion after an interrupt because they ignore it while waiting for a permit. Switching to `acquire()` with `InterruptedException` handling would make cancellation actually cancel.
- [ ] Retry policy is a fixed 100 ms wait with no backoff or jitter. For 429 responses from PIA, exponential backoff (`IntervalFunction.ofExponentialRandomBackoff`) would be more polite and more effective.
- [ ] `HttpRetry` builds a new `Retry` and `RetryConfig` per request. A `static final RetryConfig` is cheaper and clearer (the instance name is unused).
- [ ] `getCristinPersonByOrcId(null)` and `searchCristinOrganization(null)` throw NPE from `computeIfAbsent` where they previously returned empty. All current callers guard, so no live bug, but adding `isNull` guards (mirroring `getCristinPersonByCristinId`) closes the trap.
- [ ] Test polish: the two Cristin retry tests differ only by status code (collapse to `@ParameterizedTest`), and there is no happy-path retry test (e.g. 503 then 200 succeeds in 2 requests).

### Accepted trade-offs (be ready to defend, or change)

- [ ] Negative caching: a lookup that exhausts its 3 retries is cached as empty for the rest of the document, so every contributor sharing that affiliation silently loses it. Mitigated by the retry; alternative is evicting empty results.
- [ ] On a single job failure, `invokeAll` still waits for all remaining tasks before the exception propagates. Acceptable, and strictly better than the old silent serial fallback.
- [ ] Concurrency limit of 20 is a guess. The retry handling gives a feedback signal, so it can be tuned empirically against PIA's rate limits later.
- [ ] `computeIfAbsent` bin-lock nuance on Java 25 (see section above). Minor and bounded; documented here so it is a conscious choice rather than an oversight.

### Bigger levers (discuss with tech lead, separate tickets)

- [ ] Bump `MemorySize` for `NvaScopusFunction` (Globals default is 1800 MB = 1 vCPU). More vCPUs speed up XML parsing and give virtual threads more carriers; near cost-neutral if runtime drops proportionally.
- [ ] Cache across documents instead of `clearCache()` per document (TTL or container lifetime). Affiliations repeat heavily across publications, so warm invocations could skip most lookups. Trade-off: staleness within a container's lifetime.
- [ ] Ask the PIA/Cristin teams about batch endpoints. 15k HTTP round trips is the structural problem; concurrency divides it by 20, a batch API would divide it by hundreds.
- [ ] Partial-progress checkpointing (persist resolved contributors, or fan out author groups). Only worth it if the steps above do not comfortably clear the 900 s ceiling.

### Java 25 migration note (now done)

- [x] Project migrated to Java 25 and deployed to production (commit `b892b52a4`, Lambda `Runtime: java25`).
  JEP 491 removes synchronized pinning, which is what allowed the cache to be simplified back to plain `computeIfAbsent`.
