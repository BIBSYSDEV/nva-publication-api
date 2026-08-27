package no.unit.nva.transformer;

import static java.util.Objects.nonNull;
import static no.unit.nva.transformer.dto.CreatorDto.SEPARATOR;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import jakarta.xml.bind.JAXBException;
import java.util.List;
import no.unit.nva.transformer.dto.AlternateIdentifierDto;
import no.unit.nva.transformer.dto.CreatorDto;
import no.unit.nva.transformer.dto.DataCiteMetadataDto;
import no.unit.nva.transformer.dto.IdentifierDto;
import no.unit.nva.transformer.dto.IdentifierType;
import no.unit.nva.transformer.dto.PublisherDto;
import no.unit.nva.transformer.dto.ResourceTypeDto;
import no.unit.nva.transformer.dto.TitleDto;
import org.junit.jupiter.api.Test;

class TransformationTest {

  public static final String ANY_DOI = "10.5072/example-full";
  public static final String ANY_URI = "https://example.org/123";
  public static final String ANY_NAME = "Wallace, Cornelius";
  public static final String ANY_TITLE = "A long, slow depressing march";
  public static final String ANY_YEAR = "2007";
  public static final String ANY_PUBLISHER = "Hubert's Æsopian university";
  public static final String ANY_RESOURCE_TYPE = "Article";
  public static final String RESOURCE_TYPE_OTHER = "Other";

  @Test
  void dynamoRecordDtoReturnsTransformedXmlWhenInputIsValid() throws JAXBException {
    var record =
        generateRecord(
            ANY_DOI, ANY_NAME, ANY_TITLE, ANY_YEAR, ANY_PUBLISHER, ANY_RESOURCE_TYPE, ANY_URI);
    var actual = record.asXml();

    assertThat(actual, containsString(ANY_URI));
    assertThat(actual, containsString(ANY_NAME));
    assertThat(actual, containsString(ANY_TITLE));
    assertThat(actual, containsString(ANY_YEAR));
    assertThat(actual, containsString(ANY_PUBLISHER));
    assertThat(actual, containsString(ANY_RESOURCE_TYPE));
    assertThat(actual, containsString(ANY_DOI));
  }

  @Test
  void dynamoRecordDtoReturnsTransformedXmlWithSplitName() throws JAXBException {
    var surname = "Higgs";
    var forename = "Boson";
    var name = String.join(SEPARATOR, surname, forename);
    var record =
        generateRecord(
            ANY_DOI, name, ANY_TITLE, ANY_YEAR, ANY_PUBLISHER, ANY_RESOURCE_TYPE, ANY_URI);
    var actual = record.asXml();
    assertThat(actual, containsString(name));
    assertThat(actual, containsString(enclosedString(surname)));
    assertThat(actual, containsString(enclosedString(forename)));
  }

  @Test
  void dynamoRecordDtoReturnsTransformedXmlWithoutSplitName() throws JAXBException {
    String rank = "Bosun";
    String surname = "Higgs";

    var name = rank + " " + surname;
    var record =
        generateRecord(
            ANY_DOI, name, ANY_TITLE, ANY_YEAR, ANY_PUBLISHER, ANY_RESOURCE_TYPE, ANY_URI);
    var actual = record.asXml();
    assertThat(actual, containsString(name));
    assertThat(actual, not(containsString(enclosedString(rank))));
    assertThat(actual, not(containsString(enclosedString(surname))));
  }

  @Test
  void dynamoRecordDtoReturnsTransformedXmlWithResourceTypeGeneralOtherWhenResourceTypeIsNull()
      throws JAXBException {
    var record =
        generateRecord(ANY_DOI, ANY_NAME, ANY_TITLE, ANY_YEAR, ANY_PUBLISHER, null, ANY_URI);
    var actual = record.asXml();
    assertThat(actual, containsString(RESOURCE_TYPE_OTHER));
  }

  @Test
  void dynamoRecordDtoReturnsTransformedXmlWhenDoiIsNull() throws JAXBException {
    var record =
        generateRecordWithoutDoi(ANY_NAME, ANY_TITLE, ANY_YEAR, ANY_PUBLISHER, null, ANY_URI);
    var actual = record.asXml();
    assertThat(actual, containsString("identifierType=\"URL"));
    assertThat(actual, not(containsString("identifierType=\"DOI")));
  }

  private String enclosedString(String string) {
    return ">" + string + "<";
  }

  private DataCiteMetadataDto generateRecordWithoutDoi(
      String creator,
      String title,
      String year,
      String publisher,
      String resourceType,
      String resourceIdentifier) {
    return new DataCiteMetadataDto.Builder()
        .withIdentifier(getIdentifierWithUriType(resourceIdentifier))
        .withCreator(getCreator(creator))
        .withTitle(getTitle(title))
        .withPublicationYear(year)
        .withPublisher(getPublisher(publisher))
        .withResourceType(getResourceType(resourceType))
        .build();
  }

  private IdentifierDto getIdentifierWithUriType(String resourceIdentifier) {
    return new IdentifierDto.Builder()
        .withType(IdentifierType.URL)
        .withValue(resourceIdentifier)
        .build();
  }

  private DataCiteMetadataDto generateRecord(
      String doi,
      String creator,
      String title,
      String year,
      String publisher,
      String resourceType,
      String resourceIdentifier) {
    return new DataCiteMetadataDto.Builder()
        .withIdentifier(getIdentifier(doi))
        .withCreator(getCreator(creator))
        .withTitle(getTitle(title))
        .withPublicationYear(year)
        .withPublisher(getPublisher(publisher))
        .withAlternateIdentifiers(getAlternateIdentifiers(resourceIdentifier))
        .withResourceType(getResourceType(resourceType))
        .build();
  }

  private List<AlternateIdentifierDto> getAlternateIdentifiers(String resourceIdentifier) {
    return nonNull(resourceIdentifier)
        ? List.of(new AlternateIdentifierDto.Builder().withValue(resourceIdentifier).build())
        : List.of();
  }

  private ResourceTypeDto getResourceType(String resourceType) {
    return new ResourceTypeDto.Builder().withValue(resourceType).build();
  }

  private PublisherDto getPublisher(String publisher) {
    return new PublisherDto.Builder().withValue(publisher).build();
  }

  private TitleDto getTitle(String title) {
    return new TitleDto.Builder().withValue(title).build();
  }

  private IdentifierDto getIdentifier(String identifier) {
    return nonNull(identifier)
        ? new IdentifierDto.Builder().withValue(identifier).withType(IdentifierType.DOI).build()
        : null;
  }

  private List<CreatorDto> getCreator(String name) {
    return List.of(new CreatorDto.Builder().withCreatorName(name).build());
  }
}
