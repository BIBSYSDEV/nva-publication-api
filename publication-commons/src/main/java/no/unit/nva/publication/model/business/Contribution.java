package no.unit.nva.publication.model.business;

import static java.util.Objects.nonNull;
import static no.unit.nva.publication.model.business.TicketEntry.Constants.CREATED_DATE_FIELD;
import static no.unit.nva.publication.model.business.TicketEntry.Constants.CUSTOMER_ID_FIELD;
import static no.unit.nva.publication.model.business.TicketEntry.Constants.MODIFIED_DATE_FIELD;
import static no.unit.nva.publication.model.business.TicketEntry.Constants.OWNER_FIELD;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.Set;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.model.storage.ContributionDao;
import no.unit.nva.publication.model.storage.Dao;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = Id.NAME, property = "type")
public class Contribution implements Entity {

    public static final String TYPE = "Contribution";
    private static final String IDENTIFIER_FIELD = "identifier";
    private static final String RESOURCE_IDENTIFIER = "resourceIdentifier";
    private static final String CONTRIBUTOR = "contributor";

    @JsonProperty(CREATED_DATE_FIELD)
    private Instant createdDate;
    @JsonProperty(MODIFIED_DATE_FIELD)
    private Instant modifiedDate;
    @JsonProperty(IDENTIFIER_FIELD)
    private SortableIdentifier identifier;
    @JsonProperty(RESOURCE_IDENTIFIER)
    private SortableIdentifier resourceIdentifier;
    @JsonProperty(OWNER_FIELD)
    private User owner;
    @JsonProperty(CUSTOMER_ID_FIELD)
    private URI customerId;

    @JsonProperty(CONTRIBUTOR)
    private Contributor contributor;

    @JsonProperty
    private Set<AdditionalIdentifier> additionalIdentifiers;

    @JacocoGenerated
    public Contribution() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Contribution create(Resource resource,
                                 Contributor contributor) {
        var now = Instant.now();
        return builder()
                   .withCreatedDate(now)
                   .withModifiedDate(now)
                   .withCustomerId(resource.getCustomerId())
                   .withOwner(resource.getOwner())
                   .withIdentifier(SortableIdentifier.next())
                   .withContributor(contributor)
                   .withResourceIdentifier(resource.getIdentifier())
                   .withAdditionalIdentifiers(resource.getAdditionalIdentifiers())
                   .build();
    }

    @Override
    public SortableIdentifier getIdentifier() {
        return identifier;
    }

    @Override
    public void setIdentifier(SortableIdentifier identifier) {
        this.identifier = identifier;
    }

    @Override
    public Publication toPublication(ResourceService resourceService) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public Instant getCreatedDate() {
        return this.createdDate;
    }

    @Override
    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    @Override
    public Instant getModifiedDate() {
        return this.modifiedDate;
    }

    @Override
    public void setModifiedDate(Instant modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    @Override
    public User getOwner() {
        return this.owner;
    }

    @Override
    public URI getCustomerId() {
        return this.customerId;
    }

    @Override
    public Dao toDao() {
        return new ContributionDao(this);
    }

    @Override
    public String getStatusString() {
        return "Not supported";
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public void setCustomerId(URI customerId) {
        this.customerId = customerId;
    }

    private void setContributor(Contributor contributor) {
        this.contributor = contributor;
    }

    public Contributor getContributor() {
        return contributor;
    }

    public void setResourceIdentifier(SortableIdentifier resourceIdentifier) {
        this.resourceIdentifier = resourceIdentifier;
    }

    public SortableIdentifier getResourceIdentifier() {
        return resourceIdentifier;
    }

    public Set<AdditionalIdentifier> getAdditionalIdentifiers() {
        return nonNull(additionalIdentifiers) ? additionalIdentifiers : Collections.emptySet();
    }

    public void setAdditionalIdentifiers(Set<AdditionalIdentifier> additionalIdentifiers) {
        this.additionalIdentifiers = additionalIdentifiers;
    }

    public static final class Builder {

        private final Contribution contribution;

        private Builder() {
            contribution = new Contribution();
        }

        public Builder withIdentifier(SortableIdentifier identifier) {
            contribution.setIdentifier(identifier);
            return this;
        }

        public Builder withOwner(User owner) {
            contribution.setOwner(owner);
            return this;
        }

        public Builder withCustomerId(URI customerId) {
            contribution.setCustomerId(customerId);
            return this;
        }

        public Builder withResourceIdentifier(SortableIdentifier resourceIdentifier) {
            contribution.setResourceIdentifier(resourceIdentifier);
            return this;
        }


        public Builder withCreatedDate(Instant createdDate) {
            contribution.setCreatedDate(createdDate);
            return this;
        }

        public Builder withModifiedDate(Instant modifiedDate) {
            contribution.setModifiedDate(modifiedDate);
            return this;
        }

        public Builder withContributor(Contributor contributor) {
            contribution.setContributor(contributor);
            return this;
        }

        public Builder withAdditionalIdentifiers(Set<AdditionalIdentifier> additionalIdentifiers) {
            contribution.additionalIdentifiers = additionalIdentifiers;
            return this;
        }

        public Contribution build() {
            return contribution;
        }
    }

}
