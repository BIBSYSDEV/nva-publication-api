package no.unit.nva.expansion.impl;

import no.unit.nva.expansion.model.ExpandedDoiRequest;
import no.unit.nva.expansion.model.ExpandedMessage;
import no.unit.nva.expansion.IdentityClient;
import no.unit.nva.expansion.ResourceExpansionService;
import no.unit.nva.publication.storage.model.DoiRequest;
import no.unit.nva.publication.storage.model.Message;

import java.net.URI;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class ResourceExpansionServiceImpl implements ResourceExpansionService {

    private final IdentityClient identityClient;

    public ResourceExpansionServiceImpl(IdentityClient identityClient) {
        this.identityClient = identityClient;
    }

    @Override
    public ExpandedMessage expandMessage(Message message) {
        ExpandedMessage expandedMessage = new ExpandedMessage(message);
        Set<URI> organizationIds = getOrganizationIds(message.getOwner());
        expandedMessage.setOrganizationIds(organizationIds);
        return expandedMessage;
    }

    @Override
    public ExpandedDoiRequest expandDoiRequest(DoiRequest doiRequest) {
        ExpandedDoiRequest expandedDoiRequest = new ExpandedDoiRequest(doiRequest);
        Set<URI> organizationIds = getOrganizationIds(doiRequest.getOwner());
        expandedDoiRequest.setOrganizationIds(organizationIds);
        return expandedDoiRequest;
    }

    private Set<URI> getOrganizationIds(String username) {
        Set<URI> organizationIds = new HashSet<>();
        getOrganizationId(username).ifPresent(organizationIds::add);
        //TODO: add organization ids from hierarchy
        return organizationIds;
    }

    private Optional<URI> getOrganizationId(String username) {
        Optional<URI> customerId = identityClient.getCustomerId(username);
        Optional<URI> cristinId = Optional.empty();
        if (customerId.isPresent()) {
            cristinId = identityClient.getCristinId(customerId.get());
        }
        return cristinId;
    }

}
