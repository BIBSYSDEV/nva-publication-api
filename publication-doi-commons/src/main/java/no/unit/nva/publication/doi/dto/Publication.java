package no.unit.nva.publication.doi.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import nva.commons.utils.JacocoGenerated;

public class Publication {

    private final URI id;
    private final URI institutionOwner;
    private final URI doi;
    private final PublicationType type;
    private final String mainTitle;
    private final List<Contributor> contributor;
    private final PublicationDate publicationDate;

    /**
     * doi.Publication DTO to be used to as payload format.
     *
     * @param id               Ex: https://api.dev.nva.aws.unit.no/publication/identifier
     * @param institutionOwner Ex: https://api.dev.nva.aws.unit.no/customer/identifier
     * @param doi              Ex: http://doi.org/11541.2/124530
     * @param type             Ex: JOURNAL_LEADER
     * @param mainTitle        main title
     * @param contributors     list of contributors
     * @param publicationDate  date of publication
     */
    @JsonCreator
    @JacocoGenerated
    public Publication(@JsonProperty("id") URI id,
                       @JsonProperty("institution_owner") URI institutionOwner,
                       @JsonProperty("doi") URI doi,
                       @JsonProperty("type") PublicationType type,
                       @JsonProperty("mainTitle") String mainTitle,
                       @JsonProperty("contributors") List<Contributor> contributors,
                       @JsonProperty("publication_date") PublicationDate publicationDate) {
        this.id = id;
        this.institutionOwner = institutionOwner;
        this.doi = doi;
        this.type = type;
        this.mainTitle = mainTitle;
        this.contributor = contributors;
        this.publicationDate = publicationDate;
    }

    protected Publication(Builder builder) {
        this(builder.id, builder.institutionOwner, builder.doi, builder.type, builder.mainTitle, builder.contributor,
            builder.publicationDate);
    }

    public URI getId() {
        return id;
    }

    public URI getInstitutionOwner() {
        return institutionOwner;
    }

    public URI getDoi() {
        return doi;
    }

    public PublicationType getType() {
        return type;
    }

    public String getMainTitle() {
        return mainTitle;
    }

    public List<Contributor> getContributor() {
        return contributor;
    }

    public PublicationDate getPublicationDate() {
        return publicationDate;
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
        return Objects.equals(id, that.id)
            && Objects.equals(institutionOwner, that.institutionOwner)
            && Objects.equals(doi, that.doi)
            && type == that.type
            && Objects.equals(mainTitle, that.mainTitle)
            && Objects.equals(contributor, that.contributor)
            && Objects.equals(publicationDate, that.publicationDate);
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(id, institutionOwner, doi, type, mainTitle, contributor, publicationDate);
    }

    public static final class Builder {

        private URI id;
        private URI institutionOwner;
        private URI doi;
        private PublicationType type;
        private String mainTitle;
        private List<Contributor> contributor;
        private PublicationDate publicationDate;

        private Builder() {
        }

        public static Builder newBuilder() {
            return new Builder();
        }

        public Builder withId(URI id) {
            this.id = id;
            return this;
        }

        public Builder withInstitutionOwner(URI institutionOwner) {
            this.institutionOwner = institutionOwner;
            return this;
        }

        public Builder withDoi(URI doi) {
            this.doi = doi;
            return this;
        }

        public Builder withType(PublicationType type) {
            this.type = type;
            return this;
        }

        public Builder withMainTitle(String mainTitle) {
            this.mainTitle = mainTitle;
            return this;
        }

        public Builder withContributor(List<Contributor> contributor) {
            this.contributor = contributor;
            return this;
        }

        public Builder withPublicationDate(PublicationDate publicationDate) {
            this.publicationDate = publicationDate;
            return this;
        }

        public Publication build() {
            return new Publication(this);
        }
    }
}
