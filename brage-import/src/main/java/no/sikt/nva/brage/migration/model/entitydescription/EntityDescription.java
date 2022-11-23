package no.sikt.nva.brage.migration.model.entitydescription;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public class EntityDescription {

    private PublicationDate publicationDate;
    private List<String> descriptions;
    private List<String> abstracts;
    private String mainTitle;
    private List<String> alternativeTitles;
    private List<Contributor> contributors;
    private List<String> tags;
    private PublicationInstance publicationInstance;

    private Language language;

    @JacocoGenerated
    @JsonProperty("publicationDate")
    public PublicationDate getPublicationDate() {
        return publicationDate;
    }

    @JacocoGenerated
    public void setPublicationDate(PublicationDate publicationDate) {
        this.publicationDate = publicationDate;
    }

    @JacocoGenerated
    @JsonProperty("publicationInstance")
    public PublicationInstance getPublicationInstance() {
        return publicationInstance;
    }

    @JacocoGenerated
    public void setPublicationInstance(PublicationInstance publicationInstance) {
        this.publicationInstance = publicationInstance;
    }

    @JacocoGenerated
    @JsonProperty("tags")
    public List<String> getTags() {
        return this.tags;
    }

    @JacocoGenerated
    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    @JacocoGenerated
    @JsonProperty("contributors")
    public List<Contributor> getContributors() {
        return contributors;
    }

    @JacocoGenerated
    public void setContributors(List<Contributor> contributors) {
        this.contributors = contributors;
    }

    @JacocoGenerated
    @JsonProperty("mainTitle")
    public String getMainTitle() {
        return this.mainTitle;
    }

    @JacocoGenerated
    public void setMainTitle(String mainTitle) {
        this.mainTitle = mainTitle;
    }

    @JacocoGenerated
    @JsonProperty
    public List<String> getDescriptions() {
        return descriptions;
    }

    @JacocoGenerated
    public void setDescriptions(List<String> descriptions) {
        this.descriptions = descriptions;
    }

    @JacocoGenerated
    @JsonProperty
    public List<String> getAbstracts() {
        return abstracts;
    }

    @JacocoGenerated
    public void setAbstracts(List<String> abstracts) {
        this.abstracts = abstracts;
    }

    @JacocoGenerated
    @JsonProperty("alternativeTitles")
    public List<String> getAlternativeTitles() {
        return alternativeTitles;
    }

    @JacocoGenerated
    public void setAlternativeTitles(List<String> alternativeTitles) {
        this.alternativeTitles = alternativeTitles;
    }

    @JsonProperty("language")
    public Language getLanguage() {
        return language;
    }

    public void setLanguage(Language language) {
        this.language = language;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(publicationDate, descriptions, abstracts, mainTitle, alternativeTitles, contributors, tags,
                            publicationInstance, language);
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
        EntityDescription that = (EntityDescription) o;
        return Objects.equals(publicationDate, that.publicationDate)
               && Objects.equals(descriptions, that.descriptions)
               && Objects.equals(abstracts, that.abstracts)
               && Objects.equals(language, that.language)
               && Objects.equals(mainTitle, that.mainTitle)
               && Objects.equals(alternativeTitles, that.alternativeTitles)
               && Objects.equals(contributors, that.contributors)
               && Objects.equals(tags, that.tags)
               && Objects.equals(publicationInstance, that.publicationInstance);
    }

}
