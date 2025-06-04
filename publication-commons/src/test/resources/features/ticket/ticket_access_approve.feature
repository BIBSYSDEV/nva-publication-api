Feature: Permissions given claimed publisher
  As a system user
  I want publication permission to be enforced based on publication, user role and channel claim
  So that only authorized users can perform operation

  Scenario Outline: Verify permission when
  user is from the same organization as claimed publisher
    Given a "published" publication
    And publication is a degree
    And publication has claimed publisher
    And publisher is claimed by organization
    When the user have the role "<UserRole>"
    And the user is from the same organization as claimed publisher
    And the user attempts to "<Operation>"
    Then the action outcome is "<Outcome>"

    Examples:
      | UserRole                          | Operation | Outcome     |

      | Everyone else                     | approve   | Not Allowed |
      | External client                   | approve   | Not Allowed |
      | Publication owner                 | approve   | Not Allowed |
      | Contributor                       | approve   | Not Allowed |
      | File, support, doi or nvi curator | approve   | Not Allowed |
      | Editor                            | approve   | Not Allowed |
      | Degree file curator               | approve   | Allowed     |
      | Related external client           | approve   | Not Allowed |
      | Everyone else                     | transfer  | Not Allowed |
      | External client                   | transfer  | Not Allowed |
      | Publication owner                 | transfer  | Not Allowed |
      | Contributor                       | transfer  | Not Allowed |
      | File, support, doi or nvi curator | transfer  | Not Allowed |
      | Editor                            | transfer  | Not Allowed |
      | Degree file curator               | transfer  | Allowed     |
      | Related external client           | transfer  | Not Allowed |

  Scenario Outline: Verify permission when
  user is NOT from the same organization as claimed publisher
    Given a "published" publication
    And publication is a degree
    And publication has claimed publisher
    And publisher is claimed by organization
    When the user have the role "<UserRole>"
    And the user attempts to "<Operation>"
    Then the action outcome is "<Outcome>"

    Examples:
      | UserRole                          | Operation | Outcome     |

      | Everyone else                     | approve   | Not Allowed |
      | External client                   | approve   | Not Allowed |
      | Publication owner                 | approve   | Not Allowed |
      | Contributor                       | approve   | Not Allowed |
      | File, support, doi or nvi curator | approve   | Not Allowed |
      | Editor                            | approve   | Not Allowed |
      | Degree file curator               | approve   | Not Allowed |
      | Related external client           | approve   | Not Allowed |
      | Everyone else                     | transfer  | Not Allowed |
      | External client                   | transfer  | Not Allowed |
      | Publication owner                 | transfer  | Not Allowed |
      | Contributor                       | transfer  | Not Allowed |
      | File, support, doi or nvi curator | transfer  | Not Allowed |
      | Editor                            | transfer  | Not Allowed |
      | Degree file curator               | transfer  | Not Allowed |
      | Related external client           | transfer  | Not Allowed |


  Scenario Outline: Verify permission when
  user is from a contributor organization and publisher is unclaimed
    Given a "published" publication
    When the user have the role "<UserRole>"
    And the user attempts to "<Operation>"
    Then the action outcome is "<Outcome>"

    Examples:
      | UserRole                          | Operation | Outcome     |

      | Everyone else                     | approve   | Not Allowed |
      | External client                   | approve   | Not Allowed |
      | Publication owner                 | approve   | Not Allowed |
      | Contributor                       | approve   | Not Allowed |
      | File, support, doi or nvi curator | approve   | Allowed     |
      | Editor                            | approve   | Not Allowed |
      | Degree file curator               | approve   | Not Allowed |
      | Related external client           | approve   | Not Allowed |
      | Everyone else                     | transfer  | Not Allowed |
      | External client                   | transfer  | Not Allowed |
      | Publication owner                 | transfer  | Not Allowed |
      | Contributor                       | transfer  | Not Allowed |
      | File, support, doi or nvi curator | transfer  | Allowed     |
      | Editor                            | transfer  | Not Allowed |
      | Degree file curator               | transfer  | Not Allowed |
      | Related external client           | transfer  | Not Allowed |