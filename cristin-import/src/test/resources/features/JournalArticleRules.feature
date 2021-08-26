Feature: Mapping of "Article in business/trade/industry journal", "Academic article", "Popular scientific article", "Academic literature review" entries

  Background:
    Given a valid Cristin Result with secondary category "ARTIKKEL_FAG"

  Scenario: Cristin Result of type "Article in business/trade/industry journal" maps to "JournalArticle"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a Publication Instance of type "JournalArticle"
    And the NVA JournalArticle Resource has a Content type of type "Professional article"

  Scenario: Cristin Result of type "Academic article" maps to "JournalArticle"
    Given a valid Cristin Result with secondary category "ARTIKKEL"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a Publication Instance of type "JournalArticle"
    And the NVA JournalArticle Resource has a Content type of type "Research article"

  Scenario: Cristin Result of type "Popular scientific article" maps to "JournalArticle"
    Given a valid Cristin Result with secondary category "ARTIKKEL_POP"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a Publication Instance of type "JournalArticle"
    And the NVA JournalArticle Resource has a Content type of type "Popular science article"

  Scenario: Cristin Result of type "Academic literature review" maps to "JournalArticle"
    Given a valid Cristin Result with secondary category "OVERSIKTSART"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a Publication Instance of type "JournalArticle"
    And the NVA JournalArticle Resource has a Content type of type "Review article"

  Scenario: Map returns a Journal Article with printISSN copied from the Cristin Entrys's Journal Publication "issn" entry.
  Scenario: Cristin Entry's Journal Publication "issn" entry is copied to the NVA field  "printISSN".
    Given that the Cristin Result has a non empty Journal Publication
    And the Journal Publication has a "issn" entry equal to "1903-6523"
    When the Cristin Result is converted to an NVA Resource
    Then the Nva Resource has a PublicationContext with printISSN equal to "1903-6523"

  Scenario: Cristin Entry's Journal Publication "issnOnline" entry maps to NVA's field "onlineIssn".
    Given that the Cristin Result has a non empty Journal Publication
    And the Journal Publication has a "issnOnline" entry equal to "1903-6523"
    When the Cristin Result is converted to an NVA Resource
    Then the Nva Resource has a PublicationContext with onlineIssn equal to "1903-6523"

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

  Scenario: Cristin Entry's Journal Publication "doi" entry is copied as is in the Reference's doi entry.
    Given that the Cristin Result has a non empty Journal Publication
    And the Journal Publication has a "doi" entry equal to "10.1093/ajae/aaq063"
    When the Cristin Result is converted to an NVA Resource
    Then the Nva Resource has a Reference object with doi equal to "10.1093/ajae/aaq063"

  Scenario: Mapping fails when a Cristin Result of type JournalArticle has no information about the Journal title.
    Given that the Journal Article entry has an empty "publisherName" field
    When the Cristin Result is converted to an NVA Resource
    Then an error is reported.