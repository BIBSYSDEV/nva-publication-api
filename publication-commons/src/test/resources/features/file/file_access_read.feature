Feature: File metadata read and file download permissions
  As a system user
  I want file metadata read and file download permissions to be enforced based on file state and user role
  So that only authorized users can read the metadata

  Scenario Outline: Verify file metadata read permissions
    Given a file of type "<FileType>"
    When the user have the role "<UserRole>"
    And the user attempts to "read-metadata"
    Then the action outcome is "<Outcome>"

    Examples:
      | FileType            | UserRole                             | Outcome     |
      | UploadedFile        | Uploader at X                        | Allowed     |
      | UploadedFile        | Contributor at X                     | Not Allowed |
      | UploadedFile        | Other contributors                   | Not Allowed |
      | UploadedFile        | File curator at X                    | Allowed     |
      | UploadedFile        | File curators for other contributors | Not Allowed |
      | UploadedFile        | Everyone else                        | Not Allowed |
      | UploadedFile        | External client                      | Not Allowed |
      | PendingOpenFile     | Uploader at X                        | Allowed     |
      | PendingOpenFile     | Contributor at X                     | Allowed     |
      | PendingOpenFile     | Other contributors                   | Allowed     |
      | PendingOpenFile     | File curator at X                    | Allowed     |
      | PendingOpenFile     | File curators for other contributors | Allowed     |
      | PendingOpenFile     | Everyone else                        | Not Allowed |
      | PendingOpenFile     | External client                      | Not Allowed |
      | PendingInternalFile | Uploader at X                        | Allowed     |
      | PendingInternalFile | Contributor at X                     | Allowed     |
      | PendingInternalFile | Other contributors                   | Allowed     |
      | PendingInternalFile | File curator at X                    | Allowed     |
      | PendingInternalFile | File curators for other contributors | Allowed     |
      | PendingInternalFile | Everyone else                        | Not Allowed |
      | PendingInternalFile | External client                      | Not Allowed |
      | OpenFile            | Uploader at X                        | Allowed     |
      | OpenFile            | Contributor at X                     | Allowed     |
      | OpenFile            | Other contributors                   | Allowed     |
      | OpenFile            | File curator at X                    | Allowed     |
      | OpenFile            | File curators for other contributors | Allowed     |
      | OpenFile            | Everyone else                        | Allowed     |
      | OpenFile            | External client                      | Allowed     |
      | InternalFile        | Uploader at X                        | Allowed     |
      | InternalFile        | Contributor at X                     | Allowed     |
      | InternalFile        | Other contributors                   | Allowed     |
      | InternalFile        | File curator at X                    | Allowed     |
      | InternalFile        | File curators for other contributors | Allowed     |
      | InternalFile        | Everyone else                        | Not Allowed |
      | InternalFile        | External client                      | Allowed     |
      | HiddenFile          | Uploader at X                        | Not Allowed |
      | HiddenFile          | Contributor at X                     | Not Allowed |
      | HiddenFile          | Other contributors                   | Not Allowed |
      | HiddenFile          | File curator at X                    | Allowed     |
      | HiddenFile          | File curators for other contributors | Not Allowed |
      | HiddenFile          | Everyone else                        | Not Allowed |
      | HiddenFile          | External client                      | Not Allowed |
