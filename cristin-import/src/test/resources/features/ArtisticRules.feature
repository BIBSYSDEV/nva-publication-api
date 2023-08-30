Feature: Rules that apply for Artistic results


  Scenario Outline: Cristin Result "Musical Performance, Visual Arts, Film production,
  Musical Composition, Achitectural Design, Theatrical Production" is converted to NVA Resource type Artistic and correct sub-type
    Given a valid Cristin Result with secondary category "<secondaryCategory>"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a Publication Instance of type "<contentType>"
    Examples:
      | secondaryCategory | contentType      |
      | FILMPRODUKSJON    | MovingPicture    |
      | MUSIKK_FRAMFORIN  | MusicPerformance |
#      | KUNST_OG_BILDE    | VisualArts             |
#      | MUSIKK_KOMP       | MusicalWork            |
#      | ARKITEKTTEGNING   | Architecture           |
#      | TEATERPRODUKSJON  | PerformingArts         |



  Scenario: if a cristin object both have type_kunstneriskproduksjon and type_produkt present,
  the type_kunstneriskproduksjon is used for scraping. This scenario only occurs once in the august dataset:
  cristin-id 1641426
    Given a valid Cristin Result with secondary category "FILMPRODUKSJON"
    And the Cristin result with both type_kunstneriskproduksjon and type_produkt present
    When the Cristin Result is converted to an NVA Resource
    Then the NVA resources contains the data scraped from type_kunstneriskproduksjon


  Scenario Outline: The duration of the Cristin film production determines what subtype MoviePicture has.
    Given a valid Cristin Result with secondary category "FILMPRODUKSJON"
    And the cristin result has a "<metadataFields>" present with duration equal to "<duration>" minutes
    When the Cristin Result is converted to an NVA Resource
    Then the Cristin Result contains a MovingPictureSubtypeEnum equal to "<movingPictureSubtypeEnum>"
    Examples:
      | metadataFields             | duration | movingPictureSubtypeEnum |
      | type_kunstneriskproduksjon | 20       | ShortFilm                |
      | type_produkt               | 20       | ShortFilm                |
      | type_kunstneriskproduksjon | 50       | Film                     |
      | type_produkt               | 50       | Film                     |


  Scenario: If there is no duration field of the Cristin film production,
  the nva MovingPictureSubtype is MovingPictureOther
    Given a valid Cristin Result with secondary category "FILMPRODUKSJON"
    And the Cristin result with both type_kunstneriskproduksjon and type_produkt present
    And the cristin result lack the duration in both metadata fields
    When the Cristin Result is converted to an NVA Resource
    Then the Cristin Result contains a MovingPictureSubtypeEnum equal to "MovingPictureOther"



  Scenario: Cristin musical performance result with Concert performance should be mapped to Nva-publication
    Given a valid Cristin Result with secondary category "MUSIKK_FRAMFORIN"
    And the performance type is equal to "KONSERT"
    And the performance is a premiere
    And the performance has a duration of "35" minutes
    And the performance has an event start of "2018-02-03T00:00:00", title "Celebratory Concert of Swiss Philosophy Society", place equal to "Göttingen"
    And the performance has an original composer "Dániel Péter Biró"
    When the Cristin Result is converted to an NVA Resource
    Then the Nva Resource has a Concert announcements
    And the concert has a place "Göttingen", date "2018-02-03T00:00:00", and duration "35" minutes
    And the concert has a program with title "Celebratory Concert of Swiss Philosophy Society", composer "Dániel Péter Biró", and is a premiere

  Scenario: Cristin musical performance that is not a concert should be mapped to OtherPerformance
    Given a valid Cristin Result with secondary category "MUSIKK_FRAMFORIN"
    And the performance type is equal to "null"
    And the performance has a duration of "35" minutes
    And the performance has an event start of "2018-02-03T00:00:00", title "Celebratory Concert of Swiss Philosophy Society", place equal to "Göttingen"
    And the performance has an original composer "Dániel Péter Biró"
    When the Cristin Result is converted to an NVA Resource
    Then the Nva Resource has a OtherPerformance
    And the OtherPerformance has a place "Göttingen" and duration "35" minutes
    And the OtherPerformance has a musicalWorkPerformance with title "Celebratory Concert of Swiss Philosophy Society", composer "Dániel Péter Biró"

  Scenario: Cristin musical performance that contains ISRC should be mapped to AudioVisualPublication
    Given a valid Cristin Result with secondary category "MUSIKK_FRAMFORIN"
    And the performance has a ISRC equal to "NOLCA1554010"
    And the performance has a duration of "35" minutes
    And the performance has a medium equal to "CD"
    And the performance has a publisher name equal to "Austad Music"
    When the Cristin Result is converted to an NVA Resource
    Then the Nva resource has a AudioVisualPublication
    And the AudioVisualPublication has a mediaSubType equalTo "CD", ISRC equalTo "NOLCA1554010", unconfirmedPublisher name equal to "Austad Music"

