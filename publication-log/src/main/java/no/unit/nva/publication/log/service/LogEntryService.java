package no.unit.nva.publication.log.service;

import static java.util.Objects.nonNull;
import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.util.Optional;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.clients.UserDto;
import no.unit.nva.clients.cristin.CristinClient;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.business.logentry.LogEntry;
import no.unit.nva.publication.model.business.logentry.LogOrganization;
import no.unit.nva.publication.model.business.logentry.LogUser;
import no.unit.nva.publication.model.business.publicationstate.CreatedResourceEvent;
import no.unit.nva.publication.model.business.publicationstate.ImportEvent;
import no.unit.nva.publication.model.business.publicationstate.ImportedResourceEvent;
import no.unit.nva.publication.model.business.publicationstate.MergedResourceEvent;
import no.unit.nva.publication.model.business.publicationstate.ResourceEvent;
import no.unit.nva.publication.service.impl.ResourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogEntryService {

    public static final String PERSISTING_LOG_ENTRY_MESSAGE = "Persisting log entry for event {} for resource {}";
    public static final String PERSISTING_FILE_LOG_ENTRY_MESSAGE = "Persisting log entry for event {} for file {} for" +
                                                                   " resource {}";
    public static final Logger logger = LoggerFactory.getLogger(LogEntryService.class);
    private final ResourceService resourceService;
    private final IdentityServiceClient identityServiceClient;
    private final CristinClient cristinClient;

    public LogEntryService(ResourceService resourceService, IdentityServiceClient identityServiceClient,
                           CristinClient cristinClient) {
        this.resourceService = resourceService;
        this.identityServiceClient = identityServiceClient;
        this.cristinClient = cristinClient;
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
        Optional.ofNullable(doiRequest).filter(DoiRequest::hasTicketEvent).ifPresent(this::createLogEntry);
    }

    private void createLogEntry(DoiRequest doiRequest) {
        var ticketEvent = doiRequest.getTicketEvent();
        var user = createUser(ticketEvent.user(), ticketEvent.institution());
        ticketEvent.toLogEntry(doiRequest.getResourceIdentifier(), doiRequest.getIdentifier(), user)
            .persist(resourceService);
    }

    private LogEntry createLogEntry(Resource resource, ResourceEvent resourceEvent) {
        if (resourceEvent instanceof ImportedResourceEvent
            || resourceEvent instanceof MergedResourceEvent
            || resourceEvent instanceof CreatedResourceEvent event && nonNull(event.importSource())) {
            var organization = fetchOrganization(resourceEvent.institution());
            return resourceEvent.toLogEntry(resource.getIdentifier(), organization);
        } else {
            var user = createUser(resourceEvent.user(), resourceEvent.institution());
            return resourceEvent.toLogEntry(resource.getIdentifier(), user);
        }
    }

    private LogOrganization fetchOrganization(URI organizationId) {
        return cristinClient.getOrganization(organizationId)
                   .map(LogOrganization::fromCristinOrganization)
                   .orElse(LogOrganization.fromCristinId(organizationId));
    }

    private void persistFileLogEntry(FileEntry fileEntry) {
        var fileEvent = fileEntry.getFileEvent();
        if (fileEvent instanceof ImportEvent importEvent) {
            var organization = fetchOrganization(importEvent.institution());
            fileEvent.toLogEntry(fileEntry, organization).persist(resourceService);
        } else {
            var user = createUser(fileEvent.user(), null);
            fileEvent.toLogEntry(fileEntry, user).persist(resourceService);
        }
        var fileIdentifier = fileEntry.getIdentifier();
        var resourceIdentifier = fileEntry.getResourceIdentifier();

        logger.info(PERSISTING_FILE_LOG_ENTRY_MESSAGE, fileEntry.getFile().getClass().getSimpleName(), fileIdentifier,
                    resourceIdentifier);
    }

    private LogUser createUser(User user, URI institution) {
        try {
            var userDto = getUser(user);
            var cristinPersonDto = cristinClient.getPerson(userDto.cristinId()).orElse(null);
            var cristinOrganizationDto = cristinClient.getOrganization(userDto.institutionCristinId()).orElse(null);
            return LogUser.create(cristinPersonDto, cristinOrganizationDto);
        } catch (Exception e) {
            return LogUser.fromResourceEvent(user, institution);
        }
    }

    private UserDto getUser(User user) {
        return attempt(() -> identityServiceClient.getUser(user.toString())).orElseThrow();
    }
}
