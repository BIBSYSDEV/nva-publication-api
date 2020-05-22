Feature: Creator User access rights

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
    And the CREATE method takes as parameter a non empty publication object
    And a user with username "theCreator"
    And the user "theCreator" has a role CREATOR
    And the user "theCreator" does not have any other role
    And the user "theCreator" is affiliated with the institution "theInstitution"

  Scenario: Creator creates a publication
    Given a publication P
    When CREATE method is called for the user "theCreator" and publication P
    Then the method stores the publication in the "PUBLICATIONS" database
    And the method returns the saved publication.





