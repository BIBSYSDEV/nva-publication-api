package no.unit.nva.publication.migration;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MILLIS;
import static nva.commons.core.JsonUtils.objectMapper;
import static nva.commons.core.JsonUtils.objectMapperNoEmpty;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.javafaker.Faker;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.dataimport.S3IonReader;
import no.unit.nva.model.DoiRequest;
import no.unit.nva.model.DoiRequestMessage;
import no.unit.nva.model.DoiRequestStatus;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.PublicationGenerator;
import no.unit.nva.s3.S3Driver;
import software.amazon.ion.system.IonReaderBuilder;
import software.amazon.ion.system.IonTextWriterBuilder;

class FakeS3Driver extends S3Driver {

    public static final Faker FAKER = Faker.instance();
    public static final boolean WITH_DOI_REQUEST = true;
    public static final boolean WITHOUT_DOI_REQUEST = false;
    public static final List<Publication> PUBLICATIONS_WITHOUT_DOI_REQUESTS = samplePublications(WITHOUT_DOI_REQUEST);
    public static final URI PUBLISHER_URI = PUBLICATIONS_WITHOUT_DOI_REQUESTS.get(0).getPublisher().getId();
    public static final List<Publication> PUBLICATIONS_WITH_DOI_REQUESTS = samplePublications(WITH_DOI_REQUEST);
    public static final List<String> FILENAMES = sampleFilenames();
    public static final Map<String, Set<String>> fileContent = distributeContentInFiles();
    public static final String LINE_SEPARATOR = System.lineSeparator();
    private static final long ONE_YEAR = 365L * 24 * 60 * 60 * 1000;

    public FakeS3Driver() {
        super(null, null);
    }

    public static List<Publication> allSamplePublications() {
        return Stream.concat(PUBLICATIONS_WITHOUT_DOI_REQUESTS.stream(), PUBLICATIONS_WITH_DOI_REQUESTS.stream())
                   .collect(Collectors.toList());
    }

    public static List<Publication> samplePublications(boolean withDoiRequest) {
        var publicationWithDuplicate = samplePublication(withDoiRequest);
        var duplicate = duplicateWithLaterModifiedDate(publicationWithDuplicate);
        var publicationWithoutDuplicate = samplePublication(withDoiRequest);
        return List.of(publicationWithoutDuplicate, duplicate, publicationWithoutDuplicate);
    }

    public static Publication samplePublication(boolean withDoiRequest) {
        Publication publication = PublicationGenerator.publicationWithIdentifier();
        if (withDoiRequest) {
            publication.setDoiRequest(sampleDoiRequest());
        }

        return publication;
    }

    public static Publication duplicateWithLaterModifiedDate(Publication publicationWithDuplicate) {
        return publicationWithDuplicate.copy().withModifiedDate(laterModifiedDate(publicationWithDuplicate)).build();
    }

    @Override
    public List<String> getFiles(Path path) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> listFiles(Path path) {
        return FILENAMES;
    }

    @Override
    public String getFile(String filename) {
        return String.join(LINE_SEPARATOR, fileContent.get(filename));
    }

    private static Map<String, Set<String>> distributeContentInFiles() {
        Random random = new Random(System.currentTimeMillis());
        List<Publication> publications = allSamplePublications();
        Map<String, Set<String>> fileContents = new ConcurrentHashMap<>();
        for (Publication publication : publications) {
            String file = pickRandomFile(random);
            String content = attempt(() -> serializePublication(publication)).orElseThrow();
            addContentToFile(file, content, fileContents);
        }
        return fileContents;
    }

    private static String pickRandomFile(Random random) {
        return FILENAMES.get(random.nextInt(FILENAMES.size()));
    }

    private static void addContentToFile(String file, String content, Map<String, Set<String>> fileContents) {
        if (!fileContents.containsKey(file)) {
            fileContents.put(file, new HashSet<>());
        }
        fileContents.get(file).add(content);
    }

    private static List<String> sampleFilenames() {
        return List.of(randomFilename(), randomFilename());
    }

    private static String randomFilename() {
        return FAKER.lorem().word();
    }

    private static DoiRequest sampleDoiRequest() {
        Instant createdDate = randomDateAfterDate(Instant.now());
        return new DoiRequest.Builder()
                   .withCreatedDate(createdDate)
                   .withModifiedDate(createdDate.plus(10, HOURS))
                   .withMessages(generateSampleMessages(createdDate))
                   .withStatus(DoiRequestStatus.REQUESTED)
                   .build();
    }

    private static List<DoiRequestMessage> generateSampleMessages(Instant doiRequestCreationTime) {
        return List.of(randomMessage(doiRequestCreationTime), randomMessage(doiRequestCreationTime));
    }

    private static DoiRequestMessage randomMessage(Instant doiRequestCreationDate) {
        return new DoiRequestMessage.Builder()
                   .withAuthor(randomEmail())
                   .withTimestamp(randomDateAfterDate(doiRequestCreationDate))
                   .withText(randomString())
                   .build();
    }

    private static String randomString() {
        return FAKER.lorem().sentence();
    }

    private static String randomEmail() {
        return FAKER.internet().emailAddress();
    }

    private static Instant randomDateAfterDate(Instant startDate) {
        Date start = Date.from(startDate);
        Date end = Date.from(startDate.plus(ONE_YEAR, MILLIS));
        return FAKER.date().between(start, end).toInstant();
    }

    private static Instant laterModifiedDate(Publication publicationWithDuplicate) {
        return publicationWithDuplicate.getModifiedDate().plus(10, DAYS);
    }

    private static String serializePublication(Publication publication) throws JsonProcessingException {
        JsonNode jsonNode = objectMapperNoEmpty.convertValue(publication, JsonNode.class);
        JsonNode withIonParent = addParentItem(jsonNode);
        String jsonString = objectMapperNoEmpty.writeValueAsString(withIonParent);
        return convertJsonToIon(jsonString);
    }

    private static JsonNode addParentItem(JsonNode json) {
        ObjectNode root = objectMapper.createObjectNode();
        root.set(S3IonReader.ION_ITEM, json);
        return root;
    }

    private static String convertJsonToIon(String json) {
        var ionReader = IonReaderBuilder.standard().build(json);
        ionReader = IonReaderBuilder.standard().build(json);
        var stringWriter = new StringBuilder();
        var ionWriter = IonTextWriterBuilder.standard().build(stringWriter);
        try {
            ionWriter.writeValues(ionReader);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return stringWriter.toString();
    }
}
