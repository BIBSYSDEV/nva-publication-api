package no.unit.nva.publication.events.handlers.batch;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import java.io.IOException;
import no.unit.nva.publication.events.bodies.ScanDatabaseRequest;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.stubs.FakeEventBridgeClient;
import nva.commons.core.ioutils.IoUtils;
import org.junit.jupiter.api.Test;

class BatchScanStartHandlerTest {

    private final FakeContext context = new FakeContext() {
        @Override
        public String getInvokedFunctionArn() {
            return randomString();
        }
    };

    @Test
    void shouldSendInitialScanMessageForInitiatingBatchScanning() throws IOException {
        FakeEventBridgeClient client = new FakeEventBridgeClient();
        BatchScanStartHandler handler = new BatchScanStartHandler(client);
        ScanDatabaseRequest scanDatabaseRequest = new ScanDatabaseRequest(1, null);
        var request = IoUtils.stringToStream(scanDatabaseRequest.toJsonString());
        handler.handleRequest(request, null, context);
        assertThat(client.getRequestEntries(), hasSize(1));
    }
}