Feature:
  DynamoDBPublicationService does not allow reading, inserting or modifying an entry without
  the caller of the service specifying the username who is performing the action.

  Background:
    Given that PublicationService exists

  Scenario Outline: PublicationService requires username for reading a publication
    Given that PublicationService provides a <action> method for accessing a single publication
    And that <action> method is called by a module or an application
    When the <action> call does not include the username information
    Then the module or the application cannot call the <action> method

    Examples:
      | action |
      | CREATE |
      | READ   |
      | UPDATE |
      | DELETE |
      | LIST   |

  Scenario Outline: PublicationService requires non empty username for reading a publication
    Given that PublicationService provides a <action> method for accessing a single publication
    And that <action> method is called by a module or an application
    When the <action> call includes the username information
    And the username information is null or empty
    Then the <action> method returns an error message that empty values are not allowed

    Examples:
      | action |
      | CREATE |
      | READ   |
      | UPDATE |
      | DELETE |
      | LIST   |


  Scenario Outline: PublicationService requires username for reading a publication
    Given that PublicationService provides a <action> method for accessing a single publication
    And that <action> method is called by a module or an application
    When the <action> call does not include the institution information
    Then the module or the application cannot call the <action> method

    Examples:
      | action |
      | CREATE |
      | READ   |
      | UPDATE |
      | DELETE |
      | LIST   |

  Scenario Outline: PublicationService requires non empty username for reading a publication
    Given that PublicationService provides a <action> method for accessing a single publication
    And that <action> method is called by a module or an application
    When the <action> call includes the institution information
    And the institution information is null or empty
    Then the <action> method returns an error message that empty values are not allowed

    Examples:
      | action |
      | CREATE |
      | READ   |
      | UPDATE |
      | DELETE |
      | LIST   |




