Feature: Event conversion rules

  Scenario Outline: Cristin Result of type foredrag is converted to NVA Resource
  with Publication Context of type "Event"
    Given a valid Cristin Result with secondary category "<secondaryCategory>"
    And the Cristin Result has a non empty LectureOrPoster
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
    And the Cristin Result has a non empty LectureOrPoster
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


  Scenario: Cristin Result of type foredrag is converted to NVA Resource
  with Publication Context of type "Event"
    Given a valid Cristin Result with secondary category "VIT_FOREDRAG"
    And the Cristin Result has a non empty LectureOrPoster
    And the LectureOrPoster has an Event
    And the PresentationEvent has a title "Some label"
    And the PresentationEvent has an Agent "Some agent"
    And the PresentationEvent has a country code "NO"
    And the PresentationEvent has a place "Norge"
    And the PresentationEvent has a from date "2023-11-28T00:00:00"
    And the PresentationEvent has a to date "2023-11-29T00:00:00"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a PublicationContext of type "Event"
    And the Event has a label "Some label"
    And the Event has an agent "Some agent"
    And the Event has a place "Norge" and country code "NO"
    And the Event has a time Period with fromDate "2023-11-28T00:00:00Z"
    And the Event has a time Period with toDate "2023-11-29T00:00:00Z"

  Scenario: Cristin Result of type foredrag is converted to NVA Resource
  with Publication Context of type "Event" without period end date when missing
    Given a valid Cristin Result with secondary category "VIT_FOREDRAG"
    And the Cristin Result has a non empty LectureOrPoster
    And the LectureOrPoster has an Event
    And the PresentationEvent has a title "Some label"
    And the PresentationEvent has an Agent "Some agent"
    And the PresentationEvent has a country code "NO"
    And the PresentationEvent has a place "Norge"
    And the PresentationEvent has a from date "2023-11-28T00:00:00"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a PublicationContext of type "Event"
    And the Event has a label "Some label"
    And the Event has an agent "Some agent"
    And the Event has a place "Norge" and country code "NO"
    And the Event has a time Period with fromDate "2023-11-28T00:00:00Z"
    And the Event toDate is null