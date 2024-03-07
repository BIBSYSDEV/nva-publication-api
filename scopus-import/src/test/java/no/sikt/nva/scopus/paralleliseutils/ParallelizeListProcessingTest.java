package no.sikt.nva.scopus.paralleliseutils;

import static no.sikt.nva.scopus.paralleliseutils.ParallelizeListProcessing.DEFAULT_NUMBER_OF_VIRTUAL_THREADS_FOR_NETWORKING_OPERATIONS;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

public class ParallelizeListProcessingTest {

    @Test
    void shouldHandleEmptyList() {
        List<String> emptyList = new ArrayList<>();
        assertDoesNotThrow(() -> ParallelizeListProcessing
                                     .runAsVirtualNetworkingCallingThreads(emptyList,
                                                                           String::toLowerCase));
    }

    @Test
    void shouldHandleListOfLessTaskThanThreads() {
        var inputListShorterThanOutPut =
            IntStream.range(0, DEFAULT_NUMBER_OF_VIRTUAL_THREADS_FOR_NETWORKING_OPERATIONS - 1)
                .mapToObj(ignored -> randomString().toLowerCase())
                .toList();
        var result = ParallelizeListProcessing.runAsVirtualNetworkingCallingThreads(inputListShorterThanOutPut,
                                                                                    String::toUpperCase);
        assertThat(result, hasSize(inputListShorterThanOutPut.size()));
        for (var index = 0; index < inputListShorterThanOutPut.size(); index++) {
            assertThat(result.get(index), is(equalTo(inputListShorterThanOutPut.get(index).toUpperCase())));
        }
    }

    @Test
    void shouldHandleListWithSizeThatDividedByNumberOfThreadsHaveAremainder() {
        var inputListWithARemainder =
            IntStream.range(0,
                            DEFAULT_NUMBER_OF_VIRTUAL_THREADS_FOR_NETWORKING_OPERATIONS
                            + DEFAULT_NUMBER_OF_VIRTUAL_THREADS_FOR_NETWORKING_OPERATIONS / 2)
                .mapToObj(ignored -> randomString().toLowerCase())
                .toList();
        var result = ParallelizeListProcessing.runAsVirtualNetworkingCallingThreads(inputListWithARemainder,
                                                                                    String::toUpperCase);
        assertThat(result, hasSize(inputListWithARemainder.size()));
        for (var index = 0; index < inputListWithARemainder.size(); index++) {
            assertThat(result.get(index), is(equalTo(inputListWithARemainder.get(index).toUpperCase())));
        }
    }

    @Test
    void SingleFailingJobWillCauseTheEntireProcessToFail() {
        var someIrrelevantInputListWithElements =
            IntStream.range(0, DEFAULT_NUMBER_OF_VIRTUAL_THREADS_FOR_NETWORKING_OPERATIONS)
                .mapToObj(ignored -> randomString().toLowerCase())
                .toList();
        assertThrows(UnsupportedOperationException.class,
                     () -> ParallelizeListProcessing.runAsVirtualNetworkingCallingThreads(
                         someIrrelevantInputListWithElements,
                         this::throwingException));
    }

    private String throwingException(String input) {
        throw new UnsupportedOperationException();
    }
}
