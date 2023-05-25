package no.unit.nva.publication.s3imports;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.SendMessageBatchRequest;
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.SendMessageBatchResult;
import com.amazonaws.services.sqs.model.SendMessageBatchResultEntry;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import no.unit.nva.events.models.EventReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;

public class SqsBatchMessengerTest {

    private final String QUEUE_URL = randomUri().toString();
    private AmazonSQS amazonSQS;
    private SqsBatchMessenger sqsBatchMessenger;

    @BeforeEach
    void init() {
        amazonSQS = mock(AmazonSQS.class);
        sqsBatchMessenger = new SqsBatchMessenger(amazonSQS, QUEUE_URL);
    }

    @Test
    void shouldSplitListOfEventReferencesIntoBatchesNoLargerThanTen() {
        var longListOfEventReferences = listWithTwentyElements();
        when(amazonSQS.sendMessageBatch(any())).then(r -> convertArgumentToResponse(r.getArguments()[0]));
        var result = sqsBatchMessenger.sendMessages(longListOfEventReferences);
        verify(amazonSQS, times(2)).sendMessageBatch(argThat(isNotTooLarge()));
        assertThat(result.getSuccesses(), containsInAnyOrder(longListOfEventReferences.toArray()));
    }

    private static ArgumentMatcher<SendMessageBatchRequest> isNotTooLarge() {
        return sendMessageBatchRequest -> sendMessageBatchRequest.getEntries().size() >= 10;
    }

    private SendMessageBatchResult convertArgumentToResponse(Object argument) {
        if (argument instanceof SendMessageBatchRequest) {
            var request = (SendMessageBatchRequest) argument;
            var sendMessageResult = new SendMessageBatchResult();
            sendMessageResult.setSuccessful(request.getEntries().stream().map(this::createResponseEntry).collect(
                Collectors.toList()));
            return sendMessageResult;
        } else {
            return new SendMessageBatchResult();
        }
    }

    private SendMessageBatchResultEntry createResponseEntry(SendMessageBatchRequestEntry e) {
        var sendMessageBatchResultEntry = new SendMessageBatchResultEntry();
        sendMessageBatchResultEntry.withId(e.getId());
        return sendMessageBatchResultEntry;
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
