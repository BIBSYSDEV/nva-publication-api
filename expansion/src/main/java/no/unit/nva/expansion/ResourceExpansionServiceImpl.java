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
import no.unit.nva.expansion.model.ExpandedDoiRequest;
import no.unit.nva.expansion.model.ExpandedPublishingRequest;
import no.unit.nva.expansion.model.ExpandedResource;
import no.unit.nva.expansion.model.ExpandedResourceConversation;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.ResourceConversation;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.service.impl.DoiRequestService;
import no.unit.nva.publication.service.impl.MessageService;
import no.unit.nva.publication.service.impl.PublishingRequestService;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.model.business.DataEntry;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.MessageType;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import nva.commons.apigateway.exceptions.NotFoundException;

public class ResourceExpansionServiceImpl implements ResourceExpansionService {
    
    public static final String UNSUPPORTED_TYPE = "Expansion is not supported for type:";
    
    private final ResourceService resourceService;
    private final MessageService messageService;
    private final DoiRequestService doiRequestService;
    private final PublishingRequestService publishingRequestService;
    
    public ResourceExpansionServiceImpl(ResourceService resourceService,
                                        MessageService messageService,
                                        DoiRequestService doiRequestService,
                                        PublishingRequestService publishingRequestService) {
        
        this.resourceService = resourceService;
        this.messageService = messageService;
        this.doiRequestService = doiRequestService;
        this.publishingRequestService = publishingRequestService;
    }
    
    @Override
    public ExpandedDataEntry expandEntry(DataEntry dataEntry) throws JsonProcessingException, NotFoundException {
        if (dataEntry instanceof Resource) {
            return ExpandedResource.fromPublication(dataEntry.toPublication());
        } else if (dataEntry instanceof DoiRequest) {
            return ExpandedDoiRequest.create((DoiRequest) dataEntry, this, messageService);
        } else if (dataEntry instanceof Message) {
            return updateResourceConversations((Message) dataEntry);
        } else if (dataEntry instanceof PublishingRequestCase) {
            return ExpandedPublishingRequest.create((PublishingRequestCase) dataEntry,
                resourceService,
                messageService,
                this);
        }
        // will throw exception if we want to index a new type that we are not handling yet
        throw new UnsupportedOperationException(UNSUPPORTED_TYPE + dataEntry.getClass().getSimpleName());
    }
    
    @Override
    public Set<URI> getOrganizationIds(DataEntry dataEntry) throws NotFoundException {
        if (dataEntry instanceof TicketEntry) {
            var resourceIdentifier = ((TicketEntry) dataEntry).getResourceIdentifier();
            var resource = resourceService.getResourceByIdentifier(resourceIdentifier);
            return Optional.ofNullable(resource.getResourceOwner().getOwnerAffiliation())
                .stream()
                .map(this::retrieveAllHigherLevelOrgsInTheFutureWhenResourceOwnerAffiliationIsNotAlwaysTopLevelOrg)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }
    
    private List<URI> retrieveAllHigherLevelOrgsInTheFutureWhenResourceOwnerAffiliationIsNotAlwaysTopLevelOrg(
        URI affiliation) {
        return List.of(affiliation);
    }
    
    private ExpandedDataEntry updateResourceConversations(Message message) throws NotFoundException {
        if (isDoiRequestMessage(message)) {
            return updateDoiRequestConversation(message);
        } else if (isPublishingRequestMessage(message)) {
            return updatePublishingRequestConversation(message);
        }
        return updateExpandedGeneralSupportConversation(message);
    }
    
    private ExpandedDataEntry updatePublishingRequestConversation(Message message) throws NotFoundException {
        var publishingRequest = publishingRequestService
            .getPublishingRequestByResourceIdentifier(message.getCustomerId(), message.getResourceIdentifier());
        return ExpandedPublishingRequest.create(publishingRequest, resourceService, messageService, this);
    }
    
    private boolean isPublishingRequestMessage(Message message) {
        return MessageType.PUBLISHING_REQUEST.equals(message.getMessageType());
    }
    
    private boolean isDoiRequestMessage(Message message) {
        return MessageType.DOI_REQUEST.equals(message.getMessageType());
    }
    
    private ExpandedDoiRequest updateDoiRequestConversation(Message message) throws NotFoundException {
        var doiRequest =
            doiRequestService.getDoiRequestByResourceIdentifier(UserInstance.fromMessage(message),
                message.getResourceIdentifier());
        
        return ExpandedDoiRequest.create(doiRequest, this, messageService);
    }
    
    private ExpandedResourceConversation updateExpandedGeneralSupportConversation(Message message)
        throws NotFoundException {
        UserInstance userInstance = UserInstance.create(message.getOwner(), message.getCustomerId());
        SortableIdentifier publicationIdentifier = message.getResourceIdentifier();
        ResourceConversation messagesForResource =
            messageService.getMessagesForResource(userInstance, publicationIdentifier)
                .map(this::keepOnlyGeneralSupportMessages)
                .orElseThrow();
        return ExpandedResourceConversation.create(messagesForResource, message, this);
    }
    
    private ResourceConversation keepOnlyGeneralSupportMessages(ResourceConversation conversation) {
        return conversation.ofMessageTypes(MessageType.SUPPORT);
    }
}
