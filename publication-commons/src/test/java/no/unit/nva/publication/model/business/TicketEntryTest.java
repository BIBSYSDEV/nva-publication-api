package no.unit.nva.publication.model.business;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomPendingOpenFile;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.model.business.publicationstate.DoiRequestedEvent;
import no.unit.nva.publication.ticket.test.TicketTestUtils;
import nva.commons.apigateway.exceptions.ConflictException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class TicketEntryTest {

    @ParameterizedTest
    @DisplayName("should request a new ticket ")
    @MethodSource("no.unit.nva.publication.ticket.test.TicketTestUtils#ticketTypeAndPublicationStatusProvider")
    void shouldRequestNewTicket(Class<? extends TicketEntry> ticketType, PublicationStatus status) {
        var publication = TicketTestUtils.createNonPersistedPublication(status);
        var ticket = TicketEntry.requestNewTicket(publication, ticketType)
                         .withOwner(UserInstance.fromPublication(publication).getUsername());
        var actualUserInstance = UserInstance.fromTicket(ticket);
        var expectedUserInstance = getExpectedUserInstance(publication);

        assertThat(ticket.getClass(), is(equalTo(ticketType)));
        assertThat(ticket.getResourceIdentifier(), is(equalTo(publication.getIdentifier())));
        assertThat(actualUserInstance, is(equalTo(expectedUserInstance)));
    }

    @Test
    void shouldThrowExceptionForUnrecognizedTicketType() {
        var publication = TicketTestUtils.createNonPersistedPublication(PublicationStatus.DRAFT);
        assertThrows(RuntimeException.class, () -> TicketEntry.requestNewTicket(publication, DoiRequest.class));
    }

    @Test
    void shouldThrowExceptionForUnrecognizedTicketTypeRequestingNewTicket() {
        var publication = TicketTestUtils.createNonPersistedPublication(PublicationStatus.DRAFT);
        assertThrows(RuntimeException.class, () -> TicketEntry.requestNewTicket(publication, TicketEntry.class));
    }

    @Test
    void shouldThrowExceptionForUnrecognizedTicketTypeCreatingQueryObject() {
        var publication = TicketTestUtils.createNonPersistedPublication(PublicationStatus.DRAFT);
        assertThrows(UnsupportedOperationException.class,
                     () -> TicketEntry.createQueryObject(publication.getPublisher().getId(),
                                                         publication.getIdentifier(),
                                                         TicketEntry.class));
    }

    @Test
    void shouldThrowExceptionForUnrecognizedTicketCreatingNewTicket() {
        var publication = TicketTestUtils.createNonPersistedPublication(PublicationStatus.DRAFT);
        assertThrows(UnsupportedOperationException.class,
                     () -> TicketEntry.createNewTicket(publication, TicketEntry.class,
                                                       SortableIdentifier::next));
    }

    @Test
    void shouldReturnFalseWhenTicketWithoutAssignee() throws ConflictException {
        var publication = TicketTestUtils.createNonPersistedPublication(PublicationStatus.DRAFT);
        var ticket = TicketEntry.createNewTicket(publication, DoiRequest.class, SortableIdentifier::next);

        assertFalse(ticket.hasAssignee());
    }

    @Test
    void shouldReturnTrueWhenUserIsFromTheSameInstitutionAsTicket() throws ConflictException {
        var publication = TicketTestUtils.createNonPersistedPublication(PublicationStatus.DRAFT);
        var ticket = TicketEntry.createNewTicket(publication, DoiRequest.class, SortableIdentifier::next)
                         .withOwnerAffiliation(publication.getResourceOwner().getOwnerAffiliation());

        var userInstance = UserInstance.fromPublication(publication);

        assertTrue(ticket.hasSameOwnerAffiliationAs(userInstance));
    }

    @Test
    void shouldMoveFilesForApprovalToApprovedFilesWhenPublishingRequestApproveFiles() throws ConflictException {
        var publication = TicketTestUtils.createNonPersistedPublication(PublicationStatus.DRAFT);
        var ticket = (PublishingRequestCase) TicketEntry.createNewTicket(
            publication, PublishingRequestCase.class, SortableIdentifier::next);
        ticket.withFilesForApproval(Set.of(randomPendingOpenFile()))
            .withWorkflow(PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_ONLY)
            .publishApprovedFile();

        assertThat(ticket.getApprovedFiles().size(), is(equalTo(1)));
    }

    @Test
    void shouldSetTicketEventOnDoiRequest() {
        var userInstance = UserInstance.fromPublication(randomPublication());
        var doiRequest = DoiRequest.create(Resource.fromPublication(randomPublication()), userInstance);
        doiRequest.setTicketEvent(DoiRequestedEvent.create(userInstance, Instant.now()));

        assertTrue(doiRequest.hasTicketEvent());
    }

    @Test
    void shouldCreateDoiRequestWithUserFromUserInstance() {
        var resource = Resource.fromPublication(randomPublication());
        var userInstance = randomUserInstance();

        var doiRequest = DoiRequest.create(resource, userInstance);

        assertEquals(userInstance.getTopLevelOrgCristinId(), doiRequest.getOwnerAffiliation());
        assertEquals(userInstance.getPersonAffiliation(), doiRequest.getResponsibilityArea());
        assertEquals(userInstance.getUser(), doiRequest.getOwner());
    }

    @Test
    void shouldCreateGeneralSupportRequestWithUserFromUserInstance() {
        var resource = Resource.fromPublication(randomPublication());
        var userInstance = randomUserInstance();

        var generalSupportRequest = GeneralSupportRequest.create(resource, userInstance);

        assertEquals(userInstance.getTopLevelOrgCristinId(), generalSupportRequest.getOwnerAffiliation());
        assertEquals(userInstance.getPersonAffiliation(), generalSupportRequest.getResponsibilityArea());
        assertEquals(userInstance.getUser(), generalSupportRequest.getOwner());
    }

    @Test
    void shouldCreatePublishingRequestWithUserFromUserInstance() {
        var resource = Resource.fromPublication(randomPublication());
        var userInstance = randomUserInstance();

        var publishingRequestCase = PublishingRequestCase.create(resource, userInstance, PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_ONLY);

        assertEquals(userInstance.getTopLevelOrgCristinId(), publishingRequestCase.getOwnerAffiliation());
        assertEquals(userInstance.getPersonAffiliation(), publishingRequestCase.getResponsibilityArea());
        assertEquals(userInstance.getUser(), publishingRequestCase.getOwner());
    }

    @Test
    void shouldCreatePublishingRequestWithFilesForApproval() {
        var resource = Resource.fromPublication(randomPublication());
        var userInstance = randomUserInstance();
        var files = Set.of(randomPendingOpenFile());
        var publishingRequestCase = PublishingRequestCase.createWithFilesForApproval(resource, userInstance,
                                                                 PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_ONLY, files);

        assertTrue(publishingRequestCase.getFilesForApproval().containsAll(files));
    }

    @Test
    void shouldCreatePublishingRequestAndApproveFilesWhenUserPublishesMetadataAndFilesWorkflow() {
        var resource = Resource.fromPublication(randomPublication());
        var userInstance = randomUserInstance();
        var publishingRequestCase = PublishingRequestCase.create(resource, userInstance,
                                                                                     PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_AND_FILES);

        assertTrue(publishingRequestCase.getFilesForApproval().isEmpty());
        assertFalse(publishingRequestCase.getApprovedFiles().isEmpty());
    }

    private static UserInstance randomUserInstance() {
        return new UserInstance(randomString(), randomUri(), randomUri(), randomUri(),
                                randomUri(), List.of(), UserClientType.INTERNAL);
    }

    private static UserInstance getExpectedUserInstance(Publication publication) {
        return UserInstance.create(
            publication.getResourceOwner().getOwner().getValue(), publication.getPublisher().getId());
    }
}