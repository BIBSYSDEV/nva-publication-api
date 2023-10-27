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
    And the exhibition manifestation has a period with date start equal to "1986-05-10T00:00:00"
    And the exhibition manifestation has a period with date end equal to "1986-06-15T00:00:00"

  Scenario: A permanent exhibit can have a finite period and be mapped to basic exhibition
    Given a valid Cristin Result with secondary category "MUSEUM"
    And the cristin result has a museum category of "UTSTILLING"
    And the cristin museum exhibits is permanent
    And the cristin exhibit has a event start of "1986-05-10T00:00:00"
    And the cristin exhibit has a event end of "1986-06-15T00:00:00"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA resource has an instance type exhibition production with a "BASIC_EXHIBITION"

  Scenario: Cristin museum exhibit event are used for generating nva manifestation
    Given a valid Cristin Result with secondary category "MUSEUM"
    And the cristin result has a museum category of "UTSTILLING"
    And the cristin exhibition event has an organizer equal to "Nasjonalmuseet for kunst, arkitektur og design i samarbeid med UiO"
    And the cristin exhibition event has a country code equal to "NO"
    And the cristin exhibition event has an place description equal to "Nasjonalmuseet"
    When the Cristin Result is converted to an NVA Resource
    Then the exhibition manifestation has an unconfirmed place with label equal to "Nasjonalmuseet" and country equal to "NO"
    And the exhibition manifestation has a organization equal to "Nasjonalmuseet for kunst, arkitektur og design i samarbeid med UiO"

  Scenario: Cristin museum exhibit event are used for generating nva description
    Given a valid Cristin Result with secondary category "MUSEUM"
    And the cristin result has a museum category of "UTSTILLING"
    And the cristin exhibition event has a title "Spor. Norsk samtidsarkitektur 2005-2010"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA publication has a description containing "Spor. Norsk samtidsarkitektur 2005-2010"

  Scenario: Cristin museum exhibit event title "Utstilling" is not mapped to description
  (Loads of cristin museum have a title text only containing "Utstilling", this is redundant information)
    Given a valid Cristin Result with secondary category "MUSEUM"
    And the cristin result has a museum category of "UTSTILLING"
    And the cristin exhibition event has a title "Utstilling"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA publication does not have a description containing "Utstilling"

  Scenario: Cristin exhibition events data should be mapped to description
    Given a valid Cristin Result with secondary category "MUSEUM"
    And the cristin result has a museum category of "UTSTILLING"
    And the exhibition has 56 number of own objects
    And the exhibition has 26 number of total objects
    And the exhibition has 3 visitors
    And the exhibition has a budget of 56.5 NOK
    And the exhibition has an area of 345.4
    And the cristin exhibition event has a title "Spor. Norsk samtidsarkitektur 2005-2010"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA publication has a description containing "Spor. Norsk samtidsarkitektur 2005-2010"
    And the NVA publication has a description containing "Beløp: 56.5 NOK"
    And the NVA publication has a description containing "Brukt areal: 345.4 m2"
    And the NVA publication has a description containing "Antall besøkende: 3"
    And the NVA publication has a description containing "Andel egne gjenstander: 56.0%"
    And the NVA publication has a description containing "Antall gjenstander: 26"

