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
    Given the Cristin Result has an array of CristinTitles with values:
      | Title Text         | Status Original |
      | This is some title | J               |
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has an EntityDescription with mainTitle "This is some title"


  Scenario: map returns NVA Resource with main title being the Cristin title annotated as
  Original Title when there are many titles but only one annotated as original
    Given the Cristin Result has an array of CristinTitles with values:
      | Title Text                 | Status Original |
      | This is the original title | J               |
      | This is translated title   | N               |

    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has an EntityDescription with mainTitle "This is the original title"


  Scenario: map returns NVA Resource with Main Title being any Cristin Title annotated as
  Original Title when there are two titles both annotated as original
    Given the Cristin Result has an array of CristinTitles with values:
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
