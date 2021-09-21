Feature:
  Mapping rules for Reports

  Scenario: Cristin Result with type "Report" is converted to an NVA entry with PublicationInstance of type "ReportResearch".
  ReportResearch when the Cristin Result's secondary category is "Rapport"
    Given a valid Cristin Result with secondary category "RAPPORT"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a Publication Instance of type "ReportResearch"


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
   