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
      | UserRole                | Operation | Outcome     |

      | Everyone else           | approve   | Not Allowed |
      | External client         | approve   | Not Allowed |
      | Publication owner       | approve   | Not Allowed |
      | Contributor             | approve   | Not Allowed |
      | publishing curator      | approve   | Not Allowed |
      | Editor                  | approve   | Not Allowed |
      | Degree file curator     | approve   | Allowed     |
      | Related external client | approve   | Not Allowed |
      | Everyone else           | transfer  | Not Allowed |
      | External client         | transfer  | Not Allowed |
      | Publication owner       | transfer  | Not Allowed |
      | Contributor             | transfer  | Not Allowed |
      | Publishing curator      | transfer  | Not Allowed |
      | Editor                  | transfer  | Not Allowed |
      | Degree file curator     | transfer  | Not Allowed |
      | Related external client | transfer  | Not Allowed |

  Scenario Outline: Verify permission when
  user is NOT from the same organization as claimed publisher
    Given a "degree"
    And publication has publisher claimed by "not users institution"
    When the user have the role "<UserRole>"
    And the user attempts to "<Operation>"
    Then the action outcome is "<Outcome>"

    Examples:
      | UserRole                | Operation | Outcome     |

      | Everyone else           | approve   | Not Allowed |
      | External client         | approve   | Not Allowed |
      | Publication owner       | approve   | Not Allowed |
      | Contributor             | approve   | Not Allowed |
      | publishing curator      | approve   | Not Allowed |
      | Editor                  | approve   | Not Allowed |
      | Degree file curator     | approve   | Not Allowed |
      | Related external client | approve   | Not Allowed |
      | Everyone else           | transfer  | Not Allowed |
      | External client         | transfer  | Not Allowed |
      | Publication owner       | transfer  | Not Allowed |
      | Contributor             | transfer  | Not Allowed |
      | publishing curator      | transfer  | Not Allowed |
      | Editor                  | transfer  | Not Allowed |
      | Degree file curator     | transfer  | Not Allowed |
      | Related external client | transfer  | Not Allowed |


  Scenario Outline: Verify permission when
  user is from a contributor organization and publisher is unclaimed
    Given a "publication"
    And the user belongs to "creating institution"
    And the ticket receiver is "users institution"
    When the user have the role "<UserRole>"
    And the user attempts to "<Operation>"
    Then the action outcome is "<Outcome>"

    Examples:
      | UserRole                | Operation | Outcome     |

      | Everyone else           | approve   | Not Allowed |
      | External client         | approve   | Not Allowed |
      | Publication owner       | approve   | Not Allowed |
      | Contributor             | approve   | Not Allowed |
      | publishing curator      | approve   | Allowed     |
      | Editor                  | approve   | Not Allowed |
      | Degree file curator     | approve   | Not Allowed |
      | Related external client | approve   | Not Allowed |
      | Everyone else           | transfer  | Not Allowed |
      | External client         | transfer  | Not Allowed |
      | Publication owner       | transfer  | Not Allowed |
      | Contributor             | transfer  | Not Allowed |
      | publishing curator      | transfer  | Allowed     |
      | Editor                  | transfer  | Not Allowed |
      | Degree file curator     | transfer  | Not Allowed |
      | Related external client | transfer  | Not Allowed |


  Scenario: Verify transfer permission when user have no relations to publication, but owns the ticket
    Given a "publication"
    And the user belongs to "non curating institution"
    And the ticket receiver is "users institution"
    When the user attempts to "transfer"
    And the user have the role "publishing curator"
    Then the action outcome is "Allowed"

  Scenario: Should not give transfer permission when only available curation institution is same as user
    Given a "publication"
    When the user attempts to "transfer"
    And the user have the role "publishing curator"
    And the user belongs to "curating institution"
    Then the action outcome is "Not Allowed"

  Scenario Outline: Should deny ticket transfer when channel is claimed
    Given a "degree"
    And publication has "<Files>" files
    And publication has publisher claimed by "<ClaimedBy>"
    When the user attempts to "transfer"
    And the user have the role "thesis curator"
    Then the action outcome is "Not Allowed"

    Examples:
      | Files       | ClaimedBy             |
      | No          | not users institution |
      | No approved | not users institution |
      | Approved    | not users institution |

      | No          | users institution     |
      | No approved | users institution     |
      | Approved    | users institution     |