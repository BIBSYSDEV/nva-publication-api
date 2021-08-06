Feature:

  Scenario: Map returns NVA Resource with Reference having a PublicationInstance of type
  ChapterArticle when the Cristin Result's secondary category is "Kapittel"
    Given a valid Cristin Result with secondary category "KAPITTEL"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a Publication Instance of type "ChapterArticle"

  Scenario: Map returns NVA Resource with Reference having a PublicationInstance of type
  ChapterArticle when the Cristin Result's secondary category is "Kapittel"
    Given a valid Cristin Result with secondary category "FAGLIG_KAPITTEL"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a Publication Instance of type "ChapterArticle"

  Scenario: Map returns NVA Resource with Reference having a PublicationInstance of type
  ChapterArticle when the Cristin Result's secondary category is "Kapittel"
    Given a valid Cristin Result with secondary category "POPVIT_KAPITTEL"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a Publication Instance of type "ChapterArticle"
