package no.unit.nva.publication.model.business.publicationchannel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;

import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.storage.PublicationChannelDao;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName(NonClaimedPublicationChannel.TYPE)
public final class NonClaimedPublicationChannel implements PublicationChannel, JsonSerializable {

    public static final String TYPE = "NonClaimedPublicationChannel";

    private final URI id;
    private final ChannelType channelType;
    private final SortableIdentifier identifier;
    private final SortableIdentifier resourceIdentifier;
    private final Instant createdDate;
    private final Instant modifiedDate;

    @JsonCreator
    public NonClaimedPublicationChannel(
        @JsonProperty(ID_FIELD) URI id,
        @JsonProperty(CHANNEL_TYPE_FIELD) ChannelType channelType,
        @JsonProperty(IDENTIFIER_FIELD) SortableIdentifier identifier,
        @JsonProperty(RESOURCE_IDENTIFIER_FIELD) SortableIdentifier resourceIdentifier,
        @JsonProperty(CREATED_DATE_FIELD) Instant createdDate,
        @JsonProperty(MODIFIED_DATE_FIELD) Instant modifiedDate) {
        this.id = id;
        this.channelType = channelType;
        this.identifier = identifier;
        this.resourceIdentifier = resourceIdentifier;
        this.createdDate = createdDate;
        this.modifiedDate = modifiedDate;
    }

    public static NonClaimedPublicationChannel create(
        URI channelClaimId, SortableIdentifier resourceIdentifier, ChannelType channelType) {
        var identifier =
            new SortableIdentifier(UriWrapper.fromUri(channelClaimId).getLastPathElement());
        return new NonClaimedPublicationChannel(
            channelClaimId, channelType, identifier, resourceIdentifier, Instant.now(), Instant.now());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(
            getId(),
            getChannelType(),
            getIdentifier(),
            getResourceIdentifier(),
            getCreatedDate(),
            getModifiedDate());
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof NonClaimedPublicationChannel that)) {
            return false;
        }
        return Objects.equals(getId(), that.getId())
               && getChannelType() == that.getChannelType()
               && Objects.equals(getIdentifier(), that.getIdentifier())
               && Objects.equals(getResourceIdentifier(), that.getResourceIdentifier())
               && Objects.equals(getCreatedDate(), that.getCreatedDate())
               && Objects.equals(getModifiedDate(), that.getModifiedDate());
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
    public SortableIdentifier getResourceIdentifier() {
        return resourceIdentifier;
    }

    @JacocoGenerated
    @Override
    public ChannelType getChannelType() {
        return channelType;
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
        return null;
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

    public ClaimedPublicationChannel toClaimedChannel(
        URI customerId, URI organizationId, Constraint constraint) {
        return new ClaimedPublicationChannel(
            id,
            customerId,
            organizationId,
            constraint,
            channelType,
            identifier,
            resourceIdentifier,
            createdDate,
            Instant.now());
    }
}
