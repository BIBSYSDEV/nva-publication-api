Feature: Mapping of Journal Article

  Background:
    Given a valid Cristin Result with secondary category "ARTIKKEL_FAG"

  Scenario: Map returns a Journal Article NVA Resource when the Cristin Result is an "ARTICLEJOURNAL"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource is an instance of "JournalArticle"

  Scenario: Map returns a Journal Article with printISSN copied from the Cristin Entrys's Journal Publication "issn" entry.
    Given that the Cristin Result has a non empty Journal Publication
    And the Journal Publication has a "issn" entry equal to "1903-6523"
    When the Cristin Result is converted to an NVA Resource
    Then the Nva Resource has a PublicationContext with printISSN equal to "1903-6523"

  Scenario: Map returns a Journal Article with onlineIssn copied from the Cristin Entrys's Journal Publication "issnOnline" entry.
    Given that the Cristin Result has a non empty Journal Publication
    And the Journal Publication has a "issnOnline" entry equal to "1903-6523"
    When the Cristin Result is converted to an NVA Resource
    Then the Nva Resource has a PublicationContext with onlineIssn equal to "1903-6523"

  Scenario: Map returns a Journal Article with title copied from the Cristin Entrys's Journal Publication "publisheName" entry.
    Given that the Cristin Result has a non empty Journal Publication
    And the Journal Publication has a "publisherName" entry equal to "Some article publisher"
    When the Cristin Result is converted to an NVA Resource
    Then the Nva Resource has a PublicationContext with title equal to "Some article publisher"

  Scenario: Map returns a Journal Article with pagesBegin copied from the Cristin Entrys's Journal Publication "pagesBegin" entry.
    Given that the Cristin Result has a non empty Journal Publication
    And the Journal Publication has a "pagesBegin" entry equal to "1"
    When the Cristin Result is converted to an NVA Resource
    Then the Nva Resource has a PublicationContext with pagesBegin equal to "1"

  Scenario: Map returns a Journal Article with pagesEnd copied from the Cristin Entrys's Journal Publication "pagesEnd" entry.
    Given that the Cristin Result has a non empty Journal Publication
    And the Journal Publication has a "pagesEnd" entry equal to "10"
    When the Cristin Result is converted to an NVA Resource
    Then the Nva Resource has a PublicationContext with pagesEnd equal to "10"

  Scenario: Map returns a Journal Article with volume copied from the Cristin Entrys's Journal Publication "volume" entry.
    Given that the Cristin Result has a non empty Journal Publication
    And the Journal Publication has a "volume" entry equal to "1"
    When the Cristin Result is converted to an NVA Resource
    Then the Nva Resource has a PublicationContext with volume equal to "1"

  Scenario: Map returns a Journal Article with doi copied from the Cristin Entrys's Journal Publication "doi" entry.
    Given that the Cristin Result has a non empty Journal Publication
    And the Journal Publication has a "doi" entry equal to "10.1093/ajae/aaq063"
    When the Cristin Result is converted to an NVA Resource
    Then the Nva Resource has a Reference object with doi equal to "10.1093/ajae/aaq063"
