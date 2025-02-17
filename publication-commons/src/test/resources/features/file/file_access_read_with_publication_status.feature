Feature: File metadata read and file download permissions with different publication statuses
  As an unprivileged user
  I want to be able to access file metadata and file download when not logged in or no rights to the file

  Scenario Outline: Verify file metadata read permissions
    Given a file of type "<FileType>" and publication status "<PublicationStatus>"
    When the user have the role "<UserRole>"
    And the user attempts to "read-metadata"
    Then the action outcome is "<Outcome>"

    Examples:
      | FileType            | UserRole      | PublicationStatus | Outcome     |
      | UploadedFile        | Everyone else | PUBLISHED         | Not Allowed |
      | UploadedFile        | Everyone else | DRAFT             | Not Allowed |
      | PendingOpenFile     | Everyone else | PUBLISHED         | Not Allowed |
      | PendingOpenFile     | Everyone else | DRAFT             | Not Allowed |
      | PendingInternalFile | Everyone else | PUBLISHED         | Not Allowed |
      | PendingInternalFile | Everyone else | DRAFT             | Not Allowed |
      | OpenFile            | Everyone else | PUBLISHED         | Allowed     |
      | OpenFile            | Everyone else | DRAFT             | Not Allowed |
      | InternalFile        | Everyone else | PUBLISHED         | Not Allowed |
      | InternalFile        | Everyone else | DRAFT             | Not Allowed |
      | HiddenFile          | Everyone else | PUBLISHED         | Not Allowed |
      | HiddenFile          | Everyone else | DRAFT             | Not Allowed |
