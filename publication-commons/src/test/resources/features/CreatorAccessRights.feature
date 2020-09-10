Feature: Creator access rights

  Background:
    Given that there is a database "PUBLICATIONS" with Publications
    And that DynamoDBPublicationService exists
    And the DynamoDBPublicationService has a READ method

    And the Read method requires a user with non empty username
    And the Read method requires a non empty Publication ID
    And the DynamoDBPublicationService has an Update method

    And the Update method requires a user with non empty username
    And the Update method requires a non empty Publication ID
    And the DynamoDBPublicationService has an Delete method

    And the Delete method requires a user with non empty username
    And the Delete method requires a non empty Publication ID
    And the DynamoDBPublicationService has a Create method

    And the Create method requires a user with non empty username
    And the Create method requires a non empty Publication object
    And the DynamoDBPublicationService has a Publish method

    And the Publish method requires a user with non empty username
    And the Publish method requires a non empty Publication ID

    And there is an authenticated user
    And the authenticated user is affiliated with an institution
    And the user's role is CREATOR
    And the user does not have any other role

  Scenario: User with CreatorRole reads other users' Published Publications
    Given an existing Publication
    And the status of the existing Publication is Published
    And the owner of the Publication is NOT the authenticated user
    When Read is called to read the Publication on behalf of the authenticated user
    Then Read returns the saved version of the Publication

  Scenario: User with CreatorRole reads own Published Publications
    Given an existing Publication
    And the status of the existing Publication is Published
    And the owner of the Publication is the authenticated user
    When Read is called to read the Publication on behalf of the authenticated user
    Then Read returns the saved version of the Publication

  Scenario: User with CreatorRole creates a new Draft Publication
    Given a new Publication
    When Create is called to create a new Publication entry on behalf of the authenticated user
    Then the new Publication is stored in the database
    And the status of the new Publication is Draft
    And Create returns the stored version of the new Publication

  Scenario: User with CreatorRole reads own Draft Publication
    Given an existing Publication
    And the status of the existing Publication is Draft
    And the owner of the Publication is the user
    When Read is called to read the Publication on behalf of the authenticated user
    Then Read returns the saved version of "thePublication"

  Scenario Outline: User with CreatorRole updates own Publication
    Given an existing Publication
    And the status of the existing Publication is <status>
    And the owner of the Publication is the authenticated user
    And there is an update on the Publication's content
    When Update is called to update the Publication according to the new content on behalf of the authenticated user
    Then the existing Publication is updated according to the new content
    And the new version of the Publication replaces the old version in the database
    And Update returns the old version of the Publication

    Examples:
      | status    |
      | Draft     |
      | Published |

  Scenario: User with CreatorRole publishes own Draft Publication
    Given an existing Publication
    And the status of the existing Publication is Draft
    And the owner of the Publication is the authenticated user
    And there is an update on the Publication's content
    When Publish is called to publish the Publication on behalf of the authenticated user
    Then the existing Publication status is set to Publish
    And the status is updated in the database
    And Publish returns the published version of the Publication

  Scenario: User with CreatorRole deletes own Draft Publication
    Given an existing Publication
    And the status of the existing Publication is Draft
    And the owner of the Publication is the authenticated user
    When Delete is called to delete the Publication on behalf of the authenticated user
    Then the Publication is deleted from the database
    And Delete returns the last saved version of the Publication

  Scenario: User with CreatorRole cannot delete own Published Publication
    Given an existing Publication
    And the status of the existing Publication is Published
    And the owner of the Publication is the authenticated user
    When Delete is called to delete the Publication on behalf of the authenticated user
    Then Delete returns an error indicating that the user is not allowed to perform this action

  Scenario: User with CreatorRole can see published material when they list Publications
    Given that DynamoDBPublicationService has a LIST method
    And that LIST requires a user with non empty username
    And that LIST requires a Publication owner's username
    And there is a Published Publication owned by a user that is not the authenticated user
    When LIST is called to list the Publications that other user
    Then LIST returns all Published Publications whose owner is that other user

#====================================PROHIBITIONS=================================================

  Scenario: User with CreatorRole cannot read other users' Draft Publications
    Given an existing Publication
    And the status of the existing Publication is Draft
    And the owner of the Publication is not the authenticated user
    When Read is called to read the Publication on behalf of the the authenticated user
    Then Read returns that the Publication was not found.

  Scenario Outline: Forbidden non-read actions on NOT own Draft Publications for user with CreatorRole
    Given an existing Publication
    And the status of the existing Publication is Draft
    And the owner of the Publication is NOT the authenticated user
    When <action> is called to access the Publication on behalf of the authenticated user
    Then <action> returns an error that this action is not allowed for the authenticated user
    Examples:
      | action  |
      | Update  |
      | Delete  |
      | Publish |
      | Chown   |

  Scenario Outline: Forbidden actions on own Published Publications for users with CreatorRole
    Given an existing Publication
    And the status of the existing Publication is Published
    And the owner of the Publication is the authenticated user
    When <action> is called to access the Publication on behalf of the authenticated user
    Then <action> returns an error that this action is not allowed for the authenticated user
    Examples:
      | action  |
      | Delete  |
      | Publish |
      | CHOWN   |

  Scenario: User with role Creator can NOT see unpublished material when they list Publications
    Given that DynamoDBPublicationService has a List method
    And that List requires a user with non empty username
    And that List requires a Publication owner's username
    And an existing Publication has status Draft
    And the owner of that Publication is some other user
    When LIST is called to list the Publications of that other user on behalf of the authenticated user
    Then the Publication is not included in the result-set
