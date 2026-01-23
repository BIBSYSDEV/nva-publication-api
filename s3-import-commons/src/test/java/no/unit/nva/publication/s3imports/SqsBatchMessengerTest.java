package no.unit.nva.publication.s3imports;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import no.unit.nva.events.models.EventReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResultEntry;

class SqsBatchMessengerTest {

    private static final String QUEUE_URL = randomUri().toString();
    private SqsClient sqsClient;
    private SqsBatchMessenger sqsBatchMessenger;

    @BeforeEach
    void init() {
        sqsClient = mock(SqsClient.class);
        sqsBatchMessenger = new SqsBatchMessenger(sqsClient, QUEUE_URL);
    }

    @Test
    void shouldSplitListOfEventReferencesIntoBatchesNoLargerThanTen() {
        var longListOfEventReferences = listWithTwentyElements();
        when(sqsClient.sendMessageBatch(any(SendMessageBatchRequest.class)))
            .thenAnswer(invocation -> convertArgumentToResponse(invocation.getArgument(0)));
        var result = sqsBatchMessenger.sendMessages(longListOfEventReferences);

        var requestCaptor = ArgumentCaptor.forClass(SendMessageBatchRequest.class);
        verify(sqsClient, times(2)).sendMessageBatch(requestCaptor.capture());

        var capturedRequests = requestCaptor.getAllValues();
        assertThat(capturedRequests.stream()
                       .allMatch(req -> req.entries().size() <= 10), org.hamcrest.Matchers.is(true));
        assertThat(result.getSuccesses(), containsInAnyOrder(longListOfEventReferences.toArray()));
    }

    private SendMessageBatchResponse convertArgumentToResponse(SendMessageBatchRequest request) {
        var successEntries = request.entries().stream()
                                 .map(this::createResponseEntry)
                                 .toList();
        return SendMessageBatchResponse.builder()
                   .successful(successEntries)
                   .build();
    }

    private SendMessageBatchResultEntry createResponseEntry(SendMessageBatchRequestEntry entry) {
        return SendMessageBatchResultEntry.builder()
                   .id(entry.id())
                   .messageId(entry.id())
                   .build();
    }

    private List<EventReference> listWithTwentyElements() {
        return IntStream.range(0, 20)
                   .mapToObj(n -> randomEventReference())
                   .collect(Collectors.toList());
    }

    private EventReference randomEventReference() {
        return new EventReference(randomString(), randomUri());
    }
}
