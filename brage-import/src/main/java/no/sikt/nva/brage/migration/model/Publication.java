package no.sikt.nva.brage.migration.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public class Publication {

    private String journal;
    private String issn;
    private String isbn;
    private String publisher;
    private String partOfSeries;
    private String id;

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty("partOfSeries")
    public String getPartOfSeries() {
        return partOfSeries;
    }

    public void setPartOfSeries(String partOfSeries) {
        this.partOfSeries = partOfSeries;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(journal, issn, isbn, publisher, partOfSeries, id);
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
        Publication that = (Publication) o;
        return Objects.equals(journal, that.journal)
               && Objects.equals(issn, that.issn)
               && Objects.equals(isbn, that.isbn)
               && Objects.equals(publisher, that.publisher)
               && Objects.equals(partOfSeries, that.partOfSeries)
               && Objects.equals(id, that.id);
    }

    @JacocoGenerated
    @JsonProperty("journal")
    public String getJournal() {
        return journal;
    }

    public void setJournal(String journal) {
        this.journal = journal;
    }

    @JacocoGenerated
    @JsonProperty("issn")
    public String getIssn() {
        return issn;
    }

    public void setIssn(String issn) {
        this.issn = issn;
    }

    @JacocoGenerated
    @JsonProperty("isbn")
    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    @JacocoGenerated
    @JsonProperty("publisher")
    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

}
