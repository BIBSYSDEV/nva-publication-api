package no.unit.nva.expansion;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.URI;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import no.unit.nva.expansion.model.ExpandedDoiRequest;
import no.unit.nva.expansion.model.ExpandedMessage;
import no.unit.nva.expansion.model.ExpandedResource;
import no.unit.nva.expansion.model.ExpandedDataEntry;
import no.unit.nva.expansion.restclients.IdentityClient;
import no.unit.nva.expansion.restclients.InstitutionClient;
import no.unit.nva.publication.storage.model.DoiRequest;
import no.unit.nva.publication.storage.model.Message;
import no.unit.nva.publication.storage.model.Resource;
import no.unit.nva.publication.storage.model.DataEntry;

public class ResourceExpansionServiceImpl implements ResourceExpansionService {

    public static final String UNSUPPORTED_TYPE = "Expansion is not supported for type:";
    private final IdentityClient identityClient;
    private final InstitutionClient institutionClient;

    public ResourceExpansionServiceImpl(IdentityClient identityClient, InstitutionClient institutionClient) {
        this.identityClient = identityClient;
        this.institutionClient = institutionClient;
    }

    @Override
    public ExpandedDataEntry expandEntry(DataEntry dataEntry) throws JsonProcessingException {
        if (dataEntry instanceof Resource) {
            return ExpandedResource.fromPublication(dataEntry.toPublication());
        } else if (dataEntry instanceof DoiRequest) {
            return ExpandedDoiRequest.create((DoiRequest) dataEntry, this);
        } else if (dataEntry instanceof Message) {
            return ExpandedMessage.create((Message) dataEntry, this);
        }
        // will throw exception if we want to index a new type that we are not handling yet
        throw new UnsupportedOperationException(UNSUPPORTED_TYPE + dataEntry.getClass().getSimpleName());
    }

    @Override
    public Set<URI> getOrganizationIds(String username) {
        Set<URI> organizationIds = new HashSet<>();
        Optional<URI> organizationId = getOrganizationId(username);
        organizationId.ifPresent(uri -> organizationIds.addAll(institutionClient.getOrganizationIds(uri)));
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
