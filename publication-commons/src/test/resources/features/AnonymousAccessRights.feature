Feature: Anonymous access rights

  Notes:
  1. In the following, when are referring to a publication that is published,
     we mean that is has status PUBLISHED.

  Background:
    Given that there is a database "PUBLICATIONS" with publications
    And that DynamoDBPublicationService exists
    And the DynamoDBPublicationService has a READ method
    And the READ method takes as parameter a non empty username
    And the READ method takes as parameter a non empty publication ID
    And the DynamoDBPublicationService has an UPDATE method
    And the UPDATE method takes as parameter a non empty username
    And the UPDATE method takes as parameter a non empty publication ID
    And the DynamoDBPublicationService has an DELETE method
    And the DELETE method takes as parameter a non empty username
    And the DELETE method takes as parameter a non empty publication ID
    And the DynamoDBPublicationService has a CREATE method
    And the CREATE method takes as parameter a non empty username
    And the CREATE method takes as parameter a non empty publication object


  Scenario: Anonymous user tries to read published material
    Given a publication with ID "PubId"
    And the publication "PubId" is published
    When READ is called for the Anonymous user and the publication "PubId"
    Then READ returns the publication "PubId"

  Scenario: Anonymous user tries to create published material
    Given a publication P
    When CREATE is called for the Anonymous user and the publication object P
    Then CREATE returns a response that this action is not allowed


  Scenario Outline: Anonymous user tries to update/delete material
    Given a publication with ID "PubId"
    When <non-read-action> is called for the Anonymous user and the publication "PubId"
    Then <non-read-action> returns a response that this action is not allowed

    Examples:
      | non-read-action |
      | UPDATE          |
      | DELETE          |


  Scenario: Anonymous users can see published material when they list publications
    Given that DynamoDBPublicationService has a LIST method
    And that LIST takes as parameter a non empty username
    And that PUBLICATIONS has a publication with ID "PubId" that is published
    When LIST is called for the Anonymous user
    Then the "PubId" publication is in the result-set

  Scenario: Anonymous users can NOT see unpublished material when they list publications
    Given that DynamoDBPublicationService has a LIST method
    And that LIST takes as parameter a non empty username
    And that PUBLICATIONS has a publication with ID "PubId" that is NOT published
    When LIST is called for the Anonymous user
    Then the "PubId" publication is NOT in the result-set

