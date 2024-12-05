package no.unit.nva.publication.events.handlers.log;

import static nva.commons.core.attempt.Try.attempt;
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
            case Resource resource when resource.hasResourceEvent() -> persistLogEntry(resource.getIdentifier());
            case null, default -> {
                // Ignore
            }
        }
    }

    private void persistLogEntry(SortableIdentifier resourceIdentifier) {
        var persistedResource = Resource.resourceQueryObject(resourceIdentifier).fetch(resourceService);
        logger.info(PERSISTING_LOG_ENTRY_MESSAGE, persistedResource.getResourceEvent().getClass().getSimpleName(),
                    resourceIdentifier);

        var resourceEvent = persistedResource.getResourceEvent();
        var user = createUserForLogEntry(resourceEvent);

        resourceEvent.toLogEntry(resourceIdentifier, user).persist(resourceService);
        persistedResource.clearResourceEvent(resourceService);
    }

    private LogUser createUserForLogEntry(ResourceEvent resourceEvent) {
        return attempt(() -> identityServiceClient.getUser(resourceEvent.user().toString()))
                   .map(LogUser::fromGetUserResponse)
                   .orElse(failure -> LogUser.fromUsername(resourceEvent.user().toString()));
    }
}
