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
      | Degree+Embargo | Publication owner at X               | download       | Allowed     |
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