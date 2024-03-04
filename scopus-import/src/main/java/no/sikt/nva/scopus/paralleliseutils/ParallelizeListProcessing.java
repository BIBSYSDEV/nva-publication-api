package no.sikt.nva.scopus.paralleliseutils;

import static nva.commons.core.attempt.Try.attempt;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import org.apache.commons.collections4.ListUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ParallelizeListProcessing {

    //Trying to be polite here and not bomb external APIs.
    public static final int DEFAULT_NUMBER_OF_VIRTUAL_THREADS_FOR_IO_OPERATIONS = 4;
    private static final Logger logger = LoggerFactory.getLogger(ParallelizeListProcessing.class);

    private ParallelizeListProcessing() {

    }

    public static <I, R> List<R> runAsVirtualApiCallingThreads(List<I> inputList, Function<I, R> job) {
        return runAsVirtualThreads(inputList, job, DEFAULT_NUMBER_OF_VIRTUAL_THREADS_FOR_IO_OPERATIONS);
    }

    public static <I, R> List<R> runAsVirtualThreads(List<I> inputList, Function<I, R> job, int parallellLevel) {
        var listPreprosessed = ListUtils.partition(inputList, parallellLevel);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var futures =
                listPreprosessed.stream().map(i -> executor.submit(() -> processItems(i, job)));

            var result = futures.map(ParallelizeListProcessing::waitForFuture).flatMap(Collection::stream).toList();
            executor.shutdown();
            return result;
        } catch (Exception e) {
            logger.warn("Parallelising failed attempting synchronized processing");
            return inputList.stream().map(job).toList();
        }
    }

    private static <R> List<R> waitForFuture(Future<List<R>> future) {
        return attempt(future::get).orElseThrow();
    }

    private static <I, R> List<R> processItems(List<I> items, Function<I, R> job) {
        return items.stream().map(job).toList();
    }
}
