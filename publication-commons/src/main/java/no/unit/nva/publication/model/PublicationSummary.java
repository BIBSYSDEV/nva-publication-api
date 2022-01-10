package no.unit.nva.publication.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.pages.Pages;
import no.unit.nva.publication.PublicationServiceConfig;
import no.unit.nva.publication.storage.model.ConnectedToResource;
import no.unit.nva.publication.storage.model.DoiRequest;
import no.unit.nva.publication.storage.model.Message;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;

import static java.util.Objects.nonNull;

@JsonTypeName(PublicationSummary.TYPE)
public class PublicationSummary {

    public static final String TYPE = "PublicationSummary";
    @JsonProperty("id")
    private URI publicationId;
    @JsonProperty("identifier")
    private SortableIdentifier publicationIdentifier;
    @JsonProperty
    private String title;
    @JsonProperty
    private String owner;
    @JsonProperty
    private Instant createdDate;
    @JsonProperty
    private Instant modifiedDate;
    @JsonProperty
    private PublicationInstance<? extends Pages> publicationInstance;
    @JsonProperty
    private PublicationDate publicationDate;
    @JsonProperty
    private String publicationYear;
    @JsonProperty
    private List<Contributor> contributors;
    @JsonProperty
    private PublicationStatus status;

    public static PublicationSummary create(DoiRequest doiRequest) {
        var publicationSummary = new PublicationSummary();
        publicationSummary.setContributors(doiRequest.getContributors());
        publicationSummary.setPublicationId(extractPublicationId(doiRequest));
        publicationSummary.setPublicationIdentifier(doiRequest.getIdentifier());
        publicationSummary.setCreatedDate(doiRequest.getCreatedDate());
        publicationSummary.setModifiedDate(doiRequest.getResourceModifiedDate());
        publicationSummary.setPublicationDate(doiRequest.getResourcePublicationDate());
        publicationSummary.setPublicationYear(doiRequest.getResourcePublicationYear());
        publicationSummary.setTitle(doiRequest.getResourceTitle());
        publicationSummary.setOwner(doiRequest.getOwner());
        publicationSummary.setPublicationInstance(doiRequest.getResourcePublicationInstance());
        publicationSummary.setStatus(doiRequest.getResourceStatus());
        return publicationSummary;
    }

    public static PublicationSummary create(Message message) {
        var publicationSummary = new PublicationSummary();
        publicationSummary.setPublicationId(extractPublicationId(message));
        publicationSummary.setPublicationIdentifier(message.getResourceIdentifier());
        publicationSummary.setTitle(message.getResourceTitle());
        publicationSummary.setOwner(message.getOwner());
        publicationSummary.setContributors(Collections.emptyList());
        return publicationSummary;
    }

    public static PublicationSummary create(Publication publication) {
        var publicationSummary = new PublicationSummary();
        publicationSummary.setContributors(publication.getEntityDescription().getContributors());
        publicationSummary.setPublicationId(toPublicationId(publication.getIdentifier()));
        publicationSummary.setPublicationIdentifier(publication.getIdentifier());
        publicationSummary.setCreatedDate(publication.getCreatedDate());
        publicationSummary.setModifiedDate(publication.getModifiedDate());
        if (nonNull(publication.getEntityDescription().getDate())) {
            publicationSummary.setPublicationDate(publication.getEntityDescription().getDate());
            publicationSummary.setPublicationYear(publication.getEntityDescription().getDate().getYear());
        }
        publicationSummary.setTitle(publication.getEntityDescription().getMainTitle());
        publicationSummary.setOwner(publication.getOwner());
        if (nonNull(publication.getEntityDescription().getReference())) {
            publicationSummary.setPublicationInstance(publication.getEntityDescription().getReference().getPublicationInstance());
        }
        publicationSummary.setStatus(publication.getStatus());
        return publicationSummary;
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

    public SortableIdentifier getPublicationIdentifier() {
        return publicationIdentifier;
    }

    public void setPublicationIdentifier(SortableIdentifier publicationIdentifier) {
        this.publicationIdentifier = publicationIdentifier;
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

    public PublicationInstance<? extends Pages> getPublicationInstance() {
        return publicationInstance;
    }

    public void setPublicationInstance(
            PublicationInstance<? extends Pages> publicationInstance) {
        this.publicationInstance = publicationInstance;
    }

    public PublicationDate getPublicationDate() {
        return publicationDate;
    }

    public void setPublicationDate(PublicationDate publicationDate) {
        this.publicationDate = publicationDate;
    }

    public String getPublicationYear() {
        return publicationYear;
    }

    public void setPublicationYear(String publicationYear) {
        this.publicationYear = publicationYear;
    }

    public List<Contributor> getContributors() {
        return contributors;
    }

    public void setContributors(List<Contributor> contributors) {
        this.contributors = contributors;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getPublicationId(), getPublicationIdentifier(), getTitle(), getModifiedDate(), getPublicationInstance(), getPublicationDate(),
                getPublicationYear(), getContributors(), getStatus());
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PublicationSummary)) {
            return false;
        }
        PublicationSummary that = (PublicationSummary) o;
        return Objects.equals(getPublicationId(), that.getPublicationId())
                && Objects.equals(getPublicationIdentifier(), that.getPublicationIdentifier())
                && Objects.equals(getTitle(), that.getTitle())
                && Objects.equals(getModifiedDate(), that.getModifiedDate())
                && Objects.equals(getPublicationInstance(), that.getPublicationInstance())
                && Objects.equals(getPublicationDate(), that.getPublicationDate())
                && Objects.equals(getPublicationYear(), that.getPublicationYear())
                && Objects.equals(getContributors(), that.getContributors())
                && getStatus() == that.getStatus();
    }

    private static URI extractPublicationId(ConnectedToResource connectedToResource) {
        return toPublicationId(connectedToResource.getResourceIdentifier());
    }

    private static URI toPublicationId(SortableIdentifier identifier) {
        return new UriWrapper(PublicationServiceConfig.ID_NAMESPACE)
                .addChild(identifier.toString()).getUri();
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }
}
