package no.sikt.nva.scopus.utils;

import static no.unit.nva.testutils.RandomDataGenerator.randomBoolean;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import no.sikt.nva.scopus.conversion.model.pia.Affiliation;
import no.sikt.nva.scopus.conversion.model.pia.Affiliation.Builder;
import no.sikt.nva.scopus.conversion.model.pia.Author;
import no.sikt.nva.scopus.conversion.model.pia.Publication;
import no.unit.nva.commons.json.JsonUtils;
import org.jetbrains.annotations.NotNull;

public class PiaResponseGenerator {

    private static final String SOURCE_CODE = "SCOPUS";
    private static final ObjectMapper MAPPER = JsonUtils.dtoObjectMapper;

    private PiaResponseGenerator() {
    }

    public static String convertAuthorsToJson(List<Author> authors) {
        return attempt(() -> MAPPER.writeValueAsString(authors)).orElseThrow();
    }

    public static String convertAffiliationsToJson(List<Affiliation> affiliations) {
        return attempt(() -> JsonUtils.dtoObjectMapper.writeValueAsString(affiliations)).orElseThrow();
    }

    public static List<Author> generateAuthors(String scopusAuthorId, int cristinId) {
        int maxNumberOfAuthors = 20;
        var firstname = randomString();
        var surname = randomString();
        var authorName = randomString();
        return IntStream.range(0, randomInteger(maxNumberOfAuthors) + 1)
                   .boxed()
                   .map(index ->
                            generateAuthor(scopusAuthorId,
                                           firstname,
                                           surname,
                                           authorName,
                                           cristinId
                            ))
                   .collect(Collectors.toList());
    }

    public static List<Affiliation> generateAffiliations(String cristinId) {
        var uniqueRandomIntegers = generateUniqueRandomIntegers();
        var affiliationsWithoutNullValues = IntStream.range(0, randomInteger(20) + 1).boxed()
                                                .map(i -> generateAffiliation(cristinId, uniqueRandomIntegers)).collect(Collectors.toList());
        var affiliationsWithoutUnit = IntStream.range(0, randomInteger(20) + 1)
                                          .boxed()
                                          .map(i -> generateAffiliationWithoutUnit(cristinId, uniqueRandomIntegers))
                                          .collect(Collectors.toList());
        var affiliationsWithoutCount = IntStream.range(0, randomInteger(20) + 1)
                                           .boxed()
                                           .map(i -> generateAffiliationWithoutCount(cristinId))
                                           .collect(Collectors.toList());
        var affiliationsWithoutId = IntStream.range(0, randomInteger(20) + 1).boxed()
                                        .map(i -> generateAffiliationWithoutId(cristinId, uniqueRandomIntegers)).collect(Collectors.toList());
        return Stream.of(affiliationsWithoutNullValues, affiliationsWithoutCount, affiliationsWithoutUnit,
                         affiliationsWithoutId)
                   .flatMap(Collection::stream)
                   .collect(Collectors.toList());
    }

    private static Iterator<Integer> generateUniqueRandomIntegers() {
        return new Random().ints(0, 1000)
            .distinct()      // Ensure uniqueness
            .limit(100)    // Limit to 'limit' integers
            .boxed()         // Convert to Integer objects
            .toList()
            .iterator();
    }

    public static Affiliation generateAffiliation(String cristinId, Iterator<Integer> uniqueRandomIntegers) {
        return new Builder()
                   .withInstitution(cristinId)
                   .withUnit(createUnitId(cristinId))
                   .withCount(String.valueOf(uniqueRandomIntegers.next()))
                   .build();
    }

    public static Affiliation generateAffiliationWithoutCount(String cristinId) {
        return new Builder()
                   .withInstitution(cristinId)
                   .withUnit(createUnitId(cristinId))
                   .withCount(null)
                   .build();
    }

    public static Affiliation generateAffiliationWithoutId(String cristinId, Iterator<Integer> uniqueRandomIntegers) {
        return new Builder()
                   .withInstitution(null)
                   .withUnit(createUnitId(cristinId))
                   .withCount(String.valueOf(uniqueRandomIntegers.next()))
                   .build();
    }

    public static Affiliation generateAffiliationWithoutUnit(String cristinId, Iterator<Integer> uniqueRandomIntegers) {
        return new Builder()
                   .withInstitution(cristinId)
                   .withUnit(null)
                   .withCount(String.valueOf(uniqueRandomIntegers.next()))
                   .build();
    }

    @NotNull
    private static String createUnitId(String cristinId) {
        return cristinId + "." + randomInteger() + "." + randomInteger();
    }

    private static Author generateAuthor(String externalId,
                                  String firstname,
                                  String surname,
                                  String authorName,
                                  int cristinId) {
        var author = new Author();
        author.setExternalId(externalId);
        author.setFirstname(firstname);
        author.setSurname(surname);
        author.setAuthorName(authorName);
        author.setOrcid(generateRandomOrcid());
        author.setCristinId(cristinId);
        author.setSequenceNr(randomInteger());
        author.setPublication(generateRandomPublication());
        return author;
    }

    private static String generateRandomOrcid() {
        return randomBoolean() ? randomString() : null;
    }

    private static Publication generateRandomPublication() {
        var publication = new Publication();
        publication.setSourceCode(SOURCE_CODE);
        publication.setExternalId(randomString());
        return publication;
    }
}
