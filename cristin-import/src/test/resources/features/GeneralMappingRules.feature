Feature: Mappings that hold for all types of Cristin Results

  Background:
    Given a valid Cristin Result

  Scenario: Cristin entry id is saved as additional identifier
    Given the Cristin Result has id equal to 12345
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has an additional identifier with key "Cristin" and value 12345

  Scenario: NVA Resource gets as Main Title the single Cristin title which is annotated as
  "Original Title" (i.e., the Cristin entry has no more titles except for the original title).
    Given the Cristin Result has an non null array of CristinTitles
    Given the Cristin Result has an array of CristinTitles with values:
      | Title Text         | Abstract Text                 | Status Original | Language Code |
      | This is some title | This is the original abstract | J               | en            |
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has an EntityDescription with mainTitle "This is some title"

  Scenario: NVA Resource gets as Main Title the only Cristin title annotated as Original Title when
  there are many titles but only one annotated as Original.
    Given the Cristin Result has an array of CristinTitles with values:
      | Title Text                 | Abstract Text                 | Status Original | Language Code |
      | This is the original title | This is the original abstract | J               | en            |
      | This is translated title   | This is some other abstract   | N               | en            |
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has an EntityDescription with mainTitle "This is the original title"

  Scenario: NVA Resource get as Main Title being any Cristin Title annotated as
  Original Title when there are two titles both annotated as Original.
    Given the Cristin Result has an array of CristinTitles with values:
      | Title Text                     | Abstract Text                     | Status Original | Language Code |
      | This is the original title     | This is the original abstract     | J               | en            |
      | This is another original title | This is another original abstract | J               | en            |
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has an EntityDescription with mainTitle "This is the original title"


  Scenario Outline: The language of the entry is set as Lexvo URI equivalent of the
  Cristin language code of the title annotated as ORIGINAL
    Given the Cristin Result has an array of CristinTitles with values:
      | Title Text                 | Abstract Text                 | Status Original | Language Code       |
      | This is the original title | This is the original abstract | J               | <OriginalTitleCode> |
      | This is some other title   | This is some other abstract   | N               | ru                  |
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has an EntityDescription with language "<NvaLanguage>"
    Examples:
      | OriginalTitleCode | NvaLanguage                      |
      | en                | http://lexvo.org/id/iso639-3/eng |
      | EN                | http://lexvo.org/id/iso639-3/eng |
      | NO                | http://lexvo.org/id/iso639-3/nor |
      | NB                | http://lexvo.org/id/iso639-3/nob |
      | NN                | http://lexvo.org/id/iso639-3/nno |
      | garbage           | http://lexvo.org/id/iso639-3/und |
      |                   | http://lexvo.org/id/iso639-3/und |


  Scenario Outline: The Resources Publication Date is set  the Cristin Result's Publication Year
    Given the Cristin Result has publication year <publicationYear>
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a Publication Date with year equal to <publicationYear>, month equal to null and day equal to null
    Examples:
      | publicationYear |
      | "1996"          |
      | "c.a 1996"      |

  Scenario: The Cristin Entry  Creation Date is set to be the Cristin entry's creation date
    Given that Cristin Result has created date equal to the local date "2011-12-03"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a Creation Date equal to "2011-12-03T00:00:00Z"

  Scenario: The NVA Contributor names are concatenations of Cristin's Cristin First and Family names.
    Given that the Cristin Result has Contributors with names:
      | Given Name  | Family Name |
      | John        | Adams       |
      | C.J.B.      | Loremius    |
      | Have, Comma | Surname     |
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a List of NVA Contributors:
      | Name                 |
      | Adams, John          |
      | Loremius, C.J.B.     |
      | Surname, Have, Comma |

  Scenario: THe NVA Contributor sequence is the same as the Cristin Contributor Sequence
    Given that the Cristin Result has the Contributors with names and sequence:
      | Given Name  | Family Name  | Ordinal Number |
      | FirstGiven  | FirstFamily  | 1              |
      | SecondGiven | SecondFamily | 2              |
      | ThirdGiven  | ThirdFamily  | 3              |
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a List of NVA Contributors with the following sequences:
      | Name                      | Ordinal Number |
      | FirstFamily, FirstGiven   | 1              |
      | SecondFamily, SecondGiven | 2              |
      | ThirdFamily, ThirdGiven   | 3              |

  Scenario: Map returns NVA Resource with Contributors that have Affiliations With URIs
  created based on Cristin Contributor's Reference URI and Unit numbers.
    Given that the Cristin Result has the Contributors with names and sequence:
      | Given Name  | Family Name  | Ordinal Number |
      | FirstGiven  | FirstFamily  | 1              |
      | SecondGiven | SecondFamily | 2              |
      | ThirdGiven  | ThirdFamily  | 3              |
    And the Contributors are affiliated with the following Cristin Institution respectively:
      | institusjonsnr | avdnr | undavdnr | gruppenr |
      | 194            | 66    | 32       | 15       |
      | 194            | 66    | 32       | 15       |
      | 0              | 0     | 0        | 0        |

    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource Contributors have the following names, sequences and affiliation URIs
      | Name                      | Ordinal Number | Affiliation URI                              |
      | FirstFamily, FirstGiven   | 1              | https://api.cristin.no/v2/units/194.66.32.15 |
      | SecondFamily, SecondGiven | 2              | https://api.cristin.no/v2/units/194.66.32.15 |
      | ThirdFamily, ThirdGiven   | 3              | https://api.cristin.no/v2/units/0.0.0.0      |

  Scenario Outline: Mapping of Cristin Contributor roles is done based on hard-coded rules described here.
    Given that the Cristin Result has a Contributor with role "<CristinRole>"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Contributor has the role "<NvaRole>"
    Examples:
      | CristinRole | NvaRole |
      | REDAKTØR    | EDITOR  |
      | FORFATTER   | CREATOR |

  Scenario: The abstract is copied from the the Cristin Result's title entry when there
  one title entry and it is annotated as original.
    Given the Cristin Result has an array of CristinTitles with values:
      | Abstract Text                 | Title Text                 | Status Original | Language Code |
      | This is the original abstract | This is the original Title | J               | NO            |
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has the following abstract "This is the original abstract"

  Scenario: The abstract is copied form the Cristin Result's title entry that is annotated as original
  when there are many titles but only one Original Title
    Given the Cristin Result has an array of CristinTitles with values:
      | Abstract Text                   | Title Text                 | Status Original | Language Code |
      | This is the some other abstract | This is some other Title   | N               | NO            |
      | This is the original abstract   | This is the original Title | J               | NO            |
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has the following abstract "This is the original abstract"

  Scenario: Mapping does not fail when there is no abstract
    Given the Cristin Result has an array of CristinTitles with values:
      | Abstract Text                   | Title Text               | Status Original | Language Code |
      | This is the some other abstract | This is some other Title | J               | NO            |
    And the cristin title abstract is sett to null
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has no abstract


  Scenario:All tags are copied as keywords and language of the keywords is ignored.
    Given that the Cristin Result has a CristinTag object with the values:
      | Bokmal    | English | Nynorsk    |
      | kirke     | church  | kyrkje     |
      | skole     |         | skule      |
      | hus       | house   |            |
      |           |         | nynorskOrd |
      | bokmalOrd |         |            |
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has the tags:
      | kirke      |
      | church     |
      | kyrkje     |
      | skole      |
      | skule      |
      | hus        |
      | house      |
      | nynorskOrd |
      | bokmalOrd  |


  Scenario: Cristin entry's project id is transformed to NVA project URI
    Given that the Cristin Result has a PresentationalWork object that is not null
    And that the Cristin Result has PresentationalWork objects with the values:
      | Type     | Identifier |
      | PROSJEKT | 1234       |
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has Research projects with the id values:
      | https://api.test.nva.aws.unit.no/project/1234 |

  Scenario: Other PresentationWork metadata is ignored
    Given that the Cristin Result has PresentationalWork objects with the values:
      | Type     | Identifier |
      | PROSJEKT | 1234       |
      | PROSJEKT | 5678       |
      | PERSON   | 1111       |
      | GRUPPE   | 0000       |
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has Research projects with the id values:
      | https://api.test.nva.aws.unit.no/project/1234 |
      | https://api.test.nva.aws.unit.no/project/5678 |

  Scenario: Mapping does not fail when there is no ResearchProject
    Given that the Cristin Result has a ResearchProject set to null
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has no projects

  Scenario: Mapping reports error when Cristin affiliation has no role
    Given that the Cristin Result has a Contributor with no role
    When the Cristin Result is converted to an NVA Resource
    Then an error is reported.

  Scenario: Mapping reports error when Cristin Contributor has no name
    Given that the Cristin Result has a Contributor with no family and no given name
    When the Cristin Result is converted to an NVA Resource
    Then an error is reported.











