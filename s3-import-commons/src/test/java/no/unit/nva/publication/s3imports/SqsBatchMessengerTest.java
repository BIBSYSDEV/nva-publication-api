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

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import no.unit.nva.events.models.EventReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
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
        .then(r -> convertArgumentToResponse(r.getArguments()[0]));
    var result = sqsBatchMessenger.sendMessages(longListOfEventReferences);
    verify(sqsClient, times(2)).sendMessageBatch(argThat(isNotTooLarge()));
    assertThat(result.getSuccesses(), containsInAnyOrder(longListOfEventReferences.toArray()));
  }

  private static ArgumentMatcher<SendMessageBatchRequest> isNotTooLarge() {
    return sendMessageBatchRequest -> sendMessageBatchRequest.entries().size() >= 10;
  }

  private SendMessageBatchResponse convertArgumentToResponse(Object argument) {
    if (argument instanceof SendMessageBatchRequest request) {
      return SendMessageBatchResponse.builder()
          .successful(
              request.entries().stream()
                  .map(this::createResponseEntry)
                  .collect(Collectors.toList()))
          .build();
    } else {
      return SendMessageBatchResponse.builder().build();
    }
  }

  private SendMessageBatchResultEntry createResponseEntry(SendMessageBatchRequestEntry e) {
    return SendMessageBatchResultEntry.builder().id(e.id()).build();
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
