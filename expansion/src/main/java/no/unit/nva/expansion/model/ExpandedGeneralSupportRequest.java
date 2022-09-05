package no.unit.nva.expansion.model;

import static no.unit.nva.expansion.model.ExpandedTicket.Constants.IDENTIFIER_FIELD;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.util.Set;
import no.unit.nva.expansion.ResourceExpansionService;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.PublicationSummary;
import no.unit.nva.publication.model.business.GeneralSupportRequest;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.exceptions.NotFoundException;

@JsonTypeName(ExpandedGeneralSupportRequest.TYPE)
public class ExpandedGeneralSupportRequest implements ExpandedTicket {
    
    public static final String TYPE = "GeneralSupportRequest";
    @JsonProperty(IDENTIFIER_FIELD)
    private SortableIdentifier identifier;
    private PublicationSummary publicationSummary;
    private Set<URI> organizationIds;
    
    public static ExpandedDataEntry create(GeneralSupportRequest dataEntry,
                                           ResourceService resourceService,
                                           ResourceExpansionService resourceExpansionService) throws NotFoundException {
        var publication = resourceService.getPublicationByIdentifier(dataEntry.getResourceIdentifier());
        var entry = new ExpandedGeneralSupportRequest();
        entry.identifier = dataEntry.getIdentifier();
        entry.publicationSummary = PublicationSummary.create(publication);
        entry.organizationIds = resourceExpansionService.getOrganizationIds(dataEntry);
        return entry;
    }
    
    @Override
    public SortableIdentifier identifyExpandedEntry() {
        return getIdentifier();
    }
    
    @Override
    public PublicationSummary getPublicationSummary() {
        return this.publicationSummary;
    }
    
    @Override
    public Set<URI> getOrganizationIds() {
        return this.organizationIds;
    }
    
    private SortableIdentifier getIdentifier() {
        return this.identifier;
    }
}
