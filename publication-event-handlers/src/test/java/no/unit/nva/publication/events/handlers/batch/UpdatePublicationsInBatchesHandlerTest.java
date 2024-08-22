package no.unit.nva.publication.events.handlers.batch;

import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.publication.external.services.UriRetriever;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.SearchService;
import no.unit.nva.stubs.FakeEventBridgeClient;
import nva.commons.core.ioutils.IoUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;

class UpdatePublicationsInBatchesHandlerTest extends ResourcesLocalTest {

    private static final Context CONTEXT = mock(Context.class);
    private UpdatePublicationsInBatchesHandler handler;
    private ByteArrayOutputStream output;
    private EventBridgeClient eventBridgeClient;
    private ResourceService resourceService;
    private UriRetriever uriRetriever;

    @BeforeEach
    public void setUp() {
        super.init();
        output = new ByteArrayOutputStream();
        eventBridgeClient = new FakeEventBridgeClient();
        resourceService = getResourceServiceBuilder().build();
        uriRetriever = mock(UriRetriever.class);
        handler = new UpdatePublicationsInBatchesHandler(eventBridgeClient, SearchService.create(uriRetriever,
                                                                                                 resourceService), resourceService);
    }

    @Test
    public void shouldEmitEvent() {
        var event = createEvent();
        handler.handleRequest(event, output, CONTEXT);
    }

    private InputStream createEvent() {
        var event = new AwsEventBridgeEvent<MyEvent>();
        event.setAccount(randomString());
        event.setVersion(randomString());
        event.setSource(randomString());
        event.setRegion(randomElement(Region.regions()));
        event.setDetail(new MyEvent(randomString(), randomString()));
        return IoUtils.stringToStream(event.toJsonString());
    }
}