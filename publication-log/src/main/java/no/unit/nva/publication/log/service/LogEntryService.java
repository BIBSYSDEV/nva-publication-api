package no.unit.nva.publication.log.service;

import static nva.commons.core.attempt.Try.attempt;
import no.unit.nva.clients.GetCustomerResponse;
import no.unit.nva.clients.GetUserResponse;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.logentry.LogUser;
import no.unit.nva.publication.model.business.publicationstate.ResourceEvent;
import no.unit.nva.publication.service.impl.ResourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogEntryService {

    public static final String PERSISTING_LOG_ENTRY_MESSAGE = "Persisting log entry for event {} for resource {}";
    public static final Logger logger = LoggerFactory.getLogger(LogEntryService.class);
    private final ResourceService resourceService;
    private final IdentityServiceClient identityServiceClient;

    public LogEntryService(ResourceService resourceService, IdentityServiceClient identityServiceClient) {
        this.resourceService = resourceService;
        this.identityServiceClient = identityServiceClient;
    }

    public void persistLogEntry(Entity entity) {
        switch (entity) {
            case Resource resource -> persistLogEntry(resource.getIdentifier());
            case null, default -> {
                // Ignore
            }
        }
    }

    private void persistLogEntry(SortableIdentifier identifier) {
        var resource = fetchResource(identifier);
        if (resource.hasResourceEvent()) {
            var resourceEvent = resource.getResourceEvent();

            var user = createUser(resourceEvent);

            resourceEvent.toLogEntry(resource.getIdentifier(), user).persist(resourceService);

            logger.info(PERSISTING_LOG_ENTRY_MESSAGE, resource.getResourceEvent().getClass().getSimpleName(), resource);
            resource.clearResourceEvent(resourceService);
        }
    }

    private LogUser createUser(ResourceEvent resourceEvent) {
        return attempt(() -> LogUser.create(getUser(resourceEvent), getCustomer(resourceEvent)))
                   .orElse(failure -> LogUser.fromResourceEvent(resourceEvent.user(), resourceEvent.institution()));
    }

    private Resource fetchResource(SortableIdentifier identifier) {
        return attempt(() -> Resource.resourceQueryObject(identifier).fetch(resourceService)).orElseThrow();
    }

    private GetCustomerResponse getCustomer(ResourceEvent resourceEvent) {
        return attempt(() -> identityServiceClient.getCustomerByCristinId(resourceEvent.institution())).orElseThrow();
    }

    private GetUserResponse getUser(ResourceEvent resourceEvent) {
        return attempt(() -> identityServiceClient.getUser(resourceEvent.user().toString())).orElseThrow();
    }
}
