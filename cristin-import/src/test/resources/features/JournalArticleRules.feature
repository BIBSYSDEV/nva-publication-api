Feature: Mapping of "Article in business/trade/industry journal", "Academic article",
  "Popular scientific article", "Academic literature review", "Short communication" entries

  Background:
    Given a valid Cristin Result with secondary category "ARTIKKEL_FAG"


  Scenario Outline: Cristin Result of listed secondarycategory maps to NVA entry type "JournalArticle" and correct sub-type.
    Given a valid Cristin Result with secondary category "<secondarycategory>"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a Publication Instance of type "<contentType>"
    Examples:
      | secondarycategory | contentType              |
      | ARTIKKEL_FAG      | ProfessionalArticle      |
      | ARTIKKEL          | AcademicArticle          |
      | SHORTCOMM         | AcademicArticle          |
      | ARTIKKEL_POP      | PopularScienceArticle    |
      | OVERSIKTSART      | AcademicLiteratureReview |

  Scenario: Map returns a Journal Article with printISSN copied from the Cristin Entrys's Journal Publication "issn" entry.

  Scenario: Cristin Entry's Journal Publication "issn" entry is copied to the NVA field  "printISSN".
    Given that the Cristin Result has a non empty Journal Publication
    And the Journal Publication has a "issn" entry equal to "2434-561X"
    When the Cristin Result is converted to an NVA Resource
    Then the Nva Resource has a PublicationContext with printISSN equal to "2434-561X"

  Scenario: Cristin Entry's Journal Publication "issnOnline" entry maps to NVA's field "onlineIssn".
    Given that the Cristin Result has a non empty Journal Publication
    And the Journal Publication has a "issnOnline" entry equal to "2434-561X"
    When the Cristin Result is converted to an NVA Resource
    Then the Nva Resource has a PublicationContext with onlineIssn equal to "2434-561X"

  Scenario: Cristin Entry's Journal Publication "Journal name" ("tidskriftsnavn")  is copied as is
  in the field "title" of the Journal Publication Context of the NVA entry.
    Given that the Cristin Result has a non empty Journal Publication
    And the Journal Publication has a "journalName" entry equal to "Some Journal title"
    When the Cristin Result is converted to an NVA Resource
    Then the Nva Resource has a PublicationContext with title equal to "Some Journal title"

  Scenario: Cristin Entry's Journal Publication "pagesBegin" is copied as is with in NVA's field  "pagesBegin".
    Given that the Cristin Result has a non empty Journal Publication
    And the Journal Publication has a "pagesBegin" entry equal to "1"
    When the Cristin Result is converted to an NVA Resource
    Then the Nva Resource has a PublicationContext with pagesBegin equal to "1"

  Scenario:Cristin Entry's Journal Publication "pagesEnd" is copied as is with in NVA's field "pagesEnd".
    Given that the Cristin Result has a non empty Journal Publication
    And the Journal Publication has a "pagesEnd" entry equal to "10"
    When the Cristin Result is converted to an NVA Resource
    Then the Nva Resource has a PublicationContext with pagesEnd equal to "10"

  Scenario:  Cristin Entry's Journal Publication "volume" entry. is copied to Journal Article's  "volume" field.
    Given that the Cristin Result has a non empty Journal Publication
    And the Journal Publication has a "volume" entry equal to "1"
    When the Cristin Result is converted to an NVA Resource
    Then the Nva Resource has a PublicationContext with volume equal to "1"

  Scenario Outline: Cristin Entry's Journal Publication "issue" entry. is copied to Journal Article's  "issue" field.
    Given that the Cristin Result has a non empty Journal Publication
    And the Journal Publication has a "issue" entry equal to "<issue>"
    When the Cristin Result is converted to an NVA Resource
    Then the Nva Resource, Journal Article, has a PublicationContext with issue equal to "<issue>"
    Examples:
      | issue       |
      | VI          |
      | 123         |
      | some volume |

  Scenario: Cristin Entry's Journal Publication "doi" entry is copied as is in the Reference's doi entry.
    Given that the Cristin Result has a non empty Journal Publication
    And the Journal Publication has a "doi" entry equal to "10.1093/ajae/aaq063"
    When the Cristin Result is converted to an NVA Resource
    Then the Nva Resource has a Reference object with doi equal to "https://doi.org/10.1093/ajae/aaq063"

  Scenario: Mapping does not fail when a Cristin Result of type JournalArticle has no information about the Journal title.
    Given that the Journal Article entry has an empty "publisherName" field
    When the Cristin Result is converted to an NVA Resource
    Then the NVA resource is imported


  Scenario: When the the Cristin entry has a reference to an NSD journal then the
  NVA Entry contains a URI that is a reference to that NSD journal.
    Given the Journal Publication has a reference to an NSD journal with identifier 339708
    And the Journal Publication has publishing year equal to 2003
    And the year the Cristin Result was published is equal to 2003
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a Reference object with a journal URI that points to NVAs NSD proxy
    And the Journal URI specifies the Journal by the NSD ID "45F22A4F-F0CA-4F5F-B279-A2F1EF9D490B" and the year 2003.

  Scenario: When the cristin entry has a NSD code that does not exists in the csv channel-registry file,
       then an exception is thrown
    Given the Journal Publication has a reference to an NSD journal with identifier 12345
    When the Cristin Result is converted to an NVA Resource
    Then the NVA resource is imported

  Scenario: When the cristin entry is an article
    Given article has an article number 55
    When the Cristin Result is converted to an NVA Resource
    Then NVA resource has article number 55.