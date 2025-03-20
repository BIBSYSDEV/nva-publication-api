package no.unit.nva.publication.log.service;

import static java.util.Objects.isNull;
import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.util.Optional;
import no.unit.nva.clients.CustomerDto;
import no.unit.nva.clients.UserDto;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.business.logentry.LogEntry;
import no.unit.nva.publication.model.business.logentry.LogUser;
import no.unit.nva.publication.model.business.publicationstate.ImportedResourceEvent;
import no.unit.nva.publication.model.business.publicationstate.ResourceEvent;
import no.unit.nva.publication.service.impl.ResourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogEntryService {

    public static final String PERSISTING_LOG_ENTRY_MESSAGE = "Persisting log entry for event {} for resource {}";
    public static final String PERSISTING_FILE_LOG_ENTRY_MESSAGE =
        "Persisting log entry for event {} for file {} for resource {}";
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
            case FileEntry fileEntry -> persistLogEntry(fileEntry);
            case DoiRequest doiRequest -> persistLogEntry(doiRequest);
            case null, default -> {
                // Ignore
            }
        }
    }

    private void persistLogEntry(FileEntry fileEntry) {
        if (fileEntry.hasFileEvent()) {
            persistFileLogEntry(fileEntry);
        }
    }

    private void persistLogEntry(SortableIdentifier identifier) {
        Resource.resourceQueryObject(identifier)
            .fetch(resourceService)
            .filter(Resource::hasResourceEvent)
            .ifPresent(this::persistLogEntry);
    }

    private void persistLogEntry(Resource resource) {
        var resourceEvent = resource.getResourceEvent();
        createLogEntry(resource, resourceEvent).persist(resourceService);

        logger.info(PERSISTING_LOG_ENTRY_MESSAGE, resource.getResourceEvent().getClass().getSimpleName(), resource);
    }

    private void persistLogEntry(DoiRequest doiRequest) {
        Optional.ofNullable(doiRequest)
            .filter(DoiRequest::hasTicketEvent)
            .ifPresent(this::createLogEntry);
    }

    private void createLogEntry(DoiRequest doiRequest) {
        var ticketEvent = doiRequest.getTicketEvent();
        var user = createUser(ticketEvent.user());
        ticketEvent.toLogEntry(doiRequest.getResourceIdentifier(), doiRequest.getIdentifier(), user)
            .persist(resourceService);
    }

    private LogEntry createLogEntry(Resource resource, ResourceEvent resourceEvent) {
        if (resourceEvent instanceof ImportedResourceEvent importedResourceEvent && isNull(resourceEvent.user())) {
            return importedResourceEvent.toLogEntry(resource.getIdentifier(), null);
        } else {
            var user = createUser(resourceEvent);
            return resourceEvent.toLogEntry(resource.getIdentifier(), user);
        }
    }

    private void persistFileLogEntry(FileEntry fileEntry) {
        var fileEvent = fileEntry.getFileEvent();
        var user = createUser(fileEvent.user());
        var fileIdentifier = fileEntry.getIdentifier();
        var resourceIdentifier = fileEntry.getResourceIdentifier();
        fileEvent.toLogEntry(fileEntry, user).persist(resourceService);

        logger.info(PERSISTING_FILE_LOG_ENTRY_MESSAGE, fileEntry.getFile().getClass().getSimpleName(), fileIdentifier, resourceIdentifier);
    }

    private LogUser createUser(User user) {
        return attempt(() -> getUser(user))
                   .map(getUserResponse -> LogUser.create(getUserResponse, getCustomer(getUserResponse.institutionCristinId())))
                   .orElse(failure -> LogUser.fromResourceEvent(user, null));
    }

    private LogUser createUser(ResourceEvent resourceEvent) {
        return attempt(() -> LogUser.create(getUser(resourceEvent.user()), getCustomer(resourceEvent.institution())))
                   .orElse(failure -> LogUser.fromResourceEvent(resourceEvent.user(), resourceEvent.institution()));
    }

    private CustomerDto getCustomer(URI institutionCristinId) {
        return attempt(() -> identityServiceClient.getCustomerByCristinId(institutionCristinId)).orElseThrow();
    }

    private UserDto getUser(User user) {
        return attempt(() -> identityServiceClient.getUser(user.toString())).orElseThrow();
    }
}
