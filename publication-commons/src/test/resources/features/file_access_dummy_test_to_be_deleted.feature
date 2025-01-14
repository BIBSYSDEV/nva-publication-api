Feature: File metadata read and file download permissions
  As a system user
  I want file metadata read and file download permissions to be enforced based on file state and user role
  So that only authorized users can read the metadata

  Scenario Outline: Verify file metadata read permissions
    Given a file in the "<FileState>" state
    When a user with the role "<UserRole>"
    And the user attempts to "read-metadata" a file
    Then the action outcome is "<Outcome>"

    Examples:
      | FileState           | UserRole                             | Outcome     |
      | PendingOpenFile     | Everyone else                        | Not Allowed |