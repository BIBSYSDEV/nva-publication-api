Feature: Rules that apply for Artistic results


  Scenario Outline: Cristin Result "Musical Performance, Visual Arts, Film production,
  Musical Composition, Achitectural Design, Theatrical Production" is converted to NVA Resource type Artistic and correct sub-type
    Given a valid Cristin Result with secondary category "<secondaryCategory>"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a Publication Instance of type "<contentType>"
    Examples:
      | secondaryCategory | contentType            |
      | FILMPRODUKSJON    | MovingPicture          |
#      | MUSIKK_FRAMFORIN  | MusicalWorkPerformance |
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


#    movie length used to determine short film etc.