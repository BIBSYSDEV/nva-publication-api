package no.unit.nva.model.contexttypes;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * PublicationContext provides a common root object for contexts of type
 * {@link BasicContext}.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(name = "Book", value = Book.class),
    @JsonSubTypes.Type(name = "Report", value = Report.class),
    @JsonSubTypes.Type(name = "Degree", value = Degree.class),
    @JsonSubTypes.Type(name = "Anthology", value = Anthology.class),
    @JsonSubTypes.Type(name = "Chapter", value = Anthology.class),
    @JsonSubTypes.Type(name = "Journal", value = Journal.class),
    @JsonSubTypes.Type(name = "UnconfirmedJournal", value = UnconfirmedJournal.class),
    @JsonSubTypes.Type(name = "Event", value = Event.class),
    @JsonSubTypes.Type(name = "Artistic", value = Artistic.class),
    @JsonSubTypes.Type(name = "MediaContribution", value = MediaContribution.class),
    @JsonSubTypes.Type(name = "MediaContributionPeriodical", value = MediaContributionPeriodical.class),
    @JsonSubTypes.Type(name = "UnconfirmedMediaContributionPeriodical",
        value = UnconfirmedMediaContributionPeriodical.class),
    @JsonSubTypes.Type(name = "ResearchData", value = ResearchData.class),
    @JsonSubTypes.Type(name = "GeographicalContent", value = GeographicalContent.class),
    @JsonSubTypes.Type(name = "ExhibitionContent", value = ExhibitionContent.class)
})
public interface PublicationContext {

}
