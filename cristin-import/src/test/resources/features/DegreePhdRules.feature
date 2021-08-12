Feature:
  Mapping rules for DegreePhds

  Background:
    Given a valid Cristin Result with secondary category "DRGRADAVH"

  Scenario: Cristin Result "Doctoral dissertation" is converted to an NVA entry with type "DegreePhd".
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a Publication Instance of type "DegreePhd"

  Scenario: Cristin Result "Doctoral dissertation" is converted to an NVA entry grouped by "Degree".
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a PublicationContext of type Degree

  Scenario: Cristin Result's totalNumberOfPages is copied to NVA publications numberOfPages.
    Given the Cristin entry has a total number of pages equal to "123"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Degree has a PublicationContext with number of pages equal to "123"