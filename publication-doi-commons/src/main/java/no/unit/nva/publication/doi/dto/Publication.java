package no.unit.nva.publication.doi.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import nva.commons.utils.JacocoGenerated;

public class Publication extends Validatable {

    private final URI id;
    private final URI institutionOwner;
    private final Instant modifiedDate;
    private final PublicationType type;
    private final String mainTitle;
    private final PublicationStatus status;
    private final PublicationDate publicationDate;
    private final List<Contributor> contributor;
    private final URI doi;
    private final DoiRequest doiRequest;

    /**
     * doi.Publication DTO to be used to as payload format.
     *
     * @param id               Ex: https://api.dev.nva.aws.unit.no/publication/identifier
     * @param institutionOwner Ex: https://api.dev.nva.aws.unit.no/customer/identifier
     * @param doi              Ex: http://doi.org/11541.2/124530
     * @param doiRequest       doi request for publication
     * @param modifiedDate     modified date of publication
     * @param type             Ex: JOURNAL_LEADER
     * @param mainTitle        main title
     * @param status           status for publication
     * @param contributors     list of contributors
     * @param publicationDate  date of publication
     */
    @JsonCreator
    @JacocoGenerated
    @SuppressWarnings("PMD.ExcessiveParameterList")
    public Publication(@JsonProperty("id") URI id,
                       @JsonProperty("institution_owner") URI institutionOwner,
                       @JsonProperty("doi") URI doi,
                       @JsonProperty("doiRequest") DoiRequest doiRequest,
                       @JsonProperty("modifiedDate") Instant modifiedDate,
                       @JsonProperty("type") PublicationType type,
                       @JsonProperty("mainTitle") String mainTitle,
                       @JsonProperty("status") PublicationStatus status,
                       @JsonProperty("contributors") List<Contributor> contributors,
                       @JsonProperty("publication_date") PublicationDate publicationDate) {
        super();
        this.id = id;
        this.institutionOwner = institutionOwner;
        this.doi = doi;
        this.doiRequest = doiRequest;
        this.modifiedDate = modifiedDate;
        this.type = type;
        this.mainTitle = mainTitle;
        this.status = status;
        this.contributor = contributors;
        this.publicationDate = publicationDate;
    }

    /**
     * Validates.
     */
    public void validate() {
        requireFieldIsNotNull(id, "Publication.id");
        requireFieldIsNotNull(institutionOwner, "Publication.institutionOwner");
        requireFieldIsNotNull(modifiedDate, "Publication.modifiedDate");
        requireFieldIsNotNull(type, "Publication.type");
        requireFieldIsNotNull(mainTitle, "Publication.mainTitle");
        requireFieldIsNotNull(status, "Publication.status");
        requireFieldIsNotNull(publicationDate, "Publication.publicationDate");
    }

    protected Publication(Builder builder) {
        this(builder.id, builder.institutionOwner, builder.doi, builder.doiRequest, builder.modifiedDate, builder.type,
            builder.mainTitle, builder.status, builder.contributor, builder.publicationDate);
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

    public DoiRequest getDoiRequest() {
        return doiRequest;
    }

    public Instant getModifiedDate() {
        return modifiedDate;
    }

    public PublicationStatus getStatus() {
        return status;
    }

    public List<Contributor> getContributor() {
        return contributor;
    }

    public PublicationDate getPublicationDate() {
        return publicationDate;
    }

    /**
     * Flag used during EventBridge pattern matching.
     *
     * @return true if modifiedDate is same as doiRequest.modifiedDate
     */
    @JsonProperty("sameModifiedDateForDoiRequest")
    public boolean isSameModifiedDateForDoiRequest() {
        if (doiRequest == null) {
            return false;
        }
        return modifiedDate.equals(doiRequest.getModifiedDate());
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
            && Objects.equals(doiRequest, that.doiRequest)
            && Objects.equals(modifiedDate, that.modifiedDate)
            && type == that.type
            && Objects.equals(mainTitle, that.mainTitle)
            && Objects.equals(status, that.status)
            && Objects.equals(contributor, that.contributor)
            && Objects.equals(publicationDate, that.publicationDate);
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(id, institutionOwner, doi, doiRequest, modifiedDate, type, mainTitle, status, contributor,
            publicationDate);
    }



    public static final class Builder {

        private URI id;
        private URI institutionOwner;
        private URI doi;
        private DoiRequest doiRequest;
        private Instant modifiedDate;
        private PublicationType type;
        private String mainTitle;
        private PublicationStatus status;
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

        public Builder withDoiRequest(DoiRequest doiRequest) {
            this.doiRequest = doiRequest;
            return this;
        }

        public Builder withModifiedDate(Instant modifiedDate) {
            this.modifiedDate = modifiedDate;
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

        public Builder withStatus(PublicationStatus status) {
            this.status = status;
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
            Publication publication = new Publication(this);
            publication.validate();
            return publication;
        }
    }
}
