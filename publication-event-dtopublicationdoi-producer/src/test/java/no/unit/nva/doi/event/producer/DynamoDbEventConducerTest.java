package no.unit.nva.doi.event.producer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Path;
import no.unit.nva.events.handlers.EventParser;
import nva.commons.utils.IoUtils;
import nva.commons.utils.JsonUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DynamoDbEventConducerTest {

    private static final String WILDCARD = "*";
    public static final String EXAMPLE_NAMESPACE = "https://example.net/unittest/namespace/";
    public static final String DOI_PUBLICATION_TYPE = "doi.publication";
    private static final String DYNAMODB_STREAM_EVENT_OLD_AND_NEW_PRESENT_DIFFRENT =
        "dynamodbevent_old_and_new_present_different.json";
    private static final String DYNAMODB_STREAM_EVENT_OLD_AND_NEW_PRESENT_EQUAL =
        "dynamodbevent_old_and_new_present_equal.json";
    private static final String DYNAMODB_STREAM_EVENT_OLD_ONLY = "dynamodbevent_old_only.json";
    private AppConfig environmentMock;
    private static final String DYNAMODB_STREAM_EVENT = "dynamodbevent.json";
    private static ObjectMapper objectMapper = JsonUtils.objectMapper;
    private DynamoDbEventConducer handler;
    private Context context;

    /**
     * Setting up test environment.
     */
    @BeforeEach
    public void setUp() {
        handler = new DynamoDbEventConducer(EXAMPLE_NAMESPACE);
        context = mock(Context.class);
    }

    @Test
    void noNewImageWhileProcessingInputThenInEffectiveChangeIsNotIncluded() throws JsonProcessingException {
        var eventFile = IoUtils.stringFromResources(Path.of(DYNAMODB_STREAM_EVENT_OLD_ONLY));
        DynamodbEvent event = objectMapper.readValue(eventFile, DynamodbEvent.class);
        var eventBridgeEvent = new EventParser<DynamodbEvent>(
            eventFile).parse(DynamodbEvent.class);
        var actual = handler.processInput(event, eventBridgeEvent, context);

        assertThat(actual.getType(), is(equalTo(DOI_PUBLICATION_TYPE)));
        assertThat(actual.getItems(), hasSize(0));
    }

    @Test
    void differentOldAndNewImageWhileProcessingInputThenEffectiveChangeIsIncluded() throws JsonProcessingException {
        var eventFile = IoUtils.stringFromResources(Path.of(DYNAMODB_STREAM_EVENT_OLD_AND_NEW_PRESENT_DIFFRENT));
        DynamodbEvent event = objectMapper.readValue(eventFile, DynamodbEvent.class);
        var eventBridgeEvent = new EventParser<DynamodbEvent>(
            eventFile).parse(DynamodbEvent.class);
        var actual = handler.processInput(event, eventBridgeEvent, context);

        assertThat(actual.getType(), is(equalTo(DOI_PUBLICATION_TYPE)));
        assertThat(actual.getItems(), hasSize(1));
    }

    @Test
    void equalOldAndNewImageWhileProcessingInputThenInEffectiveChangeIsNotIncluded() throws JsonProcessingException {
        var eventFile = IoUtils.stringFromResources(Path.of(DYNAMODB_STREAM_EVENT_OLD_AND_NEW_PRESENT_EQUAL));
        DynamodbEvent event = objectMapper.readValue(eventFile, DynamodbEvent.class);
        var eventBridgeEvent = new EventParser<DynamodbEvent>(
            eventFile).parse(DynamodbEvent.class);
        var actual = handler.processInput(event, eventBridgeEvent, context);

        assertThat(actual.getType(), is(equalTo(DOI_PUBLICATION_TYPE)));
        assertThat(actual.getItems(), hasSize(0));
    }
}