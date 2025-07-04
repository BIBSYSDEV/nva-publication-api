Feature: Publication action permissions for publication status DELETED

  Scenario Outline: Verify publication permissions when publication has status DELETED and user relates to publication
    Given a "publication"
    And publication has status "deleted"
    And the user have the role "<UserRole>"
    When the user attempts to "<Operation>"
    And the user belongs to "creating institution"
    Then the action outcome is "<Outcome>"

    Examples:
      | UserRole                | Operation                 | Outcome     |
      | Publication creator     | update                    | Allowed     |
      | Contributor             | update                    | Allowed     |
      | Publishing curator      | update                    | Allowed     |
      | NVI curator             | update                    | Allowed     |
      | DOI curator             | update                    | Allowed     |
      | Support curator         | update                    | Allowed     |
      | Thesis curator          | update                    | Allowed     |
      | Embargo thesis curator  | update                    | Allowed     |
      | Editor                  | update                    | Allowed     |
      | Related external client | update                    | Allowed     |

      | Publication creator     | partial-update            | Allowed     |
      | Contributor             | partial-update            | Allowed     |
      | Publishing curator      | partial-update            | Allowed     |
      | NVI curator             | partial-update            | Allowed     |
      | DOI curator             | partial-update            | Allowed     |
      | Support curator         | partial-update            | Allowed     |
      | Thesis curator          | partial-update            | Allowed     |
      | Embargo thesis curator  | partial-update            | Allowed     |
      | Editor                  | partial-update            | Allowed     |
      | Related external client | partial-update            | Allowed     |

      | Publication creator     | read-hidden-files         | Not Allowed |
      | Contributor             | read-hidden-files         | Not Allowed |
      | Publishing curator      | read-hidden-files         | Allowed     |
      | NVI curator             | read-hidden-files         | Not Allowed |
      | DOI curator             | read-hidden-files         | Not Allowed |
      | Support curator         | read-hidden-files         | Not Allowed |
      | Thesis curator          | read-hidden-files         | Not Allowed |
      | Embargo thesis curator  | read-hidden-files         | Not Allowed |
      | Editor                  | read-hidden-files         | Allowed     |
      | Related external client | read-hidden-files         | Allowed     |

      | Publication creator     | unpublish                 | Not Allowed |
      | Contributor             | unpublish                 | Not Allowed |
      | Publishing curator      | unpublish                 | Not Allowed |
      | NVI curator             | unpublish                 | Not Allowed |
      | DOI curator             | unpublish                 | Not Allowed |
      | Support curator         | unpublish                 | Not Allowed |
      | Thesis curator          | unpublish                 | Not Allowed |
      | Embargo thesis curator  | unpublish                 | Not Allowed |
      | Editor                  | unpublish                 | Not Allowed |
      | Related external client | unpublish                 | Allowed     |

      | Publication creator     | republish                 | Not Allowed |
      | Contributor             | republish                 | Not Allowed |
      | Publishing curator      | republish                 | Not Allowed |
      | NVI curator             | republish                 | Not Allowed |
      | DOI curator             | republish                 | Not Allowed |
      | Support curator         | republish                 | Not Allowed |
      | Thesis curator          | republish                 | Not Allowed |
      | Embargo thesis curator  | republish                 | Not Allowed |
      | Editor                  | republish                 | Not Allowed |
      | Related external client | republish                 | Not Allowed |

      | Publication creator     | delete                    | Not Allowed |
      | Contributor             | delete                    | Not Allowed |
      | Publishing curator      | delete                    | Not Allowed |
      | NVI curator             | delete                    | Not Allowed |
      | DOI curator             | delete                    | Not Allowed |
      | Support curator         | delete                    | Not Allowed |
      | Thesis curator          | delete                    | Not Allowed |
      | Embargo thesis curator  | delete                    | Not Allowed |
      | Editor                  | delete                    | Not Allowed |
      | Related external client | delete                    | Not Allowed |

      | Publication creator     | terminate                 | Not Allowed |
      | Contributor             | terminate                 | Not Allowed |
      | Publishing curator      | terminate                 | Not Allowed |
      | NVI curator             | terminate                 | Not Allowed |
      | DOI curator             | terminate                 | Not Allowed |
      | Support curator         | terminate                 | Not Allowed |
      | Thesis curator          | terminate                 | Not Allowed |
      | Embargo thesis curator  | terminate                 | Not Allowed |
      | Editor                  | terminate                 | Not Allowed |
      | Related external client | terminate                 | Allowed     |

      | Publication creator     | doi-request-create        | Allowed     |
      | Contributor             | doi-request-create        | Allowed     |
      | Publishing curator      | doi-request-create        | Allowed     |
      | NVI curator             | doi-request-create        | Allowed     |
      | DOI curator             | doi-request-create        | Allowed     |
      | Support curator         | doi-request-create        | Allowed     |
      | Thesis curator          | doi-request-create        | Allowed     |
      | Embargo thesis curator  | doi-request-create        | Allowed     |
      | Editor                  | doi-request-create        | Allowed     |
      | Related external client | doi-request-create        | Not Allowed |

      | Publication creator     | doi-request-approve       | Not Allowed |
      | Contributor             | doi-request-approve       | Not Allowed |
      | Publishing curator      | doi-request-approve       | Not Allowed |
      | NVI curator             | doi-request-approve       | Not Allowed |
      | DOI curator             | doi-request-approve       | Allowed     |
      | Support curator         | doi-request-approve       | Not Allowed |
      | Thesis curator          | doi-request-approve       | Not Allowed |
      | Embargo thesis curator  | doi-request-approve       | Not Allowed |
      | Editor                  | doi-request-approve       | Not Allowed |
      | Related external client | doi-request-approve       | Not Allowed |

      | Publication creator     | publishing-request-create | Allowed     |
      | Contributor             | publishing-request-create | Allowed     |
      | Publishing curator      | publishing-request-create | Allowed     |
      | NVI curator             | publishing-request-create | Allowed     |
      | DOI curator             | publishing-request-create | Allowed     |
      | Support curator         | publishing-request-create | Allowed     |
      | Thesis curator          | publishing-request-create | Allowed     |
      | Embargo thesis curator  | publishing-request-create | Allowed     |
      | Editor                  | publishing-request-create | Allowed     |
      | Related external client | publishing-request-create | Not Allowed |

      | Publication creator     | approve-files             | Not Allowed |
      | Contributor             | approve-files             | Not Allowed |
      | Publishing curator      | approve-files             | Allowed     |
      | NVI curator             | approve-files             | Not Allowed |
      | DOI curator             | approve-files             | Not Allowed |
      | Support curator         | approve-files             | Not Allowed |
      | Thesis curator          | approve-files             | Not Allowed |
      | Embargo thesis curator  | approve-files             | Not Allowed |
      | Editor                  | approve-files             | Not Allowed |
      | Related external client | approve-files             | Not Allowed |

      | Publication creator     | support-request-create    | Allowed     |
      | Contributor             | support-request-create    | Allowed     |
      | Publishing curator      | support-request-create    | Allowed     |
      | NVI curator             | support-request-create    | Allowed     |
      | DOI curator             | support-request-create    | Allowed     |
      | Support curator         | support-request-create    | Allowed     |
      | Thesis curator          | support-request-create    | Allowed     |
      | Embargo thesis curator  | support-request-create    | Allowed     |
      | Editor                  | support-request-create    | Allowed     |
      | Related external client | support-request-create    | Not Allowed |

      | Publication creator     | support-request-approve   | Not Allowed |
      | Contributor             | support-request-approve   | Not Allowed |
      | Publishing curator      | support-request-approve   | Not Allowed |
      | NVI curator             | support-request-approve   | Not Allowed |
      | DOI curator             | support-request-approve   | Not Allowed |
      | Support curator         | support-request-approve   | Allowed     |
      | Thesis curator          | support-request-approve   | Not Allowed |
      | Embargo thesis curator  | support-request-approve   | Not Allowed |
      | Editor                  | support-request-approve   | Not Allowed |
      | Related external client | support-request-approve   | Not Allowed |

      | Publication creator     | upload-file               | Not Allowed |
      | Contributor             | upload-file               | Not Allowed |
      | Publishing curator      | upload-file               | Not Allowed |
      | NVI curator             | upload-file               | Not Allowed |
      | DOI curator             | upload-file               | Not Allowed |
      | Support curator         | upload-file               | Not Allowed |
      | Thesis curator          | upload-file               | Not Allowed |
      | Embargo thesis curator  | upload-file               | Not Allowed |
      | Editor                  | upload-file               | Not Allowed |
      | Related external client | upload-file               | Not Allowed |

  Scenario Outline: Verify publication permissions when publication has status DELETED and user does not relate to publication
    Given a "publication"
    And publication has status "deleted"
    And the user have the role "<UserRole>"
    And the user belongs to "non curating institution"
    When the user attempts to "<Operation>"
    Then the action outcome is "<Outcome>"

    Examples:
      | UserRole                    | Operation                 | Outcome     |
      | Unauthenticated             | update                    | Not Allowed |
      | Authenticated               | update                    | Not Allowed |
      | Publishing curator          | update                    | Not Allowed |
      | NVI curator                 | update                    | Not Allowed |
      | DOI curator                 | update                    | Not Allowed |
      | Support curator             | update                    | Not Allowed |
      | Thesis curator              | update                    | Not Allowed |
      | Embargo thesis curator      | update                    | Not Allowed |
      | Editor                      | update                    | Allowed     |
      | Not related external client | update                    | Not Allowed |

      | Unauthenticated             | partial-update            | Not Allowed |
      | Authenticated               | partial-update            | Not Allowed |
      | Publishing curator          | partial-update            | Not Allowed |
      | NVI curator                 | partial-update            | Not Allowed |
      | DOI curator                 | partial-update            | Not Allowed |
      | Support curator             | partial-update            | Not Allowed |
      | Thesis curator              | partial-update            | Not Allowed |
      | Embargo thesis curator      | partial-update            | Not Allowed |
      | Editor                      | partial-update            | Allowed     |
      | Not related external client | partial-update            | Not Allowed |

      | Unauthenticated             | read-hidden-files         | Not Allowed |
      | Authenticated               | read-hidden-files         | Not Allowed |
      | Publishing curator          | read-hidden-files         | Not Allowed |
      | NVI curator                 | read-hidden-files         | Not Allowed |
      | DOI curator                 | read-hidden-files         | Not Allowed |
      | Support curator             | read-hidden-files         | Not Allowed |
      | Thesis curator              | read-hidden-files         | Not Allowed |
      | Embargo thesis curator      | read-hidden-files         | Not Allowed |
      | Editor                      | read-hidden-files         | Not Allowed |
      | Not related external client | read-hidden-files         | Not Allowed |

      | Unauthenticated             | unpublish                 | Not Allowed |
      | Authenticated               | unpublish                 | Not Allowed |
      | Publishing curator          | unpublish                 | Not Allowed |
      | NVI curator                 | unpublish                 | Not Allowed |
      | DOI curator                 | unpublish                 | Not Allowed |
      | Support curator             | unpublish                 | Not Allowed |
      | Thesis curator              | unpublish                 | Not Allowed |
      | Embargo thesis curator      | unpublish                 | Not Allowed |
      | Editor                      | unpublish                 | Not Allowed |
      | Not related external client | unpublish                 | Not Allowed |

      | Unauthenticated             | republish                 | Not Allowed |
      | Authenticated               | republish                 | Not Allowed |
      | Publishing curator          | republish                 | Not Allowed |
      | NVI curator                 | republish                 | Not Allowed |
      | DOI curator                 | republish                 | Not Allowed |
      | Support curator             | republish                 | Not Allowed |
      | Thesis curator              | republish                 | Not Allowed |
      | Embargo thesis curator      | republish                 | Not Allowed |
      | Editor                      | republish                 | Not Allowed |
      | Not related external client | republish                 | Not Allowed |

      | Unauthenticated             | delete                    | Not Allowed |
      | Authenticated               | delete                    | Not Allowed |
      | Publishing curator          | delete                    | Not Allowed |
      | NVI curator                 | delete                    | Not Allowed |
      | DOI curator                 | delete                    | Not Allowed |
      | Support curator             | delete                    | Not Allowed |
      | Thesis curator              | delete                    | Not Allowed |
      | Embargo thesis curator      | delete                    | Not Allowed |
      | Editor                      | delete                    | Not Allowed |
      | Not related external client | delete                    | Not Allowed |

      | Unauthenticated             | terminate                 | Not Allowed |
      | Authenticated               | terminate                 | Not Allowed |
      | Publishing curator          | terminate                 | Not Allowed |
      | NVI curator                 | terminate                 | Not Allowed |
      | DOI curator                 | terminate                 | Not Allowed |
      | Support curator             | terminate                 | Not Allowed |
      | Thesis curator              | terminate                 | Not Allowed |
      | Embargo thesis curator      | terminate                 | Not Allowed |
      | Editor                      | terminate                 | Not Allowed |
      | Not related external client | terminate                 | Not Allowed |

      | Unauthenticated             | doi-request-create        | Not Allowed |
      | Authenticated               | doi-request-create        | Not Allowed |
      | Publishing curator          | doi-request-create        | Not Allowed |
      | NVI curator                 | doi-request-create        | Not Allowed |
      | DOI curator                 | doi-request-create        | Not Allowed |
      | Support curator             | doi-request-create        | Not Allowed |
      | Thesis curator              | doi-request-create        | Not Allowed |
      | Embargo thesis curator      | doi-request-create        | Not Allowed |
      | Editor                      | doi-request-create        | Not Allowed |
      | Not related external client | doi-request-create        | Not Allowed |

      | Unauthenticated             | doi-request-approve       | Not Allowed |
      | Authenticated               | doi-request-approve       | Not Allowed |
      | Publishing curator          | doi-request-approve       | Not Allowed |
      | NVI curator                 | doi-request-approve       | Not Allowed |
      | DOI curator                 | doi-request-approve       | Not Allowed |
      | Support curator             | doi-request-approve       | Not Allowed |
      | Thesis curator              | doi-request-approve       | Not Allowed |
      | Embargo thesis curator      | doi-request-approve       | Not Allowed |
      | Editor                      | doi-request-approve       | Not Allowed |
      | Not related external client | doi-request-approve       | Not Allowed |

      | Unauthenticated             | publishing-request-create | Not Allowed |
      | Authenticated               | publishing-request-create | Not Allowed |
      | Publishing curator          | publishing-request-create | Not Allowed |
      | NVI curator                 | publishing-request-create | Not Allowed |
      | DOI curator                 | publishing-request-create | Not Allowed |
      | Support curator             | publishing-request-create | Not Allowed |
      | Thesis curator              | publishing-request-create | Not Allowed |
      | Embargo thesis curator      | publishing-request-create | Not Allowed |
      | Editor                      | publishing-request-create | Not Allowed |
      | Not related external client | publishing-request-create | Not Allowed |

      | Unauthenticated             | approve-files             | Not Allowed |
      | Authenticated               | approve-files             | Not Allowed |
      | Publishing curator          | approve-files             | Not Allowed |
      | NVI curator                 | approve-files             | Not Allowed |
      | DOI curator                 | approve-files             | Not Allowed |
      | Support curator             | approve-files             | Not Allowed |
      | Thesis curator              | approve-files             | Not Allowed |
      | Embargo thesis curator      | approve-files             | Not Allowed |
      | Editor                      | approve-files             | Not Allowed |
      | Not related external client | approve-files             | Not Allowed |

      | Unauthenticated             | support-request-create    | Not Allowed |
      | Authenticated               | support-request-create    | Not Allowed |
      | Publishing curator          | support-request-create    | Not Allowed |
      | NVI curator                 | support-request-create    | Not Allowed |
      | DOI curator                 | support-request-create    | Not Allowed |
      | Support curator             | support-request-create    | Not Allowed |
      | Thesis curator              | support-request-create    | Not Allowed |
      | Embargo thesis curator      | support-request-create    | Not Allowed |
      | Editor                      | support-request-create    | Not Allowed |
      | Not related external client | support-request-create    | Not Allowed |

      | Unauthenticated             | support-request-approve   | Not Allowed |
      | Authenticated               | support-request-approve   | Not Allowed |
      | Publishing curator          | support-request-approve   | Not Allowed |
      | NVI curator                 | support-request-approve   | Not Allowed |
      | DOI curator                 | support-request-approve   | Not Allowed |
      | Support curator             | support-request-approve   | Not Allowed |
      | Thesis curator              | support-request-approve   | Not Allowed |
      | Embargo thesis curator      | support-request-approve   | Not Allowed |
      | Editor                      | support-request-approve   | Not Allowed |
      | Not related external client | support-request-approve   | Not Allowed |

      | Unauthenticated             | upload-file               | Not Allowed |
      | Authenticated               | upload-file               | Not Allowed |
      | Publishing curator          | upload-file               | Not Allowed |
      | NVI curator                 | upload-file               | Not Allowed |
      | DOI curator                 | upload-file               | Not Allowed |
      | Support curator             | upload-file               | Not Allowed |
      | Thesis curator              | upload-file               | Not Allowed |
      | Embargo thesis curator      | upload-file               | Not Allowed |
      | Editor                      | upload-file               | Not Allowed |
      | Not related external client | upload-file               | Not Allowed |
