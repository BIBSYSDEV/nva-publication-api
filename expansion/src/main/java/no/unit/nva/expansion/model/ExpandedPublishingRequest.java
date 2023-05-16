package no.unit.nva.expansion.model;

import static java.util.Objects.nonNull;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.expansion.ResourceExpansionService;
import no.unit.nva.expansion.utils.ExpandedTicketStatusMapper;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.Username;
import no.unit.nva.publication.model.PublicationSummary;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.PublishingWorkflow;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JacocoGenerated;

public class ExpandedPublishingRequest extends ExpandedTicket {

    public static final String TYPE = "PublishingRequest";
    public static final String STATUS_FIELD = "status";

    @JsonProperty("organizationIds")
    private Set<URI> organizationIds;
    @JsonProperty(STATUS_FIELD)
    private ExpandedTicketStatus status;
    private URI customerId;
    private Instant modifiedDate;
    private Instant createdDate;
    private PublishingWorkflow workflow;

    public ExpandedPublishingRequest() {
        super();
    }

    public static ExpandedPublishingRequest createEntry(PublishingRequestCase publishingRequestCase,
                                                        ResourceService resourceService,
                                                        ResourceExpansionService resourceExpansionService,
                                                        TicketService ticketService)
        throws NotFoundException {

        var publication = fetchPublication(publishingRequestCase, resourceService);
        var organizationIds = resourceExpansionService.getOrganizationIds(publishingRequestCase);
        var messages = expandMessages(publishingRequestCase.fetchMessages(ticketService), resourceExpansionService);
        var workflow = publishingRequestCase.getWorkflow();
        var owner = resourceExpansionService.expandPerson(publishingRequestCase.getOwner());
        var assignee = expandPerson(publishingRequestCase.getAssignee(), resourceExpansionService);
        var finalizedBy = expandPerson(publishingRequestCase.getFinalizedBy(), resourceExpansionService);
        var viewedBy = expandPersonViewedBy(publishingRequestCase.getViewedBy(), resourceExpansionService);
        return createRequest(publishingRequestCase, publication, organizationIds, messages, workflow, owner, assignee,
                             finalizedBy, viewedBy);
    }

    @JacocoGenerated
    @Override
    public String toJsonString() {
        return super.toJsonString();
    }

    @Override
    public SortableIdentifier identifyExpandedEntry() {
        return extractIdentifier(getId());
    }

    @Override
    public Set<URI> getOrganizationIds() {
        return nonNull(organizationIds) ? organizationIds : Collections.emptySet();
    }

    public void setOrganizationIds(Set<URI> organizationIds) {
        this.organizationIds = organizationIds;
    }

    @Override
    public ExpandedTicketStatus getStatus() {
        return status;
    }

    public void setStatus(ExpandedTicketStatus status) {
        this.status = status;
    }

    public Instant getCreatedDate() {
        return this.createdDate;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    public Instant getModifiedDate() {
        return this.modifiedDate;
    }

    public void setModifiedDate(Instant modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    public URI getCustomerId() {
        return customerId;
    }

    public void setCustomerId(URI customerId) {
        this.customerId = customerId;
    }

    public PublishingWorkflow getWorkflow() {
        return workflow;
    }

    public void setWorkflow(PublishingWorkflow workflow) {
        this.workflow = workflow;
    }

    private static List<ExpandedMessage> expandMessages(List<Message> messages,
                                                        ResourceExpansionService expansionService) {
        return messages.stream()
            .map(expansionService::expandMessage)
            .collect(Collectors.toList());
    }

    private static ExpandedPublishingRequest createRequest(PublishingRequestCase dataEntry,
                                                           Publication publication,
                                                           Set<URI> organizationIds,
                                                           List<ExpandedMessage> messages,
                                                           PublishingWorkflow workflow,
                                                           ExpandedPerson owner,
                                                           ExpandedPerson assignee,
                                                           ExpandedPerson finalizedBy,
                                                           Set<ExpandedPerson> viewedBy) throws NotFoundException {
        var publicationSummary = PublicationSummary.create(publication);
        var entry = new ExpandedPublishingRequest();
        entry.setId(generateId(publicationSummary.getPublicationId(), dataEntry.getIdentifier()));
        entry.setPublication(publicationSummary);
        entry.setOrganizationIds(organizationIds);
        entry.setStatus(ExpandedTicketStatusMapper.getExpandedTicketStatus(dataEntry));
        entry.setCustomerId(dataEntry.getCustomerId());
        entry.setCreatedDate(dataEntry.getCreatedDate());
        entry.setModifiedDate(dataEntry.getModifiedDate());
        entry.setMessages(messages);
        entry.setViewedBy(viewedBy);
        entry.setWorkflow(workflow);
        entry.setFinalizedBy(finalizedBy);
        entry.setOwner(owner);
        entry.setAssignee(assignee);
        return entry;
    }

    private static Publication fetchPublication(PublishingRequestCase publishingRequestCase,
                                                ResourceService resourceService) {
        return attempt(() -> resourceService.getPublicationByIdentifier(
            publishingRequestCase.extractPublicationIdentifier())).orElseThrow();
    }

    private static ExpandedPerson expandPerson(Username username,
                                                        ResourceExpansionService expansionService) {
        return Optional.ofNullable(username)
            .map(Username::getValue)
            .map(User::new)
            .map(expansionService::expandPerson)
            .orElse(null);
    }

    private static Set<ExpandedPerson> expandPersonViewedBy(Set<User> users,
                                                                    ResourceExpansionService resourceExpansionService) {
        return users.stream()
            .map(resourceExpansionService::expandPerson)
            .collect(Collectors.toSet());
    }
}