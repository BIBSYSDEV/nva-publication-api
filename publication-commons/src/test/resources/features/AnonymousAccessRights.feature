Feature: Anonymous access rights

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

  Scenario: Anonymous user reads published material
    Given a publication with ID "PubId"
    And the publication "PubId" has status PUBLISHED
    When READ is called on behalf of the Anonymous user for the publication "PubId"
    Then READ returns the publication "PubId"

  Scenario: Anonymous user reads published material
    Given a publication with ID "PubId"
    And the publication "PubId" has status PUBLISHED
    When READ is called for the Anonymous user and the publication "PubId"
    Then READ returns the publication "PubId"

  Scenario: Anonymous user tries to read unpublished material
    Given a publication P
    When CREATE is called for the Anonymous user and the publication object P
    Then CREATE returns a response that this action is not allowed

  Scenario: Anonymous user tries to create published material
    Given a publication PublicationA
    When CREATE is called on behalf of the Anonymous user for the publication object PublicationA
    Then CREATE returns a response that this action is not allowed

  Scenario Outline: Anonymous user tries to update/delete material
    Given a publication with ID "PubId"
    And the publication "PubID" has status <status>
    When <non-read-action> is called on behalf of the Anonymous user for the publication "PubId"
    Then <non-read-action> returns a response that this action is not allowed
    Examples:
      | status    | non-read-action |
      | PUBLISHED | UPDATE          |
      | PUBLISHED | DELETE          |
      | PUBLISHED | PUBLISH         |
      | DRAFT     | UPDATE          |
      | DRAFT     | DELETE          |
      | DRAFT     | PUBLISH         |

  Scenario: Anonymous users can see published material when they list publications
    Given that DynamoDBPublicationService has a LIST method
    And that LIST requires a user with non empty username
    And that LIST requires a publication owner's username
    When LIST is called on behalf of the Anonymous user for the user "theCreator"
    Then LIST returns all published publications whose owner is the user "theCreator"

  Scenario: Anonymous users can NOT see unpublished material when they list publications
    Given that DynamoDBPublicationService has a LIST method
    And that LIST requires a user with non empty username
    And that LIST requires a publication owner's username
    And publication "PubID" does not have status PUBLISHED.
    And the owner of "PubID" is the user with username "theCreator"
    When LIST is called on behalf of the Anonymous user for the user "theCreator"
    Then the "PubId" publication is NOT in the result-set

