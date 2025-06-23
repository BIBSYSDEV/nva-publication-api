Feature: Permissions given claimed publisher
  As a system user
  I want publication permission to be enforced based on publication, user role and channel claim
  So that only authorized users can perform operation

  Scenario Outline: Verify permission when
  user is from the same organization as claimed publisher
    Given a "degree"
    And publication has publisher claimed by "users institution"
    And the ticket receiver is "users institution"
    When the user have the role "<UserRole>"
    And the user attempts to "approve"
    Then the action outcome is "<Outcome>"

    Examples:
      | UserRole                    | Outcome     |
      | Unauthenticated             | Not Allowed |
      | Authenticated               | Not Allowed |
      | Publication creator         | Not Allowed |
      | Contributor                 | Not Allowed |
      | Publishing curator          | Not Allowed |
      | Thesis curator              | Allowed     |
      | Embargo thesis curator      | Allowed     |
      | Editor                      | Not Allowed |
      | Related external client     | Not Allowed |
      | Not related external client | Not Allowed |

  Scenario Outline: Verify permission when
  user is NOT from the same organization as claimed publisher
    Given a "degree"
    And publication has publisher claimed by "not users institution"
    When the user have the role "<UserRole>"
    And the user attempts to "approve"
    Then the action outcome is "<Outcome>"

    Examples:
      | UserRole                    | Outcome     |
      | Unauthenticated             | Not Allowed |
      | Authenticated               | Not Allowed |
      | Publication creator         | Not Allowed |
      | Contributor                 | Not Allowed |
      | Publishing curator          | Not Allowed |
      | Thesis curator              | Not Allowed |
      | Embargo thesis curator      | Not Allowed |
      | Editor                      | Not Allowed |
      | Related external client     | Not Allowed |
      | Not related external client | Not Allowed |

  Scenario Outline: Verify permission when
  user is from a contributor organization and publisher is unclaimed
    Given a "publication"
    And the user belongs to "curating institution"
    And the ticket receiver is "users institution"
    And ticket is of type "<TicketType>"
    When the user have the role "<UserRole>"
    And the user attempts to "approve"
    Then the action outcome is "<Outcome>"

    Examples:
      | TicketType      | UserRole                    | Outcome     |
      | file approval   | Unauthenticated             | Not Allowed |
      | file approval   | Authenticated               | Not Allowed |
      | file approval   | Publication creator         | Not Allowed |
      | file approval   | Contributor                 | Not Allowed |
      | file approval   | Publishing curator          | Allowed     |
      | file approval   | Doi curator                 | Not Allowed |
      | file approval   | Support curator             | Not Allowed |
      | file approval   | Editor                      | Not Allowed |
      | file approval   | Thesis curator              | Not Allowed |
      | file approval   | Embargo thesis curator      | Not Allowed |
      | file approval   | Related external client     | Not Allowed |
      | file approval   | Not related external client | Not Allowed |
      | support request | Unauthenticated             | Not Allowed |
      | support request | Authenticated               | Not Allowed |
      | support request | Publication creator         | Not Allowed |
      | support request | Contributor                 | Not Allowed |
      | support request | Publishing curator          | Not Allowed |
      | support request | Doi curator                 | Not Allowed |
      | support request | Support curator             | Allowed     |
      | support request | Editor                      | Not Allowed |
      | support request | Thesis curator              | Not Allowed |
      | support request | Embargo thesis curator      | Not Allowed |
      | support request | Related external client     | Not Allowed |
      | support request | Not related external client | Not Allowed |
      | doi request     | Unauthenticated             | Not Allowed |
      | doi request     | Authenticated               | Not Allowed |
      | doi request     | Publication creator         | Not Allowed |
      | doi request     | Contributor                 | Not Allowed |
      | doi request     | Publishing curator          | Not Allowed |
      | doi request     | Doi curator                 | Allowed     |
      | doi request     | Support curator             | Not Allowed |
      | doi request     | Editor                      | Not Allowed |
      | doi request     | Thesis curator              | Not Allowed |
      | doi request     | Embargo thesis curator      | Not Allowed |
      | doi request     | Related external client     | Not Allowed |
      | doi request     | Not related external client | Not Allowed |