Feature: File metadata write and file delete permissions
  As a system user
  I want file metadata write (editing and deletion) to have clear permissions
  So that only authorized users can perform those actions based on the file state

  Background:
  The publication metadata is published. X is an affiliation and this affiliation is given by your login context.

  Scenario Outline: Verify file write permissions when user is from file owners institution
    Given a file of type "<FileType>"
    And the file is owned by "publication creator"
    When the user have the role "<UserRole>"
    And the user attempts to "write-metadata"
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
      | PendingOpenFile     | Contributor             | Not Allowed |
      | PendingOpenFile     | Publishing curator      | Allowed     |
      | PendingOpenFile     | Related external client | Not Allowed |

      | PendingInternalFile | Authenticated           | Not Allowed |
      | PendingInternalFile | Unauthenticated         | Not Allowed |
      | PendingInternalFile | Publication creator     | Allowed     |
      | PendingInternalFile | Contributor             | Not Allowed |
      | PendingInternalFile | Publishing curator      | Allowed     |
      | PendingInternalFile | Related external client | Not Allowed |

      | OpenFile            | Unauthenticated         | Not Allowed |
      | OpenFile            | Authenticated           | Not Allowed |
      | OpenFile            | Publication creator     | Not Allowed |
      | OpenFile            | Contributor             | Not Allowed |
      | OpenFile            | Publishing curator      | Allowed     |
      | OpenFile            | Related external client | Allowed     |

      | InternalFile        | Unauthenticated         | Not Allowed |
      | InternalFile        | Authenticated           | Not Allowed |
      | InternalFile        | Publication creator     | Not Allowed |
      | InternalFile        | Contributor             | Not Allowed |
      | InternalFile        | Publishing curator      | Allowed     |
      | InternalFile        | Related external client | Allowed     |

      | HiddenFile          | Unauthenticated         | Not Allowed |
      | HiddenFile          | Authenticated           | Not Allowed |
      | HiddenFile          | Publication creator     | Not Allowed |
      | HiddenFile          | Contributor             | Not Allowed |
      | HiddenFile          | Publishing curator      | Allowed     |
      | HiddenFile          | Related external client | Not Allowed |

    # TODO: Add scenario for when user is from non curating institution?
  Scenario Outline: Verify file write permissions when user is not from file owners institution
    Given a file of type "<FileType>"
    And the file is owned by "publication creator"
    When the user have the role "<UserRole>"
    And the user belongs to "curating institution"
    And the user attempts to "write-metadata"
    Then the action outcome is "<Outcome>"

    Examples:
      | FileType            | UserRole                    | Outcome     |
      | UploadedFile        | Contributor                 | Not Allowed |
      | UploadedFile        | Publishing curator          | Not Allowed |
      | UploadedFile        | Authenticated               | Not Allowed |
      | UploadedFile        | Not related external client | Not Allowed |

      | PendingOpenFile     | Contributor                 | Not Allowed |
      | PendingOpenFile     | Publishing curator          | Not Allowed |
      | PendingOpenFile     | Authenticated               | Not Allowed |
      | PendingOpenFile     | Not related external client | Not Allowed |

      | PendingInternalFile | Contributor                 | Not Allowed |
      | PendingInternalFile | Publishing curator          | Not Allowed |
      | PendingInternalFile | Authenticated               | Not Allowed |
      | PendingInternalFile | Not related external client | Not Allowed |

      | OpenFile            | Contributor                 | Not Allowed |
      | OpenFile            | Publishing curator          | Not Allowed |
      | OpenFile            | Authenticated               | Not Allowed |
      | OpenFile            | Not related external client | Not Allowed |

      | InternalFile        | Contributor                 | Not Allowed |
      | InternalFile        | Publishing curator          | Not Allowed |
      | InternalFile        | Authenticated               | Not Allowed |
      | InternalFile        | Not related external client | Not Allowed |

      | HiddenFile          | Contributor                 | Not Allowed |
      | HiddenFile          | Publishing curator          | Not Allowed |
      | HiddenFile          | Authenticated               | Not Allowed |
      | HiddenFile          | Not related external client | Not Allowed |
