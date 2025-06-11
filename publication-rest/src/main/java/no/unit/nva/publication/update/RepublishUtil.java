package no.unit.nva.publication.update;

import no.unit.nva.model.PublicationOperation;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissions.publication.PublicationPermissions;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.apigateway.exceptions.NotFoundException;

public class RepublishUtil {

    private final ResourceService resourceService;
    private final TicketService ticketService;
    private final PublicationPermissions permissionStrategy;

    public RepublishUtil(ResourceService resourceService, TicketService ticketService,
                         PublicationPermissions permissionStrategy) {
        this.resourceService = resourceService;
        this.ticketService = ticketService;
        this.permissionStrategy = permissionStrategy;
    }

    public static RepublishUtil create(ResourceService resourceService, TicketService ticketService,
                                       PublicationPermissions permissionStrategy) {
        return new RepublishUtil(resourceService, ticketService, permissionStrategy);
    }

    public Resource republish(Resource resource, UserInstance userInstance) throws ApiGatewayException {
        validateRepublishing();
        resource.republish(resourceService, ticketService, userInstance);

        return resource.fetch(resourceService).orElseThrow(() -> new NotFoundException("Resource not found!"));
    }

    private void validateRepublishing() throws ForbiddenException {
        if (!permissionStrategy.allowsAction(PublicationOperation.REPUBLISH)) {
            throw new ForbiddenException();
        }
    }
}
