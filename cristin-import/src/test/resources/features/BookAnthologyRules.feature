Feature: Mapping of Book Anthology

  Background:
    Given a valid Cristin Result with secondary category "ANTOLOGI"


  Scenario: map returns NVA Resource with Reference having a PublicationInstance of type
  BookAnthology when the Cristin Result's secondary category is "Anthology"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a Reference with PublicationInstance of Type "BookMonograph"




