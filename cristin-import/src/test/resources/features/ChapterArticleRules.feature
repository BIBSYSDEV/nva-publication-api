Feature:

  Scenario: Cristin Result of type "Academic chapter/article/Conference paper" maps to NVA entry of type
  "ChapterArticle"
    Given a valid Cristin Result with secondary category "KAPITTEL"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a Publication Instance of type "ChapterArticle"

  Scenario: Cristin Result of type "Chapter" maps to NVA entry of type
  "ChapterArticle"
    Given a valid Cristin Result with secondary category "FAGLIG_KAPITTEL"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a Publication Instance of type "ChapterArticle"

  Scenario: MCristin Result of type "Popular scientific chapter/article" maps to NVA entry of type
  "ChapterArticle"
    Given a valid Cristin Result with secondary category "POPVIT_KAPITTEL"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a Publication Instance of type "ChapterArticle"
