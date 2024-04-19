package no.unit.nva.expansion.model;

import java.net.URI;
import java.util.Arrays;
import java.util.Optional;
import no.unit.nva.commons.json.JsonSerializable;

public record License(String type, String label) implements JsonSerializable {

    public static License fromUri(URI uri) {
        return LicenseType.fromUri(uri).toLicense();
    }

    public enum LicenseType{

        CC_NC_ND("CC-NC-ND", "by-nc-nd", Constants.CC_NC_ND_LABEL),
        CC_NC_SA("CC-NC-SA", "by-nc-sa", Constants.CC_NC_SA_LABEL),
        CC_NC("CC-NC", "by-nc", Constants.CC_NC_LABEL),
        CC_ND("CC-ND", "by-nd", Constants.CC_ND_LABEL),
        CC_SA("CC-SA", "by-sa", Constants.CC_SA_LABEL),
        CC_BY("CC-BY", "by", Constants.CC_BY_LABEL),
        OTHER("Other", "ignored", Constants.OTHER_LABEL);

        private final String value;
        private final String pathParameter;
        private final String label;

        LicenseType(String value, String pathParameter, String label) {
            this.value = value;
            this.pathParameter = pathParameter;
            this.label = label;
        }

        public static LicenseType fromUri(URI uri) {
            return Arrays.stream(LicenseType.values())
                       .filter(licenseType -> licenseType.hasPathParam(uri))
                       .findFirst()
                       .orElse(OTHER);
        }

        public boolean hasPathParam(URI uri) {
            return Optional.ofNullable(uri).map(URI::getPath).map(this::containsPathParameter).orElse(false);
        }

        public License toLicense() {
            return new License(this.value, this.label);
        }

        private boolean containsPathParameter(String value) {
            return value.contains(this.pathParameter);
        }

        private static class Constants {

            public static final String CC_NC_ND_LABEL = "Creative Commons - Attribution-NonCommercial-NoDerivs";
            public static final String CC_NC_SA_LABEL = "Creative Commons - Attribution-NonCommercial-ShareAlike";
            public static final String CC_NC_LABEL = "Creative Commons - Attribution-NonCommercial";
            public static final String CC_ND_LABEL = "Creative Commons - Attribution-NoDerivs";
            public static final String CC_SA_LABEL = "Creative Commons - Attribution-ShareAlike";
            public static final String CC_BY_LABEL = "Creative Commons - Attribution";
            public static final String OTHER_LABEL = "Other license";
        }
    }
}
