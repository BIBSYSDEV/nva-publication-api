package no.unit.nva.cristin.mapper.nva;

import java.util.List;
import no.unit.nva.cristin.mapper.CristinLocale;

public record NviReport(String publicationIdentifier,
                        String cristinIdentifier,
                        List<CristinLocale> nviReport) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String publicationIdentifier;
        private String cristinIdentifier;
        private List<CristinLocale> nviReport;

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

        public NviReport build() {
            return new NviReport(publicationIdentifier, cristinIdentifier, nviReport);
        }
    }
}
