package no.unit.nva.model.contexttypes;

import static no.unit.nva.testutils.RandomDataGenerator.randomIsbn13;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.core.attempt.Try.attempt;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.Revision;
import no.unit.nva.model.UnconfirmedCourse;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class BookTest {

  @ParameterizedTest
  @MethodSource("booksOfEveryType")
  void withSeriesShouldReplaceSeriesAndKeepTypeAndOtherFields(Book book) {
    var newSeries = new Series(randomUri());

    var updated = book.withSeries(newSeries);

    assertEquals(newSeries, updated.getSeries());
    assertEquals(
        withoutField(book, Book.JSON_PROPERTY_SERIES),
        withoutField(updated, Book.JSON_PROPERTY_SERIES));
  }

  @ParameterizedTest
  @MethodSource("booksOfEveryType")
  void withPublisherShouldReplacePublisherAndKeepTypeAndOtherFields(Book book) {
    var newPublisher = new Publisher(randomUri());

    var updated = book.withPublisher(newPublisher);

    assertEquals(newPublisher, updated.getPublisher());
    assertEquals(
        withoutField(book, Book.JSON_PROPERTY_PUBLISHER),
        withoutField(updated, Book.JSON_PROPERTY_PUBLISHER));
  }

  private static Stream<Book> booksOfEveryType() {
    return Stream.of(randomBook(), randomReport(), randomDegree());
  }

  private static ObjectNode withoutField(Book book, String fieldName) {
    ObjectNode json = JsonUtils.dtoObjectMapper.valueToTree(book);
    json.remove(fieldName);
    return json;
  }

  private static Book randomBook() {
    return new Book.BookBuilder()
        .withSeries(new Series(randomUri()))
        .withSeriesNumber(randomString())
        .withPublisher(new Publisher(randomUri()))
        .withIsbnList(List.of(randomIsbn13()))
        .withRevision(Revision.REVISED)
        .build();
  }

  private static Report randomReport() {
    return attempt(
            () ->
                new Report.Builder()
                    .withSeries(new Series(randomUri()))
                    .withSeriesNumber(randomString())
                    .withPublisher(new Publisher(randomUri()))
                    .withIsbnList(List.of(randomIsbn13()))
                    .build())
        .orElseThrow();
  }

  private static Degree randomDegree() {
    return attempt(
            () ->
                new Degree.Builder()
                    .withSeries(new Series(randomUri()))
                    .withSeriesNumber(randomString())
                    .withPublisher(new Publisher(randomUri()))
                    .withIsbnList(List.of(randomIsbn13()))
                    .withCourse(new UnconfirmedCourse(randomString()))
                    .build())
        .orElseThrow();
  }
}
