package no.unit.nva.publication.doi.dto;

import static java.util.Objects.nonNull;
import static no.unit.nva.publication.doi.dynamodb.dao.DynamodbStreamRecordJsonPointers.CONTRIBUTOR_ARP_ID_JSON_POINTER;
import static no.unit.nva.publication.doi.dynamodb.dao.DynamodbStreamRecordJsonPointers.CONTRIBUTOR_NAME_JSON_POINTER;
import static no.unit.nva.publication.doi.dynamodb.dao.DynamodbStreamRecordJsonPointers.CONTRIBUTOR_ORC_ID_JSON_POINTER;
import static no.unit.nva.publication.doi.dynamodb.dao.DynamodbStreamRecordJsonPointers.CONTRIBUTOR_POINTER;
import static no.unit.nva.publication.doi.dynamodb.dao.DynamodbStreamRecordJsonPointers.DOI_POINTER;
import static no.unit.nva.publication.doi.dynamodb.dao.DynamodbStreamRecordJsonPointers.DYNAMODB_TYPE_LIST;
import static no.unit.nva.publication.doi.dynamodb.dao.DynamodbStreamRecordJsonPointers.DYNAMODB_TYPE_STRING;
import static no.unit.nva.publication.doi.dynamodb.dao.DynamodbStreamRecordJsonPointers.ENTITY_DESCRIPTION_PUBLICATION_DATE_DAY;
import static no.unit.nva.publication.doi.dynamodb.dao.DynamodbStreamRecordJsonPointers.ENTITY_DESCRIPTION_PUBLICATION_DATE_MONTH;
import static no.unit.nva.publication.doi.dynamodb.dao.DynamodbStreamRecordJsonPointers.ENTITY_DESCRIPTION_PUBLICATION_DATE_YEAR;
import static no.unit.nva.publication.doi.dynamodb.dao.DynamodbStreamRecordJsonPointers.ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_INSTANCE_TYPE;
import static no.unit.nva.publication.doi.dynamodb.dao.DynamodbStreamRecordJsonPointers.IMAGE_IDENTIFIER_POINTER;
import static no.unit.nva.publication.doi.dynamodb.dao.DynamodbStreamRecordJsonPointers.MAIN_TITLE_POINTER;
import static no.unit.nva.publication.doi.dynamodb.dao.DynamodbStreamRecordJsonPointers.PUBLICATION_STATUS_JSON_POINTER;
import static no.unit.nva.publication.doi.dynamodb.dao.DynamodbStreamRecordJsonPointers.PUBLISHER_ID;
import static no.unit.nva.publication.doi.dynamodb.dao.DynamodbStreamRecordJsonPointers.TYPE_POINTER;

import com.amazonaws.services.lambda.runtime.events.DynamodbEvent.DynamodbStreamRecord;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.javafaker.Faker;
import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import no.unit.nva.publication.doi.dynamodb.dao.DynamodbStreamRecordDao;
import no.unit.nva.publication.doi.dynamodb.dao.Identity;
import nva.commons.utils.IoUtils;
import nva.commons.utils.JsonUtils;

public class PublicationStreamRecordTestDataGenerator {

    public static final String STREAM_RECORD_TEMPLATE_JSON = "doi/streamRecordTemplate.json";
    public static final String CONTRIBUTOR_TEMPLATE_JSON = "doi/contributorTemplate.json";

    /*public static final String ENTITY_DESCRIPTION_MAIN_TITLE_POINTER =
        "/dynamodb/newImage/entityDescription/m/mainTitle";
    public static final String PUBLICATION_INSTANCE_TYPE_POINTER =
        "/dynamodb/newImage/entityDescription/m/reference/m/publicationInstance/m/type";
    public static final JsonPointer DYNAMODB_TYPE_POINTER = JsonPointer.compile("/dynamodb/newImage/type");*/
    public static final String FIRST_RECORD_POINTER = "";

    public static final ObjectMapper objectMapper = JsonUtils.objectMapper;
    public static final String EXAMPLE_NAMESPACE = "http://example.net/nva/";
    public static final String EVENT_ID = "eventID";
    public static final String EVENT_NAME = "eventName";
    public static final String EVENT_YEAR_NAME = "year";
    public static final String EVENT_MONTH_NAME = "month";
    public static final String EVENT_DAY_NAME = "day";

    private final ObjectMapper mapper = JsonUtils.objectMapper;
    private final JsonNode contributorTemplate;
    private final String eventId;
    private final String eventName;
    private final String identifier;
    private final String instanceType;
    private final String mainTitle;
    private final List<Identity> contributors;
    private final PublicationDate date;
    private final String status;
    private final String dynamoDbType;
    private final String doi;
    private final String publisherId;

