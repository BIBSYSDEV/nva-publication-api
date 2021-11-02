package no.unit.nva.expansion.model;

import no.unit.nva.expansion.ResourceExpansionServiceImpl;
import no.unit.nva.expansion.WithOrganizationScope;
import no.unit.nva.publication.storage.model.Message;

import java.net.URI;
import java.util.Set;

import static java.util.Collections.emptySet;
import static java.util.Objects.nonNull;

public final class ExpandedMessage extends Message implements WithOrganizationScope, ExpandedResourceUpdate {

    private Set<URI> organizationIds;

    private ExpandedMessage(Message message) {
        super();
        setMessageType(message.getMessageType());
        setCreatedTime(message.getCreatedTime());
        setIdentifier(message.getIdentifier());
        setCustomerId(message.getCustomerId());
        setOwner(message.getOwner());
        setResourceIdentifier(message.getResourceIdentifier());
        setSender(message.getSender());
        setResourceTitle(message.getResourceTitle());
        setStatus(message.getStatus());
        setText(message.getText());

    }

    public static ExpandedMessage create(Message message, ResourceExpansionServiceImpl resourceExpansionService) {
        ExpandedMessage expandedMessage = new ExpandedMessage(message);
        Set<URI> organizationIds = resourceExpansionService.getOrganizationIds(message.getOwner());
        expandedMessage.setOrganizationIds(organizationIds);
        return expandedMessage;
    }

    @Override
    public Set<URI> getOrganizationIds() {
        return nonNull(organizationIds) ? organizationIds : emptySet();
    }

    @Override
    public void setOrganizationIds(Set<URI> organizationIds) {
        this.organizationIds = organizationIds;
    }
}
