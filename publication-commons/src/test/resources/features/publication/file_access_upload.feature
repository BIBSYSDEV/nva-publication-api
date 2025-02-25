Feature: File upload permissions
  As a system user
  I want publication permission to be enforced based on publication and user role
  So that only authorized users can upload the files

  Background:
  This will be exposed on publication level, not file level like the other access rights. (in terms of json allowed operations structure).


  Scenario Outline: Verify file upload permissions
    Given a "<PublicationStatus>" publication
    When the user have the role "<UserRole>"
    And the user attempts to "upload-file"
    Then the action outcome is "<Outcome>"

    Examples:
      | UserRole                             | PublicationStatus | Outcome     |
      | Publication owner                    | draft             | Allowed     |
      | Contributor                          | draft             | Allowed     |
      | File curators for other contributors | draft             | Allowed     |
      | Everyone else                        | draft             | Not Allowed |
      | External client                      | draft             | Not Allowed |
      | Publication owner                    | published         | Allowed     |
      | Contributor                          | published         | Allowed     |
      | File curators for other contributors | published         | Allowed     |
      | Everyone else                        | published         | Not Allowed |
      | External client                      | published         | Not Allowed |
      | Publication owner                    | unpublished       | Allowed     |
      | Contributor                          | unpublished       | Allowed     |
      | File curators for other contributors | unpublished       | Allowed     |
      | Everyone else                        | unpublished       | Not Allowed |
      | External client                      | unpublished       | Not Allowed |
      | Publication owner                    | deleted           | Not Allowed |
      | Contributor                          | deleted           | Not Allowed |
      | File curators for other contributors | deleted           | Not Allowed |
      | Everyone else                        | deleted           | Not Allowed |
      | External client                      | deleted           | Not Allowed |