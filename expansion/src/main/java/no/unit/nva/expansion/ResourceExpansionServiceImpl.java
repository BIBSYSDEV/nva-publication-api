package no.unit.nva.expansion;

import static no.unit.nva.expansion.OrganizationResponseObject.retrieveAllRelatedOrganizations;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.expansion.model.ExpandedDataEntry;
import no.unit.nva.expansion.model.ExpandedDoiRequest;
import no.unit.nva.expansion.model.ExpandedResource;
import no.unit.nva.expansion.model.ExpandedResourceConversation;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.ResourceConversation;
import no.unit.nva.publication.service.impl.DoiRequestService;
import no.unit.nva.publication.service.impl.MessageService;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.storage.model.ConnectedToResource;
import no.unit.nva.publication.storage.model.DataEntry;
import no.unit.nva.publication.storage.model.DoiRequest;
import no.unit.nva.publication.storage.model.Message;
import no.unit.nva.publication.storage.model.MessageType;
import no.unit.nva.publication.storage.model.Resource;
import no.unit.nva.publication.storage.model.UserInstance;
import nva.commons.apigateway.exceptions.NotFoundException;

public class ResourceExpansionServiceImpl implements ResourceExpansionService {

    public static final String UNSUPPORTED_TYPE = "Expansion is not supported for type:";

    private final ResourceService resourceService;
    private final MessageService messageService;
    private final HttpClient externalServicesHttpClient;
    private final DoiRequestService doiRequestService;

    public ResourceExpansionServiceImpl(HttpClient externalServicesHttpClient,
                                        ResourceService resourceService,
                                        MessageService messageService,
                                        DoiRequestService doiRequestService) {
        this.externalServicesHttpClient = externalServicesHttpClient;
        this.resourceService = resourceService;
        this.messageService = messageService;
        this.doiRequestService = doiRequestService;
    }

    @Override
    public ExpandedDataEntry expandEntry(DataEntry dataEntry) throws JsonProcessingException, NotFoundException {
        if (dataEntry instanceof Resource) {
            return ExpandedResource.fromPublication(dataEntry.toPublication());
        } else if (dataEntry instanceof DoiRequest) {
            return ExpandedDoiRequest.create((DoiRequest) dataEntry, this, messageService);
        } else if (dataEntry instanceof Message) {
            return updateResourceConversations((Message) dataEntry);
        }
        // will throw exception if we want to index a new type that we are not handling yet
        throw new UnsupportedOperationException(UNSUPPORTED_TYPE + dataEntry.getClass().getSimpleName());
    }

    @Override
    public Set<URI> getOrganizationIds(DataEntry dataEntry) throws NotFoundException {
        if (dataEntry instanceof ConnectedToResource) {
            var resourceIdentifier = ((ConnectedToResource) dataEntry).getResourceIdentifier();
            var resource = resourceService.getResourceByIdentifier(resourceIdentifier);
            return Optional.ofNullable(resource.getResourceOwner().getOwnerAffiliation())
                .stream()
                .map(affiliation -> retrieveAllRelatedOrganizations(externalServicesHttpClient, affiliation))
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }

    private ExpandedDataEntry updateResourceConversations(Message message) throws NotFoundException {
        if (isDoiRequestMessage(message)) {
            return updateDoiRequestConversation(message);
        }
        return updateExpandedGeneralSupportConversation(message);
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
        UserInstance userInstance = new UserInstance(message.getOwner(), message.getCustomerId());
        SortableIdentifier publicationIdentifier = message.getResourceIdentifier();
        ResourceConversation messagesForResource =
            messageService.getMessagesForResource(userInstance, publicationIdentifier)
                .map(this::keepOnlyGeneralSupportMessages)
                .orElseThrow();
        return ExpandedResourceConversation.create(messagesForResource, message, this);
    }

    private ResourceConversation keepOnlyGeneralSupportMessages(ResourceConversation conversation) {
        return conversation.ofMessageTypes(MessageType.generalSupportMessageTypes().toArray(MessageType[]::new));
    }
}
