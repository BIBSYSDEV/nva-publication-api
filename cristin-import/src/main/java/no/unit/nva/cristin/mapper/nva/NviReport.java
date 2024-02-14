package no.unit.nva.cristin.mapper.nva;

import static java.util.Objects.nonNull;
import java.time.Instant;
import java.util.List;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.cristin.lambda.PublicationRepresentations;
import no.unit.nva.cristin.mapper.CristinLocale;
import no.unit.nva.cristin.mapper.ScientificResource;
import no.unit.nva.model.PublicationDate;

public record NviReport(String publicationIdentifier,
                        String cristinIdentifier,
                        List<ScientificResource> nviReport,
                        String yearReported,
                        PublicationDate publicationDate) implements JsonSerializable {

    public static Builder builder() {
        return new Builder();
    }

    public static NviReport fromPublicationRepresentation(PublicationRepresentations publicationRepresentations) {
        return NviReport.builder()
                   .withPublicationIdentifier(publicationRepresentations.getNvaPublicationIdentifier())
                   .withCristinIdentifier(publicationRepresentations.getCristinObject().getSourceRecordIdentifier())
                   .withNviReport(publicationRepresentations.getCristinObject().getScientificResources())
                   .withYearReported(publicationRepresentations.getCristinObject().getScientificResources().getFirst().getReportedYear())
                   .withPublicationDate(publicationRepresentations.getPublication().getEntityDescription().getPublicationDate())
                   .build();
    }

    public static final class Builder {

        private String publicationIdentifier;
        private String cristinIdentifier;
        private List<ScientificResource> nviReport;
        private String yearReported;
        private PublicationDate publicationDate;

        private Builder() {
        }

        public Builder withPublicationIdentifier(String publicationIdentifier) {
            this.publicationIdentifier = publicationIdentifier;
            return this;
        }

        public Builder withCristinIdentifier(String cristinIdentifier) {
            this.cristinIdentifier = cristinIdentifier;
            return this;
        }

        public Builder withNviReport(List<ScientificResource> nviReport) {
            this.nviReport = nviReport;
            return this;
        }

        public Builder withYearReported(String yearReported) {
            this.yearReported = yearReported;
            return this;
        }

        public Builder withPublicationDate(PublicationDate publicationDate) {
            this.publicationDate = publicationDate;
            return this;
        }

        public NviReport build() {
            return new NviReport(publicationIdentifier, cristinIdentifier, nviReport, yearReported, publicationDate);
        }

    }
}
