Feature: User access rights

  User access rights as specified in Confluence:
  (https://unit.atlassian.net/wiki/spaces/NVAP/pages/443121665/Publication+access+rights+by+role)

  Background:
    Given that there is a database with publications
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

    And there is an authenticated user
    And the authenticated user is affiliated with an institution
    And the user's role is USER
    And the user does not have any other role

  Scenario: users with role USER read published material
    Given a publication
    And the publication has status PUBLISHED
    When READ is called to read the publication on behalf of the authenticated user
    Then READ returns the saved publication

  Scenario: users with role USER can see published material when they list publications
    Given that DynamoDBPublicationService has a LIST method
    And that LIST requires a user with non empty username
    And that LIST requires a publication owner's username
    When LIST is called on behalf of the authenticated user to list the publications of another user
    Then LIST returns all published publications whose owner is that other user

  Scenario: users with role USER cannot create a publication
    Given a new publication entry
    When CREATE is called on behalf of the authenticated user to create a new publication
    Then CREATE returns an error message that this action is not allowed for the authenticated user

  Scenario: users with role USER cannot read unpublished publications
    Given a publication
    And the publication is NOT published
    And the owner of the publication is not the authenticated user
    When READ is called to read the publication on behalf of the authenticated user
    Then READ returns an error response that the publication was not found

  Scenario Outline: Not allowed actions for users with role USER
    Given the DynamoDBPublicationService has an <action> method
    And a publication
    And the publication's status is <status>
    And the owner of the publication is not the authenticated user
    When <non-read-action> is called to act on the publication on behalf of the authenticated user
    Then <non-read-action> returns that this action is not allowed for the authenticated user
    Examples:
      | status    | non-read-action |
      | DRAFT     | DELETE          |
      | DRAFT     | PUBLISH         |
      | DRAFT     | CHOWN           |
      | DRAFT     | UPDATE          |
      | PUBLISHED | DELETE          |
      | PUBLISHED | PUBLISH         |
      | PUBLISHED | CHOWN           |
      | PUBLISHED | UPDATE          |

  Scenario: users with role USER can see published material when they list publications
    Given that DynamoDBPublicationService has a LIST method
    And that LIST requires a user with non empty username
    And that LIST requires a publication owner's username
    And a user with username "theCreator" that owns some publications
    When LIST is called to list the publication of the user "theCreator" on behalf of the authenticated user
    Then LIST returns all published publications whose owner is the user "theCreator"

  Scenario: users with role USER can NOT see unpublished material when they list publications
    Given that DynamoDBPublicationService has a LIST method
    And that LIST requires a user with non empty username
    And that LIST requires a publication owner's username
    And an existing publication does not have status PUBLISHED.
    And the owner of that publication is the user with username "theCreator"
    When LIST is called to list the publications of the user "theCreator" on behalf of the authenticated user
    Then the publication is NOT in the result-set



  @notmvp
  Scenario: users with role USER read unpublished publication that is shared with them
    Given a publication
    And the publication's status is DRAFT
    And the owner of the publication is not the authenticated user
    And the owner of the publication has given read access to the authenticated user for this publication
    When READ is called to read the publication on behalf of the authenticated user
    Then READ returns the saved version of the publication

  @notmvp
  Scenario: users with role USER cannot read unpublished publication that is not shared with them
    Given a publication
    And the publication's status is DRAFT
    And the owner of the publication is not the authenticated user
    And the owner of the publication has not given read access to the authenticated user for this publication
    When READ is called to read the publication on behalf of the authenticated user "theUser"
    Then READ returns an error response that the publication was not found

  @notmvp
  Scenario: users with role USER cannot update unpublished publications if they have not been given write access
    Given a publication
    And the publication's status is DRAFT
    And the owner of the publication is not the authenticated user
    And the owner of the publication has NOT given write access to the authenticated user for this publication
    When UPDATE is called to update the publication on behalf the authenticated user
    Then UPDATE returns that this action is not allowed for the authenticated user

  @notmvp
  Scenario: users with role USER can update unpublished publications if they have been given write access
    Given a publication
    And the publication's status is DRAFT
    And the owner of the publication is not the authenticated use
    And the owner of the publication has given write access to the authenticated user for this publication
    When UPDATE is called to update the publication on behalf of the authenticated user
    Then UPDATE updates the version of the publication stored in the database
    And UPDATE returns the previously stored version of the publication

