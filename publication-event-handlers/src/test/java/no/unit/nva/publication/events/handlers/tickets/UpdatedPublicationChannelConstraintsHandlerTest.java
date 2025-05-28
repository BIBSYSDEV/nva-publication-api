package no.unit.nva.publication.events.handlers.tickets;

import no.unit.nva.events.models.EventReference;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.events.bodies.DataEntryUpdateEvent;
import no.unit.nva.publication.events.handlers.PublicationEventsConfig;
import no.unit.nva.publication.model.business.publicationchannel.NonClaimedPublicationChannel;
import no.unit.nva.publication.model.business.publicationchannel.PublicationChannel;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.stubs.FakeS3Client;
import no.unit.nva.testutils.EventBridgeEventBuilder;
import nva.commons.core.paths.UnixPath;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import static no.unit.nva.publication.model.business.publicationchannel.ChannelType.PUBLISHER;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

/**
 * Updates pending file approving tickets organizational affiliation based on changes to the publication channel
 * constraints that apply.
 */
public class UpdatedPublicationChannelConstraintsHandlerTest {

    private static final String NOT_RELEVANT = "notRelevant";

    private ByteArrayOutputStream output;
    private FakeContext context;
    private S3Driver s3Driver;
    private S3Client s3Client;
    private TicketService ticketService;

    @BeforeEach
    void beforeEach() {
        output = new ByteArrayOutputStream();
        context = new FakeContext();
        s3Client = new FakeS3Client();
        s3Driver = new S3Driver(s3Client, PublicationEventsConfig.EVENTS_BUCKET);
        ticketService = mock(TicketService.class);
    }

    @Test
    void shouldThrowExceptionWhenEventReferenceIsNotAvailableOnS3() {
        var handler = new UpdatedPublicationChannelConstraintsHandler(s3Client, ticketService);

        assertThrows(RuntimeException.class, () -> handler.handleRequest(eventNotAvailableFromS3(), output, context));
    }

    @Test
    void shouldDoNothingWhenNonClaimedPublicationChannelIsAddedOnCreationOfEntity() {
        var handler = new UpdatedPublicationChannelConstraintsHandler(s3Client, ticketService);

        assertDoesNotThrow(() -> handler.handleRequest(publicationChannelConstraintAddedEvent(), output, context));
    }

    private InputStream emptyEvent() {
        return EventBridgeEventBuilder.sampleLambdaDestinationsEvent(new EventReference(NOT_RELEVANT, null));
    }

    private InputStream eventNotAvailableFromS3() {
        return EventBridgeEventBuilder.sampleLambdaDestinationsEvent(new EventReference(NOT_RELEVANT, randomUri()));
    }

    private InputStream validEvent(URI uri) {
        return EventBridgeEventBuilder.sampleLambdaDestinationsEvent(new EventReference(NOT_RELEVANT, uri));
    }

    private InputStream publicationChannelConstraintAddedEvent() throws IOException {
        var channelClaimId = UriWrapper.fromUri(randomUri()).addChild(SortableIdentifier.next().toString()).getUri();
        var eventBody = eventBody(null, NonClaimedPublicationChannel.create(channelClaimId, SortableIdentifier.next(), PUBLISHER));
        var blobUri = s3Driver.insertEvent(UnixPath.of(randomString()), eventBody);

        return validEvent(blobUri);
    }

    private String eventBody(
            PublicationChannel oldConstraint, PublicationChannel newConstraint) {
        return new DataEntryUpdateEvent(
                NOT_RELEVANT, oldConstraint, newConstraint)
                .toJsonString();
    }

}
