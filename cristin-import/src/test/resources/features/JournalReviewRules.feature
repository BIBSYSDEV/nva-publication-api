Feature:

  Background:
    Given a valid Cristin Result with secondary category "BOKANMELDELSE"

  Scenario: Map returns NVA Resource with Reference having a PublicationInstance of type
  JournalReview when the Cristin Result's secondary category is "Bokanmeldelse"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource is an instance of "JournalReview"