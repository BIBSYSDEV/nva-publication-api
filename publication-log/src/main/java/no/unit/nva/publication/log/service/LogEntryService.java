package no.unit.nva.publication.log.service;

import static no.unit.nva.publication.events.bodies.DataEntryUpdateEvent.FILE_ENTRY_CREATE_EVENT_TOPIC;
import static no.unit.nva.publication.events.bodies.DataEntryUpdateEvent.FILE_ENTRY_DELETE_EVENT_TOPIC;
import static no.unit.nva.publication.events.bodies.DataEntryUpdateEvent.RESOURCE_UPDATE_EVENT_TOPIC;
import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.time.Instant;
import no.unit.nva.clients.GetCustomerResponse;
import no.unit.nva.clients.GetUserResponse;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.events.bodies.DataEntryUpdateEvent;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.business.logentry.LogEntry;
import no.unit.nva.publication.model.business.logentry.LogTopic;
import no.unit.nva.publication.model.business.logentry.LogUser;
import no.unit.nva.publication.service.impl.ResourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogEntryService {

    public static final String PERSISTING_RESOURCE_LOG_ENTRY_MESSAGE =
        "Persisting log entry for event {} for resource {}";
    public static final String PERSISTING_FILE_LOG_ENTRY_MESSAGE = "Persisting log entry for file {} for resource {}";
    public static final Logger logger = LoggerFactory.getLogger(LogEntryService.class);
    private final ResourceService resourceService;
    private final IdentityServiceClient identityServiceClient;

    public LogEntryService(ResourceService resourceService, IdentityServiceClient identityServiceClient) {
        this.resourceService = resourceService;
        this.identityServiceClient = identityServiceClient;
    }

    public void persistLogEntry(DataEntryUpdateEvent dataEntryUpdateEvent) {
        switch (dataEntryUpdateEvent.getTopic()) {
            case RESOURCE_UPDATE_EVENT_TOPIC -> persistLogEntry(dataEntryUpdateEvent.getNewData().getIdentifier());
            case FILE_ENTRY_CREATE_EVENT_TOPIC -> persistLogEntryForFileEntry(dataEntryUpdateEvent.getNewData(), LogTopic.FILE_UPLOADED);
            case FILE_ENTRY_DELETE_EVENT_TOPIC -> persistLogEntryForFileEntry(dataEntryUpdateEvent.getNewData(), LogTopic.FILE_DELETED);
            case null, default -> {
                // Ignore
            }
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
        var user = createUser(resourceEvent.user(), resourceEvent.institution());
        resourceEvent.toLogEntry(resource.getIdentifier(), user).persist(resourceService);

        logger.info(PERSISTING_RESOURCE_LOG_ENTRY_MESSAGE, resource.getResourceEvent().getClass().getSimpleName(),
                    resource);
        resource.clearResourceEvent(resourceService);
    }

    private void persistLogEntry(FileEntry fileEntry) {
        var user = createUser(fileEntry.getOwner(), fileEntry.getOwnerAffiliation());

        createLogEntryWithTopic(fileEntry, user, LogTopic.FILE_UPLOADED).persist(resourceService);

        logger.info(PERSISTING_FILE_LOG_ENTRY_MESSAGE, fileEntry.getIdentifier(), fileEntry.getResourceIdentifier());
    }

    private void persistLogEntryForFileEntry(Entity newData, LogTopic fileUploaded) {
        var newFileEntry = (FileEntry) newData;
        newFileEntry.fetch(resourceService).ifPresent(this::persistLogEntry);
    }

    private LogEntry createLogEntryWithTopic(FileEntry fileEntry, LogUser user, LogTopic topic) {
        return LogEntry.builder()
                   .withIdentifier(SortableIdentifier.next())
                   .withResourceIdentifier(fileEntry.getResourceIdentifier())
                   .withTimestamp(Instant.now())
                   .withTopic(topic)
                   .withPerformedBy(user)
                   .build();
    }

    private LogUser createUser(User user, URI institution) {
        return attempt(() -> LogUser.create(getUser(user), getCustomer(institution))).orElse(
            failure -> LogUser.fromResourceEvent(user, institution));
    }

    private GetCustomerResponse getCustomer(URI institution) {
        return attempt(() -> identityServiceClient.getCustomerByCristinId(institution)).orElseThrow();
    }

    private GetUserResponse getUser(User user) {
        return attempt(() -> identityServiceClient.getUser(user.toString())).orElseThrow();
    }
}
