Feature:
  DynamoDBPublicationService does not allow reading, inserting or modifying an entry without
  the caller of the service specifying the username who is performing the action.

  Background:
    Given that PublicationService exists

  Scenario Outline: PublicationService requires username for reading a publication
    Given that PublicationService provides a <action> method for accessing a single publication
    And that <action> method is being called by a module or an application
    When the <action> call does not include a username as a parameter
    Then the module or the application cannot call the <action> method

    Examples:
    |action |
    |CREATE |
    |READ   |
    |UPDATE |
    |DELETE |
    |LIST   |

  Scenario Outline: PublicationService requires non empty username for reading a publication
    Given that PublicationService provides a <action> method for accessing a single publication
    And that <action> method is being called by a module or an application
    When the <action> call includes a username as a parameter
    And the parameter is null or empty
    Then the <action> method returns an error message that empty values are not allowed

    Examples:
      |action |
      |CREATE |
      |READ   |
      |UPDATE |
      |DELETE |
      |LIST   |


  Scenario Outline: PublicationService requires username for reading a publication
    Given that PublicationService provides a <action> method for accessing a single publication
    And that <action> method is being called by a module or an application
    When the <action> call does not include an institution id as a parameter
    Then the module or the application cannot call the <action> method

    Examples:
      |action |
      |CREATE |
      |READ   |
      |UPDATE |
      |DELETE |
      |LIST   |

  Scenario Outline: PublicationService requires non empty username for reading a publication
    Given that PublicationService provides a <action> method for accessing a single publication
    And that <action> method is being called by a module or an application
    When the <action> call includes an institution id as a parameter
    And the parameter is null or empty
    Then the <action> method returns an error message that empty values are not allowed

    Examples:
      |action |
      |CREATE |
      |READ   |
      |UPDATE |
      |DELETE |
      |LIST   |




