Feature:
  Mapping rules for Reports

  Background:
    Given a valid Cristin Result with secondary category "RAPPORT"

  Scenario: Cristin Result with type "Report" is converted to an NVA entry with PublicationInstance of type "ReportResearch".
  ReportResearch when the Cristin Result's secondary category is "Rapport"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a Publication Instance of type "ReportResearch"

  Scenario: Cristin Result's "publisher name" ("utgivernavn") in the "BookOrReport" ("type_bok_report") entry
  is copied as is in the "publisher" in the NVA's PublicationContext
  the Cristin Result's BookReport's value of publisherName
    Given that the Cristin Result has a non empty Book Report
    And the Book Report has a "publisher name" entry equal to "some Publisher"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource Report has a PublicationContext with publisher equal to "some Publisher"


  Scenario: Mapping fails when a Cristin Entry has no publisher name
    Given that the Cristin Result has an empty publisherName field
    When the Cristin Result is converted to an NVA Resource
    Then an error is reported.

  Scenario Outline: Map does not fail for a Cristin Result without subjectField when the secondary category does not require it.
    Given a valid Cristin Result with secondary category "<secondaryCategory>"
    And that the Cristin Result has a non empty Book Report
    And that the Book Report has no subjectField
    When the Cristin Result is converted to an NVA Resource
    Then no error is reported.
    Examples:
      | secondaryCategory |
      | RAPPORT           |
      | DRGRADAVH         |
      | MASTERGRADSOPPG   |
      | HOVEDFAGSOPPGAVE  |
      | FORSKERLINJEOPPG  |
   