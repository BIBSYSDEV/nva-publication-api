Feature: File metadata write and file delete permissions
  As a system user
  I want file metadata write (editing and deletion) to have clear permissions
  So that only authorized users can perform those actions based on the file state

  Background:
  The publication metadata is published. X is an affiliation and this affiliation is given by your login context.

  Scenario Outline: Verify file write permissions
    Given a file of type "<FileType>"
    When the user have the role "<UserRole>"
    And the user attempts to "write-metadata"
    Then the action outcome is "<Outcome>"



    Examples:
      | FileType            | UserRole                             | Outcome     |
      | PendingOpenFile     | Uploader at X                        | Allowed     |
      #| PendingOpenFile     | Contributor at X                     | Not Allowed |
      #| PendingOpenFile     | Other contributors                   | Not Allowed |
      | PendingOpenFile     | File curator at X                    | Allowed     |
      | PendingOpenFile     | File curators for other contributors | Not Allowed |
      | PendingOpenFile     | Everyone else                        | Not Allowed |
      | PendingInternalFile | Uploader at X                        | Allowed     |
      #| PendingInternalFile | Contributor at X                     | Not Allowed |
      #| PendingInternalFile | Other contributors                   | Not Allowed |
      | PendingInternalFile | File curator at X                    | Allowed     |
      | PendingInternalFile | File curators for other contributors | Not Allowed |
      | PendingInternalFile | Everyone else                        | Not Allowed |
      | OpenFile            | Uploader at X                        | Not Allowed |
      | OpenFile            | Contributor at X                     | Not Allowed |
      | OpenFile            | Other contributors                   | Not Allowed |
      | OpenFile            | File curator at X                    | Allowed     |
      | OpenFile            | File curators for other contributors | Not Allowed |
      | OpenFile            | Everyone else                        | Not Allowed |
      | InternalFile        | Uploader at X                        | Not Allowed |
      #| InternalFile        | Contributor at X                     | Not Allowed |
      #| InternalFile        | Other contributors                   | Not Allowed |
      | InternalFile        | File curator at X                    | Allowed     |
      | InternalFile        | File curators for other contributors | Not Allowed |
      | InternalFile        | Everyone else                        | Not Allowed |
      | HiddenFile          | Uploader at X                        | Not Allowed |
      #| HiddenFile          | Contributor at X                     | Not Allowed |
      #| HiddenFile          | Other contributors                   | Not Allowed |
      | HiddenFile          | File curator at X                    | Allowed     |
      | HiddenFile          | File curators for other contributors | Not Allowed |
      | HiddenFile          | Everyone else                        | Not Allowed |
