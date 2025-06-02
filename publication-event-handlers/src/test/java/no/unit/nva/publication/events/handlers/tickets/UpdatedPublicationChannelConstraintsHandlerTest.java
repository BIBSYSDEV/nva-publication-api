package no.unit.nva.publication.events.handlers.tickets;

import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.publication.model.business.PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_ONLY;
import static no.unit.nva.publication.model.business.publicationchannel.ChannelType.PUBLISHER;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.stream.Stream;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.contexttypes.Degree;
import no.unit.nva.model.contexttypes.Publisher;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.events.bodies.DataEntryUpdateEvent;
import no.unit.nva.publication.events.handlers.PublicationEventsConfig;
import no.unit.nva.publication.external.services.ChannelClaimDto;
import no.unit.nva.publication.external.services.ChannelClaimDto.ChannelClaim;
import no.unit.nva.publication.external.services.ChannelClaimDto.ChannelClaim.ChannelConstraint;
import no.unit.nva.publication.external.services.ChannelClaimDto.CustomerSummaryDto;
import no.unit.nva.publication.model.business.FilesApprovalThesis;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.PublishingWorkflow;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.business.UserClientType;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.business.publicationchannel.ClaimedPublicationChannel;
import no.unit.nva.publication.model.business.publicationchannel.NonClaimedPublicationChannel;
import no.unit.nva.publication.model.business.publicationchannel.PublicationChannel;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.publication.ticket.test.TicketTestUtils;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.stubs.FakeS3Client;
import no.unit.nva.testutils.EventBridgeEventBuilder;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.paths.UnixPath;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Updates pending file approving tickets organizational affiliation based on changes to the publication channel
 * constraints that apply.
 */
public class UpdatedPublicationChannelConstraintsHandlerTest extends ResourcesLocalTest {

    private static final UserInstance USER_INSTANCE = UserInstance.create(randomString(),
                                                                          PublicationGenerator.randomUri());
    private static final String NOT_RELEVANT = "notRelevant";
    private static final String[] STUDENT_THESIS_INSTANCE_TYPES = {
        "DegreeMaster",
        "DegreePhd",
        "ArtisticDegreePhd",
        "DegreeLicentiate",
        "OtherStudentWork"
    };

    private ByteArrayOutputStream output;
    private FakeContext context;
    private S3Driver s3Driver;
    private S3Client s3Client;
    private TicketService ticketService;
    private ResourceService resourceService;

    @BeforeEach
    void beforeEach() {
        super.init();
        output = new ByteArrayOutputStream();
        context = new FakeContext();
        s3Client = new FakeS3Client();
        s3Driver = new S3Driver(s3Client, PublicationEventsConfig.EVENTS_BUCKET);
        resourceService = getResourceServiceBuilder().build();
        ticketService = getTicketService();
    }

    @Test
    void shouldThrowExceptionWhenEventReferenceIsNotAvailableOnS3() {
        var handler = new UpdatedPublicationChannelConstraintsHandler(s3Client, ticketService, resourceService);

        assertThrows(RuntimeException.class, () -> handler.handleRequest(eventNotAvailableFromS3(), output, context));
    }

    @Test
    void shouldDoNothingWhenNonClaimedPublicationChannelIsAddedOnCreationOfEntity() throws IOException {
        var handler = new UpdatedPublicationChannelConstraintsHandler(s3Client, ticketService, resourceService);

        handler.handleRequest(nonClaimedPublicationChannelAddedEvent(), output, context);
    }

