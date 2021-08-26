Feature: Book conversion rules


  Scenario: Cristin Result "Academic anthology/Conference proceedings" is converted to
  NVA Resource of type BookAnthology
    Given a valid Cristin Result with secondary category "ANTOLOGI"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a Publication Instance of type "BookAnthology"


  Scenario: Cristin Result "Academic anthology/Conference proceedings" is converted to NVA Resource
  with Publication Context of type "Book"
    Given a valid Cristin Result with secondary category "ANTOLOGI"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a PublicationContext of type "Book"

  Scenario: Cristin Result "Academic monograph" is converted to NVA Resource of type BookMonograph
    Given a valid Cristin Result with secondary category "MONOGRAFI"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a Publication Instance of type "BookMonograph"
    And the NVA BookMonograph Resource has a Content type of type "Academic Monograph"

  Scenario: Cristin Result "Academic monograph" is converted to NVA Resource of type BookMonograph
    Given a valid Cristin Result with secondary category "LÆREBOK"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a Publication Instance of type "BookMonograph"
    And the NVA BookMonograph Resource has a Content type of type "Textbook"

  Scenario: Cristin Result "Academic monograph" is converted to NVA Resource of type BookMonograph
    Given a valid Cristin Result with secondary category "FAGBOK"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a Publication Instance of type "BookMonograph"
    And the NVA BookMonograph Resource has a Content type of type "Non-fiction Monograph"

  Scenario: Cristin Result "Academic monograph" is converted to NVA Resource of type BookMonograph
    Given a valid Cristin Result with secondary category "LEKSIKON"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a Publication Instance of type "BookMonograph"
    And the NVA BookMonograph Resource has a Content type of type "Encyclopedia"

  Scenario: Cristin Result "Academic monograph" is converted to NVA Resource of type BookMonograph
    Given a valid Cristin Result with secondary category "POPVIT_BOK"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a Publication Instance of type "BookMonograph"
    And the NVA BookMonograph Resource has a Content type of type "Popular Science Monograph"

  Scenario: Cristin Result "Academic monograph" is converted to NVA Resource with Publication Context
  of type "Book"
    Given a valid Cristin Result with secondary category "MONOGRAFI"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a PublicationContext of type "Book"


  Scenario Outline: ISBN-10 values for Books are transformed to ISBN-13 values.
    Given a valid Cristin Result with secondary category "<secondaryCategory>"
    And that the Cristin Result has a non empty Book Report
    And the Book Report has an ISBN version 10 with value "8247151464"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a PublicationContext with an ISBN list containing the value "9788247151464"
    Examples:
      | secondaryCategory |
      | MONOGRAFI         |
      | ANTOLOGI          |


  Scenario Outline: "Pages" value is copied as is.
    Given a valid Cristin Result with secondary category "<secondaryCategory>"
    And that the Cristin Result has a non empty Book Report
    And the Book Report has a "total number of pages" entry equal to "<pages>"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a PublicationContext with number of pages equal to "<pages>"
    Examples:
      | pages        | secondaryCategory |
      | 10           | MONOGRAFI         |
      | 10           | ANTOLOGI          |
      | approx. 1000 | MONOGRAFI         |
      | approx. 1000 | ANTOLOGI          |


  Scenario Outline: Publisher's name is copied from the Cristin Entry's Book report entry as is.
    Given a valid Cristin Result with secondary category "<secondaryCategory>"
    Given that the Cristin Result has a non empty Book Report
    And the Book Report has a "publisher name" entry equal to "House of Publishing"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a PublicationContext with publisher equal to "House of Publishing"
    Examples:
      | secondaryCategory |
      | MONOGRAFI         |
      | ANTOLOGI          |


  Scenario Outline: NPI subject heading is copied from Cristin Result as is.
    Given a valid Cristin Result with secondary category "<secondaryCategory>"
    And that the Cristin Result has a non empty Book Report
    And that the Book Report has a subjectField with the subjectFieldCode equal to 1234
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a npiSubjectHeading with value equal to 1234
    Examples:
      | secondaryCategory |
      | MONOGRAFI         |
      | ANTOLOGI          |


  Scenario Outline: Map fails when a Cristin Result that is a "Book" has no subjectField
    Given a valid Cristin Result with secondary category "<secondaryCategory>"
    And that the Cristin Result has a non empty Book Report
    And that the Book Report has no subjectField
    When the Cristin Result is converted to an NVA Resource
    Then an error is reported.
    Examples:
      | secondaryCategory |
      | MONOGRAFI         |
      | ANTOLOGI          |


  Scenario Outline: Mapping fails when a Cristin Result that is a "Book" has no information about the number of pages.
    Given a valid Cristin Result with secondary category "<secondaryCategory>"
    Given that the Book Report entry has an empty "numberOfPages" field
    When the Cristin Result is converted to an NVA Resource
    Then an error is reported.
    Examples:
      | secondaryCategory |
      | MONOGRAFI         |
      | ANTOLOGI          |

