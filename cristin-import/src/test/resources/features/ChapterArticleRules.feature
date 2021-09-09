Feature:

  Scenario Outline: Cristin Result of type "Academic chapter/article/Conference paper", "Chapter",
  "Popular scientific chapter/article" and "Encyclopedia article" mapps to NVA entry type "ChapterArticle" and correct sub-type.
    Given a valid Cristin Result with secondary category "<secondarycategory>"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a Publication Instance of type "ChapterArticle"
    And the NVA ChapterArticle Resource has a Content type of type "<contentType>"
    Examples:
    | secondarycategory | contentType             |
    | KAPITTEL          | Academic Chapter        |
    | FAGLIG_KAPITTEL   | Non-fiction Chapter     |
    | POPVIT_KAPITTEL   | Popular Science Chapter |
    | LEKSIKAL_INNF     | Encyclopedia Chapter    |

