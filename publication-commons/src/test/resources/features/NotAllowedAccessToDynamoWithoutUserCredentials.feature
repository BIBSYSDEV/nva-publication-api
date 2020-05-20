Feature:
  DynamoDBPublicationService does not allow reading, inserting or modifying an entry without
  the caller of the service specifying the username who is performing the action.


  Background:
    Given that PublicationService exists

  Scenario: PublicationService requires username for reading a publication
    Given that PublicationService provides a READ method for reading a single publication
    And that READ method is being called by a module or an application
    When the READ call does not include a username
    Then the module or the application cannot call the READ method


  Scenario: PublicationService requires username for listing a set of publications
    Given that PublicationService provides a LIST method for reading a set of publications
    And that LIST method is being called by a module or an application
    When the LIST call does not include a username
    Then the module or the application cannot call the LIST method


  Scenario: PublicationService requires username for creating a publication
    Given that PublicationService provides a CREATE method for reading a set of publications
    And that CREATE method is being called by a module or an application
    When the CREATE  call does not include a username
    Then the module or the application cannot call the CREATE method


  Scenario: PublicationService requires username for updating a publication
    Given that PublicationService provides a CREATE method for reading a set of publications
    And that UPDATE method is being called by a module or an application
    When the UPDATE call does not include a username
    Then the module or the application cannot call the UPDATE method


  Scenario: PublicationService requires username for deleting a publication
    Given that PublicationService provides a DELETE method for reading a set of publications
    And that DELETE method is being called by a module or an application
    When the DELETE call does not include a username
    Then the module or the application cannot call the DELETE method



