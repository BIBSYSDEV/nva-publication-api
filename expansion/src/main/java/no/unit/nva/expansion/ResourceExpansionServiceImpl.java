package no.unit.nva.expansion;

import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.expansion.model.IndexDocument;
import no.unit.nva.expansion.restclients.IdentityClient;
import no.unit.nva.expansion.restclients.InstitutionClient;
import no.unit.nva.expansion.model.ExpandedDoiRequest;
import no.unit.nva.expansion.model.ExpandedMessage;
import no.unit.nva.publication.storage.model.DoiRequest;
import no.unit.nva.publication.storage.model.Message;

import java.net.URI;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import no.unit.nva.publication.storage.model.Resource;

public class ResourceExpansionServiceImpl implements ResourceExpansionService {

    private final IdentityClient identityClient;
    private final InstitutionClient institutionClient;

    public ResourceExpansionServiceImpl(IdentityClient identityClient, InstitutionClient institutionClient) {
        this.identityClient = identityClient;
        this.institutionClient = institutionClient;
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

    @Override
    public IndexDocument expandResource(Resource resource) throws JsonProcessingException {
        return IndexDocument.fromPublication(resource.toPublication());
    }

    private Set<URI> getOrganizationIds(String username) {
        Set<URI> organizationIds = new HashSet<>();
        Optional<URI> organizationId = getOrganizationId(username);
        if (organizationId.isPresent()) {
            organizationIds.addAll(institutionClient.getOrganizationIds(organizationId.get()));
        }
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
