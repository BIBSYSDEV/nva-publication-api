Feature: Permissions given claimed publisher
  As a system user
  I want publication permission to be enforced based on publication, user role and channel claim
  So that only authorized users can perform operation

  Scenario Outline: Verify operation when user is not from the same organization as claimed
  publisher and publication has finalized files
    Given a "degree"
    And publication has "finalized" files
    And publication has publisher claimed by "not users institution"
    And channel claim has "editing" policy "OwnerOnly"
    When the user have the role "<UserRole>"
    And the user attempts to "<Operation>"
    Then the action outcome is "<Outcome>"

    Examples:
      | UserRole                | Operation      | Outcome     |
      | Unauthenticated         | partial-update | Not Allowed |
      | Authenticated           | partial-update | Not Allowed |
      | External client         | partial-update | Not Allowed |
      | Publication creator     | partial-update | Allowed     |
      | Contributor             | partial-update | Allowed     |
      | Publishing curator      | partial-update | Allowed     |
      | NVI curator             | partial-update | Allowed     |
      | DOI curator             | partial-update | Allowed     |
      | Support curator         | partial-update | Allowed     |
      | Thesis curator          | partial-update | Allowed     |
      | Embargo thesis curator  | partial-update | Allowed     |
      | Editor                  | partial-update | Allowed     |
      | Related external client | partial-update | Allowed     |

      | Unauthenticated         | update         | Not Allowed |
      | Authenticated           | update         | Not Allowed |
      | External client         | update         | Not Allowed |
      | Publication creator     | update         | Not Allowed |
      | Contributor             | update         | Not Allowed |
      | Publishing curator      | update         | Not Allowed |
      | NVI curator             | update         | Not Allowed |
      | DOI curator             | update         | Not Allowed |
      | Support curator         | update         | Not Allowed |
      | Thesis curator          | update         | Not Allowed |
      | Embargo thesis curator  | update         | Not Allowed |
      | Editor                  | update         | Not Allowed |
      | Related external client | update         | Allowed     |

      | Unauthenticated         | unpublish      | Not Allowed |
      | Authenticated           | unpublish      | Not Allowed |
      | External client         | unpublish      | Not Allowed |
      | Publication creator     | unpublish      | Not Allowed |
      | Contributor             | unpublish      | Not Allowed |
      | Publishing curator      | unpublish      | Not Allowed |
      | NVI curator             | unpublish      | Not Allowed |
      | DOI curator             | unpublish      | Not Allowed |
      | Support curator         | unpublish      | Not Allowed |
      | Thesis curator          | unpublish      | Not Allowed |
      | Embargo thesis curator  | unpublish      | Not Allowed |
      | Editor                  | unpublish      | Not Allowed |
      | Related external client | unpublish      | Allowed     |


  Scenario Outline: Verify update operation when user is from the same organization as claimed
  publisher and publication has no finalized files
    Given a "degree"
    And publication has "no finalized" files
    And publication has publisher claimed by "users institution"
    And channel claim has "publishing" policy "everyone"
    When the user have the role "<UserRole>"
    And the user attempts to "update"
    Then the action outcome is "<Outcome>"

    Examples:
      | UserRole                | Outcome     |
      | Unauthenticated         | Not Allowed |
      | Authenticated           | Not Allowed |
      | External client         | Not Allowed |
      | Publication creator     | Allowed     |
      | Contributor             | Allowed     |
      | Publishing curator      | Allowed     |
      | NVI curator             | Allowed     |
      | DOI curator             | Allowed     |
      | Support curator         | Allowed     |
      | Thesis curator          | Allowed     |
      | Embargo thesis curator  | Allowed     |
      | Editor                  | Allowed     |
      | Related external client | Allowed     |

  Scenario Outline: Verify update operation when user is not from the same organization as claimed
  publisher and publication has no finalized files
    Given a "degree"
    And publication has "no finalized" files
    And publication has publisher claimed by "not users institution"
    And channel claim has "publishing" policy "everyone"
    When the user have the role "<UserRole>"
    And the user attempts to "update"
    Then the action outcome is "<Outcome>"

    Examples:
      | UserRole                    | Outcome     |
      | Unauthenticated             | Not Allowed |
      | Authenticated               | Not Allowed |
      | Publication creator         | Allowed     |
      | Contributor                 | Allowed     |
      | Publishing curator          | Allowed     |
      | NVI curator                 | Allowed     |
      | DOI curator                 | Allowed     |
      | Support curator             | Allowed     |
      | Thesis curator              | Allowed     |
      | Embargo thesis curator      | Allowed     |
      | Editor                      | Allowed     |
      | Related external client     | Allowed     |
      | Not related external client | Not Allowed |

  Scenario Outline: Verify update operation when user is not from the same organization as claimed
  publisher, publication is an imported student thesis and has no finalized files
    Given a "degree"
    And publication is an imported degree
    And publication has "no finalized" files
    And publication has publisher claimed by "not users institution"
    And channel claim has "publishing" policy "everyone"
    When the user have the role "<UserRole>"
    And the user attempts to "update"
    Then the action outcome is "<Outcome>"

    Examples:
      | UserRole                    | Outcome     |
      | Unauthenticated             | Not Allowed |
      | Authenticated               | Not Allowed |
      | Publication creator         | Not Allowed |
      | Contributor                 | Not Allowed |
      | Publishing curator          | Not Allowed |
      | NVI curator                 | Not Allowed |
      | DOI curator                 | Not Allowed |
      | Support curator             | Not Allowed |
      | Thesis curator              | Not Allowed |
      | Embargo thesis curator      | Not Allowed |
      | Editor                      | Not Allowed |
      | Related external client     | Allowed     |
      | Not related external client | Not Allowed |

  Scenario Outline: Verify update operation when user is not from the same organization as claimed
  publisher, publication is an imported student thesis and has finalized files
    Given a "degree"
    And publication is an imported degree
    And publication has "finalized" files
    And publication has publisher claimed by "not users institution"
    And channel claim has "editing" policy "ownerOnly"
    When the user have the role "<UserRole>"
    And the user attempts to "update"
    Then the action outcome is "<Outcome>"

    Examples:
      | UserRole                    | Outcome     |
      | Unauthenticated             | Not Allowed |
      | Authenticated               | Not Allowed |
      | Publication creator         | Not Allowed |
      | Contributor                 | Not Allowed |
      | Publishing curator          | Not Allowed |
      | NVI curator                 | Not Allowed |
      | DOI curator                 | Not Allowed |
      | Support curator             | Not Allowed |
      | Thesis curator              | Not Allowed |
      | Embargo thesis curator      | Not Allowed |
      | Editor                      | Not Allowed |
      | Related external client     | Allowed     |
      | Not related external client | Not Allowed |


  Scenario Outline: Verify permission when
  user is from the same organization as claimed publisher
    Given a "degree"
    And publication has "finalized" files
    And publication has publisher claimed by "users institution"
    And channel claim has "editing" policy "ownerOnly"
    When the user have the role "<UserRole>"
    And the user attempts to "<Operation>"
    Then the action outcome is "<Outcome>"

    Examples:
      | UserRole                    | Operation     | Outcome     |
      | Unauthenticated             | update        | Not Allowed |
      | Authenticated               | update        | Not Allowed |
      | External client             | update        | Not Allowed |
      | Publication creator         | update        | Not Allowed |
      | Contributor                 | update        | Not Allowed |
      | Publishing curator          | update        | Not Allowed |
      | NVI curator                 | update        | Not Allowed |
      | DOI curator                 | update        | Not Allowed |
      | Support curator             | update        | Not Allowed |
      | Thesis curator              | update        | Allowed     |
      | Embargo thesis curator      | update        | Not Allowed |
      | Editor                      | update        | Allowed     |
      | Related external client     | update        | Allowed     |
      | Not related external client | update        | Not Allowed |

      | Unauthenticated             | unpublish     | Not Allowed |
      | Authenticated               | unpublish     | Not Allowed |
      | Publication creator         | unpublish     | Not Allowed |
      | Contributor                 | unpublish     | Not Allowed |
      | Publishing curator          | unpublish     | Not Allowed |
      | NVI curator                 | unpublish     | Not Allowed |
      | DOI curator                 | unpublish     | Not Allowed |
      | Support curator             | unpublish     | Not Allowed |
      | Thesis curator              | unpublish     | Allowed     |
      | Embargo thesis curator      | unpublish     | Not Allowed |
      | Editor                      | unpublish     | Allowed     |
      | Related external client     | unpublish     | Allowed     |
      | Not related external client | unpublish     | Not Allowed |

      | Unauthenticated             | approve-files | Not Allowed |
      | Authenticated               | approve-files | Not Allowed |
      | Publication creator         | approve-files | Not Allowed |
      | Contributor                 | approve-files | Not Allowed |
      | Publishing curator          | approve-files | Not Allowed |
      | NVI curator                 | approve-files | Not Allowed |
      | DOI curator                 | approve-files | Not Allowed |
      | Support curator             | approve-files | Not Allowed |
      | Thesis curator              | approve-files | Allowed     |
      | Embargo thesis curator      | approve-files | Allowed     |
      | Editor                      | approve-files | Not Allowed |
      | Related external client     | approve-files | Not Allowed |
      | Not related external client | approve-files | Not Allowed |


