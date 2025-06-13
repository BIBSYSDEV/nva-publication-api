Feature: File metadata read and file download permissions
  As a system user
  I want file metadata read and file download permissions to be enforced based on file state and user role
  So that only authorized users can read the metadata

  Scenario Outline: Verify file metadata read permissions when user is from file owners institution
    Given a file of type "<FileType>"
    And the file is owned by "publication creator"
    When the user have the role "<UserRole>"
    And the user attempts to "read-metadata"
    Then the action outcome is "<Outcome>"

    Examples:
      | FileType            | UserRole                | Outcome     |
      | UploadedFile        | Unauthenticated         | Not Allowed |
      | UploadedFile        | Authenticated           | Not Allowed |
      | UploadedFile        | Publication creator     | Allowed     |
      | UploadedFile        | Contributor             | Not Allowed |
      | UploadedFile        | Publishing curator      | Allowed     |
      | UploadedFile        | Related external client | Not Allowed |

      | PendingOpenFile     | Unauthenticated         | Not Allowed |
      | PendingOpenFile     | Authenticated           | Not Allowed |
      | PendingOpenFile     | Publication creator     | Allowed     |
      | PendingOpenFile     | Contributor             | Allowed     |
      | PendingOpenFile     | Publishing curator      | Allowed     |
      | PendingOpenFile     | Related external client | Not Allowed |

      | PendingInternalFile | Unauthenticated         | Not Allowed |
      | PendingInternalFile | Authenticated           | Not Allowed |
      | PendingInternalFile | Publication creator     | Allowed     |
      | PendingInternalFile | Contributor             | Allowed     |
      | PendingInternalFile | Publishing curator      | Allowed     |
      | PendingInternalFile | Related external client | Not Allowed |

      | OpenFile            | Unauthenticated         | Allowed     |
      | OpenFile            | Authenticated           | Allowed     |
      | OpenFile            | Publication creator     | Allowed     |
      | OpenFile            | Contributor             | Allowed     |
      | OpenFile            | Publishing curator      | Allowed     |
      | OpenFile            | Related external client | Allowed     |

      | InternalFile        | Unauthenticated         | Not Allowed |
      | InternalFile        | Authenticated           | Not Allowed |
      | InternalFile        | Publication creator     | Allowed     |
      | InternalFile        | Contributor             | Allowed     |
      | InternalFile        | Publishing curator      | Allowed     |
      | InternalFile        | Related external client | Allowed     |

      | HiddenFile          | Authenticated           | Not Allowed |
      | HiddenFile          | Unauthenticated         | Not Allowed |
      | HiddenFile          | Publication creator     | Not Allowed |
      | HiddenFile          | Contributor             | Not Allowed |
      | HiddenFile          | Publishing curator      | Allowed     |
      | HiddenFile          | Related external client | Not Allowed |

  Scenario Outline: Verify file metadata read permissions when user is not from file owners institution
    Given a file of type "<FileType>"
    And the file is owned by "publication creator"
    When the user have the role "<UserRole>"
    And the user belongs to "curating institution"
    And the user attempts to "read-metadata"
    Then the action outcome is "<Outcome>"

    Examples:
      | FileType            | UserRole                    | Outcome     |
      | UploadedFile        | Authenticated               | Not Allowed |
      | UploadedFile        | Contributor                 | Not Allowed |
      | UploadedFile        | Publishing curator          | Not Allowed |
      | UploadedFile        | Not related external client | Not Allowed |

      | PendingOpenFile     | Authenticated               | Not Allowed |
      | PendingOpenFile     | Contributor                 | Allowed     |
      | PendingOpenFile     | Publishing curator          | Allowed     |
      | PendingOpenFile     | Not related external client | Not Allowed |

      | PendingInternalFile | Authenticated               | Not Allowed |
      | PendingInternalFile | Contributor                 | Allowed     |
      | PendingInternalFile | Publishing curator          | Allowed     |
      | PendingInternalFile | Not related external client | Not Allowed |

      | OpenFile            | Authenticated               | Allowed     |
      | OpenFile            | Contributor                 | Allowed     |
      | OpenFile            | Publishing curator          | Allowed     |
      | OpenFile            | Not related external client | Allowed     |

      | InternalFile        | Authenticated               | Not Allowed |
      | InternalFile        | Contributor                 | Allowed     |
      | InternalFile        | Publishing curator          | Allowed     |
      | InternalFile        | Not related external client | Not Allowed |

      | HiddenFile          | Authenticated               | Not Allowed |
      | HiddenFile          | Contributor                 | Not Allowed |
      | HiddenFile          | Publishing curator          | Not Allowed |
      | HiddenFile          | Not related external client | Not Allowed |
