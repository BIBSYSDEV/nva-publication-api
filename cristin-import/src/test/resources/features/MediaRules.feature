Feature: Rules that apply for Media

  Scenario: Cristin result "Interview / MediaContribution" is converted to NVA resource of type
  MediaInterview.
    Given a valid Cristin Result with secondary category "INTERVJU"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a Publication Instance of type "MediaInterview"