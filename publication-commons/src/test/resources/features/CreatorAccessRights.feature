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

    And there is an authenticated user
    And the authenticated user is affiliated with an institution
    And the user's role is CREATOR
    And the user does not have any other role


  Scenario: uses with CREATOR role can read NOT own published publications
    Given an existing publication
    And the status of the existing publication is PUBLISHED
    And the owner of the publication is NOT the authenticated user
    When READ is called to read the publication on behalf of the authenticated user
    Then READ returns the saved version of the publication

  Scenario: uses with CREATOR role can read own published publications
    Given an existing publication
    And the status of the existing publication is PUBLISHED
    And the owner of the publication is the authenticated user
    When READ is called to read the publication on behalf of the authenticated user
    Then READ returns the saved version of the publication

  Scenario: users with CREATOR role creates a new draft publication
    Given a new publication
    When CREATE is called to create a new publication entry on behalf of the user
    Then the new publication is stored in the database
    And the status of the new publication is DRAFT
    And CREATE returns the stored version of the new publication

  Scenario: users with CREATOR role reads own draft publication
    Given an existing publication
    And the status of the existing publication is DRAFT
    And the owner of the publication is the user
    When READ is called to read the publication on behalf of the user
    Then READ returns the saved version of "thePublication"

  Scenario Outline: users with CREATOR role updates own publication
    Given an existing publication
    And the status of the existing publication is <status>
    And the owner of the publication is the authenticated user
    And there is an update on the publication's content
    When UPDATE is called to update the publication according to the new content on behalf of the authenticated user
    Then the existing publication is updated according to the new content
    And the new version of the publication replaces the old version in the database
    And UPDATE returns the old version of the publication

    Examples:
      | status    |
      | DRAFT     |
      | PUBLISHED |

  Scenario: users with CREATOR role publish own draft publication
    Given an existing publication
    And the status of the existing publication is DRAFT
    And the owner of the publication is the authenticated user
    And there is an update on the publication's content
    When PUBLISH is called to publish the publication on behalf of the authenticated user
    Then the existing publication status is set to PUBLISH
    And the status is updated in the database
    And PUBLISH returns the published version of the publication

  Scenario: users with CREATOR role deletes own draft publication
    Given an existing publication
    And the status of the existing publication is DRAFT
    And the owner of the publication is the authenticated user
    When DELETE is called to delete the publication on behalf of the authenticated user
    Then the publication is deleted from the database
    And DELETE returns the last saved version of the publication

  Scenario: users with CREATOR role cannot delete own published publication
    Given an existing publication
    And the status of the existing publication is PUBLISHED
    And the owner of the publication is the authenticated user
    When DELETE is called to delete the publication on behalf of the authenticated user
    Then DELETE returns an error indicating that the user is not allowed to perform this action


  Scenario: users with role USER can see published material when they list publications
    Given that DynamoDBPublicationService has a LIST method
    And that LIST requires a user with non empty username
    And that LIST requires a publication owner's username
    And a user with username "theCreator" that owns some publications
    When LIST is called to list the publication of the user "theCreator" on behalf of the authenticated user
    Then LIST returns all published publications whose owner is the user "theCreator"


#====================================PROHIBITIONS=================================================

  Scenario: users with CREATOR role cannot read not owned draft publication
    Given an existing publication
    And the status of the existing publication is DRAFT
    And the owner of the publication is not the authenticated user
    When READ is called to read the publication on behalf of the the authenticated user
    Then READ returns that the publication was not found.


  Scenario Outline: forbidden non read actions on NOT owned draft publications for users with CREATOR role
    Given an existing publication
    And the status of the existing publication is DRAFT
    And the owner of the publication is NOT the authenticated user
    When <action> is called to access the publication on behalf of the authenticated user
    Then <action> returns an error that this action is not allowed for the authenticated user
    Examples:
      | action  |
      | UPDATE  |
      | DELETE  |
      | PUBLISH |
      | CHOWN   |

  Scenario Outline: forbidden actions on owned published publications for users with CREATOR role
    Given an existing publication
    And the status of the existing publication is PUBLISHED
    And the owner of the publication is the authenticated user
    When <action> is called to access the publication on behalf of the authenticated user
    Then <action> returns an error that this action is not allowed for the authenticated user
    Examples:
      | action  |
      | DELETE  |
      | PUBLISH |
      | CHOWN   |

  Scenario: users with role USER can NOT see unpublished material when they list publications
    Given that DynamoDBPublicationService has a LIST method
    And that LIST requires a user with non empty username
    And that LIST requires a publication owner's username
    And an existing publication does not have status PUBLISHED.
    And the owner of that publication is the user with username "theCreator"
    When LIST is called to list the publications of the user "theCreator" on behalf of the authenticated user
    Then the publication is NOT in the result-set
