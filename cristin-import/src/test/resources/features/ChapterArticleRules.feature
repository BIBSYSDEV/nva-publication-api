Feature:

  Scenario Outline: Cristin Result of type "Academic chapter/article/Conference paper", "Chapter",
  "Popular scientific chapter/article" and "Encyclopedia article" mapps to NVA entry type "ChapterArticle" and correct sub-type.
    Given a valid Cristin Result with secondary category "<secondarycategory>"
    And the Cristin Result has a non empty Book Report Part
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a Publication Instance of type "ChapterArticle"
    And the NVA ChapterArticle Resource has a Content type of type "<contentType>"
    Examples:
    | secondarycategory | contentType             |
    | KAPITTEL          | Academic Chapter        |
    | FAGLIG_KAPITTEL   | Non-fiction Chapter     |
    | POPVIT_KAPITTEL   | Popular Science Chapter |
    | LEKSIKAL_INNF     | Encyclopedia Chapter    |

  Scenario: Cristin Result's sidenr_fra and sidenr_til is copied to the pages value of the NVA Result.
    Given a valid Cristin Result with secondary category "KAPITTEL"
    And the Cristin Result has a non empty Book Report Part
    And the Cristin Result has a page range from "1" to "9".
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a PublicationInstance with pages starting at "1" and ending at "9"
