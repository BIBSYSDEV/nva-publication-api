package no.unit.nva.expansion.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import no.unit.nva.expansion.ExpansionConstants;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.pages.Pages;
import no.unit.nva.publication.storage.model.ConnectedToResource;
import no.unit.nva.publication.storage.model.DoiRequest;
import no.unit.nva.publication.storage.model.Message;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;

@JsonTypeName(PublicationSummary.TYPE)
public class PublicationSummary {

    public static final String TYPE = "PublicationSummary";
    @JsonProperty
    private URI id;
    @JsonProperty
    private String title;
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
        publicationSummary.setId(extractPublicationId(doiRequest));
        publicationSummary.setModifiedDate(doiRequest.getResourceModifiedDate());
        publicationSummary.setPublicationDate(doiRequest.getResourcePublicationDate());
        publicationSummary.setPublicationYear(doiRequest.getResourcePublicationYear());
        publicationSummary.setTitle(doiRequest.getResourceTitle());
        publicationSummary.setPublicationInstance(doiRequest.getResourcePublicationInstance());
        publicationSummary.setStatus(doiRequest.getResourceStatus());
        return publicationSummary;
    }

    public static PublicationSummary create(Message message) {
        var publicationSummary = new PublicationSummary();
        publicationSummary.setId(extractPublicationId(message));
        publicationSummary.setTitle(message.getResourceTitle());
        return publicationSummary;
    }

    public PublicationStatus getStatus() {
        return status;
    }

    public void setStatus(PublicationStatus status) {
        this.status = status;
    }

    public URI getId() {
        return id;
    }

    public void setId(URI id) {
        this.id = id;
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
        return Objects.hash(getId(), getTitle(), getModifiedDate(), getPublicationInstance(), getPublicationDate(),
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
        return Objects.equals(getId(), that.getId())
               && Objects.equals(getTitle(), that.getTitle())
               && Objects.equals(getModifiedDate(), that.getModifiedDate())
               && Objects.equals(getPublicationInstance(), that.getPublicationInstance())
               && Objects.equals(getPublicationDate(), that.getPublicationDate())
               && Objects.equals(getPublicationYear(), that.getPublicationYear())
               && Objects.equals(getContributors(), that.getContributors())
               && getStatus() == that.getStatus();
    }

    private static URI extractPublicationId(ConnectedToResource publicationReference) {
        return new UriWrapper(ExpansionConstants.ID_NAMESPACE)
            .addChild(publicationReference.getResourceIdentifier().toString()).getUri();
    }
}
