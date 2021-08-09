Feature:
  Mapping rules for DegreePhds

  Background:
    Given a valid Cristin Result with secondary category "DRGRADAVH"

  Scenario: Cristin Result "Doctoral dissertation" is converted to an NVA entry with type "DegreePhd".
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a Publication Instance of type "DegreePhd"
