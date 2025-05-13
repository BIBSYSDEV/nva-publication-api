Feature: Partial update permission
  As a system user
  I want publication permission to be enforced based on publication and user role
  So that only authorized users can perform partial update

  Scenario Outline: Verify partial-update permissions
    Given a "published" publication
    And publication is a degree
    And publication has claimed publisher
    When the user have the role "<UserRole>"
    And the user attempts to "partial-update"
    Then the action outcome is "<Outcome>"

    Examples:
      | UserRole                          | Outcome     |
      | Everyone else                     | Not Allowed |
      | External client                   | Not Allowed |
      | Publication owner                 | Allowed     |
      | Contributor                       | Allowed     |
      | File, support, doi or nvi curator | Allowed     |
      | Related external client           | Allowed     |
      | degree file curator               | Allowed     |