Feature: File metadata read and file download permissions with different publication statuses
  As an unprivileged user
  I want to be able to access file metadata and file download when not logged in or no rights to the file

  Scenario Outline: Verify file metadata read permissions
    Given a file of type "<FileType>"
    And publication has status "<PublicationStatus>"
    When the user have the role "<UserRole>"
    And the user attempts to "read-metadata"
    Then the action outcome is "<Outcome>"

    Examples:
      | FileType            | UserRole        | PublicationStatus | Outcome     |
      | UploadedFile        | Unauthenticated | PUBLISHED         | Not Allowed |
      | UploadedFile        | Unauthenticated | DRAFT             | Not Allowed |
      | UploadedFile        | Authenticated   | PUBLISHED         | Not Allowed |
      | UploadedFile        | Authenticated   | DRAFT             | Not Allowed |

      | PendingOpenFile     | Unauthenticated | PUBLISHED         | Not Allowed |
      | PendingOpenFile     | Unauthenticated | DRAFT             | Not Allowed |
      | PendingOpenFile     | Authenticated   | PUBLISHED         | Not Allowed |
      | PendingOpenFile     | Authenticated   | DRAFT             | Not Allowed |

      | PendingInternalFile | Unauthenticated | PUBLISHED         | Not Allowed |
      | PendingInternalFile | Unauthenticated | DRAFT             | Not Allowed |
      | PendingInternalFile | Authenticated   | PUBLISHED         | Not Allowed |
      | PendingInternalFile | Authenticated   | DRAFT             | Not Allowed |

      | OpenFile            | Unauthenticated | PUBLISHED         | Allowed     |
      | OpenFile            | Unauthenticated | DRAFT             | Not Allowed |
      | OpenFile            | Authenticated   | PUBLISHED         | Allowed     |
      | OpenFile            | Authenticated   | DRAFT             | Not Allowed |

      | InternalFile        | Unauthenticated | PUBLISHED         | Not Allowed |
      | InternalFile        | Unauthenticated | DRAFT             | Not Allowed |
      | InternalFile        | Authenticated   | PUBLISHED         | Not Allowed |
      | InternalFile        | Authenticated   | DRAFT             | Not Allowed |

      | HiddenFile          | Unauthenticated | PUBLISHED         | Not Allowed |
      | HiddenFile          | Unauthenticated | DRAFT             | Not Allowed |
      | HiddenFile          | Authenticated   | PUBLISHED         | Not Allowed |
      | HiddenFile          | Authenticated   | DRAFT             | Not Allowed |
