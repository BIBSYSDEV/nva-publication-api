package no.unit.nva.publication.testing;

import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;

import java.net.URI;
import nva.commons.core.paths.UriWrapper;

public final class CristinUriGenerator {

  private static final String CRISTIN = "cristin";
  private static final String PERSON = "person";

  private CristinUriGenerator() {}

  public static URI randomCristinPersonUri() {
    return cristinPersonUri(randomUri(), String.valueOf(randomInteger()));
  }

  public static URI cristinPersonUri(URI host, String personIdentifier) {
    return UriWrapper.fromUri(host)
        .addChild(CRISTIN)
        .addChild(PERSON)
        .addChild(personIdentifier)
        .getUri();
  }
}