    @Test
    void shouldUpdatePendingTicketsWhenChannelIsClaimed() throws ApiGatewayException {
        var publication = TicketTestUtils.createPersistedDegreePublication(PUBLISHED, resourceService);
        var pendingFilesApprovalThesis = pendingFilesApprovalThesis(publication).persistNewTicket(ticketService);
        var completedFilesApprovalThesis = pendingFilesApprovalThesis(publication).
                                               complete(publication, USER_INSTANCE)
                                               .persistNewTicket(ticketService);
        var resourceIdentifier = publication.getIdentifier();
        var channelClaimId =
            ((Publisher) ((Degree) publication.getEntityDescription()
                                          .getReference()
                                          .getPublicationContext()).getPublisher()).getId();

        when(channelClaimClient.fetchChannelClaim(eq(channelClaimId))).thenThrow(new NotFoundException("Not found!"));

        var handler = new UpdatedPublicationChannelConstraintsHandler(s3Client, ticketService, resourceService);

        var claimingCustomerId = randomUri();
        var claimingOrganizationId = randomUri();
        assertDoesNotThrow(
            () -> handler.handleRequest(claimedPublicationChannelAddedEvent(claimingCustomerId,
                                                                            claimingOrganizationId,
                                                                            publication.getIdentifier()),
                                        output, context));
    }

    private PublishingRequestCase pendingFilesApprovalThesis(Publication publication) {
        var userInstance = new UserInstance(randomString(), PublicationGenerator.randomUri(),
                                            PublicationGenerator.randomUri(), PublicationGenerator.randomUri(),
                                            PublicationGenerator.randomUri(),
                                            List.of(), UserClientType.INTERNAL);
        return PublishingRequestCase.create(Resource.fromPublication(publication), userInstance,
                                            REGISTRATOR_PUBLISHES_METADATA_ONLY);
    }

    private Stream<TicketEntry> onePendingAndOneCompletedFileApprovalEntry(SortableIdentifier resourceIdentifier,
                                                                           URI customerId,
                                                                           URI organizationId) {
        var pending = FilesApprovalThesis.create(Resource.resourceQueryObject(resourceIdentifier),
                                                 UserInstance.create(new User("me@myOrg"), customerId),
                                                 organizationId,
                                                 PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_ONLY);
        pending.setStatus(TicketStatus.PENDING);
        var completed = FilesApprovalThesis.create(Resource.resourceQueryObject(resourceIdentifier),
                                                   UserInstance.create(new User("me@myOrg"), customerId),
                                                   organizationId,
                                                   PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_ONLY);
        completed.setStatus(TicketStatus.COMPLETED);

        return Stream.of(pending, completed);
    }

    private InputStream eventNotAvailableFromS3() {
        return EventBridgeEventBuilder.sampleLambdaDestinationsEvent(new EventReference(NOT_RELEVANT, randomUri()));
    }

    private InputStream validEvent(URI uri) {
        return EventBridgeEventBuilder.sampleLambdaDestinationsEvent(new EventReference(NOT_RELEVANT, uri));
    }

    private InputStream nonClaimedPublicationChannelAddedEvent() throws IOException {
        var channelClaimId = UriWrapper.fromUri(randomUri()).addChild(SortableIdentifier.next().toString()).getUri();
        var eventBody = eventBody(null, NonClaimedPublicationChannel.create(channelClaimId, SortableIdentifier.next(),
                                                                            PUBLISHER));
        var blobUri = s3Driver.insertEvent(UnixPath.of(randomString()), eventBody);

        return validEvent(blobUri);
    }

    private InputStream claimedPublicationChannelAddedEvent(URI customerId,
                                                            URI organizationId,
                                                            SortableIdentifier resourceIdentifier) throws IOException {

        var channelClaimId = UriWrapper.fromUri(randomUri()).addChild(SortableIdentifier.next().toString()).getUri();
        var claimedBy = new CustomerSummaryDto(customerId, organizationId);
        var channelClaimDto = new ChannelClaimDto(channelClaimId, claimedBy, new ChannelClaim(randomUri(),
                                                                                              new ChannelConstraint(
                                                                                                  "OwnerOnly",
                                                                                                  "Everyone",
                                                                                                  List.of(
                                                                                                      STUDENT_THESIS_INSTANCE_TYPES))));
        var eventBody = eventBody(
            null,
            ClaimedPublicationChannel.create(channelClaimDto, resourceIdentifier, PUBLISHER)
        );
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