    {
        try {
            contributorTemplate = mapper.readTree(IoUtils.inputStreamFromResources(
                Paths.get(CONTRIBUTOR_TEMPLATE_JSON)));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private PublicationStreamRecordTestDataGenerator(Builder builder) {
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
        updateDynamodbType(dynamoDbType, event);
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
    public DynamodbStreamRecordDao asDynamodbStreamRecordDao() {
        return new DynamodbStreamRecordDao.Builder()
            .withDynamodbStreamRecord(asDynamoDbStreamRecordJsonNode())
            .build();
    }

    private ObjectNode getEventTemplate() {
        return mapper.valueToTree(loadEventFromResourceFile());
    }

    private DynamodbStreamRecord loadEventFromResourceFile() {
        InputStream is = IoUtils.inputStreamFromResources(Paths.get(STREAM_RECORD_TEMPLATE_JSON));
        try {
            return mapper.readValue(is, DynamodbStreamRecord.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private DynamodbStreamRecord toDynamodbStreamRecord(JsonNode event) {
        return mapper.convertValue(event, DynamodbStreamRecord.class);
    }

    private void updatePublicationStatus(String status, ObjectNode event) {
        updateEventAtPointerWithNameAndValue(event, PUBLICATION_STATUS_JSON_POINTER, DYNAMODB_TYPE_STRING, status);
    }

    private void updateEventImageIdentifier(String id, ObjectNode event) {
        updateEventAtPointerWithNameAndValue(event, IMAGE_IDENTIFIER_POINTER, DYNAMODB_TYPE_STRING, id);
    }

    private void updateEntityDescriptionContributors(List<Identity> contributors, ObjectNode event) {
        ArrayNode contributorsArrayNode = mapper.createArrayNode();
        if (nonNull(contributors)) {
            contributors.forEach(contributor -> updateContributor(contributorsArrayNode, contributor));
            updateEventAtPointerWithNameAndArrayValue(event,
                contributorsArrayNode);
            ((ObjectNode) event.at(CONTRIBUTOR_POINTER)).set(DYNAMODB_TYPE_LIST, contributorsArrayNode);
        }
    }

    private void updateContributor(ArrayNode contributors, Identity contributor) {
        ObjectNode activeTemplate = contributorTemplate.deepCopy();

        updateEventAtPointerWithNameAndValue(activeTemplate, CONTRIBUTOR_NAME_JSON_POINTER,
            DYNAMODB_TYPE_STRING, contributor.getName());
        extractStringValue(contributor.getArpId()).ifPresent(
            arpId -> updateEventAtPointerWithNameAndValue(activeTemplate, CONTRIBUTOR_ARP_ID_JSON_POINTER,
                DYNAMODB_TYPE_STRING, contributor.getArpId()));
        extractStringValue(contributor.getOrcId())
            .ifPresent(orcId -> updateEventAtPointerWithNameAndValue(activeTemplate, CONTRIBUTOR_ORC_ID_JSON_POINTER,
                DYNAMODB_TYPE_STRING, contributor.getOrcId()));
        contributors.add(activeTemplate);
    }

    private Optional<String> extractStringValue(String value) {
        return Optional.ofNullable(value);
    }

    private void updateEntityDescriptionMainTitle(String mainTitle, ObjectNode event) {
        updateEventAtPointerWithNameAndValue(event, MAIN_TITLE_POINTER, DYNAMODB_TYPE_STRING, mainTitle);
    }

    private void updateEntityDescriptionReferenceDoi(String doi, ObjectNode event) {
        updateEventAtPointerWithNameAndValue(event, DOI_POINTER, DYNAMODB_TYPE_STRING, doi);
    }

    private void updatePublisherId(String publisherId, ObjectNode event) {
        updateEventAtPointerWithNameAndValue(event, PUBLISHER_ID, DYNAMODB_TYPE_STRING, publisherId);
    }

    private void updateReferenceType(String type, ObjectNode event) {
        updateEventAtPointerWithNameAndValue(event, ENTITY_DESCRIPTION_REFERENCE_PUBLICATION_INSTANCE_TYPE,
            DYNAMODB_TYPE_STRING, type);
    }

    private void updateDynamodbType(String dynamoDbType, ObjectNode event) {
        updateEventAtPointerWithNameAndValue(event, TYPE_POINTER, DYNAMODB_TYPE_STRING, dynamoDbType);
    }

    private void updateEventId(String eventName, ObjectNode event) {
        ((ObjectNode) event.at(FIRST_RECORD_POINTER)).put(EVENT_ID, eventName);
    }

    private void updateEventName(String eventName, ObjectNode event) {
        ((ObjectNode) event.at(FIRST_RECORD_POINTER)).put(EVENT_NAME, eventName);
    }

    private void updateDate(PublicationDate date, JsonNode event) {
        if (nonNull(date) && date.isPopulated()) {
            updateEventAtPointerWithNameAndValue(event, ENTITY_DESCRIPTION_PUBLICATION_DATE_YEAR,
                DYNAMODB_TYPE_STRING, date.getYear());
            updateEventAtPointerWithNameAndValue(event, ENTITY_DESCRIPTION_PUBLICATION_DATE_MONTH,
                DYNAMODB_TYPE_STRING, date.getMonth());
            updateEventAtPointerWithNameAndValue(event, ENTITY_DESCRIPTION_PUBLICATION_DATE_DAY,
                DYNAMODB_TYPE_STRING, date.getDay());
        }
    }

    private void updateEventAtPointerWithNameAndValue(JsonNode event, String pointer, String name, Object value) {
        if (value instanceof String) {
            ((ObjectNode) event.at(pointer)).put(name, (String) value);
        } else {
            ((ObjectNode) event.at(pointer)).put(name, (Integer) value);
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
        ((ObjectNode) event.at(CONTRIBUTOR_POINTER)).set(DYNAMODB_TYPE_LIST, value);
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
        private String status;
        private String doi;
        private String publisherId;

        public Builder() {

        }

        /**
         * Create a valid publication populated by the provided faker.
         *
         * @param faker data provider to generate fake data.
         * @return Builder populated from faker.
         */
        public static Builder createValidPublication(Faker faker) {
            var localDate = Instant.ofEpochMilli(faker.date().birthday().getTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
            return new Builder()
                .withIdentifier(UUID.randomUUID().toString())
                .withInstanceType(faker.options().nextElement(PublicationType.values()).toString())
                .withDate(new PublicationDate(
                    String.valueOf(localDate.getYear()),
                    String.valueOf(localDate.getMonth().getValue()),
                    String.valueOf(localDate.getDayOfMonth())))
                .withDynamoDbType("Publication")
                .withMainTitle(faker.book().title())
                .withDoi("http://example.net/doi/prefix/suffix/" + UUID.randomUUID().toString())
                .withPublisherId("http://example.net/nva/institution/" + UUID.randomUUID().toString())
                .withEventId(UUID.randomUUID().toString())
                .withEventName(faker.options().nextElement(List.of("MODIFY")))
                .withStatus(faker.options().nextElement(List.of("Published", "Draft")))
                .withContributorIdentities(getIdentities(faker, false));
        }

        @VisibleForTesting
        public String getEventId() {
            return eventId;
        }

        @VisibleForTesting
        public String getEventName() {
            return eventName;
        }

        @VisibleForTesting
        public String getIdentifier() {
            return identifier;
        }

        @VisibleForTesting
        public String getInstancetype() {
            return instancetype;
        }

        @VisibleForTesting
        public String getDynamoDbType() {
            return dynamoDbType;
        }

        @VisibleForTesting
        public String getMainTitle() {
            return mainTitle;
        }

        @VisibleForTesting
        public List<Identity> getContributors() {
            return contributors;
        }

        @VisibleForTesting
        public PublicationDate getDate() {
            return date;
        }

        @VisibleForTesting
        public String getStatus() {
            return status;
        }

        @VisibleForTesting
        public String getDoi() {
            return doi;
        }

        @VisibleForTesting
        public String getPublisherId() {
            return publisherId;
        }

        private static List<Identity> getIdentities(Faker faker, boolean withoutName) {
            var identities = new ArrayList<Identity>();
            for (int i = 0; i < faker.random().nextInt(1, 10); i++) {
                var builder = new Identity.Builder();
                builder.withArpId(faker.number().digits(10));
                builder.withOrcId(faker.number().digits(10));
                builder.withName(withoutName ? null : faker.superhero().name());
                identities.add(builder.build());
            }
            return identities;
        }

        private static ArrayList<URI> getIdOptions() {
            var idOptions = new ArrayList<URI>();
            idOptions.add(URI.create("https://example.net/contributor/" + UUID.randomUUID().toString()));
            idOptions.add(null);
            return idOptions;
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

        public Builder withStatus(String draft) {
            this.status = draft;
            return this;
        }

        public Builder withPublisherId(String publisherId) {
            this.publisherId = publisherId;
            return this;
        }

        public PublicationStreamRecordTestDataGenerator build() {
            return new PublicationStreamRecordTestDataGenerator(this);
        }
    }
}
