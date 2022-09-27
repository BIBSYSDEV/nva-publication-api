package no.unit.nva.publication.model.business;

import static no.unit.nva.publication.model.business.TicketEntry.Constants.IDENTIFIER_FIELD;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.model.PublicationSummary;
import nva.commons.core.JacocoGenerated;

public class PublicationDetails {
    
    public static final String TITLE_FIELD = "title";
    public static final String OWNER_FIELD = "owner";
    public static final String CREATED_DATE_FIELD = "createdDate";
    public static final String MODIFIED_DATE_FIELD = "modifiedDate";
    public static final String STATUS_FIELD = "status";
    @JsonProperty(IDENTIFIER_FIELD)
    private final SortableIdentifier identifier;
    @JsonProperty(TITLE_FIELD)
    private final String title;
    @JsonProperty(OWNER_FIELD)
    private final User owner;
    @JsonProperty(CREATED_DATE_FIELD)
    private final Instant createdDate;
    @JsonProperty(MODIFIED_DATE_FIELD)
    private final Instant modifiedDate;
    @JsonProperty(STATUS_FIELD)
    private final PublicationStatus status;
    
    @JsonCreator
    public PublicationDetails(@JsonProperty(IDENTIFIER_FIELD) SortableIdentifier identifier,
                              @JsonProperty(TITLE_FIELD) String title,
                              @JsonProperty(OWNER_FIELD) User owner,
                              @JsonProperty(CREATED_DATE_FIELD) Instant createdDate,
                              @JsonProperty(MODIFIED_DATE_FIELD) Instant modifiedDate,
                              @JsonProperty(STATUS_FIELD) PublicationStatus status) {
        this.identifier = identifier;
        this.title = title;
        this.owner = owner;
        this.createdDate = createdDate;
        this.modifiedDate = modifiedDate;
        this.status = status;
    }
    
    public static PublicationDetails create(Publication publication) {
        return new PublicationDetails(publication.getIdentifier(),
            extractMainTitle(publication),
            User.fromPublication(publication),
            publication.getCreatedDate(),
            publication.getModifiedDate(),
            publication.getStatus());
    }
    
    public static PublicationDetails create(Resource resource) {
        return new PublicationDetails(
            resource.getIdentifier(),
            extractMainTitle(resource),
            User.fromResource(resource),
            resource.getCreatedDate(),
            resource.getModifiedDate(),
            resource.getStatus()
        );
    }
    
    public static PublicationDetails create(SortableIdentifier publicationIdentifier) {
        return new PublicationDetails(publicationIdentifier, null, null, null, null, null);
    }
    
    public static PublicationDetails create(PublicationSummary publicationSummary) {
        return new PublicationDetails(
            publicationSummary.extractPublicationIdentifier(),
            publicationSummary.getTitle(),
            publicationSummary.getOwner(),
            publicationSummary.getCreatedDate(),
            publicationSummary.getModifiedDate(),
            publicationSummary.getStatus()
        );
    }
    
    public User getOwner() {
        return owner;
    }
    
    public Instant getCreatedDate() {
        return createdDate;
    }
    
    public Instant getModifiedDate() {
        return modifiedDate;
    }
    
    public PublicationStatus getStatus() {
        return status;
    }
    
    public SortableIdentifier getIdentifier() {
        return identifier;
    }
    
    public String getTitle() {
        return title;
    }
    
    public PublicationDetails update(Resource resource) {
        return PublicationDetails.create(resource);
    }
    
    private static String extractMainTitle(Resource resource) {
        return Optional.of(resource)
                   .map(Resource::getEntityDescription)
                   .map(EntityDescription::getMainTitle)
                   .orElse(null);
    }
    
    private static String extractMainTitle(Publication publication) {
        return Optional.of(publication)
                   .map(Publication::getEntityDescription)
                   .map(EntityDescription::getMainTitle)
                   .orElse(null);
    }
    
    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PublicationDetails)) {
            return false;
        }
        PublicationDetails that = (PublicationDetails) o;
        return Objects.equals(getIdentifier(), that.getIdentifier())
               && Objects.equals(getTitle(), that.getTitle())
               && Objects.equals(getOwner(), that.getOwner())
               && Objects.equals(getCreatedDate(), that.getCreatedDate())
               && Objects.equals(getModifiedDate(), that.getModifiedDate())
               && getStatus() == that.getStatus();
    }
    
    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getIdentifier(), getTitle(), getOwner(), getCreatedDate(), getModifiedDate(), getStatus());
    }
}
