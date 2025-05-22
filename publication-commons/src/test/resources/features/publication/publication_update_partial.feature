Feature: Publication update permissions
  As a system user
  I want publication permission to be enforced based on publication, user role and channel claims
  So that only authorized users can perform operation

  Scenario Outline: Verify operation when
    Given a "published" publication with "<PublicationProperty>" property
    When the user have the role "<UserRole>"
    And publisher is claimed by organization
    And the user is from the same organization as claimed publisher
    And the user attempts to "<Operation>"
    Then the action outcome is "<Outcome>"

    Examples:
      | UserRole                | PublicationProperty          | Operation      | Outcome     |
      | Publication owner       | Degree,Imported              | partial-update | Allowed     |
      | Contributor             | Degree,Imported              | partial-update | Allowed     |
      | Degree file curator     | Degree,Imported              | partial-update | Allowed     |
      | Everyone else           | Degree,Imported              | partial-update | Not Allowed |
      | Related external client | Degree,Imported              | partial-update | Allowed     |
      | Publication owner       | Degree                       | partial-update | Allowed     |
      | Contributor             | Degree                       | partial-update | Allowed     |
      | Degree file curator     | Degree                       | partial-update | Allowed     |
      | Everyone else           | Degree                       | partial-update | Not Allowed |
      | Related external client | Degree                       | partial-update | Allowed     |
      | Publication owner       | Imported                     | partial-update | Allowed     |
      | Contributor             | Imported                     | partial-update | Allowed     |
      | Degree file curator     | Imported                     | partial-update | Allowed     |
      | Everyone else           | Imported                     | partial-update | Not Allowed |
      | Related external client | Imported                     | partial-update | Allowed     |
      | Publication owner       |                              | partial-update | Allowed     |
      | Contributor             |                              | partial-update | Allowed     |
      | Degree file curator     |                              | partial-update | Allowed     |
      | Everyone else           |                              | partial-update | Not Allowed |
      | Related external client |                              | partial-update | Allowed     |
      | Publication owner       | MetadataOnly,Degree,Imported | partial-update | Allowed     |
      | Contributor             | MetadataOnly,Degree,Imported | partial-update | Allowed     |
      | Degree file curator     | MetadataOnly,Degree,Imported | partial-update | Allowed     |
      | Everyone else           | MetadataOnly,Degree,Imported | partial-update | Not Allowed |
      | Related external client | MetadataOnly,Degree,Imported | partial-update | Allowed     |
      | Publication owner       | MetadataOnly,Degree          | partial-update | Allowed     |
      | Contributor             | MetadataOnly,Degree          | partial-update | Allowed     |
      | Degree file curator     | MetadataOnly,Degree          | partial-update | Allowed     |
      | Everyone else           | MetadataOnly,Degree          | partial-update | Not Allowed |
      | Related external client | MetadataOnly,Degree          | partial-update | Allowed     |
      | Publication owner       | MetadataOnly,Imported        | partial-update | Allowed     |
      | Contributor             | MetadataOnly,Imported        | partial-update | Allowed     |
      | Degree file curator     | MetadataOnly,Imported        | partial-update | Allowed     |
      | Everyone else           | MetadataOnly,Imported        | partial-update | Not Allowed |
      | Related external client | MetadataOnly,Imported        | partial-update | Allowed     |
      | Publication owner       | MetadataOnly                 | partial-update | Allowed     |
      | Contributor             | MetadataOnly                 | partial-update | Allowed     |
      | Degree file curator     | MetadataOnly                 | partial-update | Allowed     |
      | Everyone else           | MetadataOnly                 | partial-update | Not Allowed |
      | Related external client | MetadataOnly                 | partial-update | Allowed     |
      | Publication owner       | Degree,Imported              | update         | Not Allowed |
      | Contributor             | Degree,Imported              | update         | Not Allowed |
      | Degree file curator     | Degree,Imported              | update         | Allowed     |
      | Everyone else           | Degree,Imported              | update         | Not Allowed |
      | Related external client | Degree,Imported              | update         | Allowed     |
      | Publication owner       | Degree                       | update         | Not Allowed |
      | Contributor             | Degree                       | update         | Not Allowed |
      | Degree file curator     | Degree                       | update         | Allowed     |
      | Everyone else           | Degree                       | update         | Not Allowed |
      | Related external client | Degree                       | update         | Allowed     |
      | Publication owner       | Imported                     | update         | Allowed     |
      | Contributor             | Imported                     | update         | Allowed     |
      | Degree file curator     | Imported                     | update         | Allowed     |
      | Everyone else           | Imported                     | update         | Not Allowed |
      | Related external client | Imported                     | update         | Allowed     |
      | Publication owner       |                              | update         | Allowed     |
      | Contributor             |                              | update         | Allowed     |
      | Degree file curator     |                              | update         | Allowed     |
      | Everyone else           |                              | update         | Not Allowed |
      | Related external client |                              | update         | Allowed     |
      | Publication owner       | MetadataOnly,Degree,Imported | update         | Not Allowed |
      | Contributor             | MetadataOnly,Degree,Imported | update         | Not Allowed |
      | Degree file curator     | MetadataOnly,Degree,Imported | update         | Allowed     |
      | Everyone else           | MetadataOnly,Degree,Imported | update         | Not Allowed |
      | Related external client | MetadataOnly,Degree,Imported | update         | Allowed     |
      | Publication owner       | MetadataOnly,Degree          | update         | Not Allowed |
      | Contributor             | MetadataOnly,Degree          | update         | Not Allowed |
      | Degree file curator     | MetadataOnly,Degree          | update         | Allowed     |
      | Everyone else           | MetadataOnly,Degree          | update         | Not Allowed |
      | Related external client | MetadataOnly,Degree          | update         | Allowed     |
      | Publication owner       | MetadataOnly,Imported        | update         | Allowed     |
      | Contributor             | MetadataOnly,Imported        | update         | Allowed     |
      | Degree file curator     | MetadataOnly,Imported        | update         | Allowed     |
      | Everyone else           | MetadataOnly,Imported        | update         | Not Allowed |
      | Related external client | MetadataOnly,Imported        | update         | Allowed     |
      | Publication owner       | MetadataOnly                 | update         | Allowed     |
      | Contributor             | MetadataOnly                 | update         | Allowed     |
      | Degree file curator     | MetadataOnly                 | update         | Allowed     |
      | Everyone else           | MetadataOnly                 | update         | Not Allowed |
      | Related external client | MetadataOnly                 | update         | Allowed     |