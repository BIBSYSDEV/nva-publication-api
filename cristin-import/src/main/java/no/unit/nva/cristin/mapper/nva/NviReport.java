package no.unit.nva.cristin.mapper.nva;

import java.util.List;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.cristin.lambda.PublicationRepresentations;
import no.unit.nva.cristin.mapper.CristinLocale;
import no.unit.nva.cristin.mapper.ScientificResource;
import no.unit.nva.model.PublicationDate;

public record NviReport(String publicationIdentifier,
                        String cristinIdentifier,
                        List<ScientificResource> scientificResources,
                        List<CristinLocale> cristinLocales,
                        String yearReported,
                        PublicationDate publicationDate) implements JsonSerializable {

    public static Builder builder() {
        return new Builder();
    }

    public static NviReport fromPublicationRepresentation(PublicationRepresentations publicationRepresentations) {
        return NviReport.builder()
                   .withPublicationIdentifier(publicationRepresentations.getNvaPublicationIdentifier())
                   .withCristinIdentifier(publicationRepresentations.getCristinObject().getSourceRecordIdentifier())
                   .withCristinLocales(publicationRepresentations.getCristinObject().getCristinLocales())
                   .withScientificResource(publicationRepresentations.getCristinObject().getScientificResources())
                   .withYearReported(publicationRepresentations.getCristinObject().getScientificResources().getFirst().getReportedYear())
                   .withPublicationDate(publicationRepresentations.getPublication().getEntityDescription().getPublicationDate())
                   .build();
    }

    public static final class Builder {

        private String publicationIdentifier;
        private String cristinIdentifier;
        private List<ScientificResource> scientificResources;
        private String yearReported;
        private PublicationDate publicationDate;
        private List<CristinLocale> cristinLocales;

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

        public Builder withScientificResource(List<ScientificResource> nviReport) {
            this.scientificResources = nviReport;
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

        public Builder withCristinLocales(List<CristinLocale> cristinLocales) {
            this.cristinLocales = cristinLocales;
            return this;
        }

        public NviReport build() {
            return new NviReport(publicationIdentifier, cristinIdentifier, scientificResources, cristinLocales,
                                 yearReported, publicationDate);
        }

    }
}
