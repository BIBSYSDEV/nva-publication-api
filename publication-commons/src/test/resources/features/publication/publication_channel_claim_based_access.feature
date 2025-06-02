Feature: Permissions given claimed publisher
  As a system user
  I want publication permission to be enforced based on publication, user role and channel claim
  So that only authorized users can perform operation

  Scenario Outline: Verify operation when user is not from the same organization as claimed
  publisher and publication has approved files
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

      | Everyone else                     | unpublish      | Not Allowed |
      | External client                   | unpublish      | Not Allowed |
      | Publication owner                 | unpublish      | Not Allowed |
      | Contributor                       | unpublish      | Not Allowed |
      | File, support, doi or nvi curator | unpublish      | Not Allowed |
      | Editor                            | unpublish      | Not Allowed |
      | Degree file curator               | unpublish      | Not Allowed |
      | Related external client           | unpublish      | Allowed     |


  Scenario Outline: Verify update operation when user is from the same organization as claimed
  publisher and publication has no approved files
    Given a "published" publication
    And publication has no approved files
    And publication is a degree
    And publication has claimed publisher
    And publisher is claimed by organization
    When the user have the role "<UserRole>"
    And the user is from the same organization as claimed publisher
    And the user attempts to "update"
    Then the action outcome is "<Outcome>"

    Examples:
      | UserRole                          | Outcome     |

      | Everyone else                     | Not Allowed |
      | External client                   | Not Allowed |
      | Publication owner                 | Allowed     |
      | Contributor                       | Allowed     |
      | File, support, doi or nvi curator | Allowed     |
      | Editor                            | Allowed     |
      | Degree file curator               | Allowed     |
      | Related external client           | Allowed     |

  Scenario Outline: Verify update operation when user is not from the same organization as claimed
  publisher and publication has no approved files
    Given a "published" publication
    And publication has no approved files
    And publication is a degree
    And publication has claimed publisher
    When the user have the role "<UserRole>"
    And the user attempts to "update"
    Then the action outcome is "<Outcome>"

    Examples:
      | UserRole                          | Outcome     |

      | Everyone else                     | Not Allowed |
      | External client                   | Not Allowed |
      | Publication owner                 | Allowed     |
      | Contributor                       | Allowed     |
      | File, support, doi or nvi curator | Allowed     |
      | Editor                            | Allowed     |
      | Degree file curator               | Allowed     |
      | Related external client           | Allowed     |

  Scenario Outline: Verify update operation when user is not from the same organization as claimed
  publisher, publication is an imported student thesis and has no approved files
    Given a "published" publication
    And publication has no approved files
    And publication is a degree
    And publication has claimed publisher
    And publication is an imported student thesis
    When the user have the role "<UserRole>"
    And the user attempts to "update"
    Then the action outcome is "<Outcome>"

    Examples:
      | UserRole                          | Outcome     |

      | Everyone else                     | Not Allowed |
      | External client                   | Not Allowed |
      | Publication owner                 | Not Allowed |
      | Contributor                       | Not Allowed |
      | File, support, doi or nvi curator | Not Allowed |
      | Editor                            | Not Allowed |
      | Degree file curator               | Not Allowed |
      | Related external client           | Allowed     |

  Scenario Outline: Verify update operation when user is not from the same organization as claimed
  publisher, publication is an imported student thesis and has approved files
    Given a "published" publication
    And publication is a degree
    And publication has claimed publisher
    And publication is an imported student thesis
    When the user have the role "<UserRole>"
    And the user attempts to "update"
    Then the action outcome is "<Outcome>"

    Examples:
      | UserRole                          | Outcome     |

      | Everyone else                     | Not Allowed |
      | External client                   | Not Allowed |
      | Publication owner                 | Not Allowed |
      | Contributor                       | Not Allowed |
      | File, support, doi or nvi curator | Not Allowed |
      | Editor                            | Not Allowed |
      | Degree file curator               | Not Allowed |
      | Related external client           | Allowed     |


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
      | UserRole                          | Operation     | Outcome     |

      | Everyone else                     | update        | Not Allowed |
      | External client                   | update        | Not Allowed |
      | Publication owner                 | update        | Not Allowed |
      | Contributor                       | update        | Not Allowed |
      | File, support, doi or nvi curator | update        | Not Allowed |
      | Editor                            | update        | Allowed     |
      | Degree file curator               | update        | Allowed     |
      | Related external client           | update        | Allowed     |

      | Everyone else                     | unpublish     | Not Allowed |
      | External client                   | unpublish     | Not Allowed |
      | Publication owner                 | unpublish     | Not Allowed |
      | Contributor                       | unpublish     | Not Allowed |
      | File, support, doi or nvi curator | unpublish     | Not Allowed |
      | Editor                            | unpublish     | Allowed     |
      | Degree file curator               | unpublish     | Allowed     |
      | Related external client           | unpublish     | Allowed     |

      | Everyone else                     | approve-files | Not Allowed |
      | External client                   | approve-files | Not Allowed |
      | Publication owner                 | approve-files | Not Allowed |
      | Contributor                       | approve-files | Not Allowed |
      | File, support, doi or nvi curator | approve-files | Not Allowed |
      | Editor                            | approve-files | Not Allowed |
      | Degree file curator               | approve-files | Allowed     |
      | Related external client           | approve-files | Not Allowed |


