package no.sikt.nva.scopus;

import java.net.URI;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;

public class ScopusConstants {
    // Identifier field names:
    public static final String ADDITIONAL_IDENTIFIERS_SCOPUS_ID_SOURCE_NAME = "scopusIdentifier";
    public static final String SCOPUS_ITEM_IDENTIFIER_SCP_FIELD_NAME = "scp";

    // URI constants:
    public static final String DOI_OPEN_URL_FORMAT = "https://doi.org";
    public static final String ORCID_DOMAIN_URL = "https://orcid.org/";

    //Journal constants:
    public static final String ISSN_TYPE_ELECTRONIC = "electronic";
    public static final String ISSN_TYPE_PRINT = "print";
    public static final URI DUMMY_URI = UriWrapper.fromUri("https://loremipsum.io/").getUri();

    // Affiliation constants:
    public static final String AFFILIATION_DELIMITER = ", ";

    // XML field names:
    public static final String SUP_START = "<sup>";
    public static final String SUP_END = "</sup>";
    public static final String INF_START = "<inf>";
    public static final String INF_END = "</inf>";

    // Logger messages start:
    public static final String UNKNOWN_LANGUAGE_DETECTED = "Uknown language detected, the following language is not "
                                                           + "supported %s %s";

    @JacocoGenerated
    public ScopusConstants() {
    }
}
