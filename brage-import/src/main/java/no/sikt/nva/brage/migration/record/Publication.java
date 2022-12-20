package no.sikt.nva.brage.migration.record;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

public class Publication {

    private String journal;
    private List<String> issnList;
    private List<String> isbnList;
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
        return Objects.hash(journal, issnList, isbnList, publicationContext, partOfSeries);
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
               && Objects.equals(issnList, that.issnList)
               && Objects.equals(isbnList, that.isbnList)
               && Objects.equals(publicationContext, that.publicationContext)
               && Objects.equals(partOfSeries, that.partOfSeries);
    }

    @JacocoGenerated
    @JsonProperty("journal")
    public String getJournal() {
        return journal;
    }

    @JacocoGenerated
    public void setJournal(String journal) {
        this.journal = journal;
    }

    @JacocoGenerated
    @JsonProperty("issnList")
    public List<String> getIssnList() {
        return issnList;
    }

    @JacocoGenerated
    public void setIssnList(List<String> issnList) {
        this.issnList = issnList;
    }

    @JacocoGenerated
    @JsonProperty("isbnList")
    public List<String> getIsbnList() {
        return isbnList;
    }

    @JacocoGenerated
    public void setIsbnList(List<String> isbnList) {
        this.isbnList = isbnList;
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
