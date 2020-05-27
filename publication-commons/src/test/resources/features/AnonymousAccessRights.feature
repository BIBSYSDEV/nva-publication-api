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
    Given an existing publication
    And the publication has status PUBLISHED
    When READ is called to read the publication on behalf of the Anonymous user
    Then READ returns the saved publication

  Scenario: Anonymous user cannot read unpublished material
    Given an existing publication
    And the publication has status DRAFT
    When READ is called to read the publication on behalf of the Anonymous user
    Then READ returns an error response that the publication was not found

  Scenario: Anonymous user tries to create publication
    When CREATE is called to create a new publication on behalf of the Anonymous user
    Then CREATE returns a response that this action is not allowed

  Scenario Outline: Anonymous user tries to update/delete material
    Given an existing publication
    And the publication has status <status>
    When <non-read-action> is called to act on the publication on behalf of the Anonymous user
    Then <non-read-action> returns a response that the action is not allowed for the Anonymous user
    Examples:
      | status    | non-read-action |
      | PUBLISHED | UPDATE          |
      | PUBLISHED | DELETE          |
      | PUBLISHED | PUBLISH         |
      | PUBLISHED | CHOWN           |
      | DRAFT     | UPDATE          |
      | DRAFT     | DELETE          |
      | DRAFT     | PUBLISH         |
      | DRAFT     | CHOWN           |

  Scenario: Anonymous users can see published material when they list publications
    Given that DynamoDBPublicationService has a LIST method
    And that LIST requires a user with non empty username
    And that LIST requires a publication owner's username
    And a user with username "theCreator" that owns some publications
    When LIST is called to list the publication of the user "theCreator" on behalf of the Anonymous user
    Then LIST returns all published publications whose owner is the user "theCreator"

  Scenario: Anonymous users can NOT see unpublished material when they list publications
    Given that DynamoDBPublicationService has a LIST method
    And that LIST requires a user with non empty username
    And that LIST requires a publication owner's username
    And an existing publication does not have status PUBLISHED.
    And the owner of that publication is the user with username "theCreator"
    When LIST is called to list the publications of the user "theCreator" on behalf of the Anonymous user
    Then the publication is NOT in the result-set
