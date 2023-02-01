package no.sikt.nva.scopus.utils;

import static no.unit.nva.testutils.RandomDataGenerator.randomBoolean;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import com.google.gson.Gson;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import no.sikt.nva.scopus.conversion.model.pia.Author;
import no.sikt.nva.scopus.conversion.model.pia.Publication;

public class PiaAuthorResponseGenerator {

    private static final String SOURCE_CODE = "SCOPUS";

    public static String convertToJson(List<Author> authors) {
        Gson gson = new Gson();
        return gson.toJson(authors);
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
