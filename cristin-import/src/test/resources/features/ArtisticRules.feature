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
      | KUNST_OG_BILDE    | VisualArts       |
      | MUSIKK_KOMP       | MusicPerformance |
      | ARKITEKTTEGNING   | Architecture     |
      | TEATERPRODUKSJON  | PerformingArts   |



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
    And the performance has an event with values:
      | start               | title                                           | place     | end                 |
      | 2018-02-03T00:00:00 | Celebratory Concert of Swiss Philosophy Society | Göttingen | 2020-02-03T00:00:00 |
    And the performance has an original composer "Dániel Péter Biró"
    When the Cristin Result is converted to an NVA Resource
    Then the Nva Resource has a Concert announcements
    And the concert has a place "Göttingen"
    And the concert has a date "2018-02-03T00:00:00"
    And the concert has a duration of "35" minutes
    And the concert has a program with title "Celebratory Concert of Swiss Philosophy Society"
    And the concert has a program with composer "Dániel Péter Biró"
    And the concert has a program that is a premiere

  Scenario: Cristin musical performance that is not a concert should be mapped to OtherPerformance
    Given a valid Cristin Result with secondary category "MUSIKK_FRAMFORIN"
    And the performance type is equal to "null"
    And the performance has a duration of "35" minutes
    And the performance has an event with values:
      | start               | title                                           | place     |
      | 2018-02-03T00:00:00 | Celebratory Concert of Swiss Philosophy Society | Göttingen |
    And the performance has an original composer "Dániel Péter Biró"
    When the Cristin Result is converted to an NVA Resource
    Then the Nva Resource has a OtherPerformance
    And the OtherPerformance has a place "Göttingen"
    And the OtherPerformance has a duration "35" minutes
    And the OtherPerformance has a musicalWorkPerformance with title "Celebratory Concert of Swiss Philosophy Society"
    And the OtherPerformance has a composer "Dániel Péter Biró"

  Scenario: Cristin musical performance that contains ISRC should be mapped to AudioVisualPublication
    Given a valid Cristin Result with secondary category "MUSIKK_FRAMFORIN"
    And the performance has a ISRC equal to "NOLCA1554010"
    And the performance has a duration of "35" minutes
    And the performance has a medium equal to "CD"
    And the performance has a publisher name equal to "Austad Music"
    When the Cristin Result is converted to an NVA Resource
    Then the Nva resource has a AudioVisualPublication
    And the AudioVisualPublication has a mediaSubType equalTo "CompactDisc"
    And the AudioVisualPublication has ISRC equalTo "NOLCA1554010",
    And the AudioVisualPublication has an unconfirmedPublisher name equal to "Austad Music"


  Scenario Outline: Cristin musical performance that contains valid medium types shoul be mapped to AudioVisualProduction
    Given a valid Cristin Result with secondary category "MUSIKK_FRAMFORIN"
    And the performance has a medium equal to "<medium>"
    When the Cristin Result is converted to an NVA Resource
    Then the Nva resource has a AudioVisualPublication
    And the AudioVisualPublication has a mediaSubType equalTo "<mediaSubType>"
    Examples:
      | medium            | mediaSubType |
      | cd-inpsilling     | CompactDisc  |
      | plateinnspilling  | Vinyl        |
      | digitalinspilling | DigitalFile  |
      | Strømming         | Streaming    |
      | album             | CompactDisc  |
      | LP-inspilling     | Vinyl        |
      | mp3               | DigitalFile  |
      | spotify           | Streaming    |
      | stream            | Streaming    |
      | YouTube           | Streaming    |
      | vimeo             | Streaming    |
      | Lydfil            | DigitalFile  |


  Scenario: A cristin result with valid ISMN preserved the ISMN when converting it to NVA resource
    Given a valid Cristin Result with secondary category "MUSIKK_FRAMFORIN"
    And the performance has a ISMN equal to "M-2306-7118-7"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA resource has a MusicScore
    And the MusicScore has a ISMN equal to "M-2306-7118-7"

  Scenario: A cristin result with ensemble name should be mapped to musicScore
    Given a valid Cristin Result with secondary category "MUSIKK_FRAMFORIN"
    And the performance has a ensemble name equal to "Koret og symfoniorkesteret ved Høgskulen i Volda"
    When the Cristin Result is converted to an NVA Resource
    And the MusicScore has ensemble equal to "Koret og symfoniorkesteret ved Høgskulen i Volda"

  Scenario: certain fields from cristin artistic is mapped to nva description
    Given a valid Cristin Result with secondary category "MUSIKK_FRAMFORIN"
    And the performance has a field besetning with value "janitsjar"
    And the performance has a field medskapere with value "Nordmann, Ola: vokal & keys"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA resource has a description field containing the value "janitsjar"
    And the NVA resource has a description field containing the value "Nordmann, Ola: vokal & keys"

  Scenario: A cristin theatrical production is mapped to a NVA theatrical production
    Given a valid Cristin Result with secondary category "TEATERPRODUKSJON"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a Publication Instance of type "PerformingArts"
    And the performingArts has a subtype "THEATRICAL_PRODUCTION"


  Scenario: A cristin theatrical production events are mapped to NVA
    Given a valid Cristin Result with secondary category "TEATERPRODUKSJON"
    And the performance has an event with values:
      | start               | title                                           | place     | end                 |
      | 1996-11-19T00:00:00 | Celebratory Concert of Swiss Philosophy Society | Göttingen | 1996-12-01T00:00:00 |
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a Publication Instance of type "PerformingArts"
    And the theatrical production has a PerformingArtsVenue with value:
      | place     | to                  | from                | sequence |
      | Göttingen | 1996-12-01T00:00:00 | 1996-11-19T00:00:00 | 0        |


  Scenario: A cristin Architecture should be mapped to NVA architecture with subtype other
    Given a valid Cristin Result with secondary category "ARKITEKTTEGNING"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a Publication Instance of type "Architecture"
    And the Architecture has a subtype other with descripion "Migrert fra cristin"