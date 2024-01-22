package no.unit.nva.cristin.mapper.nva;

import static java.util.Objects.nonNull;
import java.time.Instant;
import java.util.List;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.cristin.lambda.PublicationRepresentations;
import no.unit.nva.cristin.mapper.CristinLocale;

public record NviReport(String publicationIdentifier,
                        String cristinIdentifier,
                        List<CristinLocale> nviReport,
                        Integer yearReported,
                        Instant publicationDate) implements JsonSerializable {

    public static Builder builder() {
        return new Builder();
    }

    public boolean hasBeenNviReported() {
        return hasBeenReported() && isReportable();
    }

    private boolean hasBeenReported() {
        return nonNull(nviReport())
               && nviReport().stream()
                      .anyMatch(NviReport::hasControlStatusJ);
    }

    private boolean isReportable() {
        return nonNull(yearReported()) && yearReported() >= 2011;
    }

    private static boolean hasControlStatusJ(CristinLocale cristinLocale) {
        return nonNull(cristinLocale.getControlStatus())
               && "J".equals(cristinLocale.getControlStatus());
    }

    public static NviReport fromPublicationRepresentation(PublicationRepresentations publicationRepresentations) {
        return NviReport.builder()
                   .withPublicationIdentifier(publicationRepresentations.getNvaPublicationIdentifier())
                   .withCristinIdentifier(publicationRepresentations.getCristinObject().getSourceRecordIdentifier())
                   .withNviReport(publicationRepresentations.getCristinObject().getCristinLocales())
                   .withYearReported(getYearReported(publicationRepresentations))
                   .withPublicationDate(publicationRepresentations.getPublication().getCreatedDate())
                   .build();
    }

    private static Integer getYearReported(PublicationRepresentations publicationRepresentations) {
        return publicationRepresentations.getCristinObject().getYearReported();
    }

    public static final class Builder {

        private String publicationIdentifier;
        private String cristinIdentifier;
        private List<CristinLocale> nviReport;
        private Integer yearReported;
        private Instant publicationDate;

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

        public Builder withNviReport(List<CristinLocale> nviReport) {
            this.nviReport = nviReport;
            return this;
        }

        public Builder withYearReported(Integer yearReported) {
            this.yearReported = yearReported;
            return this;
        }

        public Builder withPublicationDate(Instant publicationDate) {
            this.publicationDate = publicationDate;
            return this;
        }

        public NviReport build() {
            return new NviReport(publicationIdentifier, cristinIdentifier, nviReport, yearReported, publicationDate);
        }

    }
}
