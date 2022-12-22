package no.unit.nva.expansion;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.expansion.model.ExpandedDataEntry;
import no.unit.nva.expansion.model.ExpandedResource;
import no.unit.nva.expansion.model.ExpandedTicket;
import no.unit.nva.expansion.utils.UriRetriever;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import nva.commons.apigateway.exceptions.NotFoundException;

public class ResourceExpansionServiceImpl implements ResourceExpansionService {

    public static final String UNSUPPORTED_TYPE = "Expansion is not supported for type:";

    private final ResourceService resourceService;
    private final TicketService ticketService;
    private final UriRetriever uriRetriever;

    public ResourceExpansionServiceImpl(ResourceService resourceService,
                                        TicketService ticketService) {
        this.resourceService = resourceService;
        this.ticketService = ticketService;
        this.uriRetriever = new UriRetriever();
    }

    public ResourceExpansionServiceImpl(ResourceService resourceService,
                                        TicketService ticketService,
                                        UriRetriever uriRetriever) {
        this.resourceService = resourceService;
        this.ticketService = ticketService;
        this.uriRetriever = uriRetriever;
    }

    @Override
    public ExpandedDataEntry expandEntry(Entity dataEntry) throws JsonProcessingException, NotFoundException {
        if (dataEntry instanceof Resource) {
            return ExpandedResource.fromPublication(uriRetriever, dataEntry.toPublication(resourceService));
        } else if (dataEntry instanceof TicketEntry) {
            return ExpandedTicket.create((TicketEntry) dataEntry, resourceService, this, ticketService);
        } else if (dataEntry instanceof Message) {
            var message = (Message) dataEntry;
            var ticket = ticketService.fetchTicketByIdentifier(message.getTicketIdentifier());
            return expandEntry(ticket);
        }
        // will throw exception if we want to index a new type that we are not handling yet
        throw new UnsupportedOperationException(UNSUPPORTED_TYPE + dataEntry.getClass().getSimpleName());
    }

    @Override
    public Set<URI> getOrganizationIds(Entity dataEntry) throws NotFoundException {
        if (dataEntry instanceof TicketEntry) {
            var resourceIdentifier = ((TicketEntry) dataEntry).extractPublicationIdentifier();
            var resource = resourceService.getResourceByIdentifier(resourceIdentifier);
            return Optional.ofNullable(resource.getResourceOwner().getOwnerAffiliation())
                .stream()
                .map(
                    this::retrieveAllHigherLevelOrgsInTheFutureWhenResourceOwnerAffiliationIsNotAlwaysTopLevelOrg)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }

    //TODO: does not do what the name says it does?
    private List<URI> retrieveAllHigherLevelOrgsInTheFutureWhenResourceOwnerAffiliationIsNotAlwaysTopLevelOrg(
        URI affiliation) {
        return List.of(affiliation);
    }
}
