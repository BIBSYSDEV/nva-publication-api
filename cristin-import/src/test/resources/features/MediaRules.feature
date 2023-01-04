Feature: Rules that apply for Media

  Scenario: Cristin result "Interview / MediaContribution" is converted to NVA resource of type
  MediaInterview.
    Given a valid Cristin Result with secondary category "INTERVJU"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a Publication Instance of type "MediaInterview"

  Scenario: Cristin Result "Media Interview" is converted to an NVA entry grouped by "MediaContribution".
    Given a valid Cristin Result with secondary category "INTERVJU"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a PublicationContext of type MediaContribution

  Scenario Outline: Cristin Result have mediumType is mapped correctly
    Given a valid Cristin Result with secondary category "INTERVJU"
    And the cristin result has mediaContribution with mediumType equal to "<cristinMediumType>"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA resource has a MediaContribution with medium "<medium>"
    Examples:
      | cristinMediumType | medium        |
      | TIDSSKRIFT        | JOURNAL       |
      | TV                | TV            |
      | FAGBLAD           | JOURNAL       |
      | INTERNETT         | INTERNET      |
      | AVIS              | JOURNAL       |
      | RADIO             | RADIO         |
      | NULL              | OTHER         |