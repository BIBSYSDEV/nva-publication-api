Feature: File permissions for embargo and degree files
  As a system user
  I want file operations permissions (read-metadata, download, write-metadata, delete) to be enforced based on file properties and user roles
  So that only authorized users can perform operations on files

  Scenario Outline: Verify embargo and degree file operation permissions
    Given a file of type "OpenFile" with property "<FileProperty>"
    When the user have the role "<UserRole>"
    And the file is owned by "Publication owner"
    And the user attempts to "<Operation>"
    Then the action outcome is "<Outcome>"

    Examples:
      | FileProperty   | UserRole                             | Operation      | Outcome     |
      | Degree         | Publication owner at X               | read-metadata  | Allowed     |
      | Degree         | Publication owner at X               | download       | Allowed     |
      | Degree         | Publication owner at X               | write-metadata | Not Allowed |
      | Degree         | Publication owner at X               | delete         | Not Allowed |
      | Embargo        | Publication owner at X               | read-metadata  | Allowed     |
      | Embargo        | Publication owner at X               | download       | Allowed     |
      | Embargo        | Publication owner at X               | write-metadata | Not Allowed |
      | Embargo        | Publication owner at X               | delete         | Not Allowed |
      | Degree+Embargo | Publication owner at X               | read-metadata  | Allowed     |
      | Degree+Embargo | Publication owner at X               | download       | Not Allowed |
      | Degree+Embargo | Publication owner at X               | write-metadata | Not Allowed |
      | Degree+Embargo | Publication owner at X               | delete         | Not Allowed |

      | Degree         | Contributor at X                     | read-metadata  | Allowed     |
      | Degree         | Contributor at X                     | download       | Allowed     |
      | Degree         | Contributor at X                     | write-metadata | Not Allowed |
      | Degree         | Contributor at X                     | delete         | Not Allowed |
      | Embargo        | Contributor at X                     | read-metadata  | Allowed     |
      | Embargo        | Contributor at X                     | download       | Allowed     |
      | Embargo        | Contributor at X                     | write-metadata | Not Allowed |
      | Embargo        | Contributor at X                     | delete         | Not Allowed |
      | Degree+Embargo | Contributor at X                     | read-metadata  | Allowed     |
      | Degree+Embargo | Contributor at X                     | download       | Not Allowed |
      | Degree+Embargo | Contributor at X                     | write-metadata | Not Allowed |
      | Degree+Embargo | Contributor at X                     | delete         | Not Allowed |

      | Degree         | Other contributors                   | read-metadata  | Allowed     |
      | Degree         | Other contributors                   | download       | Allowed     |
      | Degree         | Other contributors                   | write-metadata | Not Allowed |
      | Degree         | Other contributors                   | delete         | Not Allowed |
      | Embargo        | Other contributors                   | read-metadata  | Allowed     |
      | Embargo        | Other contributors                   | download       | Allowed     |
      | Embargo        | Other contributors                   | write-metadata | Not Allowed |
      | Embargo        | Other contributors                   | delete         | Not Allowed |
      | Degree+Embargo | Other contributors                   | read-metadata  | Allowed     |
      | Degree+Embargo | Other contributors                   | download       | Not Allowed |
      | Degree+Embargo | Other contributors                   | write-metadata | Not Allowed |
      | Degree+Embargo | Other contributors                   | delete         | Not Allowed |

      | Degree         | File curator at X                    | read-metadata  | Allowed     |
      | Degree         | File curator at X                    | download       | Allowed     |
      | Degree         | File curator at X                    | write-metadata | Not Allowed |
      | Degree         | File curator at X                    | delete         | Not Allowed |
      | Embargo        | File curator at X                    | read-metadata  | Allowed     |
      | Embargo        | File curator at X                    | download       | Allowed     |
      | Embargo        | File curator at X                    | write-metadata | Allowed     |
      | Embargo        | File curator at X                    | delete         | Allowed     |
      | Degree+Embargo | File curator at X                    | read-metadata  | Allowed     |
      | Degree+Embargo | File curator at X                    | download       | Not Allowed |
      | Degree+Embargo | File curator at X                    | write-metadata | Not Allowed |
      | Degree+Embargo | File curator at X                    | delete         | Not Allowed |

      | Degree         | Degree file curator at X             | read-metadata  | Allowed     |
      | Degree         | Degree file curator at X             | download       | Allowed     |
      | Degree         | Degree file curator at X             | write-metadata | Allowed     |
      | Degree         | Degree file curator at X             | delete         | Allowed     |
      | Embargo        | Degree file curator at X             | read-metadata  | Allowed     |
      | Embargo        | Degree file curator at X             | download       | Not Allowed |
      | Embargo        | Degree file curator at X             | write-metadata | Not Allowed |
      | Embargo        | Degree file curator at X             | delete         | Not Allowed |
      | Degree+Embargo | Degree file curator at X             | read-metadata  | Allowed     |
      | Degree+Embargo | Degree file curator at X             | download       | Not Allowed |
      | Degree+Embargo | Degree file curator at X             | write-metadata | Not Allowed |
      | Degree+Embargo | Degree file curator at X             | delete         | Not Allowed |

      | Degree         | Degree embargo file curator at X     | read-metadata  | Allowed     |
      | Degree         | Degree embargo file curator at X     | download       | Allowed     |
      | Degree         | Degree embargo file curator at X     | write-metadata | Allowed     |
      | Degree         | Degree embargo file curator at X     | delete         | Allowed     |
      | Embargo        | Degree embargo file curator at X     | read-metadata  | Allowed     |
      | Embargo        | Degree embargo file curator at X     | download       | Not Allowed |
      | Embargo        | Degree embargo file curator at X     | write-metadata | Not Allowed |
      | Embargo        | Degree embargo file curator at X     | delete         | Not Allowed |
      | Degree+Embargo | Degree embargo file curator at X     | read-metadata  | Allowed     |
      | Degree+Embargo | Degree embargo file curator at X     | download       | Allowed     |
      | Degree+Embargo | Degree embargo file curator at X     | write-metadata | Allowed     |
      | Degree+Embargo | Degree embargo file curator at X     | delete         | Allowed     |

      | Degree         | File curators for other contributors | read-metadata  | Allowed     |
      | Degree         | File curators for other contributors | download       | Allowed     |
      | Degree         | File curators for other contributors | write-metadata | Not Allowed |
      | Degree         | File curators for other contributors | delete         | Not Allowed |
      | Embargo        | File curators for other contributors | read-metadata  | Allowed     |
      | Embargo        | File curators for other contributors | download       | Allowed     |
      | Embargo        | File curators for other contributors | write-metadata | Not Allowed |
      | Embargo        | File curators for other contributors | delete         | Not Allowed |
      | Degree+Embargo | File curators for other contributors | read-metadata  | Allowed     |
      | Degree+Embargo | File curators for other contributors | download       | Not Allowed |
      | Degree+Embargo | File curators for other contributors | write-metadata | Not Allowed |
      | Degree+Embargo | File curators for other contributors | delete         | Not Allowed |

      | Degree         | Everyone else                        | read-metadata  | Allowed     |
      | Degree         | Everyone else                        | download       | Allowed     |
      | Degree         | Everyone else                        | write-metadata | Not Allowed |
      | Degree         | Everyone else                        | delete         | Not Allowed |
      | Embargo        | Everyone else                        | read-metadata  | Allowed     |
      | Embargo        | Everyone else                        | download       | Not Allowed |
      | Embargo        | Everyone else                        | write-metadata | Not Allowed |
      | Embargo        | Everyone else                        | delete         | Not Allowed |
      | Degree+Embargo | Everyone else                        | read-metadata  | Allowed     |
      | Degree+Embargo | Everyone else                        | download       | Not Allowed |
      | Degree+Embargo | Everyone else                        | write-metadata | Not Allowed |
      | Degree+Embargo | Everyone else                        | delete         | Not Allowed |

      | Degree         | External client                      | read-metadata  | Allowed     |
      | Degree         | External client                      | download       | Allowed     |
      | Degree         | External client                      | write-metadata | Allowed     |
      | Degree         | External client                      | delete         | Allowed     |
      | Embargo        | External client                      | read-metadata  | Allowed     |
      | Embargo        | External client                      | download       | Allowed     |
      | Embargo        | External client                      | write-metadata | Allowed     |
      | Embargo        | External client                      | delete         | Allowed     |
      | Degree+Embargo | External client                      | read-metadata  | Allowed     |
      | Degree+Embargo | External client                      | download       | Allowed     |
      | Degree+Embargo | External client                      | write-metadata | Allowed     |
      | Degree+Embargo | External client                      | delete         | Allowed     |

  Scenario Outline: Verify Degree+Embargo file operation permissions when user is not from same organization as publication channel owner
    Given a file of type "<FileType>" with property "Degree+Embargo"
    When the user have the role "<UserRole>"
    And the file is owned by "Publication owner"
    And publication has claimed publisher
    And the user attempts to "<Operation>"
    Then the action outcome is "<Outcome>"

    Examples:
      | FileType            | UserRole                             | Operation      | Outcome     |
      | PendingOpenFile     | Publication owner at X               | read-metadata  | Allowed     |
      | PendingOpenFile     | Publication owner at X               | download       | Allowed     |
      | PendingOpenFile     | Publication owner at X               | write-metadata | Allowed     |
      | PendingOpenFile     | Publication owner at X               | delete         | Allowed     |
      | PendingInternalFile | Publication owner at X               | read-metadata  | Allowed     |
      | PendingInternalFile | Publication owner at X               | download       | Allowed     |
      | PendingInternalFile | Publication owner at X               | write-metadata | Allowed     |
      | PendingInternalFile | Publication owner at X               | delete         | Allowed     |
      | OpenFile            | Publication owner at X               | read-metadata  | Allowed     |
      | OpenFile            | Publication owner at X               | download       | Not Allowed |
      | OpenFile            | Publication owner at X               | write-metadata | Not Allowed |
      | OpenFile            | Publication owner at X               | delete         | Not Allowed |
      | InternalFile        | Publication owner at X               | read-metadata  | Allowed     |
      | InternalFile        | Publication owner at X               | download       | Not Allowed |
      | InternalFile        | Publication owner at X               | write-metadata | Not Allowed |
      | InternalFile        | Publication owner at X               | delete         | Not Allowed |
      | HiddenFile          | Publication owner at X               | read-metadata  | Not Allowed |
      | HiddenFile          | Publication owner at X               | download       | Not Allowed |
      | HiddenFile          | Publication owner at X               | write-metadata | Not Allowed |
      | HiddenFile          | Publication owner at X               | delete         | Not Allowed |

      | PendingOpenFile     | Contributor at X                     | read-metadata  | Allowed     |
      | PendingOpenFile     | Contributor at X                     | download       | Not Allowed |
      | PendingOpenFile     | Contributor at X                     | write-metadata | Not Allowed |
      | PendingOpenFile     | Contributor at X                     | delete         | Not Allowed |
      | PendingInternalFile | Contributor at X                     | read-metadata  | Allowed     |
      | PendingInternalFile | Contributor at X                     | download       | Not Allowed |
      | PendingInternalFile | Contributor at X                     | write-metadata | Not Allowed |
      | PendingInternalFile | Contributor at X                     | delete         | Not Allowed |
      | OpenFile            | Contributor at X                     | read-metadata  | Allowed     |
      | OpenFile            | Contributor at X                     | download       | Not Allowed |
      | OpenFile            | Contributor at X                     | write-metadata | Not Allowed |
      | OpenFile            | Contributor at X                     | delete         | Not Allowed |
      | InternalFile        | Contributor at X                     | read-metadata  | Allowed     |
      | InternalFile        | Contributor at X                     | download       | Not Allowed |
      | InternalFile        | Contributor at X                     | write-metadata | Not Allowed |
      | InternalFile        | Contributor at X                     | delete         | Not Allowed |
      | HiddenFile          | Contributor at X                     | read-metadata  | Not Allowed |
      | HiddenFile          | Contributor at X                     | download       | Not Allowed |
      | HiddenFile          | Contributor at X                     | write-metadata | Not Allowed |
      | HiddenFile          | Contributor at X                     | delete         | Not Allowed |

      | PendingOpenFile     | Other contributors                   | read-metadata  | Allowed     |
      | PendingOpenFile     | Other contributors                   | download       | Not Allowed |
      | PendingOpenFile     | Other contributors                   | write-metadata | Not Allowed |
      | PendingOpenFile     | Other contributors                   | delete         | Not Allowed |
      | PendingInternalFile | Other contributors                   | read-metadata  | Allowed     |
      | PendingInternalFile | Other contributors                   | download       | Not Allowed |
      | PendingInternalFile | Other contributors                   | write-metadata | Not Allowed |
      | PendingInternalFile | Other contributors                   | delete         | Not Allowed |
      | OpenFile            | Other contributors                   | read-metadata  | Allowed     |
      | OpenFile            | Other contributors                   | download       | Not Allowed |
      | OpenFile            | Other contributors                   | write-metadata | Not Allowed |
      | OpenFile            | Other contributors                   | delete         | Not Allowed |
      | InternalFile        | Other contributors                   | read-metadata  | Allowed     |
      | InternalFile        | Other contributors                   | download       | Not Allowed |
      | InternalFile        | Other contributors                   | write-metadata | Not Allowed |
      | InternalFile        | Other contributors                   | delete         | Not Allowed |
      | HiddenFile          | Other contributors                   | read-metadata  | Not Allowed |
      | HiddenFile          | Other contributors                   | download       | Not Allowed |
      | HiddenFile          | Other contributors                   | write-metadata | Not Allowed |
      | HiddenFile          | Other contributors                   | delete         | Not Allowed |

      | PendingOpenFile     | File curator at X                    | read-metadata  | Allowed     |
      | PendingOpenFile     | File curator at X                    | download       | Not Allowed |
      | PendingOpenFile     | File curator at X                    | write-metadata | Not Allowed |
      | PendingOpenFile     | File curator at X                    | delete         | Not Allowed |
      | PendingInternalFile | File curator at X                    | read-metadata  | Allowed     |
      | PendingInternalFile | File curator at X                    | download       | Not Allowed |
      | PendingInternalFile | File curator at X                    | write-metadata | Not Allowed |
      | PendingInternalFile | File curator at X                    | delete         | Not Allowed |
      | OpenFile            | File curator at X                    | read-metadata  | Allowed     |
      | OpenFile            | File curator at X                    | download       | Not Allowed |
      | OpenFile            | File curator at X                    | write-metadata | Not Allowed |
      | OpenFile            | File curator at X                    | delete         | Not Allowed |
      | InternalFile        | File curator at X                    | read-metadata  | Allowed     |
      | InternalFile        | File curator at X                    | download       | Not Allowed |
      | InternalFile        | File curator at X                    | write-metadata | Not Allowed |
      | InternalFile        | File curator at X                    | delete         | Not Allowed |
      | HiddenFile          | File curator at X                    | read-metadata  | Not Allowed |
      | HiddenFile          | File curator at X                    | download       | Not Allowed |
      | HiddenFile          | File curator at X                    | write-metadata | Not Allowed |
      | HiddenFile          | File curator at X                    | delete         | Not Allowed |

      | PendingOpenFile     | Degree file curator at X             | read-metadata  | Allowed     |
      | PendingOpenFile     | Degree file curator at X             | download       | Not Allowed |
      | PendingOpenFile     | Degree file curator at X             | write-metadata | Not Allowed |
      | PendingOpenFile     | Degree file curator at X             | delete         | Not Allowed |
      | PendingInternalFile | Degree file curator at X             | read-metadata  | Allowed     |
      | PendingInternalFile | Degree file curator at X             | download       | Not Allowed |
      | PendingInternalFile | Degree file curator at X             | write-metadata | Not Allowed |
      | PendingInternalFile | Degree file curator at X             | delete         | Not Allowed |
      | OpenFile            | Degree file curator at X             | read-metadata  | Allowed     |
      | OpenFile            | Degree file curator at X             | download       | Not Allowed |
      | OpenFile            | Degree file curator at X             | write-metadata | Not Allowed |
      | OpenFile            | Degree file curator at X             | delete         | Not Allowed |
      | InternalFile        | Degree file curator at X             | read-metadata  | Allowed     |
      | InternalFile        | Degree file curator at X             | download       | Not Allowed |
      | InternalFile        | Degree file curator at X             | write-metadata | Not Allowed |
      | InternalFile        | Degree file curator at X             | delete         | Not Allowed |
      | HiddenFile          | Degree file curator at X             | read-metadata  | Not Allowed |
      | HiddenFile          | Degree file curator at X             | download       | Not Allowed |
      | HiddenFile          | Degree file curator at X             | write-metadata | Not Allowed |
      | HiddenFile          | Degree file curator at X             | delete         | Not Allowed |

      | PendingOpenFile     | Degree embargo file curator at X     | read-metadata  | Allowed     |
      | PendingOpenFile     | Degree embargo file curator at X     | download       | Not Allowed |
      | PendingOpenFile     | Degree embargo file curator at X     | write-metadata | Not Allowed |
      | PendingOpenFile     | Degree embargo file curator at X     | delete         | Not Allowed |
      | PendingInternalFile | Degree embargo file curator at X     | read-metadata  | Allowed     |
      | PendingInternalFile | Degree embargo file curator at X     | download       | Not Allowed |
      | PendingInternalFile | Degree embargo file curator at X     | write-metadata | Not Allowed |
      | PendingInternalFile | Degree embargo file curator at X     | delete         | Not Allowed |
      | OpenFile            | Degree embargo file curator at X     | read-metadata  | Allowed     |
      | OpenFile            | Degree embargo file curator at X     | download       | Not Allowed |
      | OpenFile            | Degree embargo file curator at X     | write-metadata | Not Allowed |
      | OpenFile            | Degree embargo file curator at X     | delete         | Not Allowed |
      | InternalFile        | Degree embargo file curator at X     | read-metadata  | Allowed     |
      | InternalFile        | Degree embargo file curator at X     | download       | Not Allowed |
      | InternalFile        | Degree embargo file curator at X     | write-metadata | Not Allowed |
      | InternalFile        | Degree embargo file curator at X     | delete         | Not Allowed |
      | HiddenFile          | Degree embargo file curator at X     | read-metadata  | Allowed     |
      | HiddenFile          | Degree embargo file curator at X     | download       | Not Allowed |
      | HiddenFile          | Degree embargo file curator at X     | write-metadata | Not Allowed |
      | HiddenFile          | Degree embargo file curator at X     | delete         | Not Allowed |

      | PendingOpenFile     | File curators for other contributors | read-metadata  | Allowed     |
      | PendingOpenFile     | File curators for other contributors | download       | Not Allowed |
      | PendingOpenFile     | File curators for other contributors | write-metadata | Not Allowed |
      | PendingOpenFile     | File curators for other contributors | delete         | Not Allowed |
      | PendingInternalFile | File curators for other contributors | read-metadata  | Allowed     |
      | PendingInternalFile | File curators for other contributors | download       | Not Allowed |
      | PendingInternalFile | File curators for other contributors | write-metadata | Not Allowed |
      | PendingInternalFile | File curators for other contributors | delete         | Not Allowed |
      | OpenFile            | File curators for other contributors | read-metadata  | Allowed     |
      | OpenFile            | File curators for other contributors | download       | Not Allowed |
      | OpenFile            | File curators for other contributors | write-metadata | Not Allowed |
      | OpenFile            | File curators for other contributors | delete         | Not Allowed |
      | InternalFile        | File curators for other contributors | read-metadata  | Allowed     |
      | InternalFile        | File curators for other contributors | download       | Not Allowed |
      | InternalFile        | File curators for other contributors | write-metadata | Not Allowed |
      | InternalFile        | File curators for other contributors | delete         | Not Allowed |
      | HiddenFile          | File curators for other contributors | read-metadata  | Not Allowed |
      | HiddenFile          | File curators for other contributors | download       | Not Allowed |
      | HiddenFile          | File curators for other contributors | write-metadata | Not Allowed |
      | HiddenFile          | File curators for other contributors | delete         | Not Allowed |

      | PendingOpenFile     | Everyone else                        | read-metadata  | Not Allowed |
      | PendingOpenFile     | Everyone else                        | download       | Not Allowed |
      | PendingOpenFile     | Everyone else                        | write-metadata | Not Allowed |
      | PendingOpenFile     | Everyone else                        | delete         | Not Allowed |
      | PendingInternalFile | Everyone else                        | read-metadata  | Not Allowed |
      | PendingInternalFile | Everyone else                        | download       | Not Allowed |
      | PendingInternalFile | Everyone else                        | write-metadata | Not Allowed |
      | PendingInternalFile | Everyone else                        | delete         | Not Allowed |
      | OpenFile            | Everyone else                        | read-metadata  | Allowed     |
      | OpenFile            | Everyone else                        | download       | Not Allowed |
      | OpenFile            | Everyone else                        | write-metadata | Not Allowed |
      | OpenFile            | Everyone else                        | delete         | Not Allowed |
      | InternalFile        | Everyone else                        | read-metadata  | Not Allowed |
      | InternalFile        | Everyone else                        | download       | Not Allowed |
      | InternalFile        | Everyone else                        | write-metadata | Not Allowed |
      | InternalFile        | Everyone else                        | delete         | Not Allowed |
      | HiddenFile          | Everyone else                        | read-metadata  | Not Allowed |
      | HiddenFile          | Everyone else                        | download       | Not Allowed |
      | HiddenFile          | Everyone else                        | write-metadata | Not Allowed |
      | HiddenFile          | Everyone else                        | delete         | Not Allowed |

      | PendingOpenFile     | External client                      | read-metadata  | Not Allowed |
      | PendingOpenFile     | External client                      | download       | Not Allowed |
      | PendingOpenFile     | External client                      | write-metadata | Not Allowed |
      | PendingOpenFile     | External client                      | delete         | Not Allowed |
      | PendingInternalFile | External client                      | read-metadata  | Not Allowed |
      | PendingInternalFile | External client                      | download       | Not Allowed |
      | PendingInternalFile | External client                      | write-metadata | Not Allowed |
      | PendingInternalFile | External client                      | delete         | Not Allowed |
      | OpenFile            | External client                      | read-metadata  | Allowed     |
      | OpenFile            | External client                      | download       | Allowed     |
      | OpenFile            | External client                      | write-metadata | Allowed     |
      | OpenFile            | External client                      | delete         | Allowed     |
      | InternalFile        | External client                      | read-metadata  | Allowed     |
      | InternalFile        | External client                      | download       | Allowed     |
      | InternalFile        | External client                      | write-metadata | Allowed     |
      | InternalFile        | External client                      | delete         | Allowed     |
      | HiddenFile          | External client                      | read-metadata  | Not Allowed |
      | HiddenFile          | External client                      | download       | Not Allowed |
      | HiddenFile          | External client                      | write-metadata | Not Allowed |
      | HiddenFile          | External client                      | delete         | Not Allowed |

  Scenario Outline: Verify Degree+Embargo file operation permissions when user is from the same organization as publication channel owner
    Given a file of type "<FileType>" with property "Degree+Embargo"
    When the user have the role "<UserRole>"
    And the file is owned by "Publication owner"
    And publication has claimed publisher
    And the user belongs to the organization that claimed the publisher
    And the user attempts to "<Operation>"
    Then the action outcome is "<Outcome>"

    Examples:
      | FileType            | UserRole                             | Operation      | Outcome     |
      | PendingOpenFile     | Publication owner at X               | read-metadata  | Allowed     |
      | PendingOpenFile     | Publication owner at X               | download       | Allowed     |
      | PendingOpenFile     | Publication owner at X               | write-metadata | Allowed     |
      | PendingOpenFile     | Publication owner at X               | delete         | Allowed     |
      | PendingInternalFile | Publication owner at X               | read-metadata  | Allowed     |
      | PendingInternalFile | Publication owner at X               | download       | Allowed     |
      | PendingInternalFile | Publication owner at X               | write-metadata | Allowed     |
      | PendingInternalFile | Publication owner at X               | delete         | Allowed     |
      | OpenFile            | Publication owner at X               | read-metadata  | Allowed     |
      | OpenFile            | Publication owner at X               | download       | Not Allowed |
      | OpenFile            | Publication owner at X               | write-metadata | Not Allowed |
      | OpenFile            | Publication owner at X               | delete         | Not Allowed |
      | InternalFile        | Publication owner at X               | read-metadata  | Allowed     |
      | InternalFile        | Publication owner at X               | download       | Not Allowed |
      | InternalFile        | Publication owner at X               | write-metadata | Not Allowed |
      | InternalFile        | Publication owner at X               | delete         | Not Allowed |
      | HiddenFile          | Publication owner at X               | read-metadata  | Not Allowed |
      | HiddenFile          | Publication owner at X               | download       | Not Allowed |
      | HiddenFile          | Publication owner at X               | write-metadata | Not Allowed |
      | HiddenFile          | Publication owner at X               | delete         | Not Allowed |

      | PendingOpenFile     | Contributor at X                     | read-metadata  | Allowed     |
      | PendingOpenFile     | Contributor at X                     | download       | Not Allowed |
      | PendingOpenFile     | Contributor at X                     | write-metadata | Not Allowed |
      | PendingOpenFile     | Contributor at X                     | delete         | Not Allowed |
      | PendingInternalFile | Contributor at X                     | read-metadata  | Allowed     |
      | PendingInternalFile | Contributor at X                     | download       | Not Allowed |
      | PendingInternalFile | Contributor at X                     | write-metadata | Not Allowed |
      | PendingInternalFile | Contributor at X                     | delete         | Not Allowed |
      | OpenFile            | Contributor at X                     | read-metadata  | Allowed     |
      | OpenFile            | Contributor at X                     | download       | Not Allowed |
      | OpenFile            | Contributor at X                     | write-metadata | Not Allowed |
      | OpenFile            | Contributor at X                     | delete         | Not Allowed |
      | InternalFile        | Contributor at X                     | read-metadata  | Allowed     |
      | InternalFile        | Contributor at X                     | download       | Not Allowed |
      | InternalFile        | Contributor at X                     | write-metadata | Not Allowed |
      | InternalFile        | Contributor at X                     | delete         | Not Allowed |
      | HiddenFile          | Contributor at X                     | read-metadata  | Not Allowed |
      | HiddenFile          | Contributor at X                     | download       | Not Allowed |
      | HiddenFile          | Contributor at X                     | write-metadata | Not Allowed |
      | HiddenFile          | Contributor at X                     | delete         | Not Allowed |

      | PendingOpenFile     | Other contributors                   | read-metadata  | Allowed     |
      | PendingOpenFile     | Other contributors                   | download       | Not Allowed |
      | PendingOpenFile     | Other contributors                   | write-metadata | Not Allowed |
      | PendingOpenFile     | Other contributors                   | delete         | Not Allowed |
      | PendingInternalFile | Other contributors                   | read-metadata  | Allowed     |
      | PendingInternalFile | Other contributors                   | download       | Not Allowed |
      | PendingInternalFile | Other contributors                   | write-metadata | Not Allowed |
      | PendingInternalFile | Other contributors                   | delete         | Not Allowed |
      | OpenFile            | Other contributors                   | read-metadata  | Allowed     |
      | OpenFile            | Other contributors                   | download       | Not Allowed |
      | OpenFile            | Other contributors                   | write-metadata | Not Allowed |
      | OpenFile            | Other contributors                   | delete         | Not Allowed |
      | InternalFile        | Other contributors                   | read-metadata  | Allowed     |
      | InternalFile        | Other contributors                   | download       | Not Allowed |
      | InternalFile        | Other contributors                   | write-metadata | Not Allowed |
      | InternalFile        | Other contributors                   | delete         | Not Allowed |
      | HiddenFile          | Other contributors                   | read-metadata  | Not Allowed |
      | HiddenFile          | Other contributors                   | download       | Not Allowed |
      | HiddenFile          | Other contributors                   | write-metadata | Not Allowed |
      | HiddenFile          | Other contributors                   | delete         | Not Allowed |

      | PendingOpenFile     | File curator at X                    | read-metadata  | Allowed     |
      | PendingOpenFile     | File curator at X                    | download       | Not Allowed |
      | PendingOpenFile     | File curator at X                    | write-metadata | Not Allowed |
      | PendingOpenFile     | File curator at X                    | delete         | Not Allowed |
      | PendingInternalFile | File curator at X                    | read-metadata  | Allowed     |
      | PendingInternalFile | File curator at X                    | download       | Not Allowed |
      | PendingInternalFile | File curator at X                    | write-metadata | Not Allowed |
      | PendingInternalFile | File curator at X                    | delete         | Not Allowed |
      | OpenFile            | File curator at X                    | read-metadata  | Allowed     |
      | OpenFile            | File curator at X                    | download       | Not Allowed |
      | OpenFile            | File curator at X                    | write-metadata | Not Allowed |
      | OpenFile            | File curator at X                    | delete         | Not Allowed |
      | InternalFile        | File curator at X                    | read-metadata  | Allowed     |
      | InternalFile        | File curator at X                    | download       | Not Allowed |
      | InternalFile        | File curator at X                    | write-metadata | Not Allowed |
      | InternalFile        | File curator at X                    | delete         | Not Allowed |
      | HiddenFile          | File curator at X                    | read-metadata  | Not Allowed |
      | HiddenFile          | File curator at X                    | download       | Not Allowed |
      | HiddenFile          | File curator at X                    | write-metadata | Not Allowed |
      | HiddenFile          | File curator at X                    | delete         | Not Allowed |

      | PendingOpenFile     | Degree file curator at X             | read-metadata  | Allowed     |
      | PendingOpenFile     | Degree file curator at X             | download       | Not Allowed |
      | PendingOpenFile     | Degree file curator at X             | write-metadata | Not Allowed |
      | PendingOpenFile     | Degree file curator at X             | delete         | Not Allowed |
      | PendingInternalFile | Degree file curator at X             | read-metadata  | Allowed     |
      | PendingInternalFile | Degree file curator at X             | download       | Not Allowed |
      | PendingInternalFile | Degree file curator at X             | write-metadata | Not Allowed |
      | PendingInternalFile | Degree file curator at X             | delete         | Not Allowed |
      | OpenFile            | Degree file curator at X             | read-metadata  | Allowed     |
      | OpenFile            | Degree file curator at X             | download       | Not Allowed |
      | OpenFile            | Degree file curator at X             | write-metadata | Not Allowed |
      | OpenFile            | Degree file curator at X             | delete         | Not Allowed |
      | InternalFile        | Degree file curator at X             | read-metadata  | Allowed     |
      | InternalFile        | Degree file curator at X             | download       | Not Allowed |
      | InternalFile        | Degree file curator at X             | write-metadata | Not Allowed |
      | InternalFile        | Degree file curator at X             | delete         | Not Allowed |
      | HiddenFile          | Degree file curator at X             | read-metadata  | Not Allowed |
      | HiddenFile          | Degree file curator at X             | download       | Not Allowed |
      | HiddenFile          | Degree file curator at X             | write-metadata | Not Allowed |
      | HiddenFile          | Degree file curator at X             | delete         | Not Allowed |

      | PendingOpenFile     | Degree embargo file curator at X     | read-metadata  | Allowed     |
      | PendingOpenFile     | Degree embargo file curator at X     | download       | Allowed     |
      | PendingOpenFile     | Degree embargo file curator at X     | write-metadata | Allowed     |
      | PendingOpenFile     | Degree embargo file curator at X     | delete         | Allowed     |
      | PendingInternalFile | Degree embargo file curator at X     | read-metadata  | Allowed     |
      | PendingInternalFile | Degree embargo file curator at X     | download       | Allowed     |
      | PendingInternalFile | Degree embargo file curator at X     | write-metadata | Allowed     |
      | PendingInternalFile | Degree embargo file curator at X     | delete         | Allowed     |
      | OpenFile            | Degree embargo file curator at X     | read-metadata  | Allowed     |
      | OpenFile            | Degree embargo file curator at X     | download       | Allowed     |
      | OpenFile            | Degree embargo file curator at X     | write-metadata | Allowed     |
      | OpenFile            | Degree embargo file curator at X     | delete         | Allowed     |
      | InternalFile        | Degree embargo file curator at X     | read-metadata  | Allowed     |
      | InternalFile        | Degree embargo file curator at X     | download       | Allowed     |
      | InternalFile        | Degree embargo file curator at X     | write-metadata | Allowed     |
      | InternalFile        | Degree embargo file curator at X     | delete         | Allowed     |
      | HiddenFile          | Degree embargo file curator at X     | read-metadata  | Allowed     |
      | HiddenFile          | Degree embargo file curator at X     | download       | Allowed     |
      | HiddenFile          | Degree embargo file curator at X     | write-metadata | Allowed     |
      | HiddenFile          | Degree embargo file curator at X     | delete         | Allowed     |

      | PendingOpenFile     | Degree embargo file curator          | read-metadata  | Allowed     |
      | PendingOpenFile     | Degree embargo file curator          | download       | Allowed     |
      | PendingOpenFile     | Degree embargo file curator          | write-metadata | Allowed     |
      | PendingOpenFile     | Degree embargo file curator          | delete         | Allowed     |
      | PendingInternalFile | Degree embargo file curator          | read-metadata  | Allowed     |
      | PendingInternalFile | Degree embargo file curator          | download       | Allowed     |
      | PendingInternalFile | Degree embargo file curator          | write-metadata | Allowed     |
      | PendingInternalFile | Degree embargo file curator          | delete         | Allowed     |
      | OpenFile            | Degree embargo file curator          | read-metadata  | Allowed     |
      | OpenFile            | Degree embargo file curator          | download       | Allowed     |
      | OpenFile            | Degree embargo file curator          | write-metadata | Allowed     |
      | OpenFile            | Degree embargo file curator          | delete         | Allowed     |
      | InternalFile        | Degree embargo file curator          | read-metadata  | Allowed     |
      | InternalFile        | Degree embargo file curator          | download       | Allowed     |
      | InternalFile        | Degree embargo file curator          | write-metadata | Allowed     |
      | InternalFile        | Degree embargo file curator          | delete         | Allowed     |
      | HiddenFile          | Degree embargo file curator          | read-metadata  | Allowed     |
      | HiddenFile          | Degree embargo file curator          | download       | Allowed     |
      | HiddenFile          | Degree embargo file curator          | write-metadata | Allowed     |
      | HiddenFile          | Degree embargo file curator          | delete         | Allowed     |

      | PendingOpenFile     | File curators for other contributors | read-metadata  | Allowed     |
      | PendingOpenFile     | File curators for other contributors | download       | Not Allowed |
      | PendingOpenFile     | File curators for other contributors | write-metadata | Not Allowed |
      | PendingOpenFile     | File curators for other contributors | delete         | Not Allowed |
      | PendingInternalFile | File curators for other contributors | read-metadata  | Allowed     |
      | PendingInternalFile | File curators for other contributors | download       | Not Allowed |
      | PendingInternalFile | File curators for other contributors | write-metadata | Not Allowed |
      | PendingInternalFile | File curators for other contributors | delete         | Not Allowed |
      | OpenFile            | File curators for other contributors | read-metadata  | Allowed     |
      | OpenFile            | File curators for other contributors | download       | Not Allowed |
      | OpenFile            | File curators for other contributors | write-metadata | Not Allowed |
      | OpenFile            | File curators for other contributors | delete         | Not Allowed |
      | InternalFile        | File curators for other contributors | read-metadata  | Allowed     |
      | InternalFile        | File curators for other contributors | download       | Not Allowed |
      | InternalFile        | File curators for other contributors | write-metadata | Not Allowed |
      | InternalFile        | File curators for other contributors | delete         | Not Allowed |
      | HiddenFile          | File curators for other contributors | read-metadata  | Not Allowed |
      | HiddenFile          | File curators for other contributors | download       | Not Allowed |
      | HiddenFile          | File curators for other contributors | write-metadata | Not Allowed |
      | HiddenFile          | File curators for other contributors | delete         | Not Allowed |

      | PendingOpenFile     | Everyone else                        | read-metadata  | Not Allowed |
      | PendingOpenFile     | Everyone else                        | download       | Not Allowed |
      | PendingOpenFile     | Everyone else                        | write-metadata | Not Allowed |
      | PendingOpenFile     | Everyone else                        | delete         | Not Allowed |
      | PendingInternalFile | Everyone else                        | read-metadata  | Not Allowed |
      | PendingInternalFile | Everyone else                        | download       | Not Allowed |
      | PendingInternalFile | Everyone else                        | write-metadata | Not Allowed |
      | PendingInternalFile | Everyone else                        | delete         | Not Allowed |
      | OpenFile            | Everyone else                        | read-metadata  | Allowed     |
      | OpenFile            | Everyone else                        | download       | Not Allowed |
      | OpenFile            | Everyone else                        | write-metadata | Not Allowed |
      | OpenFile            | Everyone else                        | delete         | Not Allowed |
      | InternalFile        | Everyone else                        | read-metadata  | Not Allowed |
      | InternalFile        | Everyone else                        | download       | Not Allowed |
      | InternalFile        | Everyone else                        | write-metadata | Not Allowed |
      | InternalFile        | Everyone else                        | delete         | Not Allowed |
      | HiddenFile          | Everyone else                        | read-metadata  | Not Allowed |
      | HiddenFile          | Everyone else                        | download       | Not Allowed |
      | HiddenFile          | Everyone else                        | write-metadata | Not Allowed |
      | HiddenFile          | Everyone else                        | delete         | Not Allowed |

      | PendingOpenFile     | External client                      | read-metadata  | Not Allowed |
      | PendingOpenFile     | External client                      | download       | Not Allowed |
      | PendingOpenFile     | External client                      | write-metadata | Not Allowed |
      | PendingOpenFile     | External client                      | delete         | Not Allowed |
      | PendingInternalFile | External client                      | read-metadata  | Not Allowed |
      | PendingInternalFile | External client                      | download       | Not Allowed |
      | PendingInternalFile | External client                      | write-metadata | Not Allowed |
      | PendingInternalFile | External client                      | delete         | Not Allowed |
      | OpenFile            | External client                      | read-metadata  | Allowed     |
      | OpenFile            | External client                      | download       | Allowed     |
      | OpenFile            | External client                      | write-metadata | Allowed     |
      | OpenFile            | External client                      | delete         | Allowed     |
      | InternalFile        | External client                      | read-metadata  | Allowed     |
      | InternalFile        | External client                      | download       | Allowed     |
      | InternalFile        | External client                      | write-metadata | Allowed     |
      | InternalFile        | External client                      | delete         | Allowed     |
      | HiddenFile          | External client                      | read-metadata  | Not Allowed |
      | HiddenFile          | External client                      | download       | Not Allowed |
      | HiddenFile          | External client                      | write-metadata | Not Allowed |
      | HiddenFile          | External client                      | delete         | Not Allowed |
