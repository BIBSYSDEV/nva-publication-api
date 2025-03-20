@debug
Feature: File metadata write and file delete permissions
  As a system user
  I want file metadata write (editing and deletion) to have clear permissions
  So that only authorized users can perform those actions based on the file state

  Background:
  The publication metadata is published. X is an affiliation and this affiliation is given by your login context.

  Scenario Outline: Verify file write permissions
    Given a file of type "<FileType>"
    When the user have the role "<CurrentUserRole>"
    And the file is owned by "Publication owner"
    And the user attempts to "write-metadata"
    Then the action outcome is "<Outcome>"



    Examples:
      | FileType            | CurrentUserRole                        | Outcome     |
      | UploadedFile        | Publication owner at X                 | Allowed     |
      | UploadedFile        | Contributor at X                       | Not Allowed |
      | UploadedFile        | Other contributors                     | Not Allowed |
      | UploadedFile        | File curator at X                      | Allowed     |
      | UploadedFile        | File curators for other contributors   | Not Allowed |
      | UploadedFile        | File curator by publication owner at X | Allowed     |
      | UploadedFile        | Everyone else                          | Not Allowed |
      | UploadedFile        | External client                        | Not Allowed |
      | PendingOpenFile     | Publication owner at X                 | Allowed     |
      | PendingOpenFile     | Contributor at X                       | Not Allowed |
      | PendingOpenFile     | Other contributors                     | Not Allowed |
      | PendingOpenFile     | File curator at X                      | Allowed     |
      | PendingOpenFile     | File curators for other contributors   | Not Allowed |
      | PendingOpenFile     | File curator by publication owner at X | Allowed     |
      | PendingOpenFile     | Everyone else                          | Not Allowed |
      | PendingOpenFile     | External client                        | Not Allowed |
      | PendingInternalFile | Publication owner at X                 | Allowed     |
      | PendingInternalFile | Contributor at X                       | Not Allowed |
      | PendingInternalFile | Other contributors                     | Not Allowed |
      | PendingInternalFile | File curator at X                      | Allowed     |
      | PendingInternalFile | File curators for other contributors   | Not Allowed |
      | PendingInternalFile | File curator by publication owner at X | Allowed     |
      | PendingInternalFile | Everyone else                          | Not Allowed |
      | PendingInternalFile | External client                        | Not Allowed |
      | OpenFile            | Publication owner at X                 | Not Allowed |
      | OpenFile            | Contributor at X                       | Not Allowed |
      | OpenFile            | Other contributors                     | Not Allowed |
      | OpenFile            | File curator at X                      | Allowed     |
      | OpenFile            | File curators for other contributors   | Not Allowed |
      | OpenFile            | File curator by publication owner at X | Allowed     |
      | OpenFile            | Everyone else                          | Not Allowed |
      | OpenFile            | External client                        | Allowed     |
      | InternalFile        | Publication owner at X                 | Not Allowed |
      | InternalFile        | Contributor at X                       | Not Allowed |
      | InternalFile        | Other contributors                     | Not Allowed |
      | InternalFile        | File curator at X                      | Allowed     |
      | InternalFile        | File curators for other contributors   | Not Allowed |
      | InternalFile        | File curator by publication owner at X | Allowed     |
      | InternalFile        | Everyone else                          | Not Allowed |
      | InternalFile        | External client                        | Allowed     |
      | HiddenFile          | Publication owner at X                 | Not Allowed |
      | HiddenFile          | Contributor at X                       | Not Allowed |
      | HiddenFile          | Other contributors                     | Not Allowed |
      | HiddenFile          | File curator at X                      | Allowed     |
      | HiddenFile          | File curators for other contributors   | Not Allowed |
      | HiddenFile          | File curator by publication owner at X | Allowed     |
      | HiddenFile          | Everyone else                          | Not Allowed |
      | HiddenFile          | External client                        | Not Allowed |
