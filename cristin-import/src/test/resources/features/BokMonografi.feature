Feature: Book Monografi transformation

  Background:
    Given a valid Cristin Entry

  Scenario: Hello scenario
    Given the Cristin Entry has id equal to 12345
    When the Cristin Entry is converted to an NVA entry
    Then the NVA Entry has an additional identifier with key "Cristin" and value 12345

