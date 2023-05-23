package no.unit.nva.publication.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.Reference;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.pages.Pages;
import no.unit.nva.publication.PublicationServiceConfig;
import no.unit.nva.publication.model.business.User;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;

@JsonTypeName(PublicationSummary.TYPE)
public class PublicationSummary {

    public static final String TYPE = "Publication";
    private static final int MAX_SIZE_CONTRIBUTOR_LIST = 5;

    @JsonProperty("id")
    private URI publicationId;
    @JsonProperty("identifier")
    private SortableIdentifier identifier;
    @JsonProperty("mainTitle")
    private String title;
    @JsonProperty
    private User owner;
    @JsonProperty
    private PublicationStatus status;
    @JsonProperty
    private PublicationInstance<? extends Pages> publicationInstance;
    @JsonProperty
    private Instant publishedDate;
    @JsonProperty
    private List<Contributor> contributors;

    public static PublicationSummary create(Publication publication) {
        var publicationSummary = new PublicationSummary();
        publicationSummary.setIdentifier(publication.getIdentifier());
        publicationSummary.setPublicationId(toPublicationId(publication.getIdentifier()));
        publicationSummary.setPublishedDate(publication.getPublishedDate());
        publicationSummary.setOwner(new User(publication.getResourceOwner().getOwner().getValue()));
        publicationSummary.setStatus(publication.getStatus());
        publicationSummary.setTitle(extractTitle(publication.getEntityDescription()));
        publicationSummary.setPublicationInstance(extractPublicationInstance(publication.getEntityDescription()));
        publicationSummary.setContributors(extractContributors(publication.getEntityDescription()));
        return publicationSummary;
    }

    public static PublicationSummary create(PublicationDetails publicationDetails) {
        var publicationSummary = new PublicationSummary();
        publicationSummary.setIdentifier(publicationDetails.getIdentifier());
        publicationSummary.setPublicationId(toPublicationId(publicationDetails.getIdentifier()));
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

    public Instant getPublishedDate() {
        return publishedDate;
    }

    public void setPublishedDate(Instant publishedDate) {
        this.publishedDate = publishedDate;
    }

    public List<Contributor> getContributors() {
        return contributors;
    }

    public void setContributors(List<Contributor> contributors) {
        this.contributors = contributors;
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

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public PublicationInstance<? extends Pages> getPublicationInstance() {
        return publicationInstance;
    }

    public void setPublicationInstance(PublicationInstance<? extends Pages> publicationInstance) {
        this.publicationInstance = publicationInstance;
    }

    public SortableIdentifier extractPublicationIdentifier() {
        return extractPublicationIdentifier(publicationId);
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getPublicationId(), getIdentifier(), getTitle(), getOwner(), getStatus(),
                            getPublicationInstance(), getPublishedDate(),
                            getContributors());
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
               && getStatus() == that.getStatus()
               && Objects.equals(getPublicationInstance(), that.getPublicationInstance())
               && Objects.equals(getPublishedDate(), that.getPublishedDate())
               && Objects.equals(getContributors(), that.getContributors());
    }

    private static String extractTitle(EntityDescription entityDescription) {
        return Optional.ofNullable(entityDescription)
                   .map(EntityDescription::getMainTitle)
                   .orElse(null);
    }

    private static List<Contributor> extractContributors(EntityDescription entityDescription) {
        return Optional.ofNullable(entityDescription)
                   .map(EntityDescription::getContributors)
                   .orElse(Collections.emptyList())
                   .stream()
                   .sorted(Comparator.comparing(Contributor::getSequence))
                   .limit(MAX_SIZE_CONTRIBUTOR_LIST)
                   .collect(Collectors.toList());
    }

    private static PublicationInstance<? extends Pages> extractPublicationInstance(
        EntityDescription entityDescription) {
        return Optional.ofNullable(entityDescription)
                   .map(EntityDescription::getReference)
                   .map(Reference::getPublicationInstance)
                   .orElse(null);
    }

    private static SortableIdentifier extractPublicationIdentifier(URI publicationId) {
        return new SortableIdentifier(UriWrapper.fromUri(publicationId).getLastPathElement());
    }

    private static URI toPublicationId(SortableIdentifier identifier) {
        return UriWrapper.fromUri(PublicationServiceConfig.PUBLICATION_HOST_URI)
                   .addChild(identifier.toString())
                   .getUri();
    }
}
