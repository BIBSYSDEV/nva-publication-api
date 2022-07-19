Feature: Mapping rules for Master thesis

  Background:
    Given a valid Cristin Result with secondary category "MASTERGRADSOPPG"

  Scenario Outline: Cristin Result "Master thesis", "Second degree thesis" or "Medical thesis" is converted to an NVA entry with type "DegreeMaster".
    Given a valid Cristin Result with secondary category "<secondarycategory>"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a Publication Instance of type "DegreeMaster"
    Examples:
      | secondarycategory |
      | MASTERGRADSOPPG   |
      | HOVEDFAGSOPPGAVE  |
      | FORSKERLINJEOPPG  |

  Scenario: Cristin Result "Master thesis" is converted to an NVA entry grouped by "Degree".
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a PublicationContext of type Degree

  Scenario Outline: Cristin Result's totalNumberOfPages is copied to NVA publications numberOfPages.
    Given the Cristin entry has a total number of pages equal to "<pages>"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA DegreeMaster has a PublicationContext with number of pages equal to "<pages>"
    Examples:
      | pages      |
      | 10-15      |
      | 123        |
      | some pages |