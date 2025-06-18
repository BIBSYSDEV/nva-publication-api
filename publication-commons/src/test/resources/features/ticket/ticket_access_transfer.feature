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
    And the user attempts to "<Operation>"
    Then the action outcome is "<Outcome>"

    Examples:
      | UserRole                    | Operation | Outcome     |
      | Authenticated               | transfer  | Not Allowed |
      | Publication creator         | transfer  | Not Allowed |
      | Contributor                 | transfer  | Not Allowed |
      | Publishing curator          | transfer  | Not Allowed |
      | Editor                      | transfer  | Not Allowed |
      | Thesis curator              | transfer  | Not Allowed |
      | Embargo thesis curator      | transfer  | Not Allowed |
      | Related external client     | transfer  | Not Allowed |
      | Not related external client | transfer  | Not Allowed |

  Scenario Outline: Verify permission when
  user is NOT from the same organization as claimed publisher
    Given a "degree"
    And publication has publisher claimed by "not users institution"
    When the user have the role "<UserRole>"
    And the user attempts to "<Operation>"
    Then the action outcome is "<Outcome>"

    Examples:
      | UserRole                    | Operation | Outcome     |
      | Unauthenticated             | transfer  | Not Allowed |
      | Authenticated               | transfer  | Not Allowed |
      | Publication creator         | transfer  | Not Allowed |
      | Contributor                 | transfer  | Not Allowed |
      | Publishing curator          | transfer  | Not Allowed |
      | Thesis curator              | transfer  | Not Allowed |
      | Embargo thesis curator      | transfer  | Not Allowed |
      | Editor                      | transfer  | Not Allowed |
      | Related external client     | transfer  | Not Allowed |
      | Not related external client | transfer  | Not Allowed |

  Scenario Outline: Verify permission when
  user is from a contributor organization and publisher is unclaimed
    Given a "publication"
    And the user belongs to "curating institution"
    And the ticket receiver is "users institution"
    When the user have the role "<UserRole>"
    And the user attempts to "<Operation>"
    Then the action outcome is "<Outcome>"

    Examples:
      | UserRole                    | Operation | Outcome     |
      | Authenticated               | transfer  | Not Allowed |
      | Publication creator         | transfer  | Not Allowed |
      | Contributor                 | transfer  | Not Allowed |
      | Publishing curator          | transfer  | Allowed     |
      | Editor                      | transfer  | Not Allowed |
      | Thesis curator              | transfer  | Not Allowed |
      | Embargo thesis curator      | transfer  | Not Allowed |
      | Related external client     | transfer  | Not Allowed |
      | Not related external client | transfer  | Not Allowed |


  Scenario: Verify transfer permission when user have no relations to publication, but user belongs to the receiving institution
    Given a "publication"
    And the user belongs to "non curating institution"
    And the ticket receiver is "users institution"
    When the user have the role "publishing curator"
    And the user attempts to "transfer"
    Then the action outcome is "Allowed"

  Scenario: Verify no approve permission when user have no relations to publication, but user belongs to the receiving institution
    Given a "publication"
    And the user belongs to "non curating institution"
    And the ticket receiver is "users institution"
    When the user have the role "publishing curator"
    And the user attempts to "approve"
    Then the action outcome is "Not Allowed"

  Scenario: Should not give transfer permission when only available curation institution is same as user
    Given a "publication"
    When the user have the role "publishing curator"
    And the user attempts to "transfer"
    Then the action outcome is "Not Allowed"

  Scenario Outline: Should deny ticket transfer when channel is claimed
    Given a "degree"
    And publication has "<Files>" files
    And publication has publisher claimed by "<ClaimedBy>"
    When the user have the role "thesis curator"
    And the user attempts to "transfer"
    Then the action outcome is "Not Allowed"

    Examples:
      | Files        | ClaimedBy             |
      | No           | not users institution |
      | No finalized | not users institution |
      | Finalized    | not users institution |

      | No           | users institution     |
      | No finalized | users institution     |
      | Finalized    | users institution     |