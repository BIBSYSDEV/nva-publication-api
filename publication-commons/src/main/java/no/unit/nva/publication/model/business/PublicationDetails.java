package no.unit.nva.publication.model.business;

import static no.unit.nva.publication.model.business.TicketEntry.Constants.IDENTIFIER_FIELD;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import java.util.Optional;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.model.PublicationSummary;
import nva.commons.core.JacocoGenerated;

public class PublicationDetails {
    
    public static final String TITLE_FIELD = "title";
    @JsonProperty(IDENTIFIER_FIELD)
    private final SortableIdentifier identifier;
    @JsonProperty(TITLE_FIELD)
    private final String title;
    
    @JsonCreator
    public PublicationDetails(@JsonProperty(IDENTIFIER_FIELD) SortableIdentifier identifier,
                              @JsonProperty(TITLE_FIELD) String title) {
        //TODO: validate that identifier is not null
        this.identifier = identifier;
        this.title = title;
    }
    
    public static PublicationDetails create(Publication publication) {
        return new PublicationDetails(publication.getIdentifier(), extractMainTitle(publication));
    }
    
    public static PublicationDetails create(Resource resource) {
        return new PublicationDetails(resource.getIdentifier(), extractMainTitle(resource));
    }
    
    public static PublicationDetails create(SortableIdentifier publicationIdentifier) {
        return new PublicationDetails(publicationIdentifier, null);
    }
    
    public static PublicationDetails create(PublicationSummary publicationSummary) {
        return new PublicationDetails(publicationSummary.extractPublicationIdentifier(), publicationSummary.getTitle());
    }
    
    public SortableIdentifier getIdentifier() {
        return identifier;
    }
    
    public String getTitle() {
        return title;
    }
    
    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getIdentifier(), getTitle());
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
        return Objects.equals(getIdentifier(), that.getIdentifier()) && Objects.equals(getTitle(),
            that.getTitle());
    }
    
    public PublicationDetails updateTitle(String newTitle) {
        return new PublicationDetails(this.identifier, newTitle);
    }
    
    private static String extractMainTitle(Resource publication) {
        return Optional.of(publication)
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
}
