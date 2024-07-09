package no.unit.nva.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.net.URI;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class EntityDescription implements WithCopy<EntityDescription.Builder> {

    private String mainTitle;
    private Map<String, String> alternativeTitles;
    private URI language;

    @JsonAlias("date")
    private PublicationDate publicationDate;
    private List<Contributor> contributors;
    @JsonSetter("abstract")
    private String mainLanguageAbstract;

    private Map<String, String> alternativeAbstracts;
    private String npiSubjectHeading;
    private List<String> tags;
    private String description;
    private Reference reference;
    private URI metadataSource;

    public EntityDescription() {
        contributors = Collections.emptyList();
        tags = Collections.emptyList();
        alternativeTitles = Collections.emptyMap();
        alternativeAbstracts = Collections.emptyMap();
    }

    private EntityDescription(Builder builder) {
        setMainTitle(builder.mainTitle);
        setAlternativeTitles(builder.alternativeTitles);
        setLanguage(builder.language);
        setPublicationDate(builder.publicationDate);
        setContributors(builder.contributors);
        setAbstract(builder.mainLanguageAbstract);
        setNpiSubjectHeading(builder.npiSubjectHeading);
        setTags(builder.tags);
        setDescription(builder.description);
        setReference(builder.reference);
        setMetadataSource(builder.metadataSource);
        setAlternativeAbstracts(builder.alternativeAbstracts);
    }


    public String getMainTitle() {
        return mainTitle;
    }

    public void setMainTitle(String mainTitle) {
        this.mainTitle = mainTitle;
    }

    public Map<String, String> getAlternativeTitles() {
        return Objects.nonNull(alternativeTitles) ? alternativeTitles : Collections.emptyMap();
    }

    public Map<String, String> getAlternativeAbstracts() {
        return Objects.nonNull(alternativeAbstracts) ? alternativeAbstracts : Collections.emptyMap();
    }

    public void setAlternativeAbstracts(Map<String, String> alternativeAbstracts) {
        this.alternativeAbstracts = alternativeAbstracts;
    }

    public void setAlternativeTitles(Map<String, String> alternativeTitles) {
        this.alternativeTitles = alternativeTitles;
    }

    public URI getLanguage() {
        return language;
    }

    public void setLanguage(URI language) {
        this.language = language;
    }

    public PublicationDate getPublicationDate() {
        return publicationDate;
    }

    public void setPublicationDate(PublicationDate publicationDate) {
        this.publicationDate = publicationDate;
    }

    public List<Contributor> getContributors() {
        return Objects.nonNull(contributors) ? contributors : Collections.emptyList();
    }

    public void setContributors(List<Contributor> contributors) {
        this.contributors = Objects.nonNull(contributors)
                ? extractContributors(contributors)
                : List.of();
    }

    private List<Contributor> extractContributors(List<Contributor> contributors) {
        var contributorList = contributors.stream()
              .sorted(Comparator.comparing(Contributor::getSequence, Comparator.nullsLast(Comparator.naturalOrder())))
                                  .toList();

        return updatedContributorSequence(contributorList);
    }

    private List<Contributor> updatedContributorSequence(List<Contributor> contributorList) {
        return IntStream.range(0, contributorList.size())
                .mapToObj(sequenceCounter ->
                        updateContributorWithSequence(contributorList.get(sequenceCounter), sequenceCounter))
                .toList();
    }

    private static Contributor updateContributorWithSequence(Contributor contributor, int sequenceCounter) {
        return contributor
                .copy()
                .withSequence(sequenceCounter + 1)
                .build();
    }

    public URI getMetadataSource() {
        return metadataSource;
    }

    public void setMetadataSource(URI metadataSource) {
        this.metadataSource = metadataSource;
    }

    public String getAbstract() {
        return mainLanguageAbstract;
    }

    public void setAbstract(String mainLanguageAbstract) {
        this.mainLanguageAbstract = mainLanguageAbstract;
    }

    public String getNpiSubjectHeading() {
        return npiSubjectHeading;
    }

    public void setNpiSubjectHeading(String npiSubjectHeading) {
        this.npiSubjectHeading = npiSubjectHeading;
    }

    public List<String> getTags() {
        return Objects.nonNull(tags) ? tags : Collections.emptyList();
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Reference getReference() {
        return reference;
    }

    public void setReference(Reference reference) {
        this.reference = reference;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getMainTitle(),
                getAlternativeTitles(),
                getLanguage(),
                getPublicationDate(),
                getContributors(),
                getAbstract(),
                getNpiSubjectHeading(),
                getTags(),
                getDescription(),
                getReference(),
                getMetadataSource(),
                getAlternativeAbstracts());
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EntityDescription that)) {
            return false;
        }
        return Objects.equals(getMainTitle(), that.getMainTitle())
                && Objects.equals(getAlternativeTitles(), that.getAlternativeTitles())
                && Objects.equals(getLanguage(), that.getLanguage())
                && Objects.equals(getPublicationDate(), that.getPublicationDate())
                && Objects.equals(getContributors(), that.getContributors())
                && Objects.equals(getAbstract(), that.getAbstract())
                && Objects.equals(getNpiSubjectHeading(), that.getNpiSubjectHeading())
                && Objects.equals(getTags(), that.getTags())
                && Objects.equals(getDescription(), that.getDescription())
                && Objects.equals(getReference(), that.getReference())
                && Objects.equals(getMetadataSource(), that.getMetadataSource())
                && Objects.equals(getAlternativeAbstracts(), that.getAlternativeAbstracts());
    }

    @Override
    public Builder copy() {
        return new Builder()
                .withMainTitle(getMainTitle())
                .withAlternativeTitles(getAlternativeTitles())
                .withLanguage(getLanguage())
                .withPublicationDate(getPublicationDate())
                .withContributors(getContributors())
                .withAbstract(getAbstract())
                .withAlternativeAbstracts(getAlternativeAbstracts())
                .withNpiSubjectHeading(getNpiSubjectHeading())
                .withTags(getTags())
                .withDescription(getDescription())
                .withReference(getReference())
                .withMetadataSource(getMetadataSource());
    }

    public static final class Builder {

        private String mainTitle;
        private Map<String, String> alternativeTitles;
        private URI language;
        private PublicationDate publicationDate;
        private List<Contributor> contributors;
        private String mainLanguageAbstract;
        private Map<String, String> alternativeAbstracts;
        private String npiSubjectHeading;
        private List<String> tags;
        private String description;
        private Reference reference;
        private URI metadataSource;

        public Builder() {
        }

        public Builder withMainTitle(String mainTitle) {
            this.mainTitle = mainTitle;
            return this;
        }

        public Builder withAlternativeTitles(Map<String, String> alternativeTitles) {
            this.alternativeTitles = alternativeTitles;
            return this;
        }

        public Builder withLanguage(URI language) {
            this.language = language;
            return this;
        }

        public Builder withPublicationDate(PublicationDate date) {
            this.publicationDate = date;
            return this;
        }

        public Builder withContributors(List<Contributor> contributors) {
            this.contributors = contributors;
            return this;
        }

        public Builder withAbstract(String mainLanguageAbstract) {
            this.mainLanguageAbstract = mainLanguageAbstract;
            return this;
        }

        public Builder withAlternativeAbstracts(Map<String, String> alternativeAbstracts) {
            this.alternativeAbstracts = alternativeAbstracts;
            return this;
        }

        public Builder withNpiSubjectHeading(String npiSubjectHeading) {
            this.npiSubjectHeading = npiSubjectHeading;
            return this;
        }

        public Builder withTags(List<String> tags) {
            this.tags = tags;
            return this;
        }

        public Builder withDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder withReference(Reference reference) {
            this.reference = reference;
            return this;
        }

        public Builder withMetadataSource(URI metadataSource) {
            this.metadataSource = metadataSource;
            return this;
        }

        public EntityDescription build() {
            return new EntityDescription(this);
        }
    }
}
