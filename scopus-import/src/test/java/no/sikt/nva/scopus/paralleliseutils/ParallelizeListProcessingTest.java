package no.sikt.nva.scopus.paralleliseutils;

import static no.sikt.nva.scopus.paralleliseutils.ParallelizeListProcessing.DEFAULT_MAX_CONCURRENT_NETWORKING_OPERATIONS;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class ParallelizeListProcessingTest {

  @Test
  void shouldHandleEmptyList() {
    List<String> emptyList = new ArrayList<>();
    assertDoesNotThrow(
        () ->
            ParallelizeListProcessing.runAsVirtualNetworkingCallingThreads(
                emptyList, String::toLowerCase));
  }

  @Test
  void shouldHandleListOfLessTaskThanThreads() {
    var inputListShorterThanOutPut =
        IntStream.range(0, DEFAULT_MAX_CONCURRENT_NETWORKING_OPERATIONS - 1)
            .mapToObj(ignored -> randomString().toLowerCase())
            .toList();
    var result =
        ParallelizeListProcessing.runAsVirtualNetworkingCallingThreads(
            inputListShorterThanOutPut, String::toUpperCase);
    assertThat(result, hasSize(inputListShorterThanOutPut.size()));
    for (var index = 0; index < inputListShorterThanOutPut.size(); index++) {
      assertThat(
          result.get(index), is(equalTo(inputListShorterThanOutPut.get(index).toUpperCase())));
    }
  }

  @Test
  void shouldPreserveInputOrderWhenListSizeExceedsConcurrencyLimit() {
    var inputLargerThanConcurrencyLimit =
        IntStream.range(0, DEFAULT_MAX_CONCURRENT_NETWORKING_OPERATIONS * 3 + 1)
            .mapToObj(ignored -> randomString().toLowerCase())
            .toList();
    var result =
        ParallelizeListProcessing.runAsVirtualNetworkingCallingThreads(
            inputLargerThanConcurrencyLimit, String::toUpperCase);
    assertThat(result, hasSize(inputLargerThanConcurrencyLimit.size()));
    for (var index = 0; index < inputLargerThanConcurrencyLimit.size(); index++) {
      assertThat(
          result.get(index), is(equalTo(inputLargerThanConcurrencyLimit.get(index).toUpperCase())));
    }
  }

  @Test
  void shouldRunJobsConcurrentlyUpToTheGivenLimit() {
    var maxConcurrency = 5;
    var inputSize = 50;
    var input = IntStream.range(0, inputSize).boxed().toList();
    var currentConcurrency = new AtomicInteger();
    var observedMaxConcurrency = new AtomicInteger();
    var allConcurrentTasksStarted = new CountDownLatch(maxConcurrency);

    Function<Integer, Integer> job =
        value -> {
          var running = currentConcurrency.incrementAndGet();
          observedMaxConcurrency.accumulateAndGet(running, Math::max);
          allConcurrentTasksStarted.countDown();
          awaitQuietly(allConcurrentTasksStarted);
          currentConcurrency.decrementAndGet();
          return value * 2;
        };

    var result = ParallelizeListProcessing.runAsVirtualThreads(input, job, maxConcurrency);

    assertThat(result, hasSize(inputSize));
    assertThat(observedMaxConcurrency.get(), is(greaterThan(1)));
    assertThat(observedMaxConcurrency.get(), is(lessThanOrEqualTo(maxConcurrency)));
  }

  @Test
  void singleFailingJobWillCauseTheEntireProcessToFail() {
    var someIrrelevantInputListWithElements =
        IntStream.range(0, DEFAULT_MAX_CONCURRENT_NETWORKING_OPERATIONS)
            .mapToObj(ignored -> randomString().toLowerCase())
            .toList();
    assertThrows(
        RuntimeException.class,
        () ->
            ParallelizeListProcessing.runAsVirtualNetworkingCallingThreads(
                someIrrelevantInputListWithElements, this::throwingException));
  }

  @Test
  void shouldRethrowAsCompletionExceptionWhenCallingThreadIsInterrupted() {
    var input = IntStream.range(0, 8).boxed().toList();
    Thread.currentThread().interrupt();
    try {
      assertThrows(
          CompletionException.class,
          () -> ParallelizeListProcessing.runAsVirtualThreads(input, this::sleepThenReturn, 2));
    } finally {
      Thread.interrupted();
    }
  }

  private Integer sleepThenReturn(Integer value) {
    try {
      Thread.sleep(500);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
    }
    return value;
  }

  private void awaitQuietly(CountDownLatch latch) {
    try {
      latch.await(2, TimeUnit.SECONDS);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
    }
  }

  private String throwingException(String input) {
    throw new UnsupportedOperationException();
  }
}
