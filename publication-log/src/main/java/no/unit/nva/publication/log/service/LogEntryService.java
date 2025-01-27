package no.unit.nva.publication.log.service;

import static no.unit.nva.publication.events.bodies.DataEntryUpdateEvent.FILE_ENTRY_APPROVED_EVENT_TOPIC;
import static no.unit.nva.publication.events.bodies.DataEntryUpdateEvent.FILE_ENTRY_CREATE_EVENT_TOPIC;
import static no.unit.nva.publication.events.bodies.DataEntryUpdateEvent.FILE_ENTRY_REJECTED_EVENT_TOPIC;
import static no.unit.nva.publication.events.bodies.DataEntryUpdateEvent.RESOURCE_UPDATE_EVENT_TOPIC;
import static no.unit.nva.publication.model.business.logentry.LogTopic.FILE_APPROVED;
import static no.unit.nva.publication.model.business.logentry.LogTopic.FILE_REJECTED;
import static no.unit.nva.publication.model.business.logentry.LogTopic.FILE_UPLOADED;
import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.time.Instant;
import java.util.Optional;
import no.unit.nva.clients.GetCustomerResponse;
import no.unit.nva.clients.GetUserResponse;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Username;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.publication.events.bodies.DataEntryUpdateEvent;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.business.logentry.FileLogEntry;
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
            case RESOURCE_UPDATE_EVENT_TOPIC -> persistLogEntry(dataEntryUpdateEvent.getNewData());
            case FILE_ENTRY_CREATE_EVENT_TOPIC ->
                persistLogEntryForFileEntry(dataEntryUpdateEvent.getNewData(), FILE_UPLOADED);
            case FILE_ENTRY_APPROVED_EVENT_TOPIC ->
                persistLogEntryForFileEntry(dataEntryUpdateEvent.getNewData(), FILE_APPROVED);
            case FILE_ENTRY_REJECTED_EVENT_TOPIC ->
                persistLogEntryForFileEntry(dataEntryUpdateEvent.getNewData(), FILE_REJECTED);
            case null, default -> {
                // Ignore
            }
        }
    }

    private void persistLogEntry(Entity entity) {
        Optional.ofNullable(entity)
            .map(Entity::getIdentifier)
            .map(Resource::resourceQueryObject)
            .flatMap(resource -> resource.fetch(resourceService))
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

    private void persistLogEntryForFileEntry(Entity newData, LogTopic topic) {
        var entry = (FileEntry) newData;
        var fileEntry = entry.fetch(resourceService).orElseThrow();

        var user = switch (topic) {
            case FILE_UPLOADED -> createUser(fileEntry.getOwner(), fileEntry.getOwnerAffiliation());
            case FILE_APPROVED, FILE_REJECTED -> createUserFromFinalizedTicket(fileEntry, topic);
            default -> null;
        };

        createLogEntryWithTopic(fileEntry, user, topic).persist(resourceService);

        logger.info(PERSISTING_FILE_LOG_ENTRY_MESSAGE, fileEntry.getIdentifier(), fileEntry.getResourceIdentifier());
    }

    private LogUser createUserFromFinalizedTicket(FileEntry fileEntry, LogTopic topic) {
        var ticket = getFinalizedPublishingRequestForFileEntry(fileEntry, topic);
        return createUser(ticket.getFinalizedBy());
    }

    private PublishingRequestCase getFinalizedPublishingRequestForFileEntry(FileEntry fileEntry, LogTopic topic) {
        return Resource.resourceQueryObject(fileEntry.getResourceIdentifier())
                   .fetch(resourceService)
                   .orElseThrow()
                   .fetchAllTickets(resourceService)
                   .filter(PublishingRequestCase.class::isInstance)
                   .map(PublishingRequestCase.class::cast)
                   .filter(publishingRequestCase -> FILE_APPROVED.equals(topic)
                                                        ? containsApprovedFileEntry(fileEntry, publishingRequestCase)
                                                        : containsRejectedFileEntry(fileEntry, publishingRequestCase))
                   .findFirst()
                   .orElseThrow();
    }

    private static boolean containsApprovedFileEntry(FileEntry fileEntry, PublishingRequestCase publishingRequestCase) {
        return publishingRequestCase.getApprovedFiles()
                   .stream()
                   .map(File::getIdentifier)
                   .anyMatch(id -> id.toString().equals(fileEntry.getIdentifier().toString()));
    }

    private static boolean containsRejectedFileEntry(FileEntry fileEntry, PublishingRequestCase publishingRequestCase) {
        return publishingRequestCase.getFilesForApproval()
                   .stream()
                   .map(File::getIdentifier)
                   .anyMatch(id -> id.toString().equals(fileEntry.getIdentifier().toString()));
    }

    private FileLogEntry createLogEntryWithTopic(FileEntry fileEntry, LogUser user, LogTopic topic) {
        return FileLogEntry.builder()
                   .withIdentifier(SortableIdentifier.next())
                   .withFileIdentifier(fileEntry.getIdentifier())
                   .withResourceIdentifier(fileEntry.getResourceIdentifier())
                   .withFilename(fileEntry.getFile().getName())
                   .withTimestamp(Instant.now())
                   .withTopic(topic)
                   .withPerformedBy(user)
                   .build();
    }

    private LogUser createUser(User user, URI institution) {
        return attempt(() -> LogUser.create(getUser(user), getCustomer(institution))).orElse(
            failure -> LogUser.fromResourceEvent(user, institution));
    }

    private LogUser createUser(Username username) {
        return attempt(() -> getUser(new User(username.getValue())))
                   .map(getUserResponse -> LogUser.create(getUserResponse, getCustomer(getUserResponse.institutionCristinId())))
                   .orElse(failure -> LogUser.fromResourceEvent(new User(username.getValue()), null));
    }

    private GetCustomerResponse getCustomer(URI institution) {
        return attempt(() -> identityServiceClient.getCustomerByCristinId(institution)).orElseThrow();
    }

    private GetUserResponse getUser(User user) {
        return attempt(() -> identityServiceClient.getUser(user.toString())).orElseThrow();
    }
}
