Feature:

  Background:
  Given a valid Cristin Result with secondary category "RAPPORT"

  Scenario: Map returns NVA Resource with Reference having a PublicationInstance of type
  ReportResearch when the Cristin Result's secondary category is "Rapport"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource is an instance of "ReportResearch"
    
  Scenario: Map returns NVA Resource with Reference having a Publisher with a value matching 
  the Cristin Result's BookReport's value of publisherName
    Given that the Cristin Result has a non empty Book Report
    And the Book Report has a "publisher name" entry equal to "some Publisher"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource Report has a PublicationContext with publisher equal to "some Publisher"

  Scenario: Mapping fails when a Cristin Entry has no publisher name
    Given that the Cristin Result has an empty publisherName field
    When the Cristin Result is converted to an NVA Resource
    Then an error is reported.
    