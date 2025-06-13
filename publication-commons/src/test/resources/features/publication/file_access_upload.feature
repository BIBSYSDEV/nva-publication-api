Feature: File upload permissions
  As a system user
  I want publication permission to be enforced based on publication and user role
  So that only authorized users can upload the files

  Background:
  This will be exposed on publication level, not file level like the other access rights. (in terms of json allowed operations structure).

  Scenario Outline: Verify file upload permissions
    Given a "publication"
    And publication has status "<PublicationStatus>"
    When the user have the role "<UserRole>"
    And the user attempts to "upload-file"
    Then the action outcome is "<Outcome>"

    Examples:
      | UserRole                    | PublicationStatus | Outcome     |
      | Unauthenticated             | draft             | Not Allowed |
      | Authenticated               | draft             | Not Allowed |
      | Publication creator         | draft             | Allowed     |
      | Contributor                 | draft             | Allowed     |
      | Publishing curator          | draft             | Allowed     |
      | NVI curator                 | draft             | Allowed     |
      | DOI curator                 | draft             | Allowed     |
      | Support curator             | draft             | Allowed     |
      | Thesis curator              | draft             | Allowed     |
      | Embargo thesis curator      | draft             | Allowed     |
      | Editor                      | draft             | Not Allowed |
      | Related external client     | draft             | Allowed     |
      | Not related external client | draft             | Not Allowed |

      | Unauthenticated             | published         | Not Allowed |
      | Authenticated               | published         | Not Allowed |
      | Publication creator         | published         | Allowed     |
      | Contributor                 | published         | Allowed     |
      | NVI curator                 | published         | Allowed     |
      | DOI curator                 | published         | Allowed     |
      | Support curator             | published         | Allowed     |
      | Thesis curator              | published         | Allowed     |
      | Embargo thesis curator      | published         | Allowed     |
      | Editor                      | published         | Not Allowed |
      | Related external client     | published         | Allowed     |
      | Not related external client | published         | Not Allowed |

      | Unauthenticated             | unpublished       | Not Allowed |
      | Authenticated               | unpublished       | Not Allowed |
      | Publication creator         | unpublished       | Allowed     |
      | Contributor                 | unpublished       | Allowed     |
      | NVI curator                 | unpublished       | Allowed     |
      | DOI curator                 | unpublished       | Allowed     |
      | Support curator             | unpublished       | Allowed     |
      | Thesis curator              | unpublished       | Allowed     |
      | Embargo thesis curator      | unpublished       | Allowed     |
      | Editor                      | unpublished       | Not Allowed |
      | Related external client     | unpublished       | Allowed     |
      | Not related external client | unpublished       | Not Allowed |

      | Unauthenticated             | deleted           | Not Allowed |
      | Authenticated               | deleted           | Not Allowed |
      | Publication creator         | deleted           | Not Allowed |
      | Contributor                 | deleted           | Not Allowed |
      | NVI curator                 | deleted           | Not Allowed |
      | DOI curator                 | deleted           | Not Allowed |
      | Support curator             | deleted           | Not Allowed |
      | Thesis curator              | deleted           | Not Allowed |
      | Embargo thesis curator      | deleted           | Not Allowed |
      | Editor                      | deleted           | Not Allowed |
      | Related external client     | deleted           | Not Allowed |
      | Not related external client | deleted           | Not Allowed |
