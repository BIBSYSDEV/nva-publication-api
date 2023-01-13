Feature: Rules that apply for Media

  Scenario Outline: Cristin result "Interview / MediaContribution, Program participation / MediaContribution"
  is converted to NVA resource with the correct Publication Instance type.
    Given a valid Cristin Result with secondary category "<secondaryCategory>"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a Publication Instance of type "<publicationInstance>"
    Examples:
      | secondaryCategory | publicationInstance           |
      | INTERVJU          | MediaInterview                |
      | PROGDELTAGELSE    | MediaParticipationInRadioOrTv |

  Scenario: Cristin Result "Media Interview" is converted to an NVA entry grouped by "MediaContribution".
    Given a valid Cristin Result with secondary category "INTERVJU"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a PublicationContext of type MediaContribution

  Scenario Outline: Cristin Result have mediumType is mapped correctly
    Given a valid Cristin Result with secondary category "INTERVJU"
    And the cristin result has mediaContribution with mediumType equal to "<cristinMediumType>"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA resource has a MediaContribution with medium "<medium>"
    And the NVA resource has a MediaContribution with format "<nvaFormat>"
    Examples:
      | cristinMediumType | medium   | nvaFormat |
      | TIDSSKRIFT        | Journal  | TEXT      |
      | TV                | TV       | VIDEO     |
      | FAGBLAD           | Journal  | TEXT      |
      | INTERNETT         | Internet | NULL      |
      | AVIS              | Journal  | TEXT      |
      | RADIO             | Radio    | SOUND     |

