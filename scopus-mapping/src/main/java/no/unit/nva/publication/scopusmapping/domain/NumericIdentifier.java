package no.unit.nva.publication.scopusmapping.domain;

import static java.util.Objects.requireNonNull;

import java.util.regex.Pattern;

/** An identifier that is a plain number, such as {@code 12345}. Never a URI. */
public interface NumericIdentifier {

  Pattern DIGITS = Pattern.compile("\\d+");

  String value();

  static String requireNumeric(String value, String name) {
    requireNonNull(value, name);
    if (!DIGITS.matcher(value).matches()) {
      throw new IllegalArgumentException("%s must be a plain number: %s".formatted(name, value));
    }
    return value;
  }
}
