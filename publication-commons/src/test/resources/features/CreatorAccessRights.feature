Feature: Creator access rights

  Background:
    Given that there is a database "PUBLICATIONS" with publications
    And that DynamoDBPublicationService exists
    And the DynamoDBPublicationService has a READ method
    And the READ method requires a user with non empty username
    And the READ method requires a non empty publication ID
    And the DynamoDBPublicationService has an UPDATE method
    And the UPDATE method requires a user with non empty username
    And the UPDATE method requires a non empty publication ID
    And the DynamoDBPublicationService has an DELETE method
    And the DELETE method requires a user with non empty username
    And the DELETE method requires a non empty publication ID
    And the DynamoDBPublicationService has a CREATE method
    And the CREATE method requires a user with non empty username
    And the CREATE method requires a non empty publication object
    And the DynamoDBPublicationService has a PUBLISH method
    And the PUBLISH method requires a user with non empty username
    And the PUBLISH method requires a non empty publication ID

    And there is a user with username "theCreator"
    And the user "theCreator" is affiliated with the institution "theInstitution"
    And the user's role is CREATOR
    And the user "theCreator" does not have any other role


  Scenario: CREATOR user creates a new Publication
    Given a publication "newPublication"
    When CREATE is called on behalf of the "theCreator" for the "newPublication"
    Then "newPublication" is stored in "PUBLICATIONS"
    And the status of "newPublication" is DRAFT
    And CREATE returns the stored version of "newPublication"

  Scenario: CREATOR user reads own draft publication
    Given a publication "thePublication"
    And the status of "thePublication" is DRAFT
    And the owner of "thePublication" is "theCreator"
    When READ is called on behalf of the "theCreator" for "thePublication"
    Then READ returns the saved version of "thePublication"

  Scenario: CREATOR user cannot read not own draft publication
    Given a publication "thePublication"
    And the status of "thePublication" is DRAFT
    And the owner of "thePublication" is "anotherCreator"
    When READ is called on behalf of the "theCreator" for "thePublication"
    Then READ returns that "thePublication" was not found.

  Scenario: CREATOR user updates own draft publication


