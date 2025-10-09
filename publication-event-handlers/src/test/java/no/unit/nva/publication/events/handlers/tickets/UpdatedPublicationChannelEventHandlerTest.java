package no.unit.nva.publication.events.handlers.tickets;

import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.publication.model.business.PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_ONLY;
import static no.unit.nva.publication.model.business.publicationchannel.ChannelType.PUBLISHER;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.events.bodies.DataEntryUpdateEvent;
import no.unit.nva.publication.events.handlers.PublicationEventsConfig;
import no.unit.nva.publication.external.services.ChannelClaimDto;
import no.unit.nva.publication.external.services.ChannelClaimDto.ChannelClaim;
import no.unit.nva.publication.external.services.ChannelClaimDto.ChannelClaim.ChannelConstraint;
import no.unit.nva.publication.external.services.ChannelClaimDto.CustomerSummaryDto;
import no.unit.nva.publication.model.business.FilesApprovalThesis;
import no.unit.nva.publication.model.business.Resource;
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
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.paths.UnixPath;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

/**
 * Updates pending file approving tickets organizational affiliation based on changes to the publication channel
 * constraints that apply.
 */
public class UpdatedPublicationChannelEventHandlerTest extends ResourcesLocalTest {

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
        resourceService = getResourceService(client);
        ticketService = getTicketService();
    }

    @Test
    void shouldThrowExceptionWhenEventReferenceIsNotAvailableOnS3() {
        var handler = new UpdatedPublicationChannelEventHandler(s3Client, ticketService, resourceService);

        assertThrows(NoSuchKeyException.class, () -> handler.handleRequest(eventNotAvailableFromS3(), output, context));
    }

    @Test
    void shouldDoNothingWhenNonClaimedPublicationChannelIsAddedOnCreationOfEntity() throws IOException {
        var handler = new UpdatedPublicationChannelEventHandler(s3Client, ticketService, resourceService);

        handler.handleRequest(nonClaimedPublicationChannelAddedEvent(), output, context);
    }

    @Test
    void shouldUpdatePendingTicketsWhenChannelIsClaimed() throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedDegreePublication(PUBLISHED, resourceService);
        var userTopLevelOrganizationId = randomUri();
        var userAffiliationOrganizationId = randomUri();
        var pendingTicket =
            pendingFilesApprovalThesis(publication, userTopLevelOrganizationId, userAffiliationOrganizationId)
                .persistNewTicket(ticketService);
        var completedTicket =
            pendingFilesApprovalThesis(publication, userTopLevelOrganizationId, userAffiliationOrganizationId)
                .complete(publication, USER_INSTANCE)
                .persistNewTicket(ticketService);

        var handler = new UpdatedPublicationChannelEventHandler(s3Client, ticketService, resourceService);

        var channelClaimIdentifier = SortableIdentifier.next();
        var claimingCustomerId = randomUri();
        var claimingOrganizationId = randomUri();

        var request = claimedPublicationChannelAddedEvent(channelClaimIdentifier,
                                                          claimingCustomerId,
                                                          claimingOrganizationId,
                                                          publication.getIdentifier());
        handler.handleRequest(request, output, context);

        var pendingTicketAfter = (FilesApprovalThesis) ticketService.fetchTicket(pendingTicket);
        assertTicketHasOrganizationDetails(pendingTicketAfter,
                                           claimingOrganizationId,
                                           claimingOrganizationId,
                                           channelClaimIdentifier);

        var completedTicketAfter = (FilesApprovalThesis) ticketService.fetchTicket(completedTicket);
        assertTicketHasOrganizationDetails(completedTicketAfter,
                                           completedTicketAfter.getOwnerAffiliation(),
                                           completedTicketAfter.getResponsibilityArea());
    }

    @Test
    void shouldUpdatePendingTicketsWhenChannelIsModifiedFromNonClaimedToClaimed() throws ApiGatewayException,
                                                                                         IOException {
        var publication = TicketTestUtils.createPersistedDegreePublication(PUBLISHED, resourceService);
        var userTopLevelOrganizationId = randomUri();
        var userAffiliationOrganizationId = randomUri();
        var pendingTicket =
            pendingFilesApprovalThesis(publication, userTopLevelOrganizationId, userAffiliationOrganizationId)
                .persistNewTicket(ticketService);
        var completedTicket =
            pendingFilesApprovalThesis(publication, userTopLevelOrganizationId, userAffiliationOrganizationId)
                .complete(publication, USER_INSTANCE)
                .persistNewTicket(ticketService);

        var handler = new UpdatedPublicationChannelEventHandler(s3Client, ticketService, resourceService);

        var channelClaimIdentifier = SortableIdentifier.next();
        var claimingCustomerId = randomUri();
        var claimingOrganizationId = randomUri();

        var request = nonClaimedToClaimedPublicationChannelEvent(channelClaimIdentifier,
                                                                 claimingCustomerId,
                                                                 claimingOrganizationId,
                                                                 publication.getIdentifier());
        handler.handleRequest(request, output, context);

        var pendingTicketAfter = (FilesApprovalThesis) ticketService.fetchTicket(pendingTicket);
        assertTicketHasOrganizationDetails(pendingTicketAfter,
                                           claimingOrganizationId,
                                           claimingOrganizationId,
                                           channelClaimIdentifier);

        var completedTicketAfter = (FilesApprovalThesis) ticketService.fetchTicket(completedTicket);
        assertTicketHasOrganizationDetails(completedTicketAfter,
                                           completedTicketAfter.getOwnerAffiliation(),
                                           completedTicketAfter.getResponsibilityArea());
    }

    @Test
    void shouldUpdatePendingTicketsWhenChannelIsModifiedFromClaimedToNonClaimed() throws ApiGatewayException,
                                                                                         IOException {
        var publication = TicketTestUtils.createPersistedDegreePublication(PUBLISHED, resourceService);
        var userTopLevelOrganizationId = randomUri();
        var userAffiliationOrganizationId = randomUri();

        var channelClaimIdentifier = SortableIdentifier.next();
        var claimingCustomerId = randomUri();
        var claimingOrganizationId = randomUri();

        var pendingTicket =
            pendingFilesApprovalThesis(publication, userTopLevelOrganizationId, userAffiliationOrganizationId)
                .applyPublicationChannelClaim(claimingOrganizationId, channelClaimIdentifier)
                .persistNewTicket(ticketService);
        var completedTicket =
            pendingFilesApprovalThesis(publication, userTopLevelOrganizationId, userAffiliationOrganizationId)
                .complete(publication, USER_INSTANCE)
                .persistNewTicket(ticketService);

        var handler = new UpdatedPublicationChannelEventHandler(s3Client, ticketService, resourceService);


        var request = claimedToNonClaimedPublicationChannelEvent(channelClaimIdentifier,
                                                                 claimingCustomerId,
                                                                 claimingOrganizationId,
                                                                 publication.getIdentifier());
        handler.handleRequest(request, output, context);

        var pendingTicketAfter = (FilesApprovalThesis) ticketService.fetchTicket(pendingTicket);
        assertTicketHasOrganizationDetails(pendingTicketAfter,
                                           userTopLevelOrganizationId,
                                           userAffiliationOrganizationId);

        var completedTicketAfter = (FilesApprovalThesis) ticketService.fetchTicket(completedTicket);
        assertTicketHasOrganizationDetails(completedTicketAfter,
                                           completedTicketAfter.getOwnerAffiliation(),
                                           completedTicketAfter.getResponsibilityArea());
    }

    @Test
    void shouldUpdatePendingTicketsWhenChannelClaimIsRemovedAndTicketIsStillUnderInfluence() throws ApiGatewayException,
                                                                                                    IOException {
        var publication = TicketTestUtils.createPersistedDegreePublication(PUBLISHED, resourceService);
        var userTopLevelOrganizationId = randomUri();
        var userAffiliationOrganizationId = randomUri();
        var channelClaimIdentifier = SortableIdentifier.next();
        var claimingCustomerId = randomUri();
        var claimingOrganizationId = randomUri();

        var pendingTicket =
            pendingFilesApprovalThesis(publication, userTopLevelOrganizationId, userAffiliationOrganizationId)
                .applyPublicationChannelClaim(claimingOrganizationId, channelClaimIdentifier)
                .persistNewTicket(ticketService);
        var completedTicket =
            pendingFilesApprovalThesis(publication, userTopLevelOrganizationId, userAffiliationOrganizationId)
                .complete(publication, USER_INSTANCE)
                .persistNewTicket(ticketService);

        var handler = new UpdatedPublicationChannelEventHandler(s3Client, ticketService, resourceService);

        var request = claimedPublicationChannelRemovedEvent(channelClaimIdentifier,
                                                            claimingCustomerId,
                                                            claimingOrganizationId,
                                                            publication.getIdentifier());
        handler.handleRequest(request, output, context);

        var pendingTicketAfter = (FilesApprovalThesis) ticketService.fetchTicket(pendingTicket);
        assertTicketHasOrganizationDetails(pendingTicketAfter,
                                           userTopLevelOrganizationId,
                                           userAffiliationOrganizationId);

        var completedTicketAfter = (FilesApprovalThesis) ticketService.fetchTicket(completedTicket);
        assertTicketHasOrganizationDetails(completedTicketAfter,
                                           completedTicketAfter.getOwnerAffiliation(),
                                           completedTicketAfter.getResponsibilityArea());
    }

    @Test
    void shouldNotUpdatePendingTicketsWhenChannelClaimIsRemovedAndTicketIsUnderInfluenceByDifferentChannelClaim()
        throws ApiGatewayException,
               IOException {
        var publication = TicketTestUtils.createPersistedDegreePublication(PUBLISHED, resourceService);
        var userTopLevelOrganizationId = randomUri();
        var userAffiliationOrganizationId = randomUri();

        var differentChannelClaimIdentifier = SortableIdentifier.next();
        var differentClaimingOrganizationId = randomUri();

        var pendingTicket =
            pendingFilesApprovalThesis(publication, userTopLevelOrganizationId, userAffiliationOrganizationId)
                .applyPublicationChannelClaim(differentClaimingOrganizationId, differentChannelClaimIdentifier)
                .persistNewTicket(ticketService);
        var completedTicket =
            pendingFilesApprovalThesis(publication, userTopLevelOrganizationId, userAffiliationOrganizationId)
                .complete(publication, USER_INSTANCE)
                .persistNewTicket(ticketService);

        var handler = new UpdatedPublicationChannelEventHandler(s3Client, ticketService, resourceService);

        var channelClaimIdentifier = SortableIdentifier.next();
        var claimingCustomerId = randomUri();
        var claimingOrganizationId = randomUri();

        var request = claimedPublicationChannelRemovedEvent(channelClaimIdentifier,
                                                            claimingCustomerId,
                                                            claimingOrganizationId,
                                                            publication.getIdentifier());
        handler.handleRequest(request, output, context);

        var pendingTicketAfter = (FilesApprovalThesis) ticketService.fetchTicket(pendingTicket);
        assertTicketHasOrganizationDetails(pendingTicketAfter,
                                           differentClaimingOrganizationId,
                                           differentClaimingOrganizationId,
                                           differentChannelClaimIdentifier);

        var completedTicketAfter = (FilesApprovalThesis) ticketService.fetchTicket(completedTicket);
        assertTicketHasOrganizationDetails(completedTicketAfter,
                                           completedTicketAfter.getOwnerAffiliation(),
                                           completedTicketAfter.getResponsibilityArea());
    }

    private void assertTicketHasOrganizationDetails(FilesApprovalThesis ticket,
                                                    URI topLevelOrganizationId,
                                                    URI subOrganizationId,
                                                    SortableIdentifier channelClaimIdentifier) {
        assertThat(ticket.getReceivingOrganizationDetails().topLevelOrganizationId(),
                   is(equalTo(topLevelOrganizationId)));
        assertThat(ticket.getReceivingOrganizationDetails().subOrganizationId(),
                   is(equalTo(subOrganizationId)));
        assertThat(ticket.getReceivingOrganizationDetails().influencingChannelClaim(),
                   is(equalTo(channelClaimIdentifier)));
    }

    private void assertTicketHasOrganizationDetails(FilesApprovalThesis ticket,
                                                    URI topLevelOrganizationId,
                                                    URI subOrganizationId) {
        assertThat(ticket.getReceivingOrganizationDetails().topLevelOrganizationId(),
                   is(equalTo(topLevelOrganizationId)));
        assertThat(ticket.getReceivingOrganizationDetails().subOrganizationId(),
                   is(equalTo(subOrganizationId)));
        assertThat(ticket.getReceivingOrganizationDetails().influencingChannelClaim(),
                   is(nullValue()));
    }

    private FilesApprovalThesis pendingFilesApprovalThesis(Publication publication,
                                                           URI topLevelOrganizationId,
                                                           URI userAffiliationOrganizationId) {
        var username = publication.getResourceOwner().getOwner().getValue();
        var customerId = publication.getPublisher().getId();

        var personId = PublicationGenerator.randomUri();
        var accessRights = Collections.<AccessRight>emptyList();
        var userInstance = new UserInstance(username, customerId,
                                            topLevelOrganizationId, userAffiliationOrganizationId,
                                            personId,
                                            accessRights,
                                            UserClientType.INTERNAL, null);
        return FilesApprovalThesis.createForUserInstitution(Resource.fromPublication(publication),
                                                            userInstance,
                                                            REGISTRATOR_PUBLISHES_METADATA_ONLY);
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

    private InputStream claimedPublicationChannelRemovedEvent(SortableIdentifier channelClaimIdentifier,
                                                              URI customerId,
                                                              URI organizationId,
                                                              SortableIdentifier resourceIdentifier)
        throws IOException {

        var channelClaimId = UriWrapper.fromUri(randomUri()).addChild(channelClaimIdentifier.toString()).getUri();
        var claimedBy = new CustomerSummaryDto(customerId, organizationId);
        var channelClaimDto = new ChannelClaimDto(channelClaimId, claimedBy, new ChannelClaim(randomUri(),
                                                                                              new ChannelConstraint(
                                                                                                  "OwnerOnly",
                                                                                                  "Everyone",
                                                                                                  List.of(
                                                                                                      STUDENT_THESIS_INSTANCE_TYPES))));
        var eventBody = eventBody(
            ClaimedPublicationChannel.create(channelClaimDto, resourceIdentifier, PUBLISHER),
            null
        );
        var blobUri = s3Driver.insertEvent(UnixPath.of(randomString()), eventBody);

        return validEvent(blobUri);
    }

    private InputStream claimedPublicationChannelAddedEvent(SortableIdentifier channelClaimIdentifier,
                                                            URI customerId,
                                                            URI organizationId,
                                                            SortableIdentifier resourceIdentifier) throws IOException {

        var channelClaimId = UriWrapper.fromUri(randomUri()).addChild(channelClaimIdentifier.toString()).getUri();
        var claimedPublicationChannel = buildClaimedPublicationChannel(channelClaimId,
                                                                       customerId,
                                                                       organizationId,
                                                                       resourceIdentifier);
        var eventBody = eventBody(
            null,
            claimedPublicationChannel
        );
        var blobUri = s3Driver.insertEvent(UnixPath.of(randomString()), eventBody);

        return validEvent(blobUri);
    }

    private ClaimedPublicationChannel buildClaimedPublicationChannel(URI channelClaimId,
                                                                     URI customerId,
                                                                     URI organizationId,
                                                                     SortableIdentifier resourceIdentifier) {
        var claimedBy = new CustomerSummaryDto(customerId, organizationId);
        var channelClaim = new ChannelClaim(randomUri(),
                                            new ChannelConstraint(
                                                "OwnerOnly",
                                                "Everyone",
                                                List.of(
                                                    STUDENT_THESIS_INSTANCE_TYPES)));
        var channelClaimDto = new ChannelClaimDto(channelClaimId,
                                                  claimedBy,
                                                  channelClaim);
        return ClaimedPublicationChannel.create(channelClaimDto, resourceIdentifier, PUBLISHER);
    }

    private InputStream nonClaimedToClaimedPublicationChannelEvent(SortableIdentifier channelClaimIdentifier,
                                                                   URI customerId,
                                                                   URI organizationId,
                                                                   SortableIdentifier resourceIdentifier)
        throws IOException {
        var channelClaimId = UriWrapper.fromUri(randomUri()).addChild(channelClaimIdentifier.toString()).getUri();
        var claimedPublicationChannel = buildClaimedPublicationChannel(channelClaimId,
                                                                       customerId,
                                                                       organizationId,
                                                                       resourceIdentifier);

        var eventBody = eventBody(
            NonClaimedPublicationChannel.create(channelClaimId, resourceIdentifier, PUBLISHER),
            claimedPublicationChannel
        );
        var blobUri = s3Driver.insertEvent(UnixPath.of(randomString()), eventBody);

        return validEvent(blobUri);
    }

    private InputStream claimedToNonClaimedPublicationChannelEvent(SortableIdentifier channelClaimIdentifier,
                                                                   URI customerId,
                                                                   URI organizationId,
                                                                   SortableIdentifier resourceIdentifier)
        throws IOException {
        var channelClaimId = UriWrapper.fromUri(randomUri()).addChild(channelClaimIdentifier.toString()).getUri();
        var claimedPublicationChannel = buildClaimedPublicationChannel(channelClaimId,
                                                                       customerId,
                                                                       organizationId,
                                                                       resourceIdentifier);

        var eventBody = eventBody(
            claimedPublicationChannel,
            NonClaimedPublicationChannel.create(channelClaimId, resourceIdentifier, PUBLISHER)
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
