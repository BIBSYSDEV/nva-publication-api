package no.sikt.nva.brage.migration.record.license;

import com.fasterxml.jackson.annotation.JsonValue;
import java.net.URI;
import nva.commons.core.JacocoGenerated;

public enum NvaLicenseUri {
    CC_BY(URI.create("https://creativecommons.org/licenses/by/4.0")),
    CC_BY_NC(URI.create("https://creativecommons.org/licenses/by-nc/4.0")),
    CC_BY_NC_ND(URI.create("https://creativecommons.org/licenses/by-nc-nd/4.0")),
    CC_BY_NC_SA(URI.create("https://creativecommons.org/licenses/by-nc-sa/4.0")),
    CC_BY_ND(URI.create("https://creativecommons.org/licenses/by-nd/4.0")),
    CC_BY_SA(URI.create("https://creativecommons.org/licenses/by-sa/4.0")),
    CC0(URI.create("https://creativecommons.org/publicdomain/zero/1.0")),

    DEFAULT_LICENSE(URI.create("https://rightsstatements.org/page/InC/1.0"));
    private final URI value;


    NvaLicenseUri(URI value) {
        this.value = value;
    }

    @JacocoGenerated
    @JsonValue
    public URI getValue() {
        return value;
    }
}
