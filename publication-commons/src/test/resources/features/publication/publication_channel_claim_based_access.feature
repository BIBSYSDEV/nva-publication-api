Feature: Permissions given claimed publisher
  As a system user
  I want publication permission to be enforced based on publication, user role and channel claim
  So that only authorized users can perform operation

  Scenario Outline: Verify operation when user is not from the same organization as claimed
  publisher and publication has approved files
    Given a "degree"
    And publication has "approved" files
    And publication has publisher claimed by "not users institution"
    And channel claim has "editing" policy "OwnerOnly"
    When the user have the role "<UserRole>"
    And the user attempts to "<Operation>"
    Then the action outcome is "<Outcome>"

    Examples:
      | UserRole                     | Operation      | Outcome     |
      | Unauthenticated              | partial-update | Not Allowed |
      | Everyone                     | partial-update | Not Allowed |
      | External client              | partial-update | Not Allowed |
      | Publication creator          | partial-update | Allowed     |
      | Contributor                  | partial-update | Allowed     |
      | Publishing curator           | partial-update | Allowed     |
      | NVI curator                  | partial-update | Allowed     |
      | DOI curator                  | partial-update | Allowed     |
      | Support curator              | partial-update | Allowed     |
      | Student file curator         | partial-update | Allowed     |
      | Student embargo file curator | partial-update | Allowed     |
      | Editor                       | partial-update | Allowed     |
      | Related external client      | partial-update | Allowed     |
      | Degree file curator          | partial-update | Allowed     |

      | Unauthenticated              | update         | Not Allowed |
      | Everyone                     | update         | Not Allowed |
      | External client              | update         | Not Allowed |
      | Publication creator          | update         | Not Allowed |
      | Contributor                  | update         | Not Allowed |
      | Publishing curator           | update         | Allowed     |
      | NVI curator                  | update         | Allowed     |
      | DOI curator                  | update         | Allowed     |
      | Support curator              | update         | Allowed     |
      | Student file curator         | update         | Allowed     |
      | Student embargo file curator | update         | Allowed     |
      | Editor                       | update         | Not Allowed |
      | Degree file curator          | update         | Not Allowed |
      | Related external client      | update         | Allowed     |

      | Unauthenticated              | unpublish      | Not Allowed |
      | Everyone                     | unpublish      | Not Allowed |
      | External client              | unpublish      | Not Allowed |
      | Publication creator          | unpublish      | Not Allowed |
      | Contributor                  | unpublish      | Not Allowed |
      | Publishing curator           | unpublish      | Not Allowed |
      | NVI curator                  | unpublish      | Not Allowed |
      | DOI curator                  | unpublish      | Not Allowed |
      | Support curator              | unpublish      | Not Allowed |
      | Student file curator         | unpublish      | Not Allowed |
      | Student embargo file curator | unpublish      | Not Allowed |
      | Editor                       | unpublish      | Not Allowed |
      | Degree file curator          | unpublish      | Not Allowed |
      | Related external client      | unpublish      | Allowed     |


  Scenario Outline: Verify update operation when user is from the same organization as claimed
  publisher and publication has no approved files
    Given a "degree"
    And publication has "no approved" files
    And publication has publisher claimed by "users institution"
    And channel claim has "publishing" policy "everyone"
    When the user have the role "<UserRole>"
    And the user attempts to "update"
    Then the action outcome is "<Outcome>"

    Examples:
      | UserRole                     | Outcome     |
      | Unauthenticated              | Not Allowed |
      | Everyone                     | Not Allowed |
      | External client              | Not Allowed |
      | Publication creator          | Allowed     |
      | Contributor                  | Allowed     |
      | Publishing curator           | Allowed     |
      | NVI curator                  | Allowed     |
      | DOI curator                  | Allowed     |
      | Support curator              | Allowed     |
      | Student file curator         | Allowed     |
      | Student embargo file curator | Allowed     |
      | Editor                       | Allowed     |
      | Degree file curator          | Allowed     |
      | Related external client      | Allowed     |

  Scenario Outline: Verify update operation when user is not from the same organization as claimed
  publisher and publication has no approved files
    Given a "degree"
    And publication has "no approved" files
    And publication has publisher claimed by "not users institution"
    And channel claim has "publishing" policy "everyone"
    When the user have the role "<UserRole>"
    And the user attempts to "update"
    Then the action outcome is "<Outcome>"

    Examples:
      | UserRole                     | Outcome     |
      | Everyone else                | Not Allowed |
      | External client              | Not Allowed |
      | Publication owner            | Allowed     |
      | Contributor                  | Allowed     |
      | Publishing curator           | Allowed     |
      | NVI curator                  | Allowed     |
      | DOI curator                  | Allowed     |
      | Support curator              | Allowed     |
      | Student file curator         | Allowed     |
      | Student embargo file curator | Allowed     |
      | Editor                       | Allowed     |
      | Degree file curator          | Allowed     |
      | Related external client      | Allowed     |

  Scenario Outline: Verify update operation when user is not from the same organization as claimed
  publisher, publication is an imported student thesis and has no approved files
    Given a "degree"
    And publication is an imported degree
    And publication has "no approved" files
    And publication has publisher claimed by "not users institution"
    And channel claim has "publishing" policy "everyone"
    When the user have the role "<UserRole>"
    And the user attempts to "update"
    Then the action outcome is "<Outcome>"

    Examples:
      | UserRole                     | Outcome     |
      | Everyone else                | Not Allowed |
      | External client              | Not Allowed |
      | Publication owner            | Not Allowed |
      | Contributor                  | Not Allowed |
      | Publishing curator           | Not Allowed |
      | NVI curator                  | Not Allowed |
      | DOI curator                  | Not Allowed |
      | Support curator              | Not Allowed |
      | Student file curator         | Not Allowed |
      | Student embargo file curator | Not Allowed |
      | Editor                       | Not Allowed |
      | Degree file curator          | Not Allowed |
      | Related external client      | Allowed     |

  Scenario Outline: Verify update operation when user is not from the same organization as claimed
  publisher, publication is an imported student thesis and has approved files
    Given a "degree"
    And publication is an imported degree
    And publication has "approved" files
    And publication has publisher claimed by "not users institution"
    And channel claim has "editing" policy "ownerOnly"
    When the user have the role "<UserRole>"
    And the user attempts to "update"
    Then the action outcome is "<Outcome>"

    Examples:
      | UserRole                     | Outcome     |

      | Everyone else                | Not Allowed |
      | External client              | Not Allowed |
      | Publication owner            | Not Allowed |
      | Contributor                  | Not Allowed |
      | Publishing curator           | Not Allowed |
      | NVI curator                  | Not Allowed |
      | DOI curator                  | Not Allowed |
      | Support curator              | Not Allowed |
      | Student file curator         | Not Allowed |
      | Student embargo file curator | Not Allowed |
      | Editor                       | Not Allowed |
      | Degree file curator          | Not Allowed |
      | Related external client      | Allowed     |


  Scenario Outline: Verify permission when
  user is from the same organization as claimed publisher
    Given a "degree"
    And publication has "approved" files
    And publication has claimed publisher
    And publication has publisher claimed by "users institution"
    And channel claim has "editing" policy "ownerOnly"
    When the user have the role "<UserRole>"
    And the user attempts to "<Operation>"
    Then the action outcome is "<Outcome>"

    Examples:
      | UserRole                     | Operation     | Outcome     |

      | Everyone else                | update        | Not Allowed |
      | External client              | update        | Not Allowed |
      | Publication owner            | update        | Not Allowed |
      | Contributor                  | update        | Not Allowed |
      | Publishing curator           | update        | Not Allowed |
      | NVI curator                  | update        | Not Allowed |
      | DOI curator                  | update        | Not Allowed |
      | Support curator              | update        | Not Allowed |
      | Student file curator         | update        | Not Allowed |
      | Student embargo file curator | update        | Not Allowed |
      | Editor                       | update        | Allowed     |
      | Degree file curator          | update        | Allowed     |
      | Related external client      | update        | Allowed     |

      | Everyone else                | unpublish     | Not Allowed |
      | External client              | unpublish     | Not Allowed |
      | Publication owner            | unpublish     | Not Allowed |
      | Contributor                  | unpublish     | Not Allowed |
      | Publishing curator           | unpublish     | Not Allowed |
      | NVI curator                  | unpublish     | Not Allowed |
      | DOI curator                  | unpublish     | Not Allowed |
      | Support curator              | unpublish     | Not Allowed |
      | Student file curator         | unpublish     | Not Allowed |
      | Student embargo file curator | unpublish     | Not Allowed |
      | Editor                       | unpublish     | Allowed     |
      | Degree file curator          | unpublish     | Allowed     |
      | Related external client      | unpublish     | Allowed     |

      | Everyone else                | approve-files | Not Allowed |
      | External client              | approve-files | Not Allowed |
      | Publication owner            | approve-files | Not Allowed |
      | Contributor                  | approve-files | Not Allowed |
      | Publishing curator           | approve-files | Not Allowed |
      | NVI curator                  | approve-files | Not Allowed |
      | DOI curator                  | approve-files | Not Allowed |
      | Support curator              | approve-files | Not Allowed |
      | Student file curator         | approve-files | Not Allowed |
      | Student embargo file curator | approve-files | Not Allowed |
      | Editor                       | approve-files | Not Allowed |
      | Degree file curator          | approve-files | Allowed     |
      | Related external client      | approve-files | Not Allowed |


