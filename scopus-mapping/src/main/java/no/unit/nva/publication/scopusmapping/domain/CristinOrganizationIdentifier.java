package no.unit.nva.publication.scopusmapping.domain;

import static java.util.Objects.requireNonNull;

import java.util.regex.Pattern;

/**
 * The identifier of an organization in Cristin: four numbers separated by dots, such as {@code
 * 194.63.10.0}. The first number is the institution, and the institution itself is the top level,
 * {@code 194.0.0.0}.
 */
public record CristinOrganizationIdentifier(String value) {

  private static final Pattern FOUR_NUMBERS = Pattern.compile("(\\d+)\\.\\d+\\.\\d+\\.\\d+");
  private static final String TOP_LEVEL_SUFFIX = ".0.0.0";

  public CristinOrganizationIdentifier {
    requireNonNull(value, "Cristin organization identifier");
    if (!FOUR_NUMBERS.matcher(value).matches()) {
      throw new IllegalArgumentException(
          "Cristin organization identifier must be four numbers separated by dots: " + value);
    }
  }

  public CristinOrganizationIdentifier topLevel() {
    var numbers = FOUR_NUMBERS.matcher(value);
    numbers.matches();
    return new CristinOrganizationIdentifier(numbers.group(1) + TOP_LEVEL_SUFFIX);
  }

  public boolean isTopLevel() {
    return equals(topLevel());
  }
}
