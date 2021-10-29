package no.unit.nva.expansion;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.URI;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import no.unit.nva.expansion.model.ExpandedDoiRequest;
import no.unit.nva.expansion.model.ExpandedMessage;
import no.unit.nva.expansion.model.ExpandedResourceUpdate;
import no.unit.nva.expansion.model.IndexDocument;
import no.unit.nva.expansion.restclients.IdentityClient;
import no.unit.nva.expansion.restclients.InstitutionClient;
import no.unit.nva.publication.storage.model.DoiRequest;
import no.unit.nva.publication.storage.model.Message;
import no.unit.nva.publication.storage.model.Resource;
import no.unit.nva.publication.storage.model.ResourceUpdate;

public class ResourceExpansionServiceImpl implements ResourceExpansionService {

    public static final String UNSUPPORTED_TYPE = "Expansion is not supported for type:";
    private final IdentityClient identityClient;
    private final InstitutionClient institutionClient;

    public ResourceExpansionServiceImpl(IdentityClient identityClient, InstitutionClient institutionClient) {
        this.identityClient = identityClient;
        this.institutionClient = institutionClient;
    }

    @Override
    public ExpandedResourceUpdate expandEntry(ResourceUpdate resourceUpdate) throws JsonProcessingException {
        if (resourceUpdate instanceof Resource) {
            return expandResource((Resource) resourceUpdate);
        } else if (resourceUpdate instanceof DoiRequest) {
            return expandDoiRequest((DoiRequest) resourceUpdate);
        } else if (resourceUpdate instanceof Message) {
            return expandMessage((Message) resourceUpdate);
        }
        // will throw exception if we want to index a new type that we are not handling yet
        throw new UnsupportedOperationException(UNSUPPORTED_TYPE + resourceUpdate.getClass().getSimpleName());
    }

    private ExpandedMessage expandMessage(Message message) {
        ExpandedMessage expandedMessage = new ExpandedMessage(message);
        Set<URI> organizationIds = getOrganizationIds(message.getOwner());
        expandedMessage.setOrganizationIds(organizationIds);
        return expandedMessage;
    }

    private ExpandedDoiRequest expandDoiRequest(DoiRequest doiRequest) {
        ExpandedDoiRequest expandedDoiRequest = new ExpandedDoiRequest(doiRequest);
        Set<URI> organizationIds = getOrganizationIds(doiRequest.getOwner());
        expandedDoiRequest.setOrganizationIds(organizationIds);
        return expandedDoiRequest;
    }

    private IndexDocument expandResource(Resource resource) throws JsonProcessingException {
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
