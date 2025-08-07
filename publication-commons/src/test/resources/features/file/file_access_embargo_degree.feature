Feature: File permissions for embargo and degree files
  As a system user
  I want file operations permissions (read-metadata, download, write-metadata, delete) to be enforced based on file properties and user roles
  So that only authorized users can perform operations on files

  Scenario Outline: Verify file operation permissions for OpenFile on Degree when user is from file owners institution
    Given a file of type "OpenFile"
    And the file is owned by "publication creator"
    And publication is of type "degree"
    When the user have the role "<UserRole>"
    And the user belongs to "creating institution"
    And the user attempts to "<Operation>"
    Then the action outcome is "<Outcome>"

    Examples:
      | UserRole                    | Operation      | Outcome     |
      | Unauthenticated             | read-metadata  | Allowed     |
      | Unauthenticated             | download       | Allowed     |
      | Unauthenticated             | write-metadata | Not Allowed |
      | Unauthenticated             | delete         | Not Allowed |

      | Authenticated               | read-metadata  | Allowed     |
      | Authenticated               | download       | Allowed     |
      | Authenticated               | write-metadata | Not Allowed |
      | Authenticated               | delete         | Not Allowed |

      | Publication creator         | read-metadata  | Allowed     |
      | Publication creator         | download       | Allowed     |
      | Publication creator         | write-metadata | Not Allowed |
      | Publication creator         | delete         | Not Allowed |

      | Contributor                 | read-metadata  | Allowed     |
      | Contributor                 | download       | Allowed     |
      | Contributor                 | write-metadata | Not Allowed |
      | Contributor                 | delete         | Not Allowed |

      | Publishing curator          | read-metadata  | Allowed     |
      | Publishing curator          | download       | Allowed     |
      | Publishing curator          | write-metadata | Not Allowed |
      | Publishing curator          | delete         | Not Allowed |

      | Thesis curator              | read-metadata  | Allowed     |
      | Thesis curator              | download       | Allowed     |
      | Thesis curator              | write-metadata | Allowed     |
      | Thesis curator              | delete         | Allowed     |

      | Embargo thesis curator      | read-metadata  | Allowed     |
      | Embargo thesis curator      | download       | Allowed     |
      | Embargo thesis curator      | write-metadata | Not Allowed |
      | Embargo thesis curator      | delete         | Not Allowed |

      | Related external client     | read-metadata  | Allowed     |
      | Related external client     | download       | Allowed     |
      | Related external client     | write-metadata | Allowed     |
      | Related external client     | delete         | Allowed     |

      | Not related external client | read-metadata  | Allowed     |
      | Not related external client | download       | Allowed     |
      | Not related external client | write-metadata | Not Allowed |
      | Not related external client | delete         | Not Allowed |

  Scenario Outline: Verify file operation permissions for OpenFile on Degree when user is not from file owners institution
    Given a file of type "OpenFile"
    And the file is owned by "publication creator"
    And publication is of type "degree"
    When the user have the role "<UserRole>"
    And the user belongs to "curating institution"
    And the user attempts to "<Operation>"
    Then the action outcome is "<Outcome>"

    Examples:
      | UserRole               | Operation      | Outcome     |
      | Contributor            | read-metadata  | Allowed     |
      | Contributor            | download       | Allowed     |
      | Contributor            | write-metadata | Not Allowed |
      | Contributor            | delete         | Not Allowed |

      | Publishing curator     | read-metadata  | Allowed     |
      | Publishing curator     | download       | Allowed     |
      | Publishing curator     | write-metadata | Not Allowed |
      | Publishing curator     | delete         | Not Allowed |

      | Thesis curator         | read-metadata  | Allowed     |
      | Thesis curator         | download       | Allowed     |
      | Thesis curator         | write-metadata | Not Allowed |
      | Thesis curator         | delete         | Not Allowed |

      | Embargo thesis curator | read-metadata  | Allowed     |
      | Embargo thesis curator | download       | Allowed     |
      | Embargo thesis curator | write-metadata | Not Allowed |
      | Embargo thesis curator | delete         | Not Allowed |

  Scenario Outline: Verify embargo file operation permissions for OpenFile when user is from file owners institution
    Given a file of type "OpenFile"
    And the file is owned by "publication creator"
    And the file has embargo
    When the user have the role "<UserRole>"
    And the user belongs to "creating institution"
    And the user attempts to "<Operation>"
    Then the action outcome is "<Outcome>"

    Examples:
      | UserRole                    | Operation      | Outcome     |
      | Unauthenticated             | read-metadata  | Allowed     |
      | Unauthenticated             | download       | Not Allowed |
      | Unauthenticated             | write-metadata | Not Allowed |
      | Unauthenticated             | delete         | Not Allowed |

      | Authenticated               | read-metadata  | Allowed     |
      | Authenticated               | download       | Not Allowed |
      | Authenticated               | write-metadata | Not Allowed |
      | Authenticated               | delete         | Not Allowed |

      | Publication creator         | read-metadata  | Allowed     |
      | Publication creator         | download       | Not Allowed |
      | Publication creator         | write-metadata | Not Allowed |
      | Publication creator         | delete         | Not Allowed |

      | Contributor                 | read-metadata  | Allowed     |
      | Contributor                 | download       | Not Allowed |
      | Contributor                 | write-metadata | Not Allowed |
      | Contributor                 | delete         | Not Allowed |

      | Publishing curator          | read-metadata  | Allowed     |
      | Publishing curator          | download       | Allowed     |
      | Publishing curator          | write-metadata | Allowed     |
      | Publishing curator          | delete         | Allowed     |

      | Thesis curator              | read-metadata  | Allowed     |
      | Thesis curator              | download       | Not Allowed |
      | Thesis curator              | write-metadata | Not Allowed |
      | Thesis curator              | delete         | Not Allowed |

      | Embargo thesis curator      | read-metadata  | Allowed     |
      | Embargo thesis curator      | download       | Not Allowed |
      | Embargo thesis curator      | write-metadata | Not Allowed |
      | Embargo thesis curator      | delete         | Not Allowed |

      | Related external client     | read-metadata  | Allowed     |
      | Related external client     | download       | Allowed     |
      | Related external client     | write-metadata | Allowed     |
      | Related external client     | delete         | Allowed     |

      | Not related external client | read-metadata  | Allowed     |
      | Not related external client | download       | Not Allowed |
      | Not related external client | write-metadata | Not Allowed |
      | Not related external client | delete         | Not Allowed |

  Scenario Outline: Verify embargo file operation permissions for OpenFile when user is not from file owners institution
    Given a file of type "OpenFile"
    And the file is owned by "publication creator"
    And the file has embargo
    When the user have the role "<UserRole>"
    And the user belongs to "curating institution"
    And the user attempts to "<Operation>"
    Then the action outcome is "<Outcome>"

    Examples:
      | UserRole               | Operation      | Outcome     |
      | Contributor            | read-metadata  | Allowed     |
      | Contributor            | download       | Not Allowed |
      | Contributor            | write-metadata | Not Allowed |
      | Contributor            | delete         | Not Allowed |

      | Publishing curator     | read-metadata  | Allowed     |
      | Publishing curator     | download       | Allowed     |
      | Publishing curator     | write-metadata | Not Allowed |
      | Publishing curator     | delete         | Not Allowed |

      | Thesis curator         | read-metadata  | Allowed     |
      | Thesis curator         | download       | Not Allowed |
      | Thesis curator         | write-metadata | Not Allowed |
      | Thesis curator         | delete         | Not Allowed |

      | Embargo thesis curator | read-metadata  | Allowed     |
      | Embargo thesis curator | download       | Not Allowed |
      | Embargo thesis curator | write-metadata | Not Allowed |
      | Embargo thesis curator | delete         | Not Allowed |

  Scenario Outline: Verify embargo file operation permissions for OpenFile on Degree when user is from file owners institution
    Given a file of type "OpenFile"
    And the file is owned by "publication creator"
    And the file has embargo
    And publication is of type "degree"
    When the user have the role "<UserRole>"
    And the user attempts to "<Operation>"
    Then the action outcome is "<Outcome>"

    Examples:
      | UserRole                    | Operation      | Outcome     |
      | Unauthenticated             | read-metadata  | Allowed     |
      | Unauthenticated             | download       | Not Allowed |
      | Unauthenticated             | write-metadata | Not Allowed |
      | Unauthenticated             | delete         | Not Allowed |

      | Authenticated               | read-metadata  | Allowed     |
      | Authenticated               | download       | Not Allowed |
      | Authenticated               | write-metadata | Not Allowed |
      | Authenticated               | delete         | Not Allowed |

      | Publication creator         | read-metadata  | Allowed     |
      | Publication creator         | download       | Not Allowed |
      | Publication creator         | write-metadata | Not Allowed |
      | Publication creator         | delete         | Not Allowed |

      | Contributor                 | read-metadata  | Allowed     |
      | Contributor                 | download       | Not Allowed |
      | Contributor                 | write-metadata | Not Allowed |
      | Contributor                 | delete         | Not Allowed |

      | Publishing curator          | read-metadata  | Allowed     |
      | Publishing curator          | download       | Not Allowed |
      | Publishing curator          | write-metadata | Not Allowed |
      | Publishing curator          | delete         | Not Allowed |

      | Thesis curator              | read-metadata  | Allowed     |
      | Thesis curator              | download       | Not Allowed |
      | Thesis curator              | write-metadata | Not Allowed |
      | Thesis curator              | delete         | Not Allowed |

      | Related external client     | read-metadata  | Allowed     |
      | Related external client     | download       | Allowed     |
      | Related external client     | write-metadata | Allowed     |
      | Related external client     | delete         | Allowed     |

      | Not related external client | read-metadata  | Allowed     |
      | Not related external client | download       | Not Allowed |
      | Not related external client | write-metadata | Not Allowed |
      | Not related external client | delete         | Not Allowed |

  Scenario Outline: Verify embargo file operation permissions for OpenFile on Degree when user is not from file owners institution
    Given a file of type "OpenFile"
    And the file is owned by "publication creator"
    And the file has embargo
    And publication is of type "degree"
    When the user have the role "<UserRole>"
    And the user belongs to "curating institution"
    And the user attempts to "<Operation>"
    Then the action outcome is "<Outcome>"

    Examples:
      | UserRole               | Operation      | Outcome     |
      | Contributor            | read-metadata  | Allowed     |
      | Contributor            | download       | Not Allowed |
      | Contributor            | write-metadata | Not Allowed |
      | Contributor            | delete         | Not Allowed |

      | Publishing curator     | read-metadata  | Allowed     |
      | Publishing curator     | download       | Not Allowed |
      | Publishing curator     | write-metadata | Not Allowed |
      | Publishing curator     | delete         | Not Allowed |

      | Thesis curator         | read-metadata  | Allowed     |
      | Thesis curator         | download       | Not Allowed |
      | Thesis curator         | write-metadata | Not Allowed |
      | Thesis curator         | delete         | Not Allowed |

      | Embargo thesis curator | read-metadata  | Allowed     |
      | Embargo thesis curator | download       | Not Allowed |
      | Embargo thesis curator | write-metadata | Not Allowed |
      | Embargo thesis curator | delete         | Not Allowed |

    # TODO: Should probably add test for when user is not from file owners institution as well?
  Scenario Outline: Verify embargo file operation permissions on Degree when user is not from publication channel owner organization
    Given a file of type "<FileType>"
    And the file is owned by "publication creator"
    And the file has embargo
    And publication is of type "degree"
    And publication has publisher claimed by "not users institution"
    When the user have the role "<UserRole>"
    And the user belongs to "creating institution"
    And the user attempts to "<Operation>"
    Then the action outcome is "<Outcome>"

    Examples:
      | FileType            | UserRole                    | Operation      | Outcome     |
      | PendingOpenFile     | Unauthenticated             | read-metadata  | Not Allowed |
      | PendingOpenFile     | Unauthenticated             | download       | Not Allowed |
      | PendingOpenFile     | Unauthenticated             | write-metadata | Not Allowed |
      | PendingOpenFile     | Unauthenticated             | delete         | Not Allowed |
      | PendingInternalFile | Unauthenticated             | read-metadata  | Not Allowed |
      | PendingInternalFile | Unauthenticated             | download       | Not Allowed |
      | PendingInternalFile | Unauthenticated             | write-metadata | Not Allowed |
      | PendingInternalFile | Unauthenticated             | delete         | Not Allowed |
      | OpenFile            | Unauthenticated             | read-metadata  | Allowed     |
      | OpenFile            | Unauthenticated             | download       | Not Allowed |
      | OpenFile            | Unauthenticated             | write-metadata | Not Allowed |
      | OpenFile            | Unauthenticated             | delete         | Not Allowed |
      | InternalFile        | Unauthenticated             | read-metadata  | Not Allowed |
      | InternalFile        | Unauthenticated             | download       | Not Allowed |
      | InternalFile        | Unauthenticated             | write-metadata | Not Allowed |
      | InternalFile        | Unauthenticated             | delete         | Not Allowed |
      | HiddenFile          | Unauthenticated             | read-metadata  | Not Allowed |
      | HiddenFile          | Unauthenticated             | download       | Not Allowed |
      | HiddenFile          | Unauthenticated             | write-metadata | Not Allowed |
      | HiddenFile          | Unauthenticated             | delete         | Not Allowed |

      | PendingOpenFile     | Authenticated               | read-metadata  | Not Allowed |
      | PendingOpenFile     | Authenticated               | download       | Not Allowed |
      | PendingOpenFile     | Authenticated               | write-metadata | Not Allowed |
      | PendingOpenFile     | Authenticated               | delete         | Not Allowed |
      | PendingInternalFile | Authenticated               | read-metadata  | Not Allowed |
      | PendingInternalFile | Authenticated               | download       | Not Allowed |
      | PendingInternalFile | Authenticated               | write-metadata | Not Allowed |
      | PendingInternalFile | Authenticated               | delete         | Not Allowed |
      | OpenFile            | Authenticated               | read-metadata  | Allowed     |
      | OpenFile            | Authenticated               | download       | Not Allowed |
      | OpenFile            | Authenticated               | write-metadata | Not Allowed |
      | OpenFile            | Authenticated               | delete         | Not Allowed |
      | InternalFile        | Authenticated               | read-metadata  | Not Allowed |
      | InternalFile        | Authenticated               | download       | Not Allowed |
      | InternalFile        | Authenticated               | write-metadata | Not Allowed |
      | InternalFile        | Authenticated               | delete         | Not Allowed |
      | HiddenFile          | Authenticated               | read-metadata  | Not Allowed |
      | HiddenFile          | Authenticated               | download       | Not Allowed |
      | HiddenFile          | Authenticated               | write-metadata | Not Allowed |
      | HiddenFile          | Authenticated               | delete         | Not Allowed |

      | PendingOpenFile     | Publication creator         | read-metadata  | Allowed     |
      | PendingOpenFile     | Publication creator         | download       | Allowed     |
      | PendingOpenFile     | Publication creator         | write-metadata | Allowed     |
      | PendingOpenFile     | Publication creator         | delete         | Allowed     |
      | PendingInternalFile | Publication creator         | read-metadata  | Allowed     |
      | PendingInternalFile | Publication creator         | download       | Allowed     |
      | PendingInternalFile | Publication creator         | write-metadata | Allowed     |
      | PendingInternalFile | Publication creator         | delete         | Allowed     |
      | OpenFile            | Publication creator         | read-metadata  | Allowed     |
      | OpenFile            | Publication creator         | download       | Not Allowed |
      | OpenFile            | Publication creator         | write-metadata | Not Allowed |
      | OpenFile            | Publication creator         | delete         | Not Allowed |
      | InternalFile        | Publication creator         | read-metadata  | Allowed     |
      | InternalFile        | Publication creator         | download       | Not Allowed |
      | InternalFile        | Publication creator         | write-metadata | Not Allowed |
      | InternalFile        | Publication creator         | delete         | Not Allowed |
      | HiddenFile          | Publication creator         | read-metadata  | Not Allowed |
      | HiddenFile          | Publication creator         | download       | Not Allowed |
      | HiddenFile          | Publication creator         | write-metadata | Not Allowed |
      | HiddenFile          | Publication creator         | delete         | Not Allowed |

      | PendingOpenFile     | Contributor                 | read-metadata  | Allowed     |
      | PendingOpenFile     | Contributor                 | download       | Not Allowed |
      | PendingOpenFile     | Contributor                 | write-metadata | Not Allowed |
      | PendingOpenFile     | Contributor                 | delete         | Not Allowed |
      | PendingInternalFile | Contributor                 | read-metadata  | Allowed     |
      | PendingInternalFile | Contributor                 | download       | Not Allowed |
      | PendingInternalFile | Contributor                 | write-metadata | Not Allowed |
      | PendingInternalFile | Contributor                 | delete         | Not Allowed |
      | OpenFile            | Contributor                 | read-metadata  | Allowed     |
      | OpenFile            | Contributor                 | download       | Not Allowed |
      | OpenFile            | Contributor                 | write-metadata | Not Allowed |
      | OpenFile            | Contributor                 | delete         | Not Allowed |
      | InternalFile        | Contributor                 | read-metadata  | Allowed     |
      | InternalFile        | Contributor                 | download       | Not Allowed |
      | InternalFile        | Contributor                 | write-metadata | Not Allowed |
      | InternalFile        | Contributor                 | delete         | Not Allowed |
      | HiddenFile          | Contributor                 | read-metadata  | Not Allowed |
      | HiddenFile          | Contributor                 | download       | Not Allowed |
      | HiddenFile          | Contributor                 | write-metadata | Not Allowed |
      | HiddenFile          | Contributor                 | delete         | Not Allowed |

      | PendingOpenFile     | Publishing curator          | read-metadata  | Allowed     |
      | PendingOpenFile     | Publishing curator          | download       | Not Allowed |
      | PendingOpenFile     | Publishing curator          | write-metadata | Not Allowed |
      | PendingOpenFile     | Publishing curator          | delete         | Not Allowed |
      | PendingInternalFile | Publishing curator          | read-metadata  | Allowed     |
      | PendingInternalFile | Publishing curator          | download       | Not Allowed |
      | PendingInternalFile | Publishing curator          | write-metadata | Not Allowed |
      | PendingInternalFile | Publishing curator          | delete         | Not Allowed |
      | OpenFile            | Publishing curator          | read-metadata  | Allowed     |
      | OpenFile            | Publishing curator          | download       | Not Allowed |
      | OpenFile            | Publishing curator          | write-metadata | Not Allowed |
      | OpenFile            | Publishing curator          | delete         | Not Allowed |
      | InternalFile        | Publishing curator          | read-metadata  | Allowed     |
      | InternalFile        | Publishing curator          | download       | Not Allowed |
      | InternalFile        | Publishing curator          | write-metadata | Not Allowed |
      | InternalFile        | Publishing curator          | delete         | Not Allowed |
      | HiddenFile          | Publishing curator          | read-metadata  | Not Allowed |
      | HiddenFile          | Publishing curator          | download       | Not Allowed |
      | HiddenFile          | Publishing curator          | write-metadata | Not Allowed |
      | HiddenFile          | Publishing curator          | delete         | Not Allowed |

      | PendingOpenFile     | Thesis curator              | read-metadata  | Allowed     |
      | PendingOpenFile     | Thesis curator              | download       | Not Allowed |
      | PendingOpenFile     | Thesis curator              | write-metadata | Not Allowed |
      | PendingOpenFile     | Thesis curator              | delete         | Not Allowed |
      | PendingInternalFile | Thesis curator              | read-metadata  | Allowed     |
      | PendingInternalFile | Thesis curator              | download       | Not Allowed |
      | PendingInternalFile | Thesis curator              | write-metadata | Not Allowed |
      | PendingInternalFile | Thesis curator              | delete         | Not Allowed |
      | OpenFile            | Thesis curator              | read-metadata  | Allowed     |
      | OpenFile            | Thesis curator              | download       | Not Allowed |
      | OpenFile            | Thesis curator              | write-metadata | Not Allowed |
      | OpenFile            | Thesis curator              | delete         | Not Allowed |
      | InternalFile        | Thesis curator              | read-metadata  | Allowed     |
      | InternalFile        | Thesis curator              | download       | Not Allowed |
      | InternalFile        | Thesis curator              | write-metadata | Not Allowed |
      | InternalFile        | Thesis curator              | delete         | Not Allowed |
      | HiddenFile          | Thesis curator              | read-metadata  | Not Allowed |
      | HiddenFile          | Thesis curator              | download       | Not Allowed |
      | HiddenFile          | Thesis curator              | write-metadata | Not Allowed |
      | HiddenFile          | Thesis curator              | delete         | Not Allowed |

      | PendingOpenFile     | Support curator             | read-metadata  | Allowed     |
      | PendingOpenFile     | Support curator             | download       | Not Allowed |
      | PendingOpenFile     | Support curator             | write-metadata | Not Allowed |
      | PendingOpenFile     | Support curator             | delete         | Not Allowed |
      | PendingInternalFile | Support curator             | read-metadata  | Allowed     |
      | PendingInternalFile | Support curator             | download       | Not Allowed |
      | PendingInternalFile | Support curator             | write-metadata | Not Allowed |
      | PendingInternalFile | Support curator             | delete         | Not Allowed |
      | OpenFile            | Support curator             | read-metadata  | Allowed     |
      | OpenFile            | Support curator             | download       | Not Allowed |
      | OpenFile            | Support curator             | write-metadata | Not Allowed |
      | OpenFile            | Support curator             | delete         | Not Allowed |
      | InternalFile        | Support curator             | read-metadata  | Allowed     |
      | InternalFile        | Support curator             | download       | Not Allowed |
      | InternalFile        | Support curator             | write-metadata | Not Allowed |
      | InternalFile        | Support curator             | delete         | Not Allowed |
      | HiddenFile          | Support curator             | read-metadata  | Not Allowed |
      | HiddenFile          | Support curator             | download       | Not Allowed |
      | HiddenFile          | Support curator             | write-metadata | Not Allowed |
      | HiddenFile          | Support curator             | delete         | Not Allowed |

      | PendingOpenFile     | Embargo thesis curator      | read-metadata  | Allowed     |
      | PendingOpenFile     | Embargo thesis curator      | download       | Not Allowed |
      | PendingOpenFile     | Embargo thesis curator      | write-metadata | Not Allowed |
      | PendingOpenFile     | Embargo thesis curator      | delete         | Not Allowed |
      | PendingInternalFile | Embargo thesis curator      | read-metadata  | Allowed     |
      | PendingInternalFile | Embargo thesis curator      | download       | Not Allowed |
      | PendingInternalFile | Embargo thesis curator      | write-metadata | Not Allowed |
      | PendingInternalFile | Embargo thesis curator      | delete         | Not Allowed |
      | OpenFile            | Embargo thesis curator      | read-metadata  | Allowed     |
      | OpenFile            | Embargo thesis curator      | download       | Not Allowed |
      | OpenFile            | Embargo thesis curator      | write-metadata | Not Allowed |
      | OpenFile            | Embargo thesis curator      | delete         | Not Allowed |
      | InternalFile        | Embargo thesis curator      | read-metadata  | Allowed     |
      | InternalFile        | Embargo thesis curator      | download       | Not Allowed |
      | InternalFile        | Embargo thesis curator      | write-metadata | Not Allowed |
      | InternalFile        | Embargo thesis curator      | delete         | Not Allowed |
      | HiddenFile          | Embargo thesis curator      | read-metadata  | Allowed     |
      | HiddenFile          | Embargo thesis curator      | download       | Not Allowed |
      | HiddenFile          | Embargo thesis curator      | write-metadata | Not Allowed |
      | HiddenFile          | Embargo thesis curator      | delete         | Not Allowed |

      | PendingOpenFile     | Related external client     | read-metadata  | Not Allowed |
      | PendingOpenFile     | Related external client     | download       | Not Allowed |
      | PendingOpenFile     | Related external client     | write-metadata | Not Allowed |
      | PendingOpenFile     | Related external client     | delete         | Not Allowed |
      | PendingInternalFile | Related external client     | read-metadata  | Not Allowed |
      | PendingInternalFile | Related external client     | download       | Not Allowed |
      | PendingInternalFile | Related external client     | write-metadata | Not Allowed |
      | PendingInternalFile | Related external client     | delete         | Not Allowed |
      | OpenFile            | Related external client     | read-metadata  | Allowed     |
      | OpenFile            | Related external client     | download       | Allowed     |
      | OpenFile            | Related external client     | write-metadata | Allowed     |
      | OpenFile            | Related external client     | delete         | Allowed     |
      | InternalFile        | Related external client     | read-metadata  | Allowed     |
      | InternalFile        | Related external client     | download       | Allowed     |
      | InternalFile        | Related external client     | write-metadata | Allowed     |
      | InternalFile        | Related external client     | delete         | Allowed     |
      | HiddenFile          | Related external client     | read-metadata  | Not Allowed |
      | HiddenFile          | Related external client     | download       | Not Allowed |
      | HiddenFile          | Related external client     | write-metadata | Not Allowed |
      | HiddenFile          | Related external client     | delete         | Not Allowed |

      | PendingOpenFile     | Not related external client | read-metadata  | Not Allowed |
      | PendingOpenFile     | Not related external client | download       | Not Allowed |
      | PendingOpenFile     | Not related external client | write-metadata | Not Allowed |
      | PendingOpenFile     | Not related external client | delete         | Not Allowed |
      | PendingInternalFile | Not related external client | read-metadata  | Not Allowed |
      | PendingInternalFile | Not related external client | download       | Not Allowed |
      | PendingInternalFile | Not related external client | write-metadata | Not Allowed |
      | PendingInternalFile | Not related external client | delete         | Not Allowed |
      | OpenFile            | Not related external client | read-metadata  | Allowed     |
      | OpenFile            | Not related external client | download       | Not Allowed |
      | OpenFile            | Not related external client | write-metadata | Not Allowed |
      | OpenFile            | Not related external client | delete         | Not Allowed |
      | InternalFile        | Not related external client | read-metadata  | Not Allowed |
      | InternalFile        | Not related external client | download       | Not Allowed |
      | InternalFile        | Not related external client | write-metadata | Not Allowed |
      | InternalFile        | Not related external client | delete         | Not Allowed |
      | HiddenFile          | Not related external client | read-metadata  | Not Allowed |
      | HiddenFile          | Not related external client | download       | Not Allowed |
      | HiddenFile          | Not related external client | write-metadata | Not Allowed |
      | HiddenFile          | Not related external client | delete         | Not Allowed |

       # TODO: Should probably add test for when user is not from file owners institution as well?
  Scenario Outline: Verify embargo file operation permissions on Degree when user is from publication channel owner organization
    Given a file of type "<FileType>"
    And the file is owned by "publication creator"
    And the file has embargo
    And publication is of type "degree"
    And publication has publisher claimed by "users institution"
    When the user have the role "<UserRole>"
    And the user belongs to "creating institution"
    And the user attempts to "<Operation>"
    Then the action outcome is "<Outcome>"

    Examples:
      | FileType            | UserRole                    | Operation      | Outcome     |
      | PendingOpenFile     | Authenticated               | read-metadata  | Not Allowed |
      | PendingOpenFile     | Authenticated               | download       | Not Allowed |
      | PendingOpenFile     | Authenticated               | write-metadata | Not Allowed |
      | PendingOpenFile     | Authenticated               | delete         | Not Allowed |
      | PendingInternalFile | Authenticated               | read-metadata  | Not Allowed |
      | PendingInternalFile | Authenticated               | download       | Not Allowed |
      | PendingInternalFile | Authenticated               | write-metadata | Not Allowed |
      | PendingInternalFile | Authenticated               | delete         | Not Allowed |
      | OpenFile            | Authenticated               | read-metadata  | Allowed     |
      | OpenFile            | Authenticated               | download       | Not Allowed |
      | OpenFile            | Authenticated               | write-metadata | Not Allowed |
      | OpenFile            | Authenticated               | delete         | Not Allowed |
      | InternalFile        | Authenticated               | read-metadata  | Not Allowed |
      | InternalFile        | Authenticated               | download       | Not Allowed |
      | InternalFile        | Authenticated               | write-metadata | Not Allowed |
      | InternalFile        | Authenticated               | delete         | Not Allowed |
      | HiddenFile          | Authenticated               | read-metadata  | Not Allowed |
      | HiddenFile          | Authenticated               | download       | Not Allowed |
      | HiddenFile          | Authenticated               | write-metadata | Not Allowed |
      | HiddenFile          | Authenticated               | delete         | Not Allowed |

      | PendingOpenFile     | Publication creator         | read-metadata  | Allowed     |
      | PendingOpenFile     | Publication creator         | download       | Allowed     |
      | PendingOpenFile     | Publication creator         | write-metadata | Allowed     |
      | PendingOpenFile     | Publication creator         | delete         | Allowed     |
      | PendingInternalFile | Publication creator         | read-metadata  | Allowed     |
      | PendingInternalFile | Publication creator         | download       | Allowed     |
      | PendingInternalFile | Publication creator         | write-metadata | Allowed     |
      | PendingInternalFile | Publication creator         | delete         | Allowed     |
      | OpenFile            | Publication creator         | read-metadata  | Allowed     |
      | OpenFile            | Publication creator         | download       | Not Allowed |
      | OpenFile            | Publication creator         | write-metadata | Not Allowed |
      | OpenFile            | Publication creator         | delete         | Not Allowed |
      | InternalFile        | Publication creator         | read-metadata  | Allowed     |
      | InternalFile        | Publication creator         | download       | Not Allowed |
      | InternalFile        | Publication creator         | write-metadata | Not Allowed |
      | InternalFile        | Publication creator         | delete         | Not Allowed |
      | HiddenFile          | Publication creator         | read-metadata  | Not Allowed |
      | HiddenFile          | Publication creator         | download       | Not Allowed |
      | HiddenFile          | Publication creator         | write-metadata | Not Allowed |
      | HiddenFile          | Publication creator         | delete         | Not Allowed |

      | PendingOpenFile     | Contributor                 | read-metadata  | Allowed     |
      | PendingOpenFile     | Contributor                 | download       | Not Allowed |
      | PendingOpenFile     | Contributor                 | write-metadata | Not Allowed |
      | PendingOpenFile     | Contributor                 | delete         | Not Allowed |
      | PendingInternalFile | Contributor                 | read-metadata  | Allowed     |
      | PendingInternalFile | Contributor                 | download       | Not Allowed |
      | PendingInternalFile | Contributor                 | write-metadata | Not Allowed |
      | PendingInternalFile | Contributor                 | delete         | Not Allowed |
      | OpenFile            | Contributor                 | read-metadata  | Allowed     |
      | OpenFile            | Contributor                 | download       | Not Allowed |
      | OpenFile            | Contributor                 | write-metadata | Not Allowed |
      | OpenFile            | Contributor                 | delete         | Not Allowed |
      | InternalFile        | Contributor                 | read-metadata  | Allowed     |
      | InternalFile        | Contributor                 | download       | Not Allowed |
      | InternalFile        | Contributor                 | write-metadata | Not Allowed |
      | InternalFile        | Contributor                 | delete         | Not Allowed |
      | HiddenFile          | Contributor                 | read-metadata  | Not Allowed |
      | HiddenFile          | Contributor                 | download       | Not Allowed |
      | HiddenFile          | Contributor                 | write-metadata | Not Allowed |
      | HiddenFile          | Contributor                 | delete         | Not Allowed |

      | PendingOpenFile     | Publishing curator          | read-metadata  | Allowed     |
      | PendingOpenFile     | Publishing curator          | download       | Not Allowed |
      | PendingOpenFile     | Publishing curator          | write-metadata | Not Allowed |
      | PendingOpenFile     | Publishing curator          | delete         | Not Allowed |
      | PendingInternalFile | Publishing curator          | read-metadata  | Allowed     |
      | PendingInternalFile | Publishing curator          | download       | Not Allowed |
      | PendingInternalFile | Publishing curator          | write-metadata | Not Allowed |
      | PendingInternalFile | Publishing curator          | delete         | Not Allowed |
      | OpenFile            | Publishing curator          | read-metadata  | Allowed     |
      | OpenFile            | Publishing curator          | download       | Not Allowed |
      | OpenFile            | Publishing curator          | write-metadata | Not Allowed |
      | OpenFile            | Publishing curator          | delete         | Not Allowed |
      | InternalFile        | Publishing curator          | read-metadata  | Allowed     |
      | InternalFile        | Publishing curator          | download       | Not Allowed |
      | InternalFile        | Publishing curator          | write-metadata | Not Allowed |
      | InternalFile        | Publishing curator          | delete         | Not Allowed |
      | HiddenFile          | Publishing curator          | read-metadata  | Not Allowed |
      | HiddenFile          | Publishing curator          | download       | Not Allowed |
      | HiddenFile          | Publishing curator          | write-metadata | Not Allowed |
      | HiddenFile          | Publishing curator          | delete         | Not Allowed |

      | PendingOpenFile     | Thesis curator              | read-metadata  | Allowed     |
      | PendingOpenFile     | Thesis curator              | download       | Not Allowed |
      | PendingOpenFile     | Thesis curator              | write-metadata | Not Allowed |
      | PendingOpenFile     | Thesis curator              | delete         | Not Allowed |
      | PendingInternalFile | Thesis curator              | read-metadata  | Allowed     |
      | PendingInternalFile | Thesis curator              | download       | Not Allowed |
      | PendingInternalFile | Thesis curator              | write-metadata | Not Allowed |
      | PendingInternalFile | Thesis curator              | delete         | Not Allowed |
      | OpenFile            | Thesis curator              | read-metadata  | Allowed     |
      | OpenFile            | Thesis curator              | download       | Not Allowed |
      | OpenFile            | Thesis curator              | write-metadata | Not Allowed |
      | OpenFile            | Thesis curator              | delete         | Not Allowed |
      | InternalFile        | Thesis curator              | read-metadata  | Allowed     |
      | InternalFile        | Thesis curator              | download       | Not Allowed |
      | InternalFile        | Thesis curator              | write-metadata | Not Allowed |
      | InternalFile        | Thesis curator              | delete         | Not Allowed |
      | HiddenFile          | Thesis curator              | read-metadata  | Not Allowed |
      | HiddenFile          | Thesis curator              | download       | Not Allowed |
      | HiddenFile          | Thesis curator              | write-metadata | Not Allowed |
      | HiddenFile          | Thesis curator              | delete         | Not Allowed |

      | PendingOpenFile     | Embargo thesis curator      | read-metadata  | Allowed     |
      | PendingOpenFile     | Embargo thesis curator      | download       | Allowed     |
      | PendingOpenFile     | Embargo thesis curator      | write-metadata | Allowed     |
      | PendingOpenFile     | Embargo thesis curator      | delete         | Allowed     |
      | PendingInternalFile | Embargo thesis curator      | read-metadata  | Allowed     |
      | PendingInternalFile | Embargo thesis curator      | download       | Allowed     |
      | PendingInternalFile | Embargo thesis curator      | write-metadata | Allowed     |
      | PendingInternalFile | Embargo thesis curator      | delete         | Allowed     |
      | OpenFile            | Embargo thesis curator      | read-metadata  | Allowed     |
      | OpenFile            | Embargo thesis curator      | download       | Allowed     |
      | OpenFile            | Embargo thesis curator      | write-metadata | Allowed     |
      | OpenFile            | Embargo thesis curator      | delete         | Allowed     |
      | InternalFile        | Embargo thesis curator      | read-metadata  | Allowed     |
      | InternalFile        | Embargo thesis curator      | download       | Allowed     |
      | InternalFile        | Embargo thesis curator      | write-metadata | Allowed     |
      | InternalFile        | Embargo thesis curator      | delete         | Allowed     |
      | HiddenFile          | Embargo thesis curator      | read-metadata  | Allowed     |
      | HiddenFile          | Embargo thesis curator      | download       | Allowed     |
      | HiddenFile          | Embargo thesis curator      | write-metadata | Allowed     |
      | HiddenFile          | Embargo thesis curator      | delete         | Allowed     |

      | PendingOpenFile     | Related external client     | read-metadata  | Not Allowed |
      | PendingOpenFile     | Related external client     | download       | Not Allowed |
      | PendingOpenFile     | Related external client     | write-metadata | Not Allowed |
      | PendingOpenFile     | Related external client     | delete         | Not Allowed |
      | PendingInternalFile | Related external client     | read-metadata  | Not Allowed |
      | PendingInternalFile | Related external client     | download       | Not Allowed |
      | PendingInternalFile | Related external client     | write-metadata | Not Allowed |
      | PendingInternalFile | Related external client     | delete         | Not Allowed |
      | OpenFile            | Related external client     | read-metadata  | Allowed     |
      | OpenFile            | Related external client     | download       | Allowed     |
      | OpenFile            | Related external client     | write-metadata | Allowed     |
      | OpenFile            | Related external client     | delete         | Allowed     |
      | InternalFile        | Related external client     | read-metadata  | Allowed     |
      | InternalFile        | Related external client     | download       | Allowed     |
      | InternalFile        | Related external client     | write-metadata | Allowed     |
      | InternalFile        | Related external client     | delete         | Allowed     |
      | HiddenFile          | Related external client     | read-metadata  | Not Allowed |
      | HiddenFile          | Related external client     | download       | Not Allowed |
      | HiddenFile          | Related external client     | write-metadata | Not Allowed |
      | HiddenFile          | Related external client     | delete         | Not Allowed |

      | PendingOpenFile     | Not related external client | read-metadata  | Not Allowed |
      | PendingOpenFile     | Not related external client | download       | Not Allowed |
      | PendingOpenFile     | Not related external client | write-metadata | Not Allowed |
      | PendingOpenFile     | Not related external client | delete         | Not Allowed |
      | PendingInternalFile | Not related external client | read-metadata  | Not Allowed |
      | PendingInternalFile | Not related external client | download       | Not Allowed |
      | PendingInternalFile | Not related external client | write-metadata | Not Allowed |
      | PendingInternalFile | Not related external client | delete         | Not Allowed |
      | OpenFile            | Not related external client | read-metadata  | Allowed     |
      | OpenFile            | Not related external client | download       | Not Allowed |
      | OpenFile            | Not related external client | write-metadata | Not Allowed |
      | OpenFile            | Not related external client | delete         | Not Allowed |
      | InternalFile        | Not related external client | read-metadata  | Not Allowed |
      | InternalFile        | Not related external client | download       | Not Allowed |
      | InternalFile        | Not related external client | write-metadata | Not Allowed |
      | InternalFile        | Not related external client | delete         | Not Allowed |
      | HiddenFile          | Not related external client | read-metadata  | Not Allowed |
      | HiddenFile          | Not related external client | download       | Not Allowed |
      | HiddenFile          | Not related external client | write-metadata | Not Allowed |
      | HiddenFile          | Not related external client | delete         | Not Allowed |
