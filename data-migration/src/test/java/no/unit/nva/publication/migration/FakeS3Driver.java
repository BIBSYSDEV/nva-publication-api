package no.unit.nva.publication.migration;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MILLIS;
import static nva.commons.core.JsonUtils.objectMapper;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.javafaker.Faker;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.model.DoiRequest;
import no.unit.nva.model.DoiRequestMessage;
import no.unit.nva.model.DoiRequestStatus;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.PublicationGenerator;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.attempt.Try;
import software.amazon.ion.system.IonReaderBuilder;
import software.amazon.ion.system.IonTextWriterBuilder;

class FakeS3Driver extends S3Driver {

    public static final Faker FAKER = Faker.instance();
    public static final URI PUBLISHER_URI = PUBLICATIONS_WITHOUT_DOI_REQUESTS.get(0).getPublisher().getId();
    private static final boolean WITH_DOI_REQUEST = true;
    public static final List<Publication> PUBLICATIONS_WITH_DOI_REQUESTS = samplePublications(WITH_DOI_REQUEST);
    private static final boolean WITHOUT_DOI_REQUEST = false;
    public static final List<Publication> PUBLICATIONS_WITHOUT_DOI_REQUESTS = samplePublications(WITHOUT_DOI_REQUEST);
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
        return List.of(PUBLICATIONS_WITHOUT_DOI_REQUESTS, PUBLICATIONS_WITH_DOI_REQUESTS)
                   .stream()
                   .flatMap(publications -> serializePublicationsBatch(publications).stream())
                   .map(this::convertJsonToIon)
                   .collect(Collectors.toList());
    }

    public List<String> serializePublicationsBatch(List<Publication> firstPublicationsFile) {

        return firstPublicationsFile.stream()
                   .map(attempt(publication -> objectMapper.convertValue(publication, JsonNode.class)))
                   .map(attempt -> attempt.map(this::addParentItem))
                   .map(attempt -> attempt.map(objectMapper::writeValueAsString))
                   .map(Try::orElseThrow)
                   .collect(Collectors.toList());
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

    private JsonNode addParentItem(JsonNode json) {
        ObjectNode root = objectMapper.createObjectNode();
        root.set(PublicationImporter.ION_ITEM, json);
        return root;
    }

    private String convertJsonToIon(String json) {
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
