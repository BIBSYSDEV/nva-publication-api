Feature: Book conversion rules


  Scenario: Cristin Result "Academic anthology/Conference proceedings" is converted to
  NVA Resource of type BookAnthology
    Given a valid Cristin Result with secondary category "ANTOLOGI"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a Publication Instance of type "BookAnthology"


  Scenario: Cristin Result "Academic anthology/Conference proceedings" is converted to NVA Resource
  with Publication Context of type "Book"
    Given a valid Cristin Result with secondary category "ANTOLOGI"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a PublicationContext of type "Book"

  Scenario Outline: Cristin Result "Academic Monograph, Non-fiction Monograph, Popular Science Monograph,
  Textbook, Encyclopedia, Exhibition Catalog" is converted to NVA Resource type BookMonograph and correct sub-type
    Given a valid Cristin Result with secondary category "<secondaryCategory>"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a Publication Instance of type "<contentType>"
    Examples:
      | secondaryCategory | contentType             |
      | MONOGRAFI         | AcademicMonograph       |
      | LÆREBOK           | Textbook                |
      | FAGBOK            | NonFictionMonograph     |
      | LEKSIKON          | Encyclopedia            |
      | POPVIT_BOK        | PopularScienceMonograph |
      | OPPSLAGSVERK      | Encyclopedia            |
      | UTSTILLINGSKAT    | ExhibitionCatalog       |
      | KOMMENTARUTG      | AcademicMonograph       |

  Scenario: Cristin Result "Academic monograph" is converted to NVA Resource with Publication Context
  of type "Book"
    Given a valid Cristin Result with secondary category "MONOGRAFI"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a PublicationContext of type "Book"


  Scenario Outline: ISBN-10 values for Books are transformed to ISBN-13 values.
    Given a valid Cristin Result with secondary category "<secondaryCategory>"
    And that the Cristin Result has a non empty Book Report
    And the Book Report has an ISBN version 10 with value "8247151464"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a PublicationContext with an ISBN list containing the value "9788247151464"
    Examples:
      | secondaryCategory |
      | MONOGRAFI         |
      | ANTOLOGI          |

  Scenario Outline: ISBN values only containing "0" or combination with "-" should be mapped to null.
    Given a valid Cristin Result with secondary category "MONOGRAFI"
    And that the Cristin Result has a non empty Book Report
    And the Book Report has an ISBN field with value "<ISBN>"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a PublicationContext with an empty ISBN list
    Examples:
      | ISBN              |
      | 00000000000       |
      | 000-0-0000-0000-0 |


  Scenario Outline: "Pages" value is copied as is.
    Given a valid Cristin Result with secondary category "<secondaryCategory>"
    And that the Cristin Result has a non empty Book Report
    And the Book Report has a "total number of pages" entry equal to "<pages>"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a PublicationContext with number of pages equal to "<pages>"
    Examples:
      | pages        | secondaryCategory |
      | 10           | MONOGRAFI         |
      | 10           | ANTOLOGI          |
      | approx. 1000 | MONOGRAFI         |
      | approx. 1000 | ANTOLOGI          |


  Scenario Outline: NPI subject heading is copied from Cristin Result as is.
    Given a valid Cristin Result with secondary category "<secondaryCategory>"
    And that the Cristin Result has a non empty Book Report
    And that the Book Report has a subjectField with the subjectFieldCode equal to 1234
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a npiSubjectHeading with value equal to 1234
    Examples:
      | secondaryCategory |
      | MONOGRAFI         |
      | ANTOLOGI          |


  Scenario: Map does not fail when a Cristin Result that is a "Monografi" has no subjectField
    Given a valid Cristin Result with secondary category "MONOGRAFI"
    And that the Cristin Result has a non empty Book Report
    And that the Book Report has no subjectField
    When the Cristin Result is converted to an NVA Resource
    Then no error is reported.

  Scenario Outline: Map does not fail for a Cristin Result without subjectField when the secondary category does not require it.
    Given a valid Cristin Result with secondary category "<secondaryCategory>"
    And that the Cristin Result has a non empty Book Report
    And that the Book Report has no subjectField
    When the Cristin Result is converted to an NVA Resource
    Then no error is reported.
    Examples:
      | secondaryCategory |
      | ANTOLOGI          |
      | LÆREBOK           |
      | FAGBOK            |
      | LEKSIKON          |
      | POPVIT_BOK        |


  Scenario Outline: Mapping does not fail when a Cristin Result that is a "Book" has no information about the number of pages.
    Given a valid Cristin Result with secondary category "<secondaryCategory>"
    Given that the Book Report entry has an empty "numberOfPages" field
    When the Cristin Result is converted to an NVA Resource
    Then no error is reported.
    Examples:
      | secondaryCategory |
      | MONOGRAFI         |
      | ANTOLOGI          |

  Scenario: Mapping does not fail when a Cristin Result that is a "Book" has a null value for isbn.
    Given a valid Cristin Result with secondary category "MONOGRAFI"
    And the Cristin Result does not have an ISBN
    When the Cristin Result is converted to an NVA Resource
    Then no error is reported.

  Scenario Outline: Mapping creates a reference to an NSD Series when the Cristin entry contains
  an NSD code for the series
    Given a valid Cristin Result with secondary category "<secondaryCategory>"
    And the Cristin Result refers to a Series with NSD code 339741
    And the Cristin Result has publication year 2002
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a Reference to a Series that is a URI pointing to the NVA NSD proxy
    And the Series URI contains the NSD code "CF26C859-FCFA-4869-8B12-F34BBE4719E0" and the publication year 2002
    Examples:
      | secondaryCategory |
      | MONOGRAFI         |
      | ANTOLOGI          |
      | RAPPORT           |
      | DRGRADAVH         |
      | MASTERGRADSOPPG   |
      | HOVEDFAGSOPPGAVE  |
      | FORSKERLINJEOPPG  |


  Scenario Outline: Mapping crates an Unconfirmed series when a Cristin Book has a references to
  Book series but there is no NSD code.
    Given a valid Cristin Result with secondary category "<secondaryCategory>"
    And the Cristin Result belongs to a Series
    And the Series does not include an NSD code
    And the Series mentions a title "SomeSeries"
    And the Series mentions an issn "0028-0836"
    And  the Series mentions online issn "0028-0836"
    And the Series mentions a volume "Vol 1"
    And the Series mentions an issue "Issue 2"
    When the Cristin Result is converted to an NVA Resource
    Then  the NVA Resource contains an Unconfirmed Series with title "SomeSeries", issn "0028-0836", online issn "0028-0836" and seriesNumber "Volume:Vol 1;Issue:Issue 2"

    Examples:
      | secondaryCategory |
      | MONOGRAFI         |
      | ANTOLOGI          |
      | RAPPORT           |
      | DRGRADAVH         |
      | MASTERGRADSOPPG   |
      | HOVEDFAGSOPPGAVE  |
      | FORSKERLINJEOPPG  |

  Scenario Outline: A Cristin Result with a valid isbn littered with special characters will have them removed
  when mapped to the NVA Resource
    Given a valid Cristin Result with secondary category "<secondaryCategory>"
    And the Cristin Result has an valid ISBN littered with special characters "9*7^8-3/16-1+4?8_4()1|0-0"
    When the Cristin Result is converted to an NVA Resource
    Then the NVA Resource has a PublicationContext with an ISBN list containing the value "9783161484100"
    Examples:
      | secondaryCategory |
      | MONOGRAFI         |
      | ANTOLOGI          |
      | FAGBOK            |
      | LÆREBOK           |
      | LEKSIKON          |
      | POPVIT_BOK        |
      | OPPSLAGSVERK      |
      | ANTOLOGI          |

  Scenario: Should map revision status
    Given a valid Cristin Result with secondary category "MONOGRAFI"
    And the cristin Book Report has revision status equal to "N"
    When the Cristin Result is converted to an NVA Resource
    And the NVA Resource has a publication context Book with a revision equal to "Unrevised"

  Scenario: Should map revision status
    Given a valid Cristin Result with secondary category "MONOGRAFI"
    And the cristin Book Report has revision status equal to "J"
    When the Cristin Result is converted to an NVA Resource
    And the NVA Resource has a publication context Book with a revision equal to "Revised"

  Scenario: Should persist channel registry exception when missing channel for book
    Given a random book
    And the book has series which has NSD code which does not exist in channel registry lookup file
    When the Cristin Result is converted to an NVA Resource
    Then an error is reported.

  Scenario: When the cristin entry is mapped to Book, but NSD code from channel-registry file is of type journal,
  then an exception is thrown
    Given a random book
    And the Book Publication has a reference to an NSD journal with identifier 339738
    When the Cristin Result is converted to an NVA Resource
    Then an error is reported.