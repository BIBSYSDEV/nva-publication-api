package no.unit.nva.publication.events.handlers.batch;

import static no.unit.nva.publication.events.bodies.ScanDatabaseRequest.SCAN_IMPORT_CANDIDATES_REQUEST_EVENT_TOPIC;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import java.io.IOException;
import java.util.Objects;

import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.publication.events.bodies.ScanDatabaseRequest;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.stubs.FakeEventBridgeClient;
import nva.commons.core.ioutils.IoUtils;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;

class BatchScanStartHandlerTest {
    
    public static final int NOT_SET_PAGE_SIZE = 0;
    private final FakeContext context = new FakeContext() {
        @Override
        public String getInvokedFunctionArn() {
            return randomString();
        }
    };
    
    @Test
    void shouldSendInitialScanMessageWithDefaultPageSizeWhenPageSizeIsNotSet() throws IOException {
        var client = new FakeEventBridgeClient();
        var handler = new BatchScanStartHandler(client);
        var scanDatabaseRequest = new ScanDatabaseRequest(NOT_SET_PAGE_SIZE, null, null);
        var request = IoUtils.stringToStream(scanDatabaseRequest.toJsonString());
        handler.handleRequest(request, null, context);
        assertThat(client.getRequestEntries(), hasSize(1));
        var eventDetail = client.getRequestEntries().get(0).detail();
        var sentRequest = JsonUtils.dtoObjectMapper.readValue(eventDetail, ScanDatabaseRequest.class);
        assertThat(sentRequest.getPageSize(), is(equalTo(ScanDatabaseRequest.DEFAULT_PAGE_SIZE)));
    }
    
    @Test
    void shouldSendInitialScanMessageForInitiatingBatchScanning() throws IOException {
        FakeEventBridgeClient client = new FakeEventBridgeClient();
        BatchScanStartHandler handler = new BatchScanStartHandler(client);
        ScanDatabaseRequest scanDatabaseRequest = new ScanDatabaseRequest(1, null, null);
        var request = IoUtils.stringToStream(scanDatabaseRequest.toJsonString());
        handler.handleRequest(request, null, context);
        assertThat(client.getRequestEntries(), hasSize(1));
    }

    @Test
    void shouldSendInitialScanMessageForImportCandidatesWhenTopicIsProvided() throws IOException {
        FakeEventBridgeClient client = new FakeEventBridgeClient();
        BatchScanStartHandler handler = new BatchScanStartHandler(client);
        ScanDatabaseRequest scanDatabaseRequest = new ScanDatabaseRequest(1, null, SCAN_IMPORT_CANDIDATES_REQUEST_EVENT_TOPIC);
        var request = IoUtils.stringToStream(scanDatabaseRequest.toJsonString());
        handler.handleRequest(request, null, context);
        assertThat(client.getRequestEntries(), hasSize(1));
        PutEventsRequestEntry putEventsRequestEntry = client.getRequestEntries().get(0);
        var topic = putEventsRequestEntry.getValueForField("Detail", ScanDatabaseRequest.class);
        assertThat(putEventsRequestEntry, is(equalTo(null)));
    }
}