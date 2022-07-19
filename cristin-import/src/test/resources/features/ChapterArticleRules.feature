Feature:

  Scenario Outline: Cristin Result of listed secondarycategory maps to NVA entry type "ChapterArticle" and correct sub-type.
    Given a valid Cristin Result with secondary category "<secondarycategory>"
    And the Cristin Result has a non empty Book Report Part
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a Publication Instance of type "ChapterArticle"
    And the NVA ChapterArticle Resource has a Content type of type "<contentType>"
    Examples:
      | secondarycategory | contentType           |
      | KAPITTEL          | AcademicChapter       |
      | FAGLIG_KAPITTEL   | NonFictionChapter     |
      | POPVIT_KAPITTEL   | PopularScienceChapter |
      | LEKSIKAL_INNF     | EncyclopediaChapter   |

  Scenario: Cristin Result's sidenr_fra and sidenr_til is copied to the pages value of the NVA Result.
    Given a valid Cristin Result with secondary category "KAPITTEL"
    And the Cristin Result has a non empty Book Report Part
    And the Cristin Result has a page range from "1" to "9".
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a PublicationInstance with pages starting at "1" and ending at "9"

  Scenario: When a Cristin Result has been reported in NVI then it is considered to be peer reviewed.
    Given a valid Cristin Result with secondary category "KAPITTEL"
    And the Cristin Result has a non empty Book Report Part
    And the Cristin Result has a value for the date when it was reported in NVI.
    When the Cristin Result is converted to an NVA Resource
    Then the Chapter Article has a "isPeerReviewed" equal to True
