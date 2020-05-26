Feature: User access rights


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
    And there is a user with username "theUser"
    And the user "theUser" is affiliated with the institution "theInstitution"
    And the user's role is USER
    And the user "theUser" does not have any other role


  Scenario: Users read published material
    Given a publication with ID "PubId"
    And the publication "PubId" has status PUBLISHED
    When READ is called on behalf of the Anonymous user for the publication "PubId"
    Then READ returns the publication "PubId"

  Scenario: Users can see published material when they list publications
    Given that DynamoDBPublicationService has a LIST method
    And that LIST requires a user with non empty username
    And that LIST requires a publication owner's username
    When LIST is called on behalf of the "theUser" for the user "theCreator"
    Then LIST returns all published publications whose owner is the user "theCreator"

  Scenario: Users cannot create a publication
    Given a new publication entry "newPublication"
    When CREATE is called on behalf of the user "theUser"
    Then CREATE returns an error message that this action is not allowed for the user "theUser"

  Scenario: User cannot read unpublished publication that is not shared with them
    Given a publication with id "pubID"
    And the publication "pubID" has status DRAFT
    And the owner of the publication "pubID" is the user "theCreator"
    And "theCreator" has not given read access the publication "pubID" to "theUser"
    When READ is called on behalf of the user "theUser" for the publication "pubID"
    Then READ returns an error response that the publication "pubID" was not found

  Scenario: User reads unpublished publication that is shared with them
    Given a publication with id "pubID"
    And the publication "pubID" has status DRAFT
    And the owner of the publication "pubID" is the user "theCreator"
    And "theCreator" has given read access to the publication "pubId" to "theUser"
    When READ is called on behalf of the user "theUser" for the publication "pubID"
    Then READ returns the publication "pubID"

  Scenario: Users cannot update unpublished publications if they have not been given write access
    Given a publication with id "pubID"
    And the publication "pubID" has status DRAFT
    And the owner of the publication "pubID" is the user "theCreator"
    And "theCreator" has NOT given write access to the publication "pubId" to "theUser"
    When UPDATE is called on behalf of the user "theUser" for the publication "pubID"
    Then UPDATE returns that this action is not allowed for the user "theUser"

  Scenario: Users can update unpublished publications if they have been given write access
    Given a publication with id "pubID"
    And the publication "pubID" has status DRAFT
    And the owner of the publication "pubID" is the user "theCreator"
    And "theCreator" has given write access to the publication "pubId" to "theUser"
    When UPDATE is called on behalf of the user "theUser" for the publication "pubID"
    Then UPDATE updates the publication "pubID" stored in "PUBLICATIONS"
    And UPDATE returns the previously stored version of the publication "pubID"

  Scenario: Users cannot update published publications
    Given a publication with id "pubID"
    And the publication "pubID" has status PUBLISHED
    And the owner of the publication "pubID" is the user "theCreator"
    And "theCreator" has given write access to the publication "pubId" to "theUser"
    When UPDATE is called on behalf of the user "theUser" for the publication "pubID"
    Then UPDATE returns that this action is not allowed for the user "theUser"


  Scenario Outline: Not allowed actions for Users
    Given the DynamoDBPublicationService has an <action> method
    Given a publication with id "pubID"
    And the publication "pubID" has status <status>
    And the owner of the publication "pubID" is the user "theCreator"
    And "theCreator" has given <accessType> access to the publication "pubId" to "theUser"
    When <action> is called on behalf of the user "theUser" for the publication "pubID"
    Then <action> returns that this action is not allowed for the user "theUser"
    Examples:
      | status        | accessType | action  |
      | DRAFT | no         | DELETE  |
      | DRAFT | no         | PUBLISH |
      | DRAFT | no         | CHOWN   |
      | DRAFT | read       | DELETE  |
      | DRAFT | read       | PUBLISH |
      | DRAFT | read       | CHOWN   |
      | DRAFT | write      | DELETE  |
      | DRAFT | write      | PUBLISH |
      | DRAFT | write      | CHOWN   |
      | published     | no         | DELETE  |
      | published     | no         | PUBLISH |
      | published     | no         | CHOWN   |
      | published     | read       | DELETE  |
      | published     | read       | PUBLISH |
      | published     | read       | CHOWN   |
      | published     | write      | DELETE  |
      | published     | write      | PUBLISH |
      | published     | write      | CHOWN   |



