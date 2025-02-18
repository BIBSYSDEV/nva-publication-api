Feature: File upload permissions
  As a system user
  I want publication permission to be enforced based on publication and user role
  So that only authorized users can upload the files

  Background:
  This will be exposed on publication level, not file level like the other access rights. (in terms of json allowed operations structure).


  Scenario Outline: Verify file upload permissions
    Given a file of type "<FileType>"
    When the user have the role "<UserRole>"
    And the user attempts to "upload-file"
    Then the action outcome is "<Outcome>"

    Examples:
      | FileType     | UserRole                             | Outcome     |
      | UploadedFile | Publication owner                    | Allowed     |
      | UploadedFile | Contributor                          | Allowed     |
      | UploadedFile | File curators for other contributors | Allowed     |
      | UploadedFile | Everyone else                        | Not Allowed |
      | UploadedFile | External client                      | Not Allowed |