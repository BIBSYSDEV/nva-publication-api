Feature: File metadata read and file download permissions
  As a system user
  I want file metadata read and file download permissions to be enforced based on file state and user role
  So that only authorized users can read the metadata

  Scenario Outline: Verify file metadata read permissions when user is from file owners institution
    Given a file of type "<FileType>"
    And the file is owned by "publication creator"
    When the user have the role "<UserRole>"
    And the user belongs to "creating institution"
    And the user attempts to "read-metadata"
    Then the action outcome is "<Outcome>"

    Examples:
      | FileType            | UserRole                | Outcome     |
      | UploadedFile        | Unauthenticated         | Not Allowed |
      | UploadedFile        | Authenticated           | Not Allowed |
      | UploadedFile        | Publication creator     | Allowed     |
      | UploadedFile        | Contributor             | Not Allowed |
      | UploadedFile        | Publishing curator      | Allowed     |
      | UploadedFile        | Support Curator         | Allowed     |
      | UploadedFile        | DOI Curator             | Not Allowed |
      | UploadedFile        | NVI Curator             | Not Allowed |
      | UploadedFile        | Related external client | Not Allowed |
      | UploadedFile        | Editor                  | Not Allowed |

      | PendingOpenFile     | Unauthenticated         | Not Allowed |
      | PendingOpenFile     | Authenticated           | Not Allowed |
      | PendingOpenFile     | Publication creator     | Allowed     |
      | PendingOpenFile     | Contributor             | Allowed     |
      | PendingOpenFile     | Publishing curator      | Allowed     |
      | PendingOpenFile     | Support Curator         | Allowed     |
      | PendingOpenFile     | DOI Curator             | Allowed     |
      | PendingOpenFile     | NVI Curator             | Allowed     |
      | PendingOpenFile     | Related external client | Not Allowed |
      | PendingOpenFile     | Editor                  | Allowed     |

      | PendingInternalFile | Unauthenticated         | Not Allowed |
      | PendingInternalFile | Authenticated           | Not Allowed |
      | PendingInternalFile | Publication creator     | Allowed     |
      | PendingInternalFile | Contributor             | Allowed     |
      | PendingInternalFile | Publishing curator      | Allowed     |
      | PendingInternalFile | Support Curator         | Allowed     |
      | PendingInternalFile | DOI Curator             | Allowed     |
      | PendingInternalFile | NVI Curator             | Allowed     |
      | PendingInternalFile | Related external client | Not Allowed |
      | PendingInternalFile | Editor                  | Allowed     |

      | OpenFile            | Unauthenticated         | Allowed     |
      | OpenFile            | Authenticated           | Allowed     |
      | OpenFile            | Publication creator     | Allowed     |
      | OpenFile            | Contributor             | Allowed     |
      | OpenFile            | Publishing curator      | Allowed     |
      | OpenFile            | Support Curator         | Allowed     |
      | OpenFile            | DOI Curator             | Allowed     |
      | OpenFile            | NVI Curator             | Allowed     |
      | OpenFile            | Related external client | Allowed     |
      | OpenFile            | Editor                  | Allowed     |

      | InternalFile        | Unauthenticated         | Not Allowed |
      | InternalFile        | Authenticated           | Not Allowed |
      | InternalFile        | Publication creator     | Allowed     |
      | InternalFile        | Contributor             | Allowed     |
      | InternalFile        | Publishing curator      | Allowed     |
      | InternalFile        | Support Curator         | Allowed     |
      | InternalFile        | DOI Curator             | Allowed     |
      | InternalFile        | NVI Curator             | Allowed     |
      | InternalFile        | Related external client | Allowed     |
      | InternalFile        | Editor                  | Allowed     |

      | HiddenFile          | Authenticated           | Not Allowed |
      | HiddenFile          | Unauthenticated         | Not Allowed |
      | HiddenFile          | Publication creator     | Not Allowed |
      | HiddenFile          | Contributor             | Not Allowed |
      | HiddenFile          | Publishing curator      | Allowed     |
      | HiddenFile          | Support Curator         | Not Allowed |
      | HiddenFile          | DOI Curator             | Not Allowed |
      | HiddenFile          | NVI Curator             | Not Allowed |
      | HiddenFile          | Related external client | Not Allowed |
      | HiddenFile          | Editor                  | Not Allowed |

  Scenario Outline: Verify file metadata read permissions when user is not from file owners institution, but still belongs to contributing institution
    Given a file of type "<FileType>"
    And the file is owned by "someone else"
    When the user have the role "<UserRole>"
    And the user belongs to "creating institution"
    And the user attempts to "read-metadata"
    Then the action outcome is "<Outcome>"

    Examples:
      | FileType            | UserRole                    | Outcome     |
      | UploadedFile        | Authenticated               | Not Allowed |
      | UploadedFile        | Contributor                 | Not Allowed |
      | UploadedFile        | Publishing curator          | Not Allowed |
      | UploadedFile        | Support Curator             | Not Allowed |
      | UploadedFile        | DOI Curator                 | Not Allowed |
      | UploadedFile        | NVI Curator                 | Not Allowed |
      | UploadedFile        | Not related external client | Not Allowed |
      | UploadedFile        | Editor                      | Not Allowed |

      | PendingOpenFile     | Authenticated               | Not Allowed |
      | PendingOpenFile     | Contributor                 | Allowed     |
      | PendingOpenFile     | Publishing curator          | Allowed     |
      | PendingOpenFile     | Support Curator             | Allowed     |
      | PendingOpenFile     | DOI Curator                 | Allowed     |
      | PendingOpenFile     | NVI Curator                 | Allowed     |
      | PendingOpenFile     | Not related external client | Not Allowed |
      | PendingOpenFile     | Editor                      | Allowed     |

      | PendingInternalFile | Authenticated               | Not Allowed |
      | PendingInternalFile | Contributor                 | Allowed     |
      | PendingInternalFile | Publishing curator          | Allowed     |
      | PendingInternalFile | Support Curator             | Allowed     |
      | PendingInternalFile | DOI Curator                 | Allowed     |
      | PendingInternalFile | NVI Curator                 | Allowed     |
      | PendingInternalFile | Not related external client | Not Allowed |
      | PendingInternalFile | Editor                      | Allowed     |

      | OpenFile            | Authenticated               | Allowed     |
      | OpenFile            | Contributor                 | Allowed     |
      | OpenFile            | Publishing curator          | Allowed     |
      | OpenFile            | Support Curator             | Allowed     |
      | OpenFile            | DOI Curator                 | Allowed     |
      | OpenFile            | NVI Curator                 | Allowed     |
      | OpenFile            | Not related external client | Allowed     |
      | OpenFile            | Editor                      | Allowed     |

      | InternalFile        | Authenticated               | Not Allowed |
      | InternalFile        | Contributor                 | Allowed     |
      | InternalFile        | Publishing curator          | Allowed     |
      | InternalFile        | Support Curator             | Allowed     |
      | InternalFile        | DOI Curator                 | Allowed     |
      | InternalFile        | NVI Curator                 | Allowed     |
      | InternalFile        | Not related external client | Not Allowed |
      | InternalFile        | Editor                      | Allowed     |

      | HiddenFile          | Authenticated               | Not Allowed |
      | HiddenFile          | Contributor                 | Not Allowed |
      | HiddenFile          | Publishing curator          | Not Allowed |
      | HiddenFile          | Support Curator             | Not Allowed |
      | HiddenFile          | DOI Curator                 | Not Allowed |
      | HiddenFile          | NVI Curator                 | Not Allowed |
      | HiddenFile          | Not related external client | Not Allowed |
      | HiddenFile          | Editor                      | Not Allowed |

  Scenario Outline: Verify file metadata read permissions when user is not from file owners institution or contributing institution
    Given a file of type "<FileType>"
    And the file is owned by "publication creator"
    When the user have the role "<UserRole>"
    And the user belongs to "non curating institution"
    And the user attempts to "read-metadata"
    Then the action outcome is "<Outcome>"

    Examples:
      | FileType            | UserRole           | Outcome     |
      | UploadedFile        | Authenticated      | Not Allowed |
      | UploadedFile        | Contributor        | Not Allowed |
      | UploadedFile        | Publishing curator | Not Allowed |
      | UploadedFile        | Editor             | Not Allowed |

      | PendingOpenFile     | Authenticated      | Not Allowed |
      | PendingOpenFile     | Contributor        | Allowed     |
      | PendingOpenFile     | Publishing curator | Not Allowed |
      | PendingOpenFile     | Editor             | Allowed     |

      | PendingInternalFile | Authenticated      | Not Allowed |
      | PendingInternalFile | Contributor        | Allowed     |
      | PendingInternalFile | Publishing curator | Not Allowed |
      | PendingInternalFile | Editor             | Not Allowed |

      | OpenFile            | Authenticated      | Allowed     |
      | OpenFile            | Contributor        | Allowed     |
      | OpenFile            | Publishing curator | Allowed     |
      | OpenFile            | Editor             | Allowed     |

      | InternalFile        | Authenticated      | Not Allowed |
      | InternalFile        | Contributor        | Allowed     |
      | InternalFile        | Publishing curator | Not Allowed |
      | InternalFile        | Editor             | Allowed     |

      | HiddenFile          | Authenticated      | Not Allowed |
      | HiddenFile          | Contributor        | Not Allowed |
      | HiddenFile          | Publishing curator | Not Allowed |
      | HiddenFile          | Editor             | Not Allowed |


  Scenario: Verify file download permissions when user is not contributing institution and PendingOpenFile has embargo
    Given a file of type "PendingOpenFile"
    And the file is owned by "publication creator"
    And the file has embargo
    When the user have the role "Editor"
    And the user belongs to "non curating institution"
    And the user attempts to "download"
    Then the action outcome is "Not Allowed"