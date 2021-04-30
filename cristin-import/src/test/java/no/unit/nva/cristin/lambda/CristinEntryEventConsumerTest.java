package no.unit.nva.cristin.lambda;

import static java.util.Objects.nonNull;
import static no.unit.nva.cristin.lambda.CristinEntryEventConsumer.ERROR_SAVING_CRISTIN_RESULT;
import static nva.commons.core.JsonUtils.objectMapperNoEmpty;
import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.ioutils.IoUtils.stringToStream;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.time.Clock;
import no.unit.nva.cristin.mapper.CristinObject;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.service.ResourcesDynamoDbLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.storage.model.UserInstance;
import no.unit.nva.testutils.IoUtils;
import nva.commons.core.SingletonCollector;
import nva.commons.logutils.LogUtils;
import nva.commons.logutils.TestAppender;
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.core.diff.Diff;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

public class CristinEntryEventConsumerTest extends ResourcesDynamoDbLocalTest {

    public static final Path VALID_CRISTIN_ENTRY_EVENT = Path.of("valid_cristin_entry_event.json");
    public static final AwsEventBridgeEvent<CristinObject> VALID_CRISTIN_ENTRY_EVENT_OBJECT =
        parseEvent(IoUtils.stringFromResources(VALID_CRISTIN_ENTRY_EVENT));

    public static final Context CONTEXT = mock(Context.class);
    public static final String DETAIL_FIELD = "detail";
    public static final Javers JAVERS = JaversBuilder.javers().build();
    public static final String RESOURCE_EXCEPTION_MESSAGE = "resourceExceptionMessage";

    private CristinEntryEventConsumer handler;
    private ByteArrayOutputStream outputStream;
    private ResourceService resourceService;

    @BeforeEach
    public void init() {
        super.init();
        resourceService = new ResourceService(client, Clock.systemDefaultZone());
        handler = new CristinEntryEventConsumer(resourceService);
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    public void handlerReturnsAnNvaPublicationEntryWhenInputIsEventWithCristinResult() throws JsonProcessingException {
        String input = validEvent();
        handler.handleRequest(stringToStream(input), outputStream, CONTEXT);
        String json = outputStream.toString();
        Publication actualPublication = objectMapperNoEmpty.readValue(json, Publication.class);

        Publication expectedPublication = generatePublicationFromResource(input).toPublication();
        expectedPublication.setIdentifier(actualPublication.getIdentifier());
        assertThat(actualPublication, is(equalTo(expectedPublication)));
    }

    @Test
    public void handlerSavesPublicationToDynamoDbWhenInputIsEventWithCristinResult() throws JsonProcessingException {
        String input = validEvent();
        handler.handleRequest(stringToStream(input), outputStream, CONTEXT);
        CristinObject cristinObject = generatePublicationFromResource(input);
        Publication expectedPublication = cristinObject.toPublication();
        UserInstance userInstance = extractUserInstance(expectedPublication);

        Publication actualPublication = resourceService.getPublicationsByOwner(userInstance)
                                            .stream().collect(SingletonCollector.collect());

        expectedPublication.setIdentifier(actualPublication.getIdentifier());
        Diff diff = JAVERS.compare(expectedPublication, actualPublication);
        assertThat(diff.prettyPrint(), actualPublication, is(equalTo(expectedPublication)));
    }

    @Test
    public void handlerThrowsExceptionWhenInputDetailTypeIsNotTheExpected() throws JsonProcessingException {
        String unexpectedDetailType = "unexpectedDetailType";
        String input = eventWithInvalidDetailType(unexpectedDetailType);
        Executable action = () -> handler.handleRequest(stringToStream(input), outputStream, CONTEXT);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, action);
        assertThat(exception.getMessage(), containsString(CristinEntriesEventEmitter.EVENT_DETAIL_TYPE));
        assertThat(exception.getMessage(), containsString(unexpectedDetailType));
    }

    @Test
    public void handlerLogsErrorWhenFailingToStorePublicationToDynamo() {

        TestAppender appender = LogUtils.getTestingAppender(CristinEntryEventConsumer.class);
        resourceService = new ResourceService(client, Clock.systemDefaultZone()) {
            @Override
            public Publication createPublicationWithPredefinedCreationDate(Publication publication) {
                throw new RuntimeException(RESOURCE_EXCEPTION_MESSAGE);
            }
        };
        handler = new CristinEntryEventConsumer(resourceService);
        String cristinIdentifier = VALID_CRISTIN_ENTRY_EVENT_OBJECT.getDetail().getId();
        Executable action = () -> handler.handleRequest(stringToStream(validEvent()), outputStream, CONTEXT);
        RuntimeException exception = assertThrows(RuntimeException.class, action);
        assertThat(exception.getMessage(), containsString(ERROR_SAVING_CRISTIN_RESULT + cristinIdentifier));
        assertThat(appender.getMessages(), containsString(ERROR_SAVING_CRISTIN_RESULT + cristinIdentifier));
        assertThat(exception.getCause().getMessage(), containsString(RESOURCE_EXCEPTION_MESSAGE));
        assertThat(appender.getMessages(), containsString(RESOURCE_EXCEPTION_MESSAGE));
    }

    private static AwsEventBridgeEvent<CristinObject> parseEvent(String input) {
        JavaType javaType =
            objectMapperNoEmpty.getTypeFactory()
                .constructParametricType(AwsEventBridgeEvent.class, CristinObject.class);
        return attempt(
            () -> objectMapperNoEmpty.<AwsEventBridgeEvent<CristinObject>>readValue(input, javaType)).orElseThrow();
    }

    private String validEvent() {
        return IoUtils.stringFromResources(VALID_CRISTIN_ENTRY_EVENT);
    }

    private String eventWithInvalidDetailType(String invalidDetailType) throws JsonProcessingException {
        String input = validEvent();
        AwsEventBridgeEvent<CristinObject> event = parseEvent(input);

        event.setDetailType(invalidDetailType);
        input = objectMapperNoEmpty.writeValueAsString(event);
        return input;
    }

    private UserInstance extractUserInstance(Publication publication) {
        return new UserInstance(publication.getOwner(), publication.getPublisher().getId());
    }

    private CristinObject generatePublicationFromResource(String input) throws JsonProcessingException {
        JsonNode jsonNode = objectMapperNoEmpty.readTree(input);
        JsonNode detail = jsonNode.get(DETAIL_FIELD);
        CristinObject object = objectMapperNoEmpty.convertValue(detail, CristinObject.class);
        assert nonNull(object.getId()); //java assertion produces Error not exception
        return object;
    }
}