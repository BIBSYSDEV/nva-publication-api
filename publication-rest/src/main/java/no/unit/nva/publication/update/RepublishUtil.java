package no.unit.nva.publication.update;

import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.model.Username;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permission.strategy.PublicationPermissionStrategy;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ForbiddenException;

public class RepublishUtil {

    private final ResourceService resourceService;
    private final TicketService ticketService;
    private final PublicationPermissionStrategy permissionStrategy;

    public RepublishUtil(ResourceService resourceService, TicketService ticketService,
                         PublicationPermissionStrategy permissionStrategy) {
        this.resourceService = resourceService;
        this.ticketService = ticketService;
        this.permissionStrategy = permissionStrategy;
    }

    public static RepublishUtil create(ResourceService resourceService, TicketService ticketService,
                                       PublicationPermissionStrategy permissionStrategy) {
        return new RepublishUtil(resourceService, ticketService, permissionStrategy);
    }

    public Publication republish(Publication publication, UserInstance userInstance) throws ApiGatewayException {
        validateRepublishing();
        var publicationIdentifier = publication.getIdentifier();
        resourceService.publishPublication(UserInstance.fromPublication(publication), publicationIdentifier);
        persistCompletedPublishingRequest(publication, userInstance);

        return resourceService.getPublicationByIdentifier(publicationIdentifier);
    }

    private void persistCompletedPublishingRequest(Publication publication, UserInstance userInstance)
        throws ApiGatewayException {
        var publishingRequest = (PublishingRequestCase) PublishingRequestCase
                                                            .createNewTicket(publication,
                                                                             PublishingRequestCase.class,
                                                                             SortableIdentifier::next);
        publishingRequest.persistAutoComplete(ticketService, publication, new Username(userInstance.getUsername()));
    }

    private void validateRepublishing() throws ForbiddenException {
        if (!permissionStrategy.allowsAction(PublicationOperation.REPUBLISH)) {
            throw new ForbiddenException();
        }
    }
}