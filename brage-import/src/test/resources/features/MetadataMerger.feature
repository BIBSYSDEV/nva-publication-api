Feature:
  Mapping rules that apply for merging of existing publication in nva with brage-migration publication for metadata

  Background: matching brage publication and nva publication
    Given a brage publication with cristin identifier "1234"
    And a nva publication with cristin identifier "1234"

  Scenario: If the NVA publication has a handle present, the Brage handle should be stored as an additionalIdentifeir
    Given a brage publication with handle "https://hdl.handle.net/11250/2506045"
    And the nva publication has main handle "https://hdl.handle.net/11250/1234567"
    When the nva publications are merged
    Then the NVA publication has an additionalIdentifier with type "handle" and value "https://hdl.handle.net/11250/2506045"

  Scenario: If the NVA publication is missing contributors, the contributors from the brage publication should be kept
    Given a brage publication with an creator with properties:
      | name          | role    |  sequence |
      | navn, navnesen | Creator |  1        |
    And a NVA publication without contributors
    When the nva publications are merged
    Then the NVA publication has a contributor with properties:
      | name          | role    |  sequence |
      | navn, navnesen | Creator |  1        |