Feature:
  Mapping rules for Degree_phds

  Background:
    Given a valid Cristin Result with secondary category "DRGRADAVH"

  Scenario: Cristin Result with type "Drgradavh" is converted to an NVA entry with PublicationInstance of type "DegreePhd".
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a Publication Instance of type "DegreePhd"
