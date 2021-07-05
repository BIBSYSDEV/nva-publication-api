Feature: Mappings that hold for all types of Cristin Results

  Background:
    Given a valid Cristin Result

  Scenario: Cristin entry id is saved as additional identifier
    Given the Cristin Result has id equal to 12345
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has an additional identifier with key "Cristin" and value 12345

  Scenario: map returns NVA Resource with main title being the Cristin title annotated as
  Original Title when there is only one CristinTitle and it is annotated as original
    Given the Cristin Result has an non null array of CristinTitles
    Given the Cristin Result has an  CristinTitles with values:
      | Title Text         | Status Original |
      | This is some title | J               |
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has an EntityDescription with mainTitle "This is some title"


  Scenario: map returns NVA Resource with main title being the Cristin title annotated as
  Original Title when there are many titles but only one annotated as original
    Given the Cristin Result has an  CristinTitles with values:
      | Title Text                 | Status Original |
      | This is the original title | J               |
      | This is translated title   | N               |

    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has an EntityDescription with mainTitle "This is the original title"


  Scenario: map returns NVA Resource with Main Title being any Cristin Title annotated as
  Original Title when there are two titles both annotated as original
    Given the Cristin Result has an  CristinTitles with values:
      | Title Text                     | Status Original |
      | This is the original title     | J               |
      | This is another original title | J               |
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has an EntityDescription with mainTitle "This is the original title"

  Scenario Outline: map returns NVA Resource with Publication Date being equal to the Cristin Result's
  Publication Year
    Given the Cristin Result has publication year <publicationYear>
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a Publication Date with year equal to <publicationYear>, month equal to null and day equal to null
    Examples:
      | publicationYear |
      | "1996"          |
      | "c.a 1996"      |

  Scenario: map returns NVA Resource with entry creation date equal to Cristin entry's creation date
    Given that Cristin Result has created date equal to the local date "2011-12-03"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a Creation Date equal to "2011-12-03T00:00:00Z"


  Scenario: map returns NVA Resource where the Contributor names are concatenations of the
  Cristin First and Family names.
    Given that the Cristin Result has Contributors with names:
      | Given Name  | Family Name |
      | John        | Adams       |
      | C.J.B.      | Loremius    |
      | Have, Comma | Surname     |
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a List of NVA Contributors :
      | Name                 |
      | Adams, John          |
      | Loremius, C.J.B.     |
      | Surname, Have, Comma |


  Scenario: map returns NVA Resource where NVA Contributor sequence is the same as the Cristin
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


  Scenario: map returns NVA Resource with Contributors that have Affiliations With URIs
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
      | Name                      | Ordinal Number | Affiliation                                  |
      | FirstFamily, FirstGiven   | 1              | https://api.cristin.no/v2/units/194.66.32.15 |
      | SecondFamily, SecondGiven | 2              | https://api.cristin.no/v2/units/194.66.32.15 |
      | ThirdFamily, ThirdGiven   | 3              | https://api.cristin.no/v2/units/0.0.0.0      |






