Feature: Permissions given claimed publisher
  As a system user
  I want publication permission to be enforced based on publication, user role and channel claim
  So that only authorized users can perform operation

  Scenario Outline: Verify permission when user is from the same organization
    Given a "publication"
    And ticket is of type "<TicketType>"
    And the ticket receiver is "users institution"
    And ticket status is "Completed"
    And ticket creator is "other user at same institution"
    When the user have the role "<UserRole>"
    And the user attempts to "Read"
    Then the action outcome is "<Outcome>"

    Examples:
      | TicketType      | UserRole               | Outcome |
      | File approval   | Publication creator    | Allowed |
      | File approval   | Contributor            | Allowed |
      | File approval   | Publishing curator     | Allowed |
      | File approval   | Thesis curator         | Allowed |
      | File approval   | Embargo thesis curator | Allowed |
      | File approval   | Editor                 | Allowed |

      | Doi request     | Publication creator    | Allowed |
      | Doi request     | Contributor            | Allowed |
      | Doi request     | Publishing curator     | Allowed |
      | Doi request     | Thesis curator         | Allowed |
      | Doi request     | Embargo thesis curator | Allowed |
      | Doi request     | Editor                 | Allowed |

      | Support request | Publication creator    | Allowed |
      | Support request | Contributor            | Allowed |
      | Support request | Publishing curator     | Allowed |
      | Support request | Thesis curator         | Allowed |
      | Support request | Embargo thesis curator | Allowed |
      | Support request | Editor                 | Allowed |

  Scenario: Verify permission when unauthenticated user
    Given a "publication"
    And ticket creator is "other user at different institution"
    When the user have the role "Unauthenticated"
    And the user attempts to "read"
    Then the action outcome is "Not Allowed"

  Scenario: Verify permission when authenticated user without relations to publication or ticket
    Given a "publication"
    And ticket creator is "other user at different institution"
    When the user have the role "Authenticated"
    And the user attempts to "read"
    Then the action outcome is "Not Allowed"

  Scenario Outline: Verify permission when user is NOT from the same organization
    Given a "publication"
    And ticket creator is "other user at different institution"
    When the user have the role "<UserRole>"
    And the user attempts to "<Operation>"
    Then the action outcome is "<Outcome>"

    Examples:
      | UserRole                    | Operation | Outcome     |
      | Publication creator         | read      | Not Allowed |
      | Contributor                 | read      | Not Allowed |
      | Publishing curator          | read      | Not Allowed |
      | Thesis curator              | read      | Not Allowed |
      | Embargo thesis curator      | read      | Not Allowed |
      | Editor                      | read      | Not Allowed |
      | Related external client     | read      | Not Allowed |
      | Not related external client | read      | Not Allowed |

  Scenario: Verify read permission when user have no relations to publication, but owns the ticket
    Given a "publication"
    And the user belongs to "non curating institution"
    And the ticket receiver is "users institution"
    When the user have the role "publishing curator"
    And the user attempts to "read"
    Then the action outcome is "Allowed"

  Scenario: Verify no read permission when user have no relations to publication or ticket
    Given a "publication"
    And the user belongs to "non curating institution"
    And the ticket receiver is "non curating institution"
    And ticket is of type "support request"
    And ticket creator is "other user at different institution"
    When the user have the role "Authenticated"
    And the user attempts to "read"
    Then the action outcome is "Not Allowed"