Feature: Publication update permissions
  As a system user
  I want publication permission to be enforced based on publication, user role and channel claims
  So that only authorized users can perform operation

  Scenario Outline: Verify operation when
    Given a "degree"
    And publication is an imported degree
    And publication has "finalized" files
    And publication has publisher claimed by "users institution"
    When the user have the role "<UserRole>"
    And the user attempts to "<Operation>"
    Then the action outcome is "<Outcome>"

    Examples:
      | UserRole                | Operation      | Outcome     |
      | Publication creator     | partial-update | Allowed     |
      | Contributor             | partial-update | Allowed     |
      | Thesis curator          | partial-update | Allowed     |
      | Editor                  | partial-update | Allowed     |
      | Everyone                | partial-update | Not Allowed |
      | Related external client | partial-update | Allowed     |

      | Publication creator     | update         | Not Allowed |
      | Contributor             | update         | Not Allowed |
      | Thesis curator          | update         | Allowed     |
      | Editor                  | update         | Allowed     |
      | Everyone                | update         | Not Allowed |
      | Related external client | update         | Allowed     |

  Scenario Outline: Verify operation when
    Given a "degree"
    And publication has "finalized" files
    And publication has publisher claimed by "users institution"
    When the user have the role "<UserRole>"
    And the user attempts to "<Operation>"
    Then the action outcome is "<Outcome>"

    Examples:
      | UserRole                | Operation      | Outcome     |
      | Publication creator     | partial-update | Allowed     |
      | Contributor             | partial-update | Allowed     |
      | Thesis curator          | partial-update | Allowed     |
      | Editor                  | partial-update | Allowed     |
      | Everyone                | partial-update | Not Allowed |
      | Related external client | partial-update | Allowed     |

      | Publication creator     | update         | Not Allowed |
      | Contributor             | update         | Not Allowed |
      | Thesis curator          | update         | Allowed     |
      | Editor                  | update         | Allowed     |
      | Everyone                | update         | Not Allowed |
      | Related external client | update         | Allowed     |

  Scenario Outline: Verify operation when
    Given a "publication"
    And publication has "finalized" files
    And publication has publisher claimed by "users institution"
    When the user have the role "<UserRole>"
    And the user attempts to "<Operation>"
    Then the action outcome is "<Outcome>"

    Examples:
      | UserRole                | Operation      | Outcome     |
      | Publication creator     | partial-update | Allowed     |
      | Contributor             | partial-update | Allowed     |
      | Thesis curator          | partial-update | Allowed     |
      | Editor                  | partial-update | Allowed     |
      | Everyone                | partial-update | Not Allowed |
      | Related external client | partial-update | Allowed     |

      | Publication creator     | update         | Allowed     |
      | Contributor             | update         | Allowed     |
      | Thesis curator          | update         | Allowed     |
      | Editor                  | update         | Allowed     |
      | Everyone                | update         | Not Allowed |
      | Related external client | update         | Allowed     |

  Scenario Outline: Verify operation when
    Given a "degree"
    And publication is an imported degree
    And publication has "no" files
    And publication has publisher claimed by "users institution"
    When the user have the role "<UserRole>"
    And the user attempts to "<Operation>"
    Then the action outcome is "<Outcome>"

    Examples:
      | UserRole                | Operation      | Outcome     |
      | Publication creator     | partial-update | Allowed     |
      | Contributor             | partial-update | Allowed     |
      | Thesis curator          | partial-update | Allowed     |
      | Editor                  | partial-update | Allowed     |
      | Everyone                | partial-update | Not Allowed |
      | Related external client | partial-update | Allowed     |

      | Publication creator     | update         | Not Allowed |
      | Contributor             | update         | Not Allowed |
      | Thesis curator          | update         | Allowed     |
      | Editor                  | update         | Allowed     |
      | Everyone                | update         | Not Allowed |
      | Related external client | update         | Allowed     |

  Scenario Outline: Verify operation when
    Given a "degree"
    And publication has "no" files
    And publication has publisher claimed by "users institution"
    When the user have the role "<UserRole>"
    And the user attempts to "<Operation>"
    Then the action outcome is "<Outcome>"

    Examples:
      | UserRole                | Operation      | Outcome     |
      | Publication creator     | partial-update | Allowed     |
      | Contributor             | partial-update | Allowed     |
      | Thesis curator          | partial-update | Allowed     |
      | Editor                  | partial-update | Allowed     |
      | Everyone                | partial-update | Not Allowed |
      | Related external client | partial-update | Allowed     |

      | Publication creator     | update         | Allowed     |
      | Contributor             | update         | Allowed     |
      | Thesis curator          | update         | Allowed     |
      | Editor                  | update         | Allowed     |
      | Everyone                | update         | Not Allowed |
      | Related external client | update         | Allowed     |

  Scenario Outline: Verify operation when
    Given a "publication"
    And publication has "no" files
    And publication has publisher claimed by "users institution"
    When the user have the role "<UserRole>"
    And the user attempts to "<Operation>"
    Then the action outcome is "<Outcome>"

    Examples:
      | UserRole                | Operation      | Outcome     |
      | Publication creator     | partial-update | Allowed     |
      | Contributor             | partial-update | Allowed     |
      | Thesis curator          | partial-update | Allowed     |
      | Editor                  | partial-update | Allowed     |
      | Everyone                | partial-update | Not Allowed |
      | Related external client | partial-update | Allowed     |

      | Publication creator     | update         | Allowed     |
      | Contributor             | update         | Allowed     |
      | Thesis curator          | update         | Allowed     |
      | Editor                  | update         | Allowed     |
      | Everyone                | update         | Not Allowed |
      | Related external client | update         | Allowed     |