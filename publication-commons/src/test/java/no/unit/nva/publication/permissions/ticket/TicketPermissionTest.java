package no.unit.nva.publication.permissions.ticket;

import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.publication.model.business.publicationchannel.ChannelPolicy.EVERYONE;
import static no.unit.nva.publication.model.business.publicationchannel.ChannelPolicy.OWNER_ONLY;
import static no.unit.nva.publication.permissions.PermissionsTestUtils.setPublicationChannelWithinScope;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.apigateway.AccessRight.MANAGE_DEGREE;
import static nva.commons.apigateway.AccessRight.MANAGE_DEGREE_EMBARGO;
import static nva.commons.apigateway.AccessRight.MANAGE_PUBLISHING_REQUESTS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.net.URI;
import java.util.List;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.TicketOperation;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.ReceivingOrganizationDetails;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissions.PermissionsTestUtils.Institution;
import no.unit.nva.publication.permissions.publication.PublicationPermissions;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import org.junit.jupiter.api.Test;

class TicketPermissionTest {

    @Test
    void shouldAllowApproveOperation() throws ConflictException {
        var curatingInstitution = Institution.random();
        var resource = randomResource().copy().withStatus(PUBLISHED).build();
        setPublicationChannelWithinScope(resource, curatingInstitution, EVERYONE, OWNER_ONLY);
        var userInstance = fileCuratorUserInstance(curatingInstitution.getTopLevelCristinId());
        var ticket = TicketEntry.createNewTicket(resource.toPublication(), PublishingRequestCase.class,
                                                 SortableIdentifier::next);
        ticket.setReceivingOrganizationDetails(new ReceivingOrganizationDetails(userInstance.getTopLevelOrgCristinId(),
                                                                                userInstance.getPersonAffiliation()));
        var publicationPermissions = PublicationPermissions.create(resource, userInstance);
        var ticketPermissions = TicketPermissions.create(ticket, userInstance, resource, publicationPermissions);

        assertTrue(ticketPermissions.allowsAction(TicketOperation.APPROVE));
        assertThat(ticketPermissions.getAllAllowedActions(), hasItems(TicketOperation.APPROVE));
        assertDoesNotThrow(() -> ticketPermissions.authorize(TicketOperation.APPROVE));
    }

    @Test
    void shouldDenyWhenMissingAccessRights() throws ConflictException {
        var resource = randomResource().copy().withStatus(PUBLISHED).build();
        var userInstance = UserInstance.fromPublication(resource.toPublication());
        var ticket = TicketEntry.createNewTicket(resource.toPublication(), PublishingRequestCase.class,
                                                 SortableIdentifier::next);
        var publicationPermissions = PublicationPermissions.create(resource, userInstance);
        var ticketPermissions = TicketPermissions.create(ticket, userInstance, resource, publicationPermissions);

        assertThrows(UnauthorizedException.class, () -> ticketPermissions.authorize(TicketOperation.APPROVE));
    }

    @Test
    void shouldDenyApprovalWhenTicketIsFinalized() throws ConflictException {
        var curatingInstitution = Institution.random();
        var resource = randomResource().copy().withStatus(PUBLISHED).build();
        setPublicationChannelWithinScope(resource, curatingInstitution, EVERYONE, OWNER_ONLY);
        var userInstance = fileCuratorUserInstance(curatingInstitution.getTopLevelCristinId());
        var ticket = TicketEntry.createNewTicket(resource.toPublication(), PublishingRequestCase.class,
                                                 SortableIdentifier::next);
        ticket.setReceivingOrganizationDetails(new ReceivingOrganizationDetails(userInstance.getTopLevelOrgCristinId(),
                                                                                userInstance.getPersonAffiliation()));
        ticket.setStatus(TicketStatus.COMPLETED);
        var publicationPermissions = PublicationPermissions.create(resource, userInstance);
        var ticketPermissions = TicketPermissions.create(ticket, userInstance, resource, publicationPermissions);

        assertThrows(UnauthorizedException.class, () -> ticketPermissions.authorize(TicketOperation.APPROVE));
    }

    private static UserInstance fileCuratorUserInstance(URI institutionId) {
        return UserInstance.create(randomString(), randomUri(), randomUri(),
                                   List.of(MANAGE_DEGREE, MANAGE_DEGREE_EMBARGO, MANAGE_PUBLISHING_REQUESTS),
                                   institutionId);
    }

    private static Resource randomResource() {
        return Resource.fromPublication(randomPublication());
    }
}
