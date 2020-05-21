Feature:
  1. DynamoDBPublicationService allows a user to read any document that is marked as published and
  therefore it is publicly available.

  2. DynamoDBPublicationService does not allow any user to modify or delete any document that is marked
  as published.

  Notes:
  1. By "published" we mean that a document has been published in a Journal, Conference proceedings,
  or is part of a book and any logged in user can access it.

  2. We assume that when a user is logged in, he/she has read access to all material that is published.

  Background:
    Given that DynamoDBPublicationService exists
    And the DynamoDBPublicationService has a READ method
    And the READ method takes as parameter a non empty username
    And the READ method takes as parameter a non empty publication ID
    And the DynamoDBPublicationService has an UPDATE method
    And the UPDATE method takes as parameter a non empty username
    And the UPDATE method takes as parameter a non empty publication ID
    And the DynamoDBPublicationService has an DELETE method
    And the DELETE method takes as parameter a non empty username
    And the DELETE method takes as parameter a non empty publication ID


  Scenario Outline: A logged-in user reads a published publication
    Given that "PubId" is an existing publication ID
    And that the publication with ID "PubId" has been published
    And the owner of the publication is "theOwner"
    When a call to the READ method requests the publication with Id "PubId"
    And the call to the READ method makes the request on behalf of the user <callerUser>
    Then the READ method returns the publication with ID "PubId"

    Examples:
    |callerUser   |
    |theOwner     |
    |notTheOwner  |

  Scenario: An anonymous user reads a published publication
    Given that "PubId" is an existing publication ID
    And that the publication with ID "PubId" has been published
    And the owner of the publication is "theOwner"
    When a call to the READ method requests the publication with Id "PubId"
    And the call to the READ method makes the request on behalf of an Anonymous user
    Then the READ method returns the publication with ID "PubId"


  Scenario Outline: User is forbidden to update/delete a published publication
    Given that "PubId" is an existing publication ID
    And that the publication with ID "PubId" has been published
    And the owner of the publication is "theOwner"
    When a call to the <action> method requests to update the publication with Id "PubId"
    And the call to the <action> method makes the request on behalf of <callerUser>
    Then the <action> method returns a response indicating that this operation is now allowed

    Examples:
      | action | callerUser  |
      | UPDATE | theOwner    |
      | UPDATE | notTheOwner |
      | UPDATE | Anonymous   |
      | DELETE | theOwner    |
      | DELETE | notTheOwner |
      | DELETE | Anonymous   |
