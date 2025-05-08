package no.unit.nva.publication.events.handlers.batch;

import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.UUID;
import no.unit.nva.publication.events.handlers.batch.ChannelUpdateEvent.Action;
import no.unit.nva.publication.events.handlers.batch.ChannelUpdateEvent.PublicationChannelSummary;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.EventBridgeEventBuilder;
import nva.commons.core.paths.UriWrapper;
import nva.commons.logutils.LogUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PublicationChannelsUpdateHandlerTest {

    private PublicationChannelsUpdateHandler handler;
    private PublicationChannelsBatchUpdateService service;
    private ByteArrayOutputStream output;

    @BeforeEach
    void setUp() {
        output = new ByteArrayOutputStream();
        service = mock(PublicationChannelsBatchUpdateService.class);
        handler = new PublicationChannelsUpdateHandler(service);
    }

    @Test
    void shouldUpdatePublicationChannels() {
        handler.handleRequest(event(), output, new FakeContext());
    }

    @Test
    void shouldLogWhenSomethingWentWrongUpdatingPublicationChannels() {
        doThrow(RuntimeException.class).when(service).updateChannels(any());
        var appender = LogUtils.getTestingAppenderForRootLogger();
        handler.handleRequest(event(), output, new FakeContext());

        assertTrue(appender.getMessages().contains("Something went wrong updating publication channels"));
    }

    private static ChannelUpdateEvent randomChannelUpdateEvent() {
        return new ChannelUpdateEvent(Action.UPDATED, new PublicationChannelSummary(
            UriWrapper.fromUri(randomUri()).addChild(UUID.randomUUID().toString()).getUri(), randomUri(), randomUri(),
            randomUri(), null));
    }

    private InputStream event() {
        return EventBridgeEventBuilder.sampleLambdaDestinationsEvent(randomChannelUpdateEvent());
    }
}