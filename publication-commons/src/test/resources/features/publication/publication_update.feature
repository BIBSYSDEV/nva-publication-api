Feature: Partial update permission
  As a system user
  I want publication permission to be enforced based on publication and user role
  So that only authorized users can perform partial update

  Scenario Outline: Verify partial-update permissions
    Given a "published" publication
    And publication is a degree
    And publication has claimed publisher
    When the user have the role "<UserRole>"
    And the user attempts to "<Operation>"
    Then the action outcome is "<Outcome>"

    Examples:
      | UserRole                          | Outcome     | Operation      |

#      partial-update
      | Everyone else                     | Not Allowed | partial-update |
      | External client                   | Not Allowed | partial-update |
      | Publication owner                 | Allowed     | partial-update |
      | Contributor                       | Allowed     | partial-update |
      | File, support, doi or nvi curator | Allowed     | partial-update |
      | Related external client           | Allowed     | partial-update |
      | Degree file curator               | Allowed     | partial-update |

#      update
      | Everyone else                     | Not Allowed | update         |
      | External client                   | Not Allowed | update         |
      | Publication owner                 | Not Allowed | update         |
      | Contributor                       | Not Allowed | update         |
      | File, support, doi or nvi curator | Not Allowed | update         |
      | Editor                            | Not Allowed | update         |
      | Degree file curator               | Not Allowed | update         |
      | Related external client           | Allowed     | update         |
