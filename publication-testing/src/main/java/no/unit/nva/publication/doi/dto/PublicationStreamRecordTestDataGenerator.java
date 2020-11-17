package no.unit.nva.publication.doi.dto;

import static java.util.Objects.nonNull;
import static no.unit.nva.publication.doi.dynamodb.dao.DynamodbStreamRecordJsonPointers.DYNAMODB_TYPE_LIST;
import static no.unit.nva.publication.doi.dynamodb.dao.DynamodbStreamRecordJsonPointers.DYNAMODB_TYPE_STRING;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent.DynamodbStreamRecord;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.StreamViewType;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.javafaker.Faker;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import no.unit.nva.publication.doi.dynamodb.dao.DynamodbStreamRecordImageDao;
import no.unit.nva.publication.doi.dynamodb.dao.DynamodbStreamRecordJsonPointers;
import no.unit.nva.publication.doi.dynamodb.dao.DynamodbStreamRecordJsonPointers.DynamodbImageType;
import no.unit.nva.publication.doi.dynamodb.dao.Identity;
import nva.commons.utils.IoUtils;
import nva.commons.utils.JacocoGenerated;
import nva.commons.utils.JsonUtils;

@SuppressWarnings("PMD.TooManyFields")
public class PublicationStreamRecordTestDataGenerator {

    public static final String STREAM_RECORD_TEMPLATE_JSON = "doi/streamRecordTemplate.json";
    public static final String CONTRIBUTOR_TEMPLATE_JSON = "doi/contributorTemplate.json";

    public static final String FIRST_RECORD_POINTER = "";

    public static final String EVENT_ID = "eventID";
    public static final String EVENT_NAME = "eventName";
    public static final String STREAM_VIEW_TYPE = "streamViewType";
    public static final String DYNAMODB = "/dynamodb";

    private final DynamodbStreamRecordJsonPointers jsonPointers = new DynamodbStreamRecordJsonPointers(
        DynamodbImageType.NEW);

    private final ObjectMapper mapper = JsonUtils.objectMapper;
    private final JsonNode contributorTemplate;
    private final String eventId;
    private final String eventName;
    private final String identifier;
    private final String instanceType;
    private final String mainTitle;
    private final List<Identity> contributors;
    private final PublicationDate date;
    private final PublicationStatus status;
    private final DoiRequest doiRequest;
    private final Instant modifiedDate;
    private final String dynamoDbType;
    private final String doi;
    private final String publisherId;
    private final String streamViewType;

