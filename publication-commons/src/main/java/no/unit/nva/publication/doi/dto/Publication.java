package no.unit.nva.publication.doi.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.List;
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
     * @param id               Ex: https://api.dev.nva.aws.unit.no/publication/<identifier>
     * @param institutionOwner Ex: https://api.dev.nva.aws.unit.no/customer/<identifier>
     * @param doi              Ex: http://doi.org/11541.2/124530
     * @param type             Ex: JOURNAL_LEADER
     * @param mainTitle        Tittel
     * @param contributors     list of contributors
     * @param publicationDate
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

    protected Publication(PublicationBuilder builder) {
        this.id = builder.id;
        this.institutionOwner = builder.institutionOwner;
        this.doi = builder.doi;
        this.type = builder.type;
        this.mainTitle = builder.title;
        this.contributor = builder.contributor;
        this.publicationDate = builder.publicationDate;
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

    public static final class PublicationBuilder {

        private URI id;
        private URI institutionOwner;
        private URI doi;
        private PublicationType type;
        private String title;
        private List<Contributor> contributor;
        private PublicationDate publicationDate;

        private PublicationBuilder() {
        }

        public static PublicationBuilder newBuilder() {
            return new PublicationBuilder();
        }

        public PublicationBuilder withId(URI id) {
            this.id = id;
            return this;
        }

        public PublicationBuilder withInstitutionOwner(URI institutionOwner) {
            this.institutionOwner = institutionOwner;
            return this;
        }

        public PublicationBuilder withDoi(URI doi) {
            this.doi = doi;
            return this;
        }

        public PublicationBuilder withType(PublicationType type) {
            this.type = type;
            return this;
        }

        public PublicationBuilder withTitle(String title) {
            this.title = title;
            return this;
        }

        public PublicationBuilder withContributor(List<Contributor> contributor) {
            this.contributor = contributor;
            return this;
        }

        public PublicationBuilder withPublicationDate(PublicationDate publicationDate) {
            this.publicationDate = publicationDate;
            return this;
        }

        public Publication build() {
            return new Publication(this);
        }
    }
}
