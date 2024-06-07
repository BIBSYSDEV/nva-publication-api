Feature: Mapping of "Abstract" entries

  Background:
    Given a valid Cristin Result with secondary category "SAMMENDRAG"

  Scenario: Cristin Result of type "Abstract" maps to NVA "ConferenceAbstract".
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a Publication Instance of type "ConferenceAbstract"