package no.unit.nva.publication.doi.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.List;
import nva.commons.utils.JacocoGenerated;

public class Publication {

    private final URI id;
    private final String institutionOwner;

    private final URI doi;
    private final PublicationType type;
    private final String title;
    private final List<Contributor> contributor;
    private final PublicationDate publicationDate;

    @JsonCreator
    @JacocoGenerated
    public Publication(@JsonProperty("id") URI id,
                       @JsonProperty("institution_owner") String institutionOwner,
                       @JsonProperty("doi") URI doi,
                       @JsonProperty("type") PublicationType type,
                       @JsonProperty("title") String title,
                       @JsonProperty("contributors") List<Contributor> contributors,
                       @JsonProperty("publication_date") PublicationDate publicationDate) {
        this.id = id;
        this.institutionOwner = institutionOwner;
        this.doi = doi;
        this.type = type;
        this.title = title;
        this.contributor = contributors;
        this.publicationDate = publicationDate;
    }

    protected Publication(PublicationBuilder builder) {
        this.id = builder.id;
        this.institutionOwner = builder.institutionOwner;
        this.doi = builder.doi;
        this.type = builder.type;
        this.title = builder.title;
        this.contributor = builder.contributor;
        this.publicationDate = builder.publicationDate;
    }

    public URI getId() {
        return id;
    }

    public String getInstitutionOwner() {
        return institutionOwner;
    }

    public URI getDoi() {
        return doi;
    }

    public PublicationType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public List<Contributor> getContributor() {
        return contributor;
    }

    public PublicationDate getPublicationDate() {
        return publicationDate;
    }

    public static final class PublicationBuilder {

        private URI id;
        private String institutionOwner;
        private URI doi;
        private PublicationType type;
        private String title;
        private List<Contributor> contributor;
        private PublicationDate publicationDate;

        private PublicationBuilder() {
        }

        public static PublicationBuilder aPublication() {
            return new PublicationBuilder();
        }

        public PublicationBuilder withId(URI id) {
            this.id = id;
            return this;
        }

        public PublicationBuilder withInstitutionOwner(String institutionOwner) {
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
