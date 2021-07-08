Feature: Mapping of Journal Article

  Background:
    Given a valid Cristin Result with secondary category "ARTIKKEL_FAG"

  Scenario: Map returns NVA Resource with Reference having a PublicationInstance of type
  JournalArticle when the Cristin Result's secondary category is "ARTIKKEL_FAG"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a Reference with PublicationInstance of Type "JournalArticle"