    private PublicationStreamRecordTestDataGenerator(Builder builder) {
        try {
            contributorTemplate = mapper.readTree(IoUtils.inputStreamFromResources(
                Paths.get(CONTRIBUTOR_TEMPLATE_JSON)));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        eventId = builder.eventId;
        eventName = builder.eventName;
        identifier = builder.identifier;
        doi = builder.doi;
        instanceType = builder.instancetype;
        dynamoDbType = builder.dynamoDbType;
        mainTitle = builder.mainTitle;
        contributors = builder.contributors;
        publisherId = builder.publisherId;
        date = builder.date;
        status = builder.status;
        doiRequest = builder.doiRequest;
        modifiedDate = builder.modifiedDate;
        streamViewType = builder.streamViewType;
    }

    /**
     * Provides a DynamodbEvent object representation of the object.
     *
     * @return DynamodbEvent representation of the object.
     * @throws IOException thrown if the template files cannot be found.
     */
    public DynamodbStreamRecord asDynamoDbStreamRecord() {
        ObjectNode event = getEventTemplate();
        updateEventImageIdentifier(identifier, event);
        updateEventId(eventId, event);
        updateEventName(eventName, event);
        updateReferenceType(instanceType, event);
        updateEntityDescriptionMainTitle(mainTitle, event);
        updateEntityDescriptionContributors(contributors, event);
        updateEntityDescriptionReferenceDoi(doi, event);
        updatePublisherId(publisherId, event);
        updateDate(date, event);
        updatePublicationStatus(status, event);
        updateDoiRequest(doiRequest, event);
        updateModifiedDate(modifiedDate, event);
        updateDynamodbType(dynamoDbType, event);
        updateStreamViewType(streamViewType, event);
        return toDynamodbStreamRecord(event);
    }

    public JsonNode asDynamoDbStreamRecordJsonNode() {
        return mapper.convertValue(asDynamoDbStreamRecord(), JsonNode.class);
    }

    /**
     * Provides an IndexDocument representation of the object.
     *
     * @return IndexDocument representation of object.
     */
    public DynamodbStreamRecordImageDao asDynamodbStreamRecordDao(DynamodbStreamRecordJsonPointers jsonPointers) {
        return new DynamodbStreamRecordImageDao.Builder(jsonPointers)
            .withDynamodbStreamRecordImage(asDynamoDbStreamRecordJsonNode())
            .build();
    }

    /**
     * Provides an IndexDocument representation of the object.
     *
     * @return IndexDocument representation of object.
     */
    public DynamodbStreamRecordImageDao asDynamodbStreamRecordDao() {
        return new DynamodbStreamRecordImageDao.Builder(jsonPointers)
            .withDynamodbStreamRecordImage(asDynamoDbStreamRecordJsonNode())
            .build();
    }

    private void updateDoiRequest(DoiRequest doiRequest, ObjectNode event) {
        var jsonNode = event.at(jsonPointers.getDoiRequestJsonPointer());

        updateEventAtPointerWithNameAndValue(jsonNode, jsonPointers.getDoiRequestModifiedDateJsonPointer(),
            DYNAMODB_TYPE_STRING, doiRequest.getModifiedDate().toString());
        updateEventAtPointerWithNameAndValue(jsonNode, jsonPointers.getDoiRequestStatusJsonPointer(),
            DYNAMODB_TYPE_STRING, doiRequest.getStatus().name());
    }

    private void updateModifiedDate(Instant modifiedDate, ObjectNode event) {
        updateEventAtPointerWithNameAndValue(event, jsonPointers.getModifiedDateJsonPointer(),
            DYNAMODB_TYPE_STRING, modifiedDate.toString());
    }

    private ObjectNode getEventTemplate() {
        return mapper.valueToTree(loadEventFromResourceFile());
    }

    private DynamodbStreamRecord loadEventFromResourceFile() {
        try (InputStream is = IoUtils.inputStreamFromResources(Paths.get(STREAM_RECORD_TEMPLATE_JSON))) {
            return mapper.readValue(is, DynamodbStreamRecord.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private DynamodbStreamRecord toDynamodbStreamRecord(JsonNode event) {
        return mapper.convertValue(event, DynamodbStreamRecord.class);
    }

    private void updatePublicationStatus(PublicationStatus status, ObjectNode event) {
        updateEventAtPointerWithNameAndValue(event, jsonPointers.getStatusJsonPointer(),
            DYNAMODB_TYPE_STRING, status.getValue());
    }

    private void updateEventImageIdentifier(String id, ObjectNode event) {
        updateEventAtPointerWithNameAndValue(event, jsonPointers.getIdentifierJsonPointer(),
            DYNAMODB_TYPE_STRING, id);
    }

    private void updateEntityDescriptionContributors(List<Identity> contributors, ObjectNode event) {
        ArrayNode contributorsArrayNode = mapper.createArrayNode();
        if (nonNull(contributors)) {
            contributors.forEach(contributor -> updateContributor(contributorsArrayNode, contributor));
            updateEventAtPointerWithNameAndArrayValue(event,
                contributorsArrayNode);
            ((ObjectNode) event.at(jsonPointers.getContributorsJsonPointer())).set(
                DYNAMODB_TYPE_LIST, contributorsArrayNode);
        }
    }

    private void updateContributor(ArrayNode contributors, Identity contributor) {
        ObjectNode activeTemplate = contributorTemplate.deepCopy();

        updateEventAtPointerWithNameAndValue(activeTemplate, jsonPointers.getIdentityNameJsonPointer(),
            DYNAMODB_TYPE_STRING, contributor.getName());
        extractStringValue(contributor.getArpId()).ifPresent(
            arpId -> updateEventAtPointerWithNameAndValue(
                activeTemplate,
                jsonPointers.getIdentityArpIdJsonPointer(),
                DYNAMODB_TYPE_STRING, contributor.getArpId()));
        extractStringValue(contributor.getOrcId())
            .ifPresent(orcId -> updateEventAtPointerWithNameAndValue(
                activeTemplate,
                jsonPointers.getIdentityOrcIdJsonPointer(),
                DYNAMODB_TYPE_STRING, contributor.getOrcId()));
        contributors.add(activeTemplate);
    }

    private Optional<String> extractStringValue(String value) {
        return Optional.ofNullable(value);
    }

    private void updateEntityDescriptionMainTitle(String mainTitle, ObjectNode event) {
        updateEventAtPointerWithNameAndValue(event, jsonPointers.getMainTitleJsonPointer(),
            DYNAMODB_TYPE_STRING, mainTitle);
    }

    private void updateEntityDescriptionReferenceDoi(String doi, ObjectNode event) {
        updateEventAtPointerWithNameAndValue(event, jsonPointers.getDoiJsonPointer(), DYNAMODB_TYPE_STRING, doi);
    }

    private void updatePublisherId(String publisherId, ObjectNode event) {
        updateEventAtPointerWithNameAndValue(event, jsonPointers.getPublisherIdJsonPointer(),
            DYNAMODB_TYPE_STRING, publisherId);
    }

    private void updateReferenceType(String type, ObjectNode event) {
        updateEventAtPointerWithNameAndValue(event, jsonPointers.getEntityDescriptionReferenceTypeJsonPointer(),
            DYNAMODB_TYPE_STRING, type);
    }

    private void updateDynamodbType(String dynamoDbType, ObjectNode event) {
        updateEventAtPointerWithNameAndValue(event, jsonPointers.getTypeJsonPointer(),
            DYNAMODB_TYPE_STRING, dynamoDbType);
    }

    private void updateEventId(String eventName, ObjectNode event) {
        ((ObjectNode) event.at(FIRST_RECORD_POINTER)).put(EVENT_ID, eventName);
    }

    private void updateEventName(String eventName, ObjectNode event) {
        ((ObjectNode) event.at(FIRST_RECORD_POINTER)).put(EVENT_NAME, eventName);
    }

    private void updateStreamViewType(String streamViewType, ObjectNode event) {
        ((ObjectNode) event.at(DYNAMODB)).put(STREAM_VIEW_TYPE, streamViewType);
    }

    private void updateDate(PublicationDate date, JsonNode event) {
        if (nonNull(date) && date.isPopulated()) {
            updateEventAtPointerWithNameAndValue(
                event,
                jsonPointers.getEntityDescriptionDateYearJsonPointer(),
                DYNAMODB_TYPE_STRING, date.getYear());
            updateEventAtPointerWithNameAndValue(
                event,
                jsonPointers.getEntityDescriptionDateMonthJsonPointer(),
                DYNAMODB_TYPE_STRING, date.getMonth());
            updateEventAtPointerWithNameAndValue(
                event,
                jsonPointers.getEntityDescriptionDateDayJsonPointer(),
                DYNAMODB_TYPE_STRING, date.getDay());
        }
    }

    private void updateEventAtPointerWithNameAndValue(JsonNode event, JsonPointer pointer, String name, Object value) {
        if (value instanceof String) {
            ((ObjectNode) event.at(pointer.head())).put(name, (String) value);
        } else {
            ((ObjectNode) event.at(pointer.head())).put(name, (Integer) value);
        }
    }

    private void updateEventAtPointerWithNameAndArrayValue(ObjectNode event,
                                                           ArrayNode value) {
        ((ObjectNode) event.at(jsonPointers.getContributorsJsonPointer())).set(
            DYNAMODB_TYPE_LIST, value);
    }

    public static final class Builder {

        private String eventId;
        private String eventName;
        private String identifier;
        private String instancetype;
        private String dynamoDbType;
        private String mainTitle;
        private List<Identity> contributors;
        private PublicationDate date;
        private DoiRequest doiRequest;
        private Instant modifiedDate;
        private PublicationStatus status;
        private String doi;
        private String publisherId;
        private String streamViewType;

        public Builder() {
        }

        /**
         * Create a valid publication populated by the provided faker.
         *
         * @param faker data provider to generate fake data.
         * @return Builder populated from faker.
         */
        public static Builder createValidPublication(
            Faker faker,
            DynamodbStreamRecordJsonPointers jsonPointers,
            String streamViewType) {
            var localDate = Instant.ofEpochMilli(faker.date().birthday().getTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
            var now = Instant.now();
            return new Builder()
                .withIdentifier(UUID.randomUUID().toString())
                .withInstanceType(faker.options().nextElement(PublicationType.values()).toString())
                .withDate(new PublicationDate(
                    String.valueOf(localDate.getYear()),
                    String.valueOf(localDate.getMonth().getValue()),
                    String.valueOf(localDate.getDayOfMonth())))
                .withDynamoDbType("Publication")
                .withMainTitle(faker.book().title())
                .withDoiRequest(new DoiRequest(faker.options().nextElement(DoiRequestStatus.values()), now))
                .withModifiedDate(now)
                .withDoi("http://example.net/doi/prefix/suffix/" + UUID.randomUUID().toString())
                .withPublisherId("http://example.net/nva/institution/" + UUID.randomUUID().toString())
                .withEventId(UUID.randomUUID().toString())
                .withEventName(faker.options().nextElement(List.of("MODIFY")))
                .withStatus(faker.options().nextElement(PublicationStatus.values()))
                .withContributorIdentities(getIdentities(faker, jsonPointers))
                .withStreamViewType(streamViewType);
        }

        public static Builder createValidPublication(Faker faker, DynamodbStreamRecordJsonPointers jsonPointers) {
            return createValidPublication(faker, jsonPointers, StreamViewType.NEW_IMAGE.getValue());
        }

        // Getters public due to VisibleForTesting. (dont want to pull in Guava just because of this)

        @JacocoGenerated
        public String getEventId() {
            return eventId;
        }

        @JacocoGenerated
        public String getEventName() {
            return eventName;
        }

        @JacocoGenerated
        public String getIdentifier() {
            return identifier;
        }

        @JacocoGenerated
        public String getInstancetype() {
            return instancetype;
        }

        @JacocoGenerated
        public String getDynamoDbType() {
            return dynamoDbType;
        }

        @JacocoGenerated
        public String getMainTitle() {
            return mainTitle;
        }

        @JacocoGenerated
        public List<Identity> getContributors() {
            return contributors;
        }

        @JacocoGenerated
        public PublicationDate getDate() {
            return date;
        }

        @JacocoGenerated
        public PublicationStatus getStatus() {
            return status;
        }

        @JacocoGenerated
        public DoiRequest getDoiRequest() {
            return doiRequest;
        }

        @JacocoGenerated
        public Instant getModifiedDate() {
            return modifiedDate;
        }

        @JacocoGenerated
        public String getDoi() {
            return doi;
        }

        @JacocoGenerated
        public String getPublisherId() {
            return publisherId;
        }

        @JacocoGenerated
        public String getStreamViewType() {
            return streamViewType;
        }

        public Builder withEventId(String eventId) {
            this.eventId = eventId;
            return this;
        }

        public Builder withEventName(String eventName) {
            this.eventName = eventName;
            return this;
        }

        public Builder withIdentifier(String identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder withInstanceType(String instanceType) {
            this.instancetype = instanceType;
            return this;
        }

        public Builder withDynamoDbType(String dynamoDbType) {
            this.dynamoDbType = dynamoDbType;
            return this;
        }

        public Builder withMainTitle(String mainTitle) {
            this.mainTitle = mainTitle;
            return this;
        }

        public Builder withDoi(String doi) {
            this.doi = doi;
            return this;
        }

        public Builder withContributorIdentities(List<Identity> contributors) {
            this.contributors = contributors;
            return this;
        }

        public Builder withDate(PublicationDate date) {
            this.date = date;
            return this;
        }

        public Builder withStatus(PublicationStatus status) {
            this.status = status;
            return this;
        }

        public Builder withDoiRequest(DoiRequest doiRequest) {
            this.doiRequest = doiRequest;
            return this;
        }

        public Builder withModifiedDate(Instant modifiedDate) {
            this.modifiedDate = modifiedDate;
            return this;
        }

        public Builder withPublisherId(String publisherId) {
            this.publisherId = publisherId;
            return this;
        }

        public Builder withStreamViewType(String streamViewType) {
            this.streamViewType = streamViewType;
            return this;
        }

        public PublicationStreamRecordTestDataGenerator build() {
            return new PublicationStreamRecordTestDataGenerator(this);
        }

        private static List<Identity> getIdentities(Faker faker,
                                                    DynamodbStreamRecordJsonPointers jsonPointers) {
            var identities = new ArrayList<Identity>();
            for (int i = 0; i < faker.random().nextInt(1, 10); i++) {
                identities.add(createRandomIdentity(faker, jsonPointers));
            }
            return identities;
        }

        private static Identity createRandomIdentity(Faker faker,
                                                     DynamodbStreamRecordJsonPointers jsonPointers) {
            var builder = new Identity.Builder(jsonPointers);
            builder.withArpId(faker.number().digits(10));
            builder.withOrcId(faker.number().digits(10));
            builder.withName(faker.superhero().name());
            return builder.build();
        }
    }
}
