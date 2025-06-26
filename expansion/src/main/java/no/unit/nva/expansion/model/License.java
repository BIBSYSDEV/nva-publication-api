package no.unit.nva.expansion.model;

import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.commons.json.JsonUtils;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public record License(URI value, String name, Map<String, String> labels) implements JsonSerializable {

    public static License fromUri(URI uri) {
        return LicenseType.fromUri(uri).toLicense(uri);
    }

    public JsonNode toJsonNode() {
        return attempt(() -> JsonUtils.dtoObjectMapper.readTree(this.toJsonString())).orElseThrow();
    }

    public enum LicenseType {

        CC_NC_ND("CC-NC-ND", "by-nc-nd", Constants.CC_NC_ND_LABELS),
        CC_NC_SA("CC-NC-SA", "by-nc-sa", Constants.CC_NC_SA_LABELS),
        CC_NC("CC-NC", "by-nc", Constants.CC_NC_LABELS),
        CC_ND("CC-ND", "by-nd", Constants.CC_ND_LABELS),
        CC_SA("CC-SA", "by-sa", Constants.CC_SA_LABELS),
        CC_BY("CC-BY", "by", Constants.CC_BY_LABELS),
        CC_ZERO("CC0", "zero", Constants.CC_ZERO_LABELS),
        COPYRIGHT_ACT("COPYRIGHT-ACT", "copyright-act", Constants.COPYRIGHT_ACT_LABELS),
        OTHER("Other", "ignored", Constants.OTHER_LABELS);

        private final String value;
        private final String pathParameter;
        private final Map<String, String> labels;

        LicenseType(String value, String pathParameter, Map<String, String> label) {
            this.value = value;
            this.pathParameter = pathParameter;
            this.labels = label;
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

        public License toLicense(URI uri) {
            return new License(uri, this.value, this.labels);
        }

        private boolean containsPathParameter(String value) {
            return value.toLowerCase(Locale.getDefault()).contains(this.pathParameter.toLowerCase(Locale.getDefault()));
        }

        private static final class Constants {

            public static final String ENGLISH_LABEL = "en";
            public static final String NORWEGIAN_LABEL = "nb";
            public static final Map<String, String> CC_NC_ND_LABELS = Map.of(
                ENGLISH_LABEL, "Creative Commons - Attribution-NonCommercial-NoDerivs",
                NORWEGIAN_LABEL, "Creative Commons - Navngivelse-IkkeKommersiell-IngenBearbeidelser");
            public static final Map<String, String> CC_NC_SA_LABELS = Map.of(
                ENGLISH_LABEL, "Creative Commons - Attribution-NonCommercial-ShareAlike",
                NORWEGIAN_LABEL, "Creative Commons - Navngivelse-IkkeKommersiell-DelPåSammeVilkår");

            public static final Map<String, String> CC_NC_LABELS = Map.of(
                ENGLISH_LABEL, "Creative Commons - Attribution-NonCommercial",
                NORWEGIAN_LABEL, "Creative Commons - Navngivelse-IkkeKommersiell");

            public static final Map<String, String> CC_ND_LABELS = Map.of(
                ENGLISH_LABEL, "Creative Commons - Attribution-NoDerivs",
                NORWEGIAN_LABEL, "Creative Commons - Navngivelse-IngenBearbeidelse");

            public static final Map<String, String> CC_SA_LABELS = Map.of(
                ENGLISH_LABEL, "Creative Commons - Attribution-ShareAlike",
                NORWEGIAN_LABEL, "Creative Commons - Navngivelse-DelPåSammeVilkår");

            public static final Map<String, String> CC_BY_LABELS = Map.of(
                ENGLISH_LABEL, "Creative Commons - Attribution",
                NORWEGIAN_LABEL, "Creative Commons - Navngivelse");

            public static final Map<String, String> CC_ZERO_LABELS = Map.of(
                ENGLISH_LABEL, "Creative Commons - No Rights Reserved",
                NORWEGIAN_LABEL, "Creative Commons - Ingen opphavsrett");

            public static final Map<String, String> COPYRIGHT_ACT_LABELS = Map.of(
                ENGLISH_LABEL, "Generelle bruksvilkår",
                NORWEGIAN_LABEL, "General Terms of Use");

            public static final Map<String, String> OTHER_LABELS = Map.of(
                ENGLISH_LABEL, "Other license",
                NORWEGIAN_LABEL, "Andre lisenser");
        }
    }
}
