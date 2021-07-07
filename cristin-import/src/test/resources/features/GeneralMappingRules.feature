Feature: Mappings that hold for all types of Cristin Results

  Background:
    Given a valid Cristin Result

  Scenario: Cristin entry id is saved as additional identifier
    Given the Cristin Result has id equal to 12345
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has an additional identifier with key "Cristin" and value 12345

  Scenario: Map returns NVA Resource with main title being the Cristin title annotated as
  Original Title when there is only one CristinTitle and it is annotated as original
    Given the Cristin Result has an non null array of CristinTitles
    Given the Cristin Result has an array of CristinTitles with values:
      | Title Text         | Status Original | Language Code |
      | This is some title | J               | en            |
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has an EntityDescription with mainTitle "This is some title"


  Scenario: Map returns NVA Resource with main title being the Cristin title annotated as
  Original Title when there are many titles but only one annotated as original
    Given the Cristin Result has an array of CristinTitles with values:
      | Title Text                 | Status Original | Language Code |
      | This is the original title | J               | en            |
      | This is translated title   | N               | en            |
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has an EntityDescription with mainTitle "This is the original title"

  Scenario: Map returns NVA Resource with Main Title being any Cristin Title annotated as
  Original Title when there are two titles both annotated as original
    Given the Cristin Result has an array of CristinTitles with values:
      | Title Text                     | Status Original | Language Code |
      | This is the original title     | J               | en            |
      | This is another original title | J               | en            |
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has an EntityDescription with mainTitle "This is the original title"


  Scenario Outline: map returns NVA Resource with language being the Lexvo URI equivalent of the
  Cristin language code of the title annotated as original
    Given the Cristin Result has an array of CristinTitles with values:
      | Title Text                 | Status Original | Language Code       |
      | This is the original title | J               | <OriginalTitleCode> |
      | This is some other title   | N               | ru                  |
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has an EntityDescription with language "<NvaLanguage>"
    Examples:
      | OriginalTitleCode | NvaLanguage                      |
      | en                | http://lexvo.org/id/iso639-3/eng |
      | EN                | http://lexvo.org/id/iso639-3/eng |
      | NO                | http://lexvo.org/id/iso639-3/nor |
      | NB                | http://lexvo.org/id/iso639-3/nob |
      | NN                | http://lexvo.org/id/iso639-3/nno |
      | garbage           | http://lexvo.org/id/iso639-3/und |
      |                   | http://lexvo.org/id/iso639-3/und |


  Scenario Outline: map returns NVA Resource with Publication Date being equal to the Cristin Result's
  Publication Year
    Given the Cristin Result has publication year <publicationYear>
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a Publication Date with year equal to <publicationYear>, month equal to null and day equal to null
    Examples:
      | publicationYear |
      | "1996"          |
      | "c.a 1996"      |

  Scenario: Map returns NVA Resource with entry creation date equal to Cristin entry's creation date
    Given that Cristin Result has created date equal to the local date "2011-12-03"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a Creation Date equal to "2011-12-03T00:00:00Z"

  Scenario: Map returns NVA Resource where the Contributor names are concatenations of the
  Cristin First and Family names.
    Given that the Cristin Result has Contributors with names:
      | Given Name  | Family Name |
      | John        | Adams       |
      | C.J.B.      | Loremius    |
      | Have, Comma | Surname     |
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a List of NVA Contributors:
      | Name                 |
      | Adams, John          |
      | Loremius, C.J.B.     |
      | Surname, Have, Comma |

  Scenario: Map returns NVA Resource where NVA Contributor sequence is the same as the Cristin
  Contributor Sequence
    Given that the Cristin Result has the Contributors with names and sequence:
      | Given Name  | Family Name  | Ordinal Number |
      | FirstGiven  | FirstFamily  | 1              |
      | SecondGiven | SecondFamily | 2              |
      | ThirdGiven  | ThirdFamily  | 3              |
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a List of NVA Contributors with the following sequences:
      | Name                      | Ordinal Number |
      | FirstFamily, FirstGiven   | 1              |
      | SecondFamily, SecondGiven | 2              |
      | ThirdFamily, ThirdGiven   | 3              |

  Scenario: Map returns NVA Resource with Contributors that have Affiliations With URIs
  created based on Cristin Contributor's Reference URI and Unit numbers.
    Given that the Cristin Result has the Contributors with names and sequence:
      | Given Name  | Family Name  | Ordinal Number |
      | FirstGiven  | FirstFamily  | 1              |
      | SecondGiven | SecondFamily | 2              |
      | ThirdGiven  | ThirdFamily  | 3              |
    And the Contributors are affiliated with the following Cristin Institution respectively:
      | institusjonsnr | avdnr | undavdnr | gruppenr |
      | 194            | 66    | 32       | 15       |
      | 194            | 66    | 32       | 15       |
      | 0              | 0     | 0        | 0        |

    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource Contributors have the following names, sequences and affiliation URIs
      | Name                      | Ordinal Number | Affiliation URI                              |
      | FirstFamily, FirstGiven   | 1              | https://api.cristin.no/v2/units/194.66.32.15 |
      | SecondFamily, SecondGiven | 2              | https://api.cristin.no/v2/units/194.66.32.15 |
      | ThirdFamily, ThirdGiven   | 3              | https://api.cristin.no/v2/units/0.0.0.0      |

  Scenario Outline: Mapping of Cristin Contributor roles
    Given that the Cristin Result has a Contributor with role "<CristinRole>"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Contributor has the role "<NvaRole>"
    Examples:
      | CristinRole | NvaRole |
      | REDAKTØR    | EDITOR  |
      | FORFATTER   | CREATOR |

  Scenario: Mapping reports error when Cristin affiliation has no role
    Given that the Cristin Result has a Contributor with no role
    When the Cristin Result is converted to an NVA Resource
    Then an error is reported.

  Scenario: Mapping reports error when Cristin Contributor has no name
    Given that the Cristin Result has a Contributor with no family and no given name
    When the Cristin Result is converted to an NVA Resource
    Then an error is reported.











