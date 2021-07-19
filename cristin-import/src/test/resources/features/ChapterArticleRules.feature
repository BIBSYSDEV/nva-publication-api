Feature:

  Background:
    Given a valid Cristin Result with secondary category "KAPITTEL"

  Scenario: Map returns NVA Resource with Reference having a PublicationInstance of type
  ChapterArticle when the Cristin Result's secondary category is "Kapittel"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource is an instance of "ChapterArticle"
