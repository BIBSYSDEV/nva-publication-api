Feature:
  Mapping rules that apply for merging of existing publication in nva with brage-migration publication

  Background: matching brage publication and nva publication
    Given a brage publication with cristin identifier "1234"
    And a nva publication with cristin identifier "1234"

  Scenario: The brage-archive handle does not match the handle present in the NVA-publication, but the NVA-publication lacks associatedArtifacts
    Given a brage publication with handle "https://hdl.handle.net/11250/2506045"
    And the nva publication has main handle "https://hdl.handle.net/11250/1234567"
    And the brage publication has a file with values:
      | filename | identifier                           | mimeType                                                                  | size | lisense | publisherAuthority | embargoDate | publishedDate                  |
      | a.pptx   | 9ba7b547-36ee-467b-b145-12bc5b3b9d9e | application/vnd.openxmlformats-officedocument.presentationml.presentation | 123  | null    | false              | null        | 2023-10-20T11:48:59.131046060Z |
    And the nva publication has no associatedArtifacts
    When the nva publications are merged
    Then the merged nva publication has a file with values:
      | filename | identifier                           | mimeType                                                                  | size | lisense | publisherAuthority | embargoDate | publishedDate                  |
      | a.pptx   | 9ba7b547-36ee-467b-b145-12bc5b3b9d9e | application/vnd.openxmlformats-officedocument.presentationml.presentation | 123  | null    | false              | null        | 2023-10-20T11:48:59.131046060Z |
    And the merged nva publication has a handle equal to "https://hdl.handle.net/11250/2506045" in additional identifiers
    And the merged nva publication has a root level handle equal to "https://hdl.handle.net/11250/1234567"
