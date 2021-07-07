Feature:

  Background:
    Given a valid Cristin Result with secondary category "MONOGRAFI"


  Scenario: Map returns NVA Resource with Reference having a PublicationInstance of type
  BookAnthology when the Cristin Result's secondary category is "Monografi"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a Reference with PublicationInstance of Type "BookMonograph"

  Scenario: Map returns NVA Resource with Reference having a PublicationContext of type "Book"
  with ISBN values being the ISBN-13 version of the ISBN value of the Cristin TYPE_BOK_RAPPORT
  field.
    Given that the Cristin Result has a non empty Book Report
    And the Book Report has an ISBN version 10 with value "8247151464"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a PublicationContext with an ISBN list containing the value "9788247151464"


  Scenario Outline: Map returns BookMonograph with pages copied from the Cristin Entry's Book Report
  pages entry.
    Given that the Cristin Result has a non empty Book Report
    And the Book Report has a "total number of pages" entry equal to "<pages>"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a PublicationContext with number of pages equal to "<pages>"
    Examples:
      | pages        |
      | 10           |
      | approx. 1000 |


  Scenario: Map returns BookMonograph with Publisher copied from the Cristin Entry's Book Report
  "publisher name" entry
    Given that the Cristin Result has a non empty Book Report
    And the Book Report has a "publisher name" entry equal to "House of Publishing"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a PublicationContext with publisher equal to "House of Publishing"


  Scenario: Map throws exception when Cristin with type book report has no number of pages value.
    Given that the Book Report entry has an empty numberOfPages field
    When the Cristin Result is converted to an NVA Resource
    Then an error is reported.