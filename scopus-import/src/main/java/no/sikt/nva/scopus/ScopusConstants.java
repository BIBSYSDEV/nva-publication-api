package no.sikt.nva.scopus;

import java.time.Duration;

public final class ScopusConstants {

  // HTTP timeout constants:
  public static final Duration HTTP_CONNECT_TIMEOUT = Duration.ofSeconds(10);
  public static final Duration HTTP_REQUEST_TIMEOUT = Duration.ofSeconds(20);

  // URI constants:
  public static final String DOI_OPEN_URL_FORMAT = "https://doi.org";
  public static final String ORCID_DOMAIN_URL = "https://orcid.org/";

  // Journal constants:
  public static final String ISSN_TYPE_ELECTRONIC = "electronic";
  public static final String ISSN_TYPE_PRINT = "print";

  // Affiliation constants:
  public static final String AFFILIATION_DELIMITER = ", ";

  // XML field names:
  public static final String SUP_START = "<sup>";
  public static final String SUP_END = "</sup>";
  public static final String INF_START = "<inf>";
  public static final String INF_END = "</inf>";

  // Logger messages start:
  public static final String UNKNOWN_LANGUAGE_DETECTED =
      "Uknown language detected, the following language is not " + "supported %s %s";

  private ScopusConstants() {}
}
