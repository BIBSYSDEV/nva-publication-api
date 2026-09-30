package no.unit.nva.transformer.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class IdentifierTypeTest {

  @Test
  void fromValueReturnsMatchingTypeIgnoringCase() {
    assertEquals(IdentifierType.DOI, IdentifierType.fromValue("doi"));
    assertEquals(IdentifierType.URL, IdentifierType.fromValue("URL"));
  }

  @Test
  void fromValueReturnsNullWhenValueIsUnknown() {
    assertNull(IdentifierType.fromValue("unknown"));
  }
}
