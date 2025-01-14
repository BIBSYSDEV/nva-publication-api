@ignore
Feature: File upload permissions
  As a system user
  I want file download permissions to be enforced based on file state and user role
  So that only authorized users can upload the files

  Background:
  This will be exposed on publication level, not file level like the other access rights. (in terms of json allowed operations structure).


  Scenario Outline: Verify file upload permissions
    Given a file in the "<FileState>" state
    When a user with the role "<UserRole>"
    And the user attempts to upload a file
    Then the action outcome is "<Outcome>"

    Examples:
      | FileState           | UserRole          | Outcome     |
      | PendingOpenFile     | Publication owner | Allowed     |
      | PendingOpenFile     | Contributor       | Allowed     |
      | PendingOpenFile     | File curator      | Allowed     |
      | PendingOpenFile     | Everyone else     | Not Allowed |
      | PendingInternalFile | Publication owner | Allowed     |
      | PendingInternalFile | Contributor       | Allowed     |
      | PendingInternalFile | File curator      | Allowed     |
      | PendingInternalFile | Everyone else     | Not Allowed |
      | OpenFile            | Publication owner | Not Allowed |
      | OpenFile            | Contributor       | Not Allowed |
      | OpenFile            | File curator      | Not Allowed |
      | OpenFile            | Everyone else     | Not Allowed |
      | InternalFile        | Publication owner | Not Allowed |
      | InternalFile        | Contributor       | Not Allowed |
      | InternalFile        | File curator      | Not Allowed |
      | InternalFile        | Everyone else     | Not Allowed |
      | HiddenFile          | Publication owner | Not Allowed |
      | HiddenFile          | Contributor       | Not Allowed |
      | HiddenFile          | File curator      | Allowed     |
      | HiddenFile          | Everyone else     | Not Allowed |
