package no.unit.nva.publication.model.business.publicationchannel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.time.Instant;
import java.util.Objects;
import no.unit.nva.clients.ChannelClaimDto;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.storage.PublicationChannelDao;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName(ClaimedPublicationChannel.TYPE)
public final class ClaimedPublicationChannel implements PublicationChannel, JsonSerializable {

    static final String TYPE = "ClaimedPublicationChannel";

    private final URI id;
    private final URI customerId;
    private final URI organizationId;
    private final Constraint constraint;
    private final ChannelType channelType;
    private final SortableIdentifier identifier;
    private final SortableIdentifier resourceIdentifier;
    private final Instant createdDate;
    private final Instant modifiedDate;

    @JsonCreator
    public ClaimedPublicationChannel(@JsonProperty(ID_FIELD) URI id, @JsonProperty(CUSTOMER_ID_FIELD) URI customerId,
                                     @JsonProperty(ORGANIZATION_ID_FIELD) URI organizationId,
                                     @JsonProperty(CONSTRAINT_FIELD) Constraint constraint,
                                     @JsonProperty(CHANNEL_TYPE_FIELD) ChannelType channelType,
                                     @JsonProperty(IDENTIFIER_FIELD) SortableIdentifier identifier,
                                     @JsonProperty(RESOURCE_IDENTIFIER_FIELD) SortableIdentifier resourceIdentifier,
                                     @JsonProperty(CREATED_DATE_FIELD) Instant createdDate,
                                     @JsonProperty(MODIFIED_DATE_FIELD) Instant modifiedDate) {
        this.id = id;
        this.customerId = customerId;
        this.organizationId = organizationId;
        this.constraint = constraint;
        this.channelType = channelType;
        this.identifier = identifier;
        this.resourceIdentifier = resourceIdentifier;
        this.createdDate = createdDate;
        this.modifiedDate = modifiedDate;
    }

    public static ClaimedPublicationChannel create(URI id, ChannelClaimDto channelClaimDto,
                                                   SortableIdentifier resourceIdentifier, ChannelType channelType) {
        var identifier = new SortableIdentifier(UriWrapper.fromUri(id).getLastPathElement());
        return new ClaimedPublicationChannel(id, channelClaimDto.claimedBy().id(),
                                             channelClaimDto.claimedBy().organizationId(), new Constraint(
            ChannelPolicy.fromValue(channelClaimDto.channelClaim().constraint().publishingPolicy()),
            ChannelPolicy.fromValue(channelClaimDto.channelClaim().constraint().editingPolicy()),
            channelClaimDto.channelClaim().constraint().scope()),
                                             channelType,
                                             identifier, resourceIdentifier, Instant.now(), Instant.now());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getId(), getCustomerId(), getOrganizationId(), getConstraint(), getChannelType(),
                            getIdentifier(), getResourceIdentifier(), getCreatedDate(), getModifiedDate());
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ClaimedPublicationChannel that)) {
            return false;
        }
        return Objects.equals(getId(), that.getId()) && Objects.equals(getCustomerId(), that.getCustomerId()) &&
               Objects.equals(getOrganizationId(), that.getOrganizationId()) &&
               Objects.equals(getConstraint(), that.getConstraint()) && getChannelType() == that.getChannelType() &&
               Objects.equals(getIdentifier(), that.getIdentifier()) &&
               Objects.equals(getResourceIdentifier(), that.getResourceIdentifier()) &&
               Objects.equals(getCreatedDate(), that.getCreatedDate()) &&
               Objects.equals(getModifiedDate(), that.getModifiedDate());
    }

    @JacocoGenerated
    @Override
    public URI getId() {
        return id;
    }

    @JacocoGenerated
    @Override
    public SortableIdentifier getIdentifier() {
        return identifier;
    }

    @JacocoGenerated
    @Override
    public void setIdentifier(SortableIdentifier identifier) {
        throw new UnsupportedOperationException();
    }

    @JacocoGenerated
    @Override
    public Publication toPublication(ResourceService resourceService) {
        throw new UnsupportedOperationException();
    }

    @JacocoGenerated
    @Override
    public String getType() {
        return TYPE;
    }

    @JacocoGenerated
    @Override
    public Instant getCreatedDate() {
        return createdDate;
    }

    @JacocoGenerated
    @Override
    public void setCreatedDate(Instant now) {
        throw new UnsupportedOperationException();
    }

    @JacocoGenerated
    @Override
    public Instant getModifiedDate() {
        return modifiedDate;
    }

    @JacocoGenerated
    @Override
    public void setModifiedDate(Instant now) {
        throw new UnsupportedOperationException();
    }

    @JacocoGenerated
    @Override
    public User getOwner() {
        return null;
    }

    @JacocoGenerated
    @Override
    public URI getCustomerId() {
        return customerId;
    }

    @JacocoGenerated
    @Override
    public PublicationChannelDao toDao() {
        return PublicationChannelDao.fromPublicationChannel(this);
    }

    @JacocoGenerated
    @Override
    public String getStatusString() {
        return null;
    }

    @JacocoGenerated
    @Override
    public SortableIdentifier getResourceIdentifier() {
        return resourceIdentifier;
    }

    @JacocoGenerated
    @Override
    public ChannelType getChannelType() {
        return channelType;
    }

    @JacocoGenerated
    public URI getOrganizationId() {
        return organizationId;
    }

    @JacocoGenerated
    public Constraint getConstraint() {
        return constraint;
    }
}
