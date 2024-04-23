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

  Scenario: Cristin Result mediumPlaceName is used as dissemnitaionChannel in NVA
    Given a valid Cristin Result with secondary category "INTERVJU"
    And the cristin medium has a medium place name "Helgemorgen, NRK P2"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA publicationContext has a disseminationChannel equalTo "Helgemorgen, NRK P2"

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

  Scenario: Cristin result with secondary category "INTERVJUSKRIFTL" is converted to NVA entry
    Given a valid Cristin Result with secondary category "INTERVJUSKRIFTL"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA resource has a MediaContribution with medium "Journal"
    And the NVA resource has a MediaContribution with format "TEXT"


  Scenario: Cristin restult with secondary category "PROGLEDELSE" is converted to NVA entry
    Given a valid Cristin Result with secondary category "PROGLEDELSE"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA resource has a MediaContribution with medium "TV"
    And the NVA resource has a MediaContribution with format "VIDEO"


  Scenario Outline: Cristin result with main category "MEDIEBIDRAG" is converted to NVA entry
    Given the Cristin Result has main category MEDIEBIDRAG
    And varbeid_url has url "<url>" of type "<urltypekode>"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA publication should contain associatedArtifacts containing associatedLink with "<url>"
    Examples:
      | urltypekode | url              |
      | FULLTEKST   | www.example.com  |
      | DATA        | www.example.com  |
      | OMTALE      | www.example.com  |

