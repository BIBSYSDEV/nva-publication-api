package no.sikt.nva.brage.migration.record;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

public class Publication {

    private String journal;
    private String issn;
    private String isbn;
    private PublicationContext publicationContext;
    private String partOfSeries;

    public Publication() {
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
        return Objects.hash(journal, issn, isbn, publicationContext, partOfSeries);
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
               && Objects.equals(publicationContext, that.publicationContext)
               && Objects.equals(partOfSeries, that.partOfSeries);
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
    @JsonProperty("publicationContext")
    public PublicationContext getPublicationContext() {
        return publicationContext;
    }

    public void setPublicationContext(PublicationContext publicationContext) {
        this.publicationContext = publicationContext;
    }
}
