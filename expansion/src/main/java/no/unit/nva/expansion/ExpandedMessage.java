package no.unit.nva.expansion;

import no.unit.nva.publication.storage.model.Message;

import java.net.URI;
import java.util.Set;

import static java.util.Collections.emptySet;
import static java.util.Objects.nonNull;

public class ExpandedMessage extends Message implements WithOrganizationScope {

    private Set<URI> organizationIds;

    public ExpandedMessage(Message message) {
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

    @Override
    public Set<URI> getOrganizationIds() {
        return nonNull(organizationIds) ? organizationIds : emptySet();
    }

    @Override
    public void setOrganizationIds(Set<URI> organizationIds) {
        this.organizationIds = organizationIds;
    }
}
