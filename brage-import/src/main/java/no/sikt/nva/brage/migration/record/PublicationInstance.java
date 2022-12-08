package no.sikt.nva.brage.migration.record;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

public class PublicationInstance {

    private String volume;
    private String issue;
    private Pages pages;
    private String articleNumber;

    public PublicationInstance() {
    }

    @JsonProperty("articleNumber")
    public String getArticleNumber() {
        return articleNumber;
    }

    @JacocoGenerated
    public void setArticleNumber(String articleNumber) {
        this.articleNumber = articleNumber;
    }

    @JsonProperty("volume")
    public String getVolume() {
        return volume;
    }

    @JacocoGenerated
    public void setVolume(String volume) {
        this.volume = volume;
    }

    @JsonProperty("issue")
    public String getIssue() {
        return issue;
    }

    @JacocoGenerated
    public void setIssue(String issue) {
        this.issue = issue;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(volume, issue, pages, articleNumber);
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
        PublicationInstance that = (PublicationInstance) o;
        return Objects.equals(volume, that.volume)
               && Objects.equals(issue, that.issue)
               && Objects.equals(pages, that.pages)
               && Objects.equals(articleNumber, that.articleNumber);
    }

    @JsonProperty("pages")
    public Pages getPages() {
        return pages;
    }

    public void setPageNumber(Pages pages) {
        this.pages = pages;
    }
}
