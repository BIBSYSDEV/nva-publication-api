Feature:
  Mapping rules for Licentiate Dissertion

  Background:
    Given a valid Cristin Result with secondary category "LISENSIATAVH"

  Scenario: Cristin Result "Licentiate dissertation" is converted to an NVA entry with type "DegreeLicentiate".
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a Publication Instance of type "DegreeLicentiate"

  Scenario: Cristin Result "Licentiate dissertation" is converted to an NVA entry grouped by "Degree".
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a PublicationContext of type Degree

  Scenario Outline: Cristin Result's totalNumberOfPages is copied to NVA publications numberOfPages.
    Given the Cristin entry has a total number of pages equal to "<pages>"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA DegreeLicentiate has a PublicationContext with number of pages equal to "<pages>"
    Examples:
      | pages      |
      | 10-15      |
      | 123        |
      | some pages |