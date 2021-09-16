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

  Scenario Outline: Cristin Result "Academic Monograph, Non-fiction Monograph, Popular Science Monograph,
  Textbook, Encyclopedia" is converted to NVA Resource type BookMonograph and correct sub-type
    Given a valid Cristin Result with secondary category "<secondaryCategory>"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a Publication Instance of type "BookMonograph"
    And the NVA BookMonograph Resource has a Content type of type "<contentType>"
    Examples:
      | secondaryCategory | contentType               |
      | MONOGRAFI         | Academic Monograph        |
      | LÆREBOK           | Textbook                  |
      | FAGBOK            | Non-fiction Monograph     |
      | LEKSIKON          | Encyclopedia              |
      | POPVIT_BOK        | Popular Science Monograph |

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
    Then the NVA Resource has a PublicationContext with publisher with name equal to "House of Publishing"
    Then NVA Resource has a Publisher that cannot be verified through a URI
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


  Scenario: Map fails when a Cristin Result that is a "Monografi" has no subjectField
    Given a valid Cristin Result with secondary category "MONOGRAFI"
    And that the Cristin Result has a non empty Book Report
    And that the Book Report has no subjectField
    When the Cristin Result is converted to an NVA Resource
    Then an error is reported.

  Scenario Outline: Map does not fail for a Cristin Result without subjectField when the secondary category does not require it.
    Given a valid Cristin Result with secondary category "<secondaryCategory>"
    And that the Cristin Result has a non empty Book Report
    And that the Book Report has no subjectField
    When the Cristin Result is converted to an NVA Resource
    Then no error is reported.
    Examples:
      | secondaryCategory |
      | ANTOLOGI          |
      | LÆREBOK           |
      | FAGBOK            |
      | LEKSIKON          |
      | POPVIT_BOK        |


  Scenario Outline: Mapping does not fail when a Cristin Result that is a "Book" has no information about the number of pages.
    Given a valid Cristin Result with secondary category "<secondaryCategory>"
    Given that the Book Report entry has an empty "numberOfPages" field
    When the Cristin Result is converted to an NVA Resource
    Then no error is reported.
    Examples:
      | secondaryCategory |
      | MONOGRAFI         |
      | ANTOLOGI          |

  Scenario: Mapping does not fail when a Cristin Result that is a "Book" has a null value for isbn.
    Given a valid Cristin Result with secondary category "MONOGRAFI"
    And the Cristin Result does not have an ISBN
    When the Cristin Result is converted to an NVA Resource
    Then no error is reported.

  Scenario Outline: Mapping creates a reference to an NSD publisher when the Cristin entry contains
    an NSD code for the publisher
    Given a valid Cristin Result with secondary category "<secondaryCategory>"
    And the Cristin Result refers to a Series with NSD code 12345
    And the Cristin Result has publication year "2002"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a Reference to a Publisher that is a URI pointing to the NVA NSD proxy
    And the URI contains the NSD code 12345 and the publication year 2002
    Examples:
      | secondaryCategory |
      | MONOGRAFI         |
      | ANTOLOGI          |

