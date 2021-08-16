Feature: Mapping of "Letter to the editor" entries

  Background:
    Given a valid Cristin Result with secondary category "BREV_TIL_RED"

  Scenario: Cristin Result of type "Letter to the editor" maps to "JournalLetter"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a Publication Instance of type "JournalLetter"

  Scenario Outline: Cristin Entry's Journal Publication "pagesBegin" is copied as is with in NVA's field  "pagesBegin".
    Given that the Cristin Result has a non empty Journal Publication
    And the Journal Publication has a "pagesBegin" entry equal to "<pagesEnd>"
    When the Cristin Result is converted to an NVA Resource
    Then the Nva Resource, JournalLetter, has a PublicationContext with pagesBegin equal to "<pagesEnd>"
    Examples:
      | pagesEnd     |
      | XI           |
      | 123          |
      | some page    |

  Scenario Outline: Cristin Entry's Journal Publication "pagesEnd" is copied as is with in NVA's field "pagesEnd".
    Given that the Cristin Result has a non empty Journal Publication
    And the Journal Publication has a "pagesEnd" entry equal to "<pagesBegin>"
    When the Cristin Result is converted to an NVA Resource
    Then the Nva Resource, JournalLetter, has a PublicationContext with pagesEnd equal to "<pagesBegin>"
    Examples:
      | pagesBegin   |
      | XI           |
      | 123          |
      | some page    |

  Scenario Outline: Cristin Entry's Journal Publication "volume" entry. is copied to Journal Article's  "volume" field.
    Given that the Cristin Result has a non empty Journal Publication
    And the Journal Publication has a "volume" entry equal to "<volume>"
    When the Cristin Result is converted to an NVA Resource
    Then the Nva Resource, JournalLetter, has a PublicationContext with volume equal to "<volume>"
    Examples:
      | volume       |
      | VI           |
      | 123          |
      | some volume  |