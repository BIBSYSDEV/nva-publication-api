Feature: Mapping rules for exhibition production

  Scenario Outline: Should skip mapping of non Exhibition Cristin result with Museum category
    Given a valid Cristin Result with secondary category "MUSEUM"
    And the cristin result has a museum category of "<museumCategory>"
    When the Cristin Result is converted to an NVA Resource
    Then an error is reported.
    Examples:
      | museumCategory |
      | ANNET          |
      | ARRANGEMEN     |
      | KONFERANSE     |
      | GJESTEFORE     |
      | FAGMESSE       |
      | TV-RADIO       |
      | SEMINAR        |
      | WORKSHOP       |

  Scenario: permanent exhibits should be mapped to basic exhibits
    Given a valid Cristin Result with secondary category "MUSEUM"
    And the cristin result has a museum category of "UTSTILLING"
    And the cristin museum exhibits is permanent
    When the Cristin Result is converted to an NVA Resource
    Then the NVA resource has an instance type exhibition production with a "BASIC_EXHIBITION"
    And the NVA Resource has a PublicationContext of type "ExhibitionContent"

  Scenario: cristin museum exhibits that has no to date set should be considered as permanent exhibits
    Given a valid Cristin Result with secondary category "MUSEUM"
    And the cristin result has a museum category of "UTSTILLING"
    And the cristin exhibition event has a to date equal to null
    When the Cristin Result is converted to an NVA Resource
    Then the NVA resource has an instance type exhibition production with a "BASIC_EXHIBITION"

  Scenario: Cristin museum exhibits that are not permanent with finite date should be mapped to temporary exhibits
    Given a valid Cristin Result with secondary category "MUSEUM"
    And the cristin result has a museum category of "UTSTILLING"
    And the cristin exhibit has a event start of "1986-05-10T00:00:00"
    And the cristin exhibit has a event end of "1986-06-15T00:00:00"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA resource has an instance type exhibition production with a "TEMPORARY_EXHIBITION"