package no.unit.nva.publication.model;

import static java.util.Objects.nonNull;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.time.Instant;
import java.util.Objects;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.PublicationServiceConfig;
import no.unit.nva.publication.model.business.PublicationDetails;
import no.unit.nva.publication.model.business.User;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;

@JsonTypeName(PublicationSummary.TYPE)
public class PublicationSummary {
    
    public static final String TYPE = "Publication";
    
    @JsonProperty("id")
    private URI publicationId;
    @JsonProperty("identifier")
    private SortableIdentifier identifier;
    @JsonProperty("mainTitle")
    private String title;
    @JsonProperty
    private User owner;
    @JsonProperty
    private Instant createdDate;
    @JsonProperty
    private Instant modifiedDate;
    @JsonProperty
    private PublicationStatus status;
    
    public static PublicationSummary create(Publication publication) {
        var publicationSummary = new PublicationSummary();
        publicationSummary.setIdentifier(publication.getIdentifier());
        publicationSummary.setPublicationId(toPublicationId(publication.getIdentifier()));
        publicationSummary.setCreatedDate(publication.getCreatedDate());
        publicationSummary.setModifiedDate(publication.getModifiedDate());
        publicationSummary.setOwner(new User(publication.getResourceOwner().getOwner().getValue()));
        publicationSummary.setStatus(publication.getStatus());
        if (nonNull(publication.getEntityDescription())) {
            publicationSummary.setTitle(publication.getEntityDescription().getMainTitle());
        }
        return publicationSummary;
    }
    
    public static PublicationSummary create(PublicationDetails publicationDetails) {
        var publicationSummary = new PublicationSummary();
        publicationSummary.setIdentifier(publicationDetails.getIdentifier());
        publicationSummary.setPublicationId(toPublicationId(publicationDetails.getIdentifier()));
        publicationSummary.setCreatedDate(publicationDetails.getCreatedDate());
        publicationSummary.setModifiedDate(publicationDetails.getModifiedDate());
        publicationSummary.setOwner(new User(publicationDetails.getOwner().toString()));
        publicationSummary.setStatus(publicationDetails.getStatus());
        publicationSummary.setTitle(publicationDetails.getTitle());
        return publicationSummary;
    }
    
    public static PublicationSummary create(URI publicationId, String publicationTitle) {
        var publicationSummary = new PublicationSummary();
        publicationSummary.setIdentifier(extractPublicationIdentifier(publicationId));
        publicationSummary.setPublicationId(publicationId);
        publicationSummary.setTitle(publicationTitle);
        return publicationSummary;
    }
    
    public SortableIdentifier getIdentifier() {
        return identifier;
    }
    
    public void setIdentifier(SortableIdentifier identifier) {
        this.identifier = identifier;
    }
    
    public PublicationStatus getStatus() {
        return status;
    }
    
    public void setStatus(PublicationStatus status) {
        this.status = status;
    }
    
    public URI getPublicationId() {
        return publicationId;
    }
    
    public void setPublicationId(URI publicationId) {
        this.publicationId = publicationId;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public Instant getModifiedDate() {
        return modifiedDate;
    }
    
    public void setModifiedDate(Instant modifiedDate) {
        this.modifiedDate = modifiedDate;
    }
    
    public Instant getCreatedDate() {
        return createdDate;
    }
    
    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }
    
    public User getOwner() {
        return owner;
    }
    
    public void setOwner(User owner) {
        this.owner = owner;
    }
    
    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getPublicationId(), getIdentifier(), getTitle(), getOwner(), getCreatedDate(),
            getModifiedDate(), getStatus());
    }
    
    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PublicationSummary)) {
            return false;
        }
        PublicationSummary that = (PublicationSummary) o;
        return Objects.equals(getPublicationId(), that.getPublicationId())
               && Objects.equals(getIdentifier(), that.getIdentifier())
               && Objects.equals(getTitle(), that.getTitle())
               && Objects.equals(getOwner(), that.getOwner())
               && Objects.equals(getCreatedDate(), that.getCreatedDate())
               && Objects.equals(getModifiedDate(), that.getModifiedDate())
               && getStatus() == that.getStatus();
    }
    
    public SortableIdentifier extractPublicationIdentifier() {
        return extractPublicationIdentifier(publicationId);
    }
    
    private static SortableIdentifier extractPublicationIdentifier(URI publicationId) {
        return new SortableIdentifier(UriWrapper.fromUri(publicationId).getLastPathElement());
    }
    
    private static URI toPublicationId(SortableIdentifier identifier) {
        return UriWrapper.fromUri(PublicationServiceConfig.PUBLICATION_HOST_URI)
                   .addChild(identifier.toString()).getUri();
    }
}
