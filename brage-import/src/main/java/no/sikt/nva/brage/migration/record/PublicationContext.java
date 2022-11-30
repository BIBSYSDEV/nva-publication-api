package no.sikt.nva.brage.migration.record;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public class PublicationContext {

    private String bragePublisher;
    private Publisher publisher;
    private Journal journal;
    private Series series;

    public PublicationContext() {
    }

    @JsonProperty("journal")
    public Journal getJournal() {
        return journal;
    }

    public void setJournal(Journal journal) {
        this.journal = journal;
    }

    @JsonProperty("series")
    public Series getSeries() {
        return series;
    }

    public void setSeries(Series series) {
        this.series = series;
    }

    @JsonProperty("bragePublisher")
    public String getBragePublisher() {
        return bragePublisher;
    }

    public void setBragePublisher(String bragePublisher) {
        this.bragePublisher = bragePublisher;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(bragePublisher, publisher, journal, series);
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PublicationContext that = (PublicationContext) o;
        return Objects.equals(bragePublisher, that.bragePublisher)
               && Objects.equals(publisher, that.publisher)
               && Objects.equals(journal, that.journal)
               && Objects.equals(series, that.series);
    }

    @JsonProperty("publisher")
    public Publisher getPublisher() {
        return publisher;
    }

    public void setPublisher(Publisher publisher) {
        this.publisher = publisher;
    }
}
