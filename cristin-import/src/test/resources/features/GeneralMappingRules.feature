Feature: Mappings that hold for all types of Cristin Results

  Background:
    Given a valid Cristin Result

  Scenario: Cristin entry id is saved as additional identifier
    Given the Cristin Result has id equal to 12345
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has an additional identifier with key "cristin@sikt" and value 12345

  Scenario: Cristin sources are saved as additional identifiers
    Given the Cristin Result has id equal to 12345
    And the Cristin Result has a non null array of CristinSources with values:
      | Source Code Text | Source Identifier Text |
      | SomeCode         | Some identifier        |
      | Some other code  | Some other identifier  |
      | SCOPUS           | Some third identifier  |
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has an additional identifier with key "SomeCode" and value "Some identifier"
    And the NVA Resource has an additional identifier with key "cristin@sikt" and value 12345
    And the NVA Resource has an additional identifier with key "Some other code" and value "Some other identifier"
    And the NVA Resource has an additional identifier with key "Scopus" and value "Some third identifier"

  Scenario: CristinSource collides with sourceCode
    Given the Cristin Result has id equal to 12345
    And the Cristin Result has sourceCode equal to "SomeCode"
    And the Cristin Result has sourceRecordIdentifier equal to "Some other identifier"
    And the Cristin Result has a non null array of CristinSources with values:
      | Source Code Text | Source Identifier Text |
      | SomeCode         | Some identifier        |
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has an additional identifier with key "SomeCode" and value "Some identifier"
    And the NVA Resource has an additional identifier with key "cristin@sikt" and value 12345
    And the NVA Resource does not have an additional identifier with key "SomeCode" and value "Some other identifier"

  Scenario: NVA Resource gets the single Cristin title which is annotated as
  "Original Title" as Main Title. (i.e., the Cristin entry has no more titles except for the original title).
    Given the Cristin Result has an non null array of CristinTitles
    Given the Cristin Result has an array of CristinTitles with values:
      | Title Text         | Abstract Text                 | Status Original | Language Code |
      | This is some title | This is the original abstract | J               | en            |
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has an EntityDescription with mainTitle "This is some title"

  Scenario: When there are many titles but only one annotated as Original,
  the NVA Resource gets the Cristin title annotated as Original Title as Main Title.
    Given the Cristin Result has an array of CristinTitles with values:
      | Title Text                 | Abstract Text                 | Status Original | Language Code |
      | This is the original title | This is the original abstract | J               | en            |
      | This is translated title   | This is some other abstract   | N               | en            |
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has an EntityDescription with mainTitle "This is the original title"

  Scenario: When there are two titles both annotated as Original the NVA Resource gets any
  of the Cristin Titles annotated as Original Title as Main Title.
    Given the Cristin Result has an array of CristinTitles with values:
      | Title Text                     | Abstract Text                     | Status Original | Language Code |
      | This is the original title     | This is the original abstract     | J               | en            |
      | This is another original title | This is another original abstract | J               | en            |
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has an EntityDescription with mainTitle "This is the original title"

  Scenario: When there is only one title present but not annotated as Original the NVA Resource gets the Cristin Title as Main Title.
    Given the Cristin Result has an array of CristinTitles with values:
      | Title Text         | Abstract Text         | Status Original | Language Code |
      | This is some title | This is some abstract | N               | en            |
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has an EntityDescription with mainTitle "This is some title"


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


  Scenario: The Resources Publication Date is set  the Cristin Result's Publication Year
    Given the Cristin Result has publication year 1996
    And that the cristin Result has published date equal to null
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a Publication Date with year equal to "1996", month equal to "null" and day equal to "null"

  Scenario: The Resources Publication Date is set to the Cristin Result's publication Date
    Approximately 300 000 cristin entries have the publication date set.
    Given that the Cristin Result has published date equal to the local date "2001-05-31"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a Publication Date with year equal to "2001", month equal to "5" and day equal to "31"

  Scenario: The NVA Contributor names are concatenations of Cristin's Cristin First and Family names.
    Given that the Cristin Result has Contributors with names:
      | Given Name  | Family Name |
      | John        | Adams       |
      | C.J.B.      | Loremius    |
      | Have, Comma | Surname     |
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a List of NVA Contributors:
      | Name                |
      | John Adams          |
      | C.J.B. Loremius     |
      | Have, Comma Surname |

  Scenario: The NVA Contributor sequence is the same as the Cristin Contributor Sequence
    Given that the Cristin Result has the Contributors with names and sequence:
      | Given Name  | Family Name  | Ordinal Number |
      | FirstGiven  | FirstFamily  | 1              |
      | SecondGiven | SecondFamily | 2              |
      | ThirdGiven  | ThirdFamily  | 3              |
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a List of NVA Contributors with the following sequences:
      | Name                     | Ordinal Number |
      | FirstGiven FirstFamily   | 1              |
      | SecondGiven SecondFamily | 2              |
      | ThirdGiven ThirdFamily   | 3              |

  Scenario: Map returns NVA Resource with Contributors that have Affiliations With URIs
  created based on Cristin Contributor's Reference URI and Unit numbers.
    Given that the Cristin Result has the Contributors with names and sequence:
      | Given Name  | Family Name  | Ordinal Number |
      | FirstGiven  | FirstFamily  | 1              |
      | SecondGiven | SecondFamily | 2              |
    And the Contributors are affiliated with the following Cristin Institution respectively:
      | institusjonsnr | avdnr | undavdnr | gruppenr |
      | 194            | 66    | 32       | 15       |
      | 194            | 66    | 32       | 15       |
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource Contributors have the following names, sequences and affiliation URIs
      | Name                     | Ordinal Number | Affiliation URI                                                    |
      | FirstGiven FirstFamily   | 1              | https://api.test.nva.aws.unit.no/cristin/organization/194.66.32.15 |
      | SecondGiven SecondFamily | 2              | https://api.test.nva.aws.unit.no/cristin/organization/194.66.32.15 |

  Scenario: Contributors with unknown affiliation is mapped to empty list of affiliations
    Given that the Cristin Result has the Contributors with names and sequence:
      | Given Name | Family Name | Ordinal Number |
      | FirstGiven | FirstFamily | 1              |
    And the Contributors are affiliated with the following Cristin Institution respectively:
      | institusjonsnr | avdnr | undavdnr | gruppenr |
      | 0              | 0     | 0        | 0        |
    And the contributor has a role "REDAKTØR" at the unknown affiliation
    When the Cristin Result is converted to an NVA Resource
    Then  the NVA contributor has no affiliation
    And the NVA Contributor has the role "EDITOR"

  Scenario: Contributors missing affiliations caues errors during mapping
    Given that the Cristin Result has the Contributors with names and sequence:
      | Given Name | Family Name | Ordinal Number |
      | FirstGiven | FirstFamily | 1              |
    And the contributor is missing affiliation
    When the Cristin Result is converted to an NVA Resource
    Then an error is reported.

  Scenario: unverified cristin contributors does not get their contributor identifiers mapped to NVA
    Given a cristin result with a single contributor that is not verified
    When the Cristin Result is converted to an NVA Resource
    Then the NVA contributor does not have an id

  Scenario: verified cristin contributors with id get their contributor identifiers mapped to NVA
    Given a cristin result with a single contributor that is verified and has a cristin-id equal to 1234
    When the Cristin Result is converted to an NVA Resource
    Then the NVA contributor has an id equal to "https://api.test.nva.aws.unit.no/cristin/person/1234"

  Scenario Outline: Mapping of Cristin Contributor roles is done based on hard-coded rules described here.
    Given that the Cristin Result has a Contributor with role "<CristinRole>"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Contributor has the role "<NvaRole>"
    Examples:
      | CristinRole      | NvaRole                |
      | REDAKTØR         | EDITOR                 |
      | FORFATTER        | CREATOR                |
      | PROGRAMDELTAGER  | PROGRAMME_PARTICIPANT  |
      | PROGRAMLEDER     | PROGRAMME_LEADER       |
      | OPPHAVSMANN      | RIGHTS_HOLDER          |
      | JOURNALIST       | JOURNALIST             |
      | REDAKSJONSKOM    | EDITORIAL_BOARD_MEMBER |
      | INTERVJUOBJEKT   | INTERVIEW_SUBJECT      |
      | FAGLIG_ANSVARLIG | ACADEMIC_COORDINATOR   |
      | KUNSTNER         | ARTIST                 |
      | ARKITEKT         | ARCHITECT              |
      | KOMPONIST        | COMPOSER               |
      | OVERSETTER       | TRANSLATOR_ADAPTER     |
      | ARRANGØR         | ORGANIZER              |
      | KONSERVATOR      | CURATOR                |
      | DIRIGENT         | CONDUCTOR              |
      | UTØVER           | ARTIST                 |
      | BIDRAGSYTER      | OTHER                  |
      | EIER             | OTHER                  |
      | PROGRAMMERER     | OTHER                  |

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
      | https://api.test.nva.aws.unit.no/cristin/project/1234 |

  Scenario: Other PresentationWork metadata is ignored
    Given that the Cristin Result has PresentationalWork objects with the values:
      | Type     | Identifier |
      | PROSJEKT | 1234       |
      | PROSJEKT | 5678       |
      | PERSON   | 1111       |
      | GRUPPE   | 0000       |
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has Research projects with the id values:
      | https://api.test.nva.aws.unit.no/cristin/project/1234 |
      | https://api.test.nva.aws.unit.no/cristin/project/5678 |

  Scenario: Mapping does not fail when there is no ResearchProject
    Given that the Cristin Result has a ResearchProject set to null
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has no projects

  Scenario: The Cristin Result's HRCS values are used to generate the URIs for the NVA Resource
    Given a valid Cristin Result
    And the Cristin Result has the HRCS values:
      | helsekategorikode | aktivitetskode |
      | 4                 | 6.4            |
      | 13                | 4              |
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has the following subjects:
      | https://nva.unit.no/hrcs/category/hrcs_hc_12mus |
      | https://nva.unit.no/hrcs/category/hrcs_hc_20gen |
      | https://nva.unit.no/hrcs/activity/hrcs_ra_6_4   |
      | https://nva.unit.no/hrcs/activity/hrcs_rag_4    |

  Scenario Outline: The Cristin Result's HRCS values are used to generate the URIs for the NVA Resource
    Given a valid Cristin Result
    And the Cristin Result has the HRCS values "<helsekategorikode>" and "<aktivitetskode>"
    When the Cristin Result is converted to an NVA Resource
    Then an error is reported.
    Examples:
      | helsekategorikode | aktivitetskode |
      | 4                 | 0.0            |
      | notANumber        | 1.1            |
      | 100               | 1.3            |
      | 7                 | 1.12           |
      | 8                 | NotANumber     |

  Scenario: Mapping a Cristin Result to an NVA Resource creates a publisher id based on environment
  and a hardcoded organization id.
    Given a valid Cristin Result
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource Publishers id is "https://api.test.nva.aws.unit.no/customer/0baf8fcb-b18d-4c09-88bb-956b4f659103"

  Scenario: Mapping creates NVA Resource with contributor with role Other
    Given that the Cristin Result has a Contributor with no role
    When the Cristin Result is converted to an NVA Resource
    Then the NVA contributor has role other.

  Scenario: Mapping reports error when Cristin Contributor has no name
    Given that the Cristin Result has a Contributor with no family and no given name
    When the Cristin Result is converted to an NVA Resource
    Then an error is reported.

  Scenario: Mapping cristin Funding with NFR should create nva Funding with id set.
    Given that Cristin Result has a grant with properties finansieringsreferanse "3013" and sourceCode "NFR":
    When the Cristin Result is converted to an NVA Resource
    Then the publication should have a Confirmed Nva funding with identifier equal to "3013" and id equal to "https://api.test.nva.aws.unit.no/verified-funding/nfr/3013"


  Scenario: When CristinGrants year from and / or year to is present they are mapped.
    Given that Cristin Result has grants:
      | finansieringslopenr | finansieringskildekode | arstall_fra | arstall_til | finansieringsreferanse |
      | 619                 | EU                     | 2005        | 2006        | SCP8-GA-2009-233969    |
      | 3013                | KI                     |             |             | 456                    |
    When the Cristin Result is converted to an NVA Resource
    Then publication should have a nva Fundings:
      | identifier          | activeFrom           | activeTo             | source                                                      | label |
      | SCP8-GA-2009-233969 | 2005-01-01T00:00:00Z | 2006-01-01T00:00:00Z | https://api.test.nva.aws.unit.no/cristin/funding-sources/EU | null  |
      | 456                 |                      |                      | https://api.test.nva.aws.unit.no/cristin/funding-sources/KI | null  |


  Scenario: When CristinGrants should not be url encoded twice
    Given that Cristin Result has grants:
      | finansieringslopenr | finansieringskildekode | arstall_fra | arstall_til | finansieringsreferanse |
      | 17157               | MILJØDIR               | 2005        | 2006        | 17011442               |
      | 10228               | EC/H2020               | 2005        | 2006        | 642080                 |
    When the Cristin Result is converted to an NVA Resource
    Then publication should have a nva Fundings:
      | identifier | activeFrom           | activeTo             | source                                                                 | label |
      | 17011442   | 2005-01-01T00:00:00Z | 2006-01-01T00:00:00Z | https://api.test.nva.aws.unit.no/cristin/funding-sources/MILJ%C3%98DIR | null  |
      | 642080     | 2005-01-01T00:00:00Z | 2006-01-01T00:00:00Z | https://api.test.nva.aws.unit.no/cristin/funding-sources/EC%2FH2020    | null  |


  Scenario: When a eierkode_opprettet matches one of the vitenskapeligarbeid_lokal, the institution is used as owner
    Given that Cristin Result has eierkode_opprett "FHI"
    And the Cristin Result has vitenskapeligarbeid_lokal:
      | eierkode | institusjonsnr | avdnr | undavdnr | gruppenr |
      | NTNU     | 34502          | 0     | 0        | 0        |
      | FHI      | 7502           | 0     | 0        | 0        |
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource should have a owner "fhi@7502.0.0.0" and ownerAffiliation: "https://api.test.nva.aws.unit.no/cristin/organization/7502.0.0.0"


  Scenario: When eierkode_opprettet is missing, one of the vitenskapeligarbeid_lokal is used for resource owner
    Given the Cristin Result has vitenskapeligarbeid_lokal:
      | eierkode | institusjonsnr | avdnr | undavdnr | gruppenr |
      | NTNU     | 34502          | 0     | 0        | 0        |
      | FHI      | 7502           | 0     | 0        | 0        |
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource should have a owner "ntnu@34502.0.0.0" and ownerAffiliation: "https://api.test.nva.aws.unit.no/cristin/organization/34502.0.0.0"

  Scenario: When eierkode_opprettet does not match one of the vitenskapeligarbeid_lokal, the first vitenskapeligarbeid_lokal is used as resource owner
    Given that Cristin Result has eierkode_opprett "UIO"
    And the Cristin Result has vitenskapeligarbeid_lokal:
      | eierkode | institusjonsnr | avdnr | undavdnr | gruppenr |
      | NTNU     | 34502          | 0     | 0        | 0        |
      | FHI      | 7502           | 0     | 0        | 0        |
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource should have a owner "ntnu@34502.0.0.0" and ownerAffiliation: "https://api.test.nva.aws.unit.no/cristin/organization/34502.0.0.0"

  Scenario: When eierkode_opprettet is used as resourceOwner if vitenskapeligarbeid_lokal is missing.
    Given that Cristin Result has eierkode_opprett "NTNU"
    And the cristin has institusjonsnr_opprettet equal to "34502", and avdnr, undavdnr and gruppenr equal to "0"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource should have a owner "ntnu@34502.0.0.0" and ownerAffiliation: "https://api.test.nva.aws.unit.no/cristin/organization/34502.0.0.0"

  Scenario: When eierkode_opprettet is used as resourceOwner if vitenskapeligarbeid_lokal is a empty list.
    Given that Cristin Result has eierkode_opprett "NTNU"
    And the cristin has institusjonsnr_opprettet equal to "34502", and avdnr, undavdnr and gruppenr equal to "0"
    And the Cristin Result has vitenskapeligarbeid_lokal:
      | eierkode | institusjonsnr | avdnr | undavdnr | gruppenr |
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource should have a owner "ntnu@34502.0.0.0" and ownerAffiliation: "https://api.test.nva.aws.unit.no/cristin/organization/34502.0.0.0"

  Scenario Outline: if eierkode_opprettet is certain codes, then fallback Sikt owner should be applied.
    Given that Cristin Result has eierkode_opprett "<eierkode_opprettet>"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource should have a owner "sikt@20754.0.0.0" and ownerAffiliation: "https://api.test.nva.aws.unit.no/cristin/organization/20754.0.0.0"
    Examples:
      | eierkode_opprettet |
      | CRIS               |
      | UNIT               |

  Scenario: if neither eierkode_opprettet nor vitenskapeligarbeid_lokal can be used as resource owner, then Sikt is used as owner
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource should have a owner "sikt@20754.0.0.0" and ownerAffiliation: "https://api.test.nva.aws.unit.no/cristin/organization/20754.0.0.0"

  Scenario: when vitenskapeligarbeid_lokal with eierkode equal to certain codes should be skipped
    Given the Cristin Result has vitenskapeligarbeid_lokal:
      | eierkode | institusjonsnr | avdnr | undavdnr | gruppenr |
      | CRIS     | 1235           | 0     | 0        | 0        |
      | UNIT     | 1234           | 0     | 0        | 0        |
      | NTNU     | 34502          | 0     | 0        | 0        |
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource should have a owner "ntnu@34502.0.0.0" and ownerAffiliation: "https://api.test.nva.aws.unit.no/cristin/organization/34502.0.0.0"

  Scenario: when brage-archive handle is present they should be mapped
    Given the Cristin Result has the following varbeid_url present:
      | urltypekode | url                                  |
      | FULLTEKST   | wwww.example.com                     |
      | ARKIV       | https://hdl.handle.net/11250/2977385 |
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource should have the archive handle set to "https://hdl.handle.net/11250/2977385"

  Scenario: when brage-archive handles is not present the result is still mapped
    Given the Cristin Result has the following varbeid_url present:
      | urltypekode | url              |
      | FULLTEKST   | wwww.example.com |
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource should have the handle set to null

  Scenario: when brage-archive is set to non-url, import without brage-archive
    Given the Cristin Result has the following varbeid_url present:
      | urltypekode | url                       |
      | ARKIV       | Voyage Television, France |
    When the Cristin Result is converted to an NVA Resource
    Then the NVA resource is imported without handle.

  Scenario: Cristin notes should be mapped to NVA notes
    Given a valid Cristin Result
    And the cristin result has a note equal to "Dette er ikke en merknad, men en artikkel 5/3-18 SH"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA resource has a notes field equal to "Dette er ikke en merknad, men en artikkel 5/3-18 SH"

  Scenario: Cristin result without notes should be mapped to empty list
    Given a valid Cristin Result
    And the cristin result has a note equal to ""
    When the Cristin Result is converted to an NVA Resource
    Then the NVA resource has a empty list as publicationNotes