Feature:
  Mapping rules for Reports

  Scenario: Cristin Result with type "Report" is converted to an NVA entry with PublicationInstance of type "ReportResearch".
  ReportResearch when the Cristin Result's secondary category is "Rapport"
    Given a valid Cristin Result with secondary category "RAPPORT"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a Publication Instance of type "ReportResearch"

  Scenario: Cristin Result with main type "Report"  and secondary type Compendium is converted to an NVA publication.
    Given a valid Cristin Result with main category "RAPPORT" and secondary category "KOMPENDIUM"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a Publication Instance of type "ReportWorkingPaper"

  Scenario: Cristin Result with main type InformationMaterial and secondary type Briefs is converted to an NVA publication for given customer.
    Given a valid Cristin Result with main category "INFORMASJONSMATR" and secondary category "BRIEFS"
    And publication owner is "NIFU"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a Publication Instance of type "ReportPolicy"

  Scenario: Cristin Result with main type InformationMaterial and secondary type Briefs is not converted to an NVA publication for not provided customer.
    Given a valid Cristin Result with main category "INFORMASJONSMATR" and secondary category "BRIEFS"
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

  Scenario Outline: A Cristin Result with a valid isbn littered with special characters will have them removed
  when mapped to the NVA Resource
    Given a valid Cristin Result with secondary category "<secondaryCategory>"
    And the Cristin Result has an valid ISBN littered with special characters "9*7^8-3/16-1+4?8_4()1|0-0"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a PublicationContext with an ISBN list containing the value "9783161484100"
    Examples:
      | secondaryCategory |
      | RAPPORT           |
      | DRGRADAVH         |
      | MASTERGRADSOPPG   |
      | HOVEDFAGSOPPGAVE  |
      | FORSKERLINJEOPPG  |

   