Feature: Mapping of "Editorial" entries

  Background:
    Given a valid Cristin Result with secondary category "LEDER"

  Scenario: Cristin Result of type "Editorial" maps to "JournalLeader"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a Publication Instance of type "JournalLeader"

  Scenario Outline: Cristin Entry's Journal Publication "pagesBegin" is copied as is with in NVA's field  "pagesBegin".
    Given that the Cristin Result has a non empty Journal Publication
    And the Journal Publication has a "pagesBegin" entry equal to "<pagesEnd>"
    When the Cristin Result is converted to an NVA Resource
    Then the Nva Resource, JournalLeader, has a PublicationContext with pagesBegin equal to "<pagesEnd>"
    Examples:
      | pagesEnd  |
      | XI        |
      | 123       |
      | some page |

  Scenario Outline: Cristin Entry's Journal Publication "pagesEnd" is copied as is with in NVA's field "pagesEnd".
    Given that the Cristin Result has a non empty Journal Publication
    And the Journal Publication has a "pagesEnd" entry equal to "<pagesBegin>"
    When the Cristin Result is converted to an NVA Resource
    Then the Nva Resource, JournalLeader, has a PublicationContext with pagesEnd equal to "<pagesBegin>"
    Examples:
      | pagesBegin |
      | XI         |
      | 123        |
      | some page  |

  Scenario Outline: Cristin Entry's Journal Publication "volume" entry. is copied to Journal Leader's  "volume" field.
    Given that the Cristin Result has a non empty Journal Publication
    And the Journal Publication has a "volume" entry equal to "<volume>"
    When the Cristin Result is converted to an NVA Resource
    Then the Nva Resource, JournalLeader, has a PublicationContext with volume equal to "<volume>"
    Examples:
      | volume      |
      | VI          |
      | 123         |
      | some volume |

  Scenario Outline: Cristin Entry's Journal Publication "issue" entry. is copied to Journal Leader's  "issue" field.
    Given that the Cristin Result has a non empty Journal Publication
    And the Journal Publication has a "issue" entry equal to "<issue>"
    When the Cristin Result is converted to an NVA Resource
    Then the Nva Resource, Journal Leader, has a PublicationContext with issue equal to "<issue>"
    Examples:
      | issue       |
      | VI          |
      | 123         |
      | some volume |