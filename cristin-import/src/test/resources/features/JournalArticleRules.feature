Feature: Mapping of Journal Article

  Background:
    Given a valid Cristin Result with secondary category "ARTIKKEL_FAG"

  Scenario: Map returns a Journal Article NVA Resource when the Cristin Result is an "ARTICLEJOURNAL"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource is an instance of "JournalArticle"
