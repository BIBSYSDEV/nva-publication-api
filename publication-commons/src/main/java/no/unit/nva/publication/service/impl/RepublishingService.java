package no.unit.nva.publication.service.impl;

import static java.util.Collections.emptyList;
import static no.unit.nva.publication.model.business.TicketStatus.NOT_APPLICABLE;
import static no.unit.nva.publication.model.business.TicketStatus.PENDING;

import java.util.Collection;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.publication.commons.customer.CustomerApiClient;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.GeneralSupportRequest;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketChanges;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissions.publication.PublicationPermissions;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.apigateway.exceptions.NotFoundException;

public class RepublishingService {

  private static final String RESOURCE_NOT_FOUND_MESSAGE = "Resource not found!";
  private final ResourceService resourceService;
  private final CustomerApiClient customerApiClient;

  public RepublishingService(ResourceService resourceService, CustomerApiClient customerApiClient) {
    this.resourceService = resourceService;
    this.customerApiClient = customerApiClient;
  }

  /** Republishes the resource together with its tickets, in one write. */
  public Resource republish(Resource resource, UserInstance userInstance)
      throws ApiGatewayException {
    var persistedResource = fetch(resource);
    validatePermissions(persistedResource, userInstance);
    republishWithTickets(persistedResource, userInstance);
    return fetch(resource);
  }

  private static void validatePermissions(Resource resource, UserInstance userInstance)
      throws ForbiddenException {
    var permissionStrategy = PublicationPermissions.create(resource, userInstance);
    if (!permissionStrategy.allowsAction(PublicationOperation.REPUBLISH)) {
      throw new ForbiddenException();
    }
  }

  private Resource fetch(Resource resource) throws NotFoundException {
    return resource
        .fetch(resourceService)
        .orElseThrow(() -> new NotFoundException(RESOURCE_NOT_FOUND_MESSAGE));
  }

  private void republishWithTickets(Resource resource, UserInstance userInstance) {
    var tickets = resourceService.fetchAllTicketsForResource(resource).toList();
    resource.republish(userInstance);
    var reactivatedTickets = reactivateDoiAndSupportTickets(tickets);
    var fileApprovalTickets = PendingFileTickets.changesFor(resource, tickets, customerApiClient);

    var ticketChanges = reactivatedTickets.combinedWith(fileApprovalTickets);
    resourceService.updateResourceWithTickets(resource, userInstance, ticketChanges);
  }

  /** File approval tickets are replaced instead, by {@link PendingFileTickets}. */
  private static TicketChanges reactivateDoiAndSupportTickets(Collection<TicketEntry> tickets) {
    var ticketsToReactivate =
        tickets.stream().filter(RepublishingService::isSetAsideDoiOrSupportTicket).toList();
    ticketsToReactivate.forEach(ticket -> ticket.setStatus(PENDING));
    return new TicketChanges(ticketsToReactivate, emptyList());
  }

  private static boolean isSetAsideDoiOrSupportTicket(TicketEntry ticket) {
    return (ticket instanceof GeneralSupportRequest || ticket instanceof DoiRequest)
        && NOT_APPLICABLE == ticket.getStatus();
  }
}
