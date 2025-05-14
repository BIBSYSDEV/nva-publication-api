Feature: Update permissions - user not from the same organization as publisher
  As a system user
  I want publication permission to be enforced based on publication, user role and channel claim
  So that only authorized users can perform operation

  Scenario Outline: Verify partial-update and update permissions when
  user is not from the same organization as claimed publisher
    Given a "published" publication
    And publication is a degree
    And publication has claimed publisher
    When the user have the role "<UserRole>"
    And the user attempts to "<Operation>"
    Then the action outcome is "<Outcome>"

    Examples:
      | UserRole                          | Operation      | Outcome     |

      | Everyone else                     | partial-update | Not Allowed |
      | External client                   | partial-update | Not Allowed |
      | Publication owner                 | partial-update | Allowed     |
      | Contributor                       | partial-update | Allowed     |
      | File, support, doi or nvi curator | partial-update | Allowed     |
      | Editor                            | partial-update | Allowed     |
      | Related external client           | partial-update | Allowed     |
      | Degree file curator               | partial-update | Allowed     |

      | Everyone else                     | update         | Not Allowed |
      | External client                   | update         | Not Allowed |
      | Publication owner                 | update         | Not Allowed |
      | Contributor                       | update         | Not Allowed |
      | File, support, doi or nvi curator | update         | Not Allowed |
      | Editor                            | update         | Not Allowed |
      | Degree file curator               | update         | Not Allowed |
      | Related external client           | update         | Allowed     |


  Scenario Outline: Verify update permissions when
  user is from the same organization as claimed publisher
    Given a "published" publication
    And publication is a degree
    And publication has claimed publisher
    And publisher is claimed by organization
    When the user have the role "<UserRole>"
    And the user is from the same organization
    And the user attempts to "<Operation>"
    Then the action outcome is "<Outcome>"

    Examples:
      | UserRole                          | Operation | Outcome     |
      | Everyone else                     | update    | Not Allowed |
      | External client                   | update    | Not Allowed |
      | Publication owner                 | update    | Not Allowed |
      | Contributor                       | update    | Not Allowed |
      | File, support, doi or nvi curator | update    | Not Allowed |
      | Editor                            | update    | Not Allowed |
      | Degree file curator               | update    | Allowed     |
      | Related external client           | update    | Allowed     |


