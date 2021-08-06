Feature:

  Background:
    Given a valid Cristin Result with secondary category "BOKANMELDELSE"

  Scenario: Cristin Result of type "Book Review" maps to NVA "JournalReview".
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource is an instance of "JournalReview"