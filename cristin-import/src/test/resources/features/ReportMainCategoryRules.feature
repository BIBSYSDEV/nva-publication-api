Feature:
  Mapping rules for Cristin entries with main category RAPPORT.

  Scenario Outline: Cristin Result's isbn value is copied to the isbn in the NVA's PublicationContext
    Given a valid Cristin Result with secondary category "<secondaryCategory>"
    And the Cristin Result has an valid ISBN with the value "9788247151464"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a PublicationContext with an ISBN list containing the value "9788247151464"
    Examples:
      | secondaryCategory  |
      | RAPPORT            |
      | DRGRADAVH          |
      | MASTERGRADSOPPG    |

  Scenario Outline: Mapping does not fail when a Cristin Result that is a "Book" has a null value for isbn.
    Given a valid Cristin Result with secondary category "<secondaryCategory>"
    And the Cristin Result does not have an ISBN
    When the Cristin Result is converted to an NVA Resource
    Then no error is reported.
    Examples:
      | secondaryCategory  |
      | RAPPORT            |
      | DRGRADAVH          |
      | MASTERGRADSOPPG    |

