package no.sikt.nva.scopus.utils;

import static no.unit.nva.testutils.RandomDataGenerator.randomBoolean;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import com.google.gson.Gson;
import java.util.Collection;
import java.util.List;
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

    public static String convertAuthorsToJson(List<Author> authors) {
        Gson gson = new Gson();
        return gson.toJson(authors);
    }

    public static String convertAffiliationsToJson(List<Affiliation> affiliations) {
        return attempt(() -> JsonUtils.dtoObjectMapper.writeValueAsString(affiliations)).orElseThrow();
    }

    public List<Author> generateAuthors(String scopusId, int cristinId) {
        int maxNumberOfAuthors = 20;
        var firstname = randomString();
        var surname = randomString();
        var authorName = randomString();
        return IntStream.range(0, randomInteger(maxNumberOfAuthors) + 1)
                   .boxed()
                   .map(index ->
                            generateAuthor(scopusId,
                                           firstname,
                                           surname,
                                           authorName,
                                           cristinId
                            ))
                   .collect(Collectors.toList());
    }

    public List<Affiliation> generateAffiliations(String cristinId) {
        var affiliationsWithoutNullValues = IntStream.range(0, randomInteger(20)).boxed()
                                                .map(i -> generateAffiliation(cristinId)).collect(Collectors.toList());
        var affiliationsWithoutUnit = IntStream.range(0, randomInteger(20))
                                          .boxed()
                                          .map(i -> generateAffiliationWithoutUnit(cristinId))
                                          .collect(Collectors.toList());
        var affiliationsWithoutCount = IntStream.range(0, randomInteger(20))
                                           .boxed()
                                           .map(i -> generateAffiliationWithoutCount(cristinId))
                                           .collect(Collectors.toList());
        var affiliationsWithoutId = IntStream.range(0, randomInteger(20)).boxed()
                                        .map(i -> generateAffiliationWithoutId(cristinId)).collect(Collectors.toList());
        return Stream.of(affiliationsWithoutNullValues, affiliationsWithoutCount, affiliationsWithoutUnit,
                         affiliationsWithoutId)
                   .flatMap(Collection::stream)
                   .collect(Collectors.toList());
    }

    public Affiliation generateAffiliation(String cristinId) {
        return new Builder()
                   .withInstitution(cristinId)
                   .withUnit(createUnitId(cristinId))
                   .withCount(String.valueOf(randomInteger(1000)))
                   .build();
    }

    @NotNull
    private static String createUnitId(String cristinId) {
        return cristinId + "." + randomInteger() + "." + randomInteger();
    }

    public Affiliation generateAffiliationWithoutCount(String cristinId) {
        return new Builder()
                   .withInstitution(cristinId)
                   .withUnit(createUnitId(cristinId))
                   .withCount(null)
                   .build();
    }

    public Affiliation generateAffiliationWithoutId(String cristinId) {
        return new Builder()
                   .withInstitution(null)
                   .withUnit(createUnitId(cristinId))
                   .withCount(String.valueOf(randomInteger(1000)))
                   .build();
    }

    public Affiliation generateAffiliationWithoutUnit(String cristinId) {
        return new Builder()
                   .withInstitution(cristinId)
                   .withUnit(null)
                   .withCount(String.valueOf(randomInteger(1000)))
                   .build();
    }

    private Author generateAuthor(String externalId,
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

    private String generateRandomOrcid() {
        return randomBoolean() ? randomString() : null;
    }

    private Publication generateRandomPublication() {
        var publication = new Publication();
        publication.setSourceCode(SOURCE_CODE);
        publication.setExternalId(randomString());
        return publication;
    }
}
