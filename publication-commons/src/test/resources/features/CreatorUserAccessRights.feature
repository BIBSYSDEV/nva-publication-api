Feature: Creator User access rights

  Background:
    Given that there is a database "PUBLICATIONS" with publications
    And that DynamoDBPublicationService exists
    And the DynamoDBPublicationService has a READ method
    And the READ method requires a non empty username
    And the READ method requires a non empty publication ID
    And the DynamoDBPublicationService has an UPDATE method
    And the UPDATE method requires a non empty username
    And the UPDATE method requires a non empty publication ID
    And the DynamoDBPublicationService has an DELETE method
    And the DELETE method requires a non empty username
    And the DELETE method requires a non empty publication ID
    And the DynamoDBPublicationService has a CREATE method
    And the CREATE method requires a non empty username
    And the CREATE method requires a non empty publication ID
    And the CREATE method requires a non empty publication object

    And a user with username "theUser"
    And the user "theUser" has a role CREATOR
    And the user "theUser" is affiliated with the institution "theInstitution"
    And the user "theUser" does not have any other role

  Scenario: Creator creates a publication with own institution affiliation
    Given a publication P
    When CREATE method is called on behalf of user "theUser" and publication P
    Then the method stores the publication in the "PUBLICATIONS" database
    And the owner of publication P is "theUser"
    And the affiliated institution of publication P is "theInstitution"
    And the method returns the saved publication


  Scenario: Creator creates tries to create publication with other institution affiliation
    Given a publication P
    And an institution "anotherInstitution" that is not "theInstitution"
    When CREATE method is called on behalf of user "theUser" for the institution "anotherInstitution" and publication P
    Then the CREATE method cannot be run.

# Implementation details regarding the DELETE operation are specified in another file
# TODO: Link (as comment) to the DELETE operation specification
  Scenario Outline: Creator reads/deletes own draft publication.
    Given a publication with ID "pubID" and status DRAFT
    And the owner of publication "pubID" is the user "theUser"
    When <action> is called on behalf of user "theUser" and publication "pubID"
    Then <action> is executed
    And the last saved version of the publication "pubID" is returned

    Examples:
      | action |
      | READ   |
      | DELETE |

  Scenario Outline: Creator reads  published publication
    Given a publication with ID "pubID" and status PUBLISHED
    And the owner of publication "pubId" is the user <username>
    When READ is called on behalf of the user "theUser" for the publication "pubID"
    Then the last saved version of the publication "pubID" is returned

    Examples:
      | username    |
      | theUser  |
      | anotherUser |

  Scenario: Creator deletes own published publication
    Given a publication with ID "pubID" and status PUBLISHED
    And the owner of publication "pubID" is the user "theUser"
    When DELETE is called on behalf of user "theUser" and publication "pubID"
    Then DELETE returns an error indicating that this action is not permitted


  Scenario Outline: Creator updates own publication.
    Given a publication with ID "pubID" and status <publicationStatus>
    And the owner of publication "pubID" is the user "theUser"
    When UPDATE is called on behalf of user "theUser" for the  publication "pubID"
    And the new version is stored in "PUBLICATIONS"
    And the previous version is returned by the method

    Examples:
      | publicationStatus |
      | PUBLISHED         |
      | DRAFT             |


  Scenario: Creator updates publication that is shared with them
    Given a publication with ID "pubID" and status DRAFT
    And  the owner of publication "pubID" is the user "anotherUser"
    And the user "anotherUser" has allowed the user "theUser" to update the publication "pubID"
    When UPDATE method is called on behalf of user "theUser" for the publication "pubID"
    And the new version is stored in "PUBLICATIONS"
    And UPDATE returns the previous version.


  Scenario: Creator is not allowed to update publication that is not shared with them
    Given a publication with ID "pubID" and status DRAFT
    And  the owner of publication "pubID" is the user "anotherUser"
    And the user "anotherUser" has NOT allowed the user "theUser" to update the publication "pubID"
    When UPDATE method is called on behalf of user "theUser" for the publication "pubID"
    Then UPDATE returns an error indicating that the operation is not allowed.

  Scenario Outline: Creator is not allowed to delete another user's publication
    Given a publication with ID "pubID" and status <publicationStatus>
    And  the owner of publication "pubID" is the user "anotherUser"
    And the user "anotherUser" <allowedOrNot> the user "theUser" to update the publication "pubID"
    When DELETE method is called on behalf of user "theUser" for the publication "pubID"
    Then DELETE returns an error indicating that the operation is not allowed.

    Examples:
    |publicationStatus| allowedOrNot     |
    | DRAFT           | has allowed      |
    | DRAFT           | has not allowed  |
    | PUBLISHED       | has allowed     |
    | PUBLISHED       | has not allowed  |



