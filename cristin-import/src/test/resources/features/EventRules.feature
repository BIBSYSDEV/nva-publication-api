Feature: Event conversion rules

  Scenario Outline: Cristin Result of type foredrag is converted to NVA Resource
  with Publication Context of type "Event"
    Given a valid Cristin Result with secondary category "<secondaryCategory>"
    And the Cristin Result has a non empty LectureOrPoster.
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a PublicationContext of type "Event"
    Examples:
    | secondaryCategory |
    | VIT_FOREDRAG      |
    | POSTER            |
    | FOREDRAG_FAG      |
    | POPVIT_FOREDRAG   |

  Scenario Outline: Cristin Result of type foredrag is converted to
  an NVA Resource with the correct Publication Instance type
    Given a valid Cristin Result with secondary category "<secondaryCategory>"
    And the Cristin Result has a non empty LectureOrPoster.
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a Publication Instance of type "<instanceType>"
    Examples:
    | secondaryCategory | instanceType      |
    | VIT_FOREDRAG      | ConferenceLecture |
    | POSTER            | ConferencePoster  |
    | FOREDRAG_FAG      | Lecture           |
    | POPVIT_FOREDRAG   | Lecture           |
