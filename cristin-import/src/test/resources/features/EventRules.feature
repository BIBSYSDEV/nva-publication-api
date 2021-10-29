Feature: Event conversion rules

  Scenario Outline: Cristin Result of type foredrag is converted to NVA Resource
  with Publication Context of type "Event"
    Given a valid Cristin Result with secondary category "<secondaryCategory>"
    And the Cristin Result has a non empty LectureOrPoster.
    And the Cristin Result has an event
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a PublicationContext of type "Event"
    Examples:
    | secondaryCategory |
    | VIT_FOREDRAG      |
    | POSTER            |
    | FOREDRAG_FAG      |
    | POPVIT_FOREDRAG   |
    | ANNEN_PRESENTASJ  |
    | UTST_WEB          |

  Scenario Outline: Cristin Result of type foredrag is converted to
  an NVA Resource with the correct Publication Instance type
    Given a valid Cristin Result with secondary category "<secondaryCategory>"
    And the Cristin Result has a non empty LectureOrPoster.
    And the Cristin Result has an event
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a Publication Instance of type "<instanceType>"
    Examples:
    | secondaryCategory | instanceType      |
    | VIT_FOREDRAG      | ConferenceLecture |
    | POSTER            | ConferencePoster  |
    | FOREDRAG_FAG      | Lecture           |
    | POPVIT_FOREDRAG   | Lecture           |
    | ANNEN_PRESENTASJ  | OtherPresentation |
    | UTST_WEB          | OtherPresentation |

  Scenario: Title is copied as is from the Cristin Result event to the NVA Resource event
    Given a valid Cristin Result with secondary category "VIT_FOREDRAG"
    And the Cristin Result has an event
    And the Cristin Result has a event with the title "Some Title"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a event with the title "Some Title"

