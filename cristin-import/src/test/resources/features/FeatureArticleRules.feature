Feature: Mapping of "Feature article" entries

  Background:
    Given a valid Cristin Result with secondary category "KRONIKK"

  Scenario: Cristin Result of type "Feature article" maps to "FeatureArticle"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a Publication Instance of type "FeatureArticle"

  Scenario: Cristin Entry's Journal Publication "pagesBegin" is copied as is with in NVA's field  "pagesBegin".
    Given that the Cristin Result has a non empty Journal Publication
    And the Journal Publication has a "pagesBegin" entry equal to "1"
    When the Cristin Result is converted to an NVA Resource
    Then the Nva Resource, FeatureArticle, has a PublicationContext with pagesBegin equal to "1"

  Scenario:Cristin Entry's Journal Publication "pagesEnd" is copied as is with in NVA's field "pagesEnd".
    Given that the Cristin Result has a non empty Journal Publication
    And the Journal Publication has a "pagesEnd" entry equal to "10"
    When the Cristin Result is converted to an NVA Resource
    Then the Nva Resource, FeatureArticle, has a PublicationContext with pagesEnd equal to "10"

  Scenario:  Cristin Entry's Journal Publication "volume" entry. is copied to Journal Article's  "volume" field.
    Given that the Cristin Result has a non empty Journal Publication
    And the Journal Publication has a "volume" entry equal to "1"
    When the Cristin Result is converted to an NVA Resource
    Then the Nva Resource, FeatureArticle, has a PublicationContext with volume equal to "1"