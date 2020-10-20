package no.unit.nva.publication.doi.dto;

import static java.util.Objects.nonNull;

import com.amazonaws.services.lambda.runtime.events.DynamodbEvent.DynamodbStreamRecord;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.javafaker.Faker;
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
import no.unit.nva.publication.doi.PublicationMapper;
import nva.commons.utils.IoUtils;
import nva.commons.utils.JsonUtils;

public class PublicationStreamRecordTestDataGenerator {

    public static final String STREAM_RECORD_TEMPLATE_JSON = "doi/streamRecordTemplate.json";
    public static final String CONTRIBUTOR_TEMPLATE_JSON = "doi/contributorTemplate.json";

    public static final String EVENT_JSON_STRING_NAME = "s";
    public static final String EVENT_ID = "eventID";
    public static final String CONTRIBUTOR_NAME_POINTER = "/m/identity/m/name";
    public static final String CONTRIBUTOR_ID_POINTER = "/m/identity/m/id";
    public static final String CONTRIBUTOR_POINTER = "/dynamodb/newImage/entityDescription/m/contributors";
    public static final String EVENT_JSON_LIST_NAME = "l";

    public static final String ENTITY_DESCRIPTION_MAIN_TITLE_POINTER =
        "/dynamodb/newImage/entityDescription/m/mainTitle";
    public static final String PUBLICATION_INSTANCE_TYPE_POINTER =
        "/dynamodb/newImage/entityDescription/m/reference/m/publicationInstance/m/type";
    public static final JsonPointer DYNAMODB_TYPE_POINTER = JsonPointer.compile("/dynamodb/newImage/type");
    public static final String FIRST_RECORD_POINTER = "";
    public static final String EVENT_NAME = "eventName";

    public static final String IMAGE_IDENTIFIER_JSON_POINTER = "/dynamodb/newImage/identifier";
    public static final String ENTITY_DESCRIPTION_PUBLICATION_DATE_JSON_POINTER =
        "/dynamodb/newImage/entityDescription/m/date/m";
    public static final String EVENT_YEAR_NAME = "year";
    public static final String EVENT_MONTH_NAME = "month";
    public static final String EVENT_DAY_NAME = "day";
    public static final String PUBLICATION_STATUS_JSON_POINTER = "/dynamodb/newImage/status";
    public static final ObjectMapper objectMapper = JsonUtils.objectMapper;

    private final ObjectMapper mapper = JsonUtils.objectMapper;
    private final JsonNode contributorTemplate;
    private final String eventId;
    private final String eventName;
    private final UUID id;
    private final String instanceType;
    private final String mainTitle;
    private final List<Contributor> contributors;
    private final PublicationDate date;
    private final String status;
    private final String dynamoDbType;

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
        id = builder.id;
        instanceType = builder.instancetype;
        dynamoDbType = builder.dynamoDbType;
        mainTitle = builder.mainTitle;
        contributors = builder.contributors;
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
        updateEventImageIdentifier(id.toString(), event);
        updateEventId(eventId, event);
        updateEventName(eventName, event);
        updateReferenceType(instanceType, event);
        updateEntityDescriptionMainTitle(mainTitle, event);
        updateEntityDescriptionContributors(contributors, event);
        updateDate(date, event);
        updatePublicationStatus(status, event);
        updateDynamodbType(dynamoDbType, event);
        return toDynamodbStreamRecord(event);
    }

    /**
     * Provides an IndexDocument representation of the object.
     *
     * @return IndexDocument representation of object.
     */
    public Publication asPublicationDto() {
        try {
            return new PublicationMapper().fromDynamodbStreamRecord("http://example.net/foo",
                objectMapper.writeValueAsString(asDynamoDbStreamRecord()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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
        updateEventAtPointerWithNameAndValue(event, PUBLICATION_STATUS_JSON_POINTER, EVENT_JSON_STRING_NAME, status);
    }

    private void updateEventImageIdentifier(String id, ObjectNode event) {
        updateEventAtPointerWithNameAndValue(event, IMAGE_IDENTIFIER_JSON_POINTER, EVENT_JSON_STRING_NAME, id);
    }

    private void updateEntityDescriptionContributors(List<Contributor> contributors, ObjectNode event) {
        ArrayNode contributorsArrayNode = mapper.createArrayNode();
        if (nonNull(contributors)) {
            contributors.forEach(contributor -> updateContributor(contributorsArrayNode, contributor));
            updateEventAtPointerWithNameAndArrayValue(event,
                contributorsArrayNode);
            ((ObjectNode) event.at(CONTRIBUTOR_POINTER)).set(EVENT_JSON_LIST_NAME, contributorsArrayNode);
        }
    }

    private void updateContributor(ArrayNode contributors, Contributor contributor) {
        ObjectNode activeTemplate = contributorTemplate.deepCopy();
        updateEventAtPointerWithNameAndValue(activeTemplate, CONTRIBUTOR_NAME_POINTER,
            EVENT_JSON_STRING_NAME, contributor.getName());
        var id = Optional.ofNullable(contributor.getId());
        id.ifPresent(uri -> updateEventAtPointerWithNameAndValue(activeTemplate, CONTRIBUTOR_ID_POINTER,
            EVENT_JSON_STRING_NAME, uri.toString()));
        contributors.add(activeTemplate);
    }

    private void updateEntityDescriptionMainTitle(String mainTitle, ObjectNode event) {
        ((ObjectNode) event.at(ENTITY_DESCRIPTION_MAIN_TITLE_POINTER))
            .put(EVENT_JSON_STRING_NAME, mainTitle);
    }

    private void updateReferenceType(String type, ObjectNode event) {
        updateEventAtPointerWithNameAndValue(event, PUBLICATION_INSTANCE_TYPE_POINTER,
            EVENT_JSON_STRING_NAME, type);
    }

    private void updateDynamodbType(String dynamoDbType, ObjectNode event) {
        updateEventAtPointerWithNameAndValue(event, DYNAMODB_TYPE_POINTER, EVENT_JSON_STRING_NAME, dynamoDbType);
    }

    private void updateEventId(String eventName, ObjectNode event) {
        ((ObjectNode) event.at(FIRST_RECORD_POINTER)).put(EVENT_ID, eventName);
    }

    private void updateEventName(String eventName, ObjectNode event) {
        ((ObjectNode) event.at(FIRST_RECORD_POINTER)).put(EVENT_NAME, eventName);
    }

    private void updateDate(PublicationDate date, JsonNode event) {
        if (nonNull(date) && date.isPopulated()) {
            updateEventAtPointerWithNameAndValue(event, ENTITY_DESCRIPTION_PUBLICATION_DATE_JSON_POINTER,
                EVENT_YEAR_NAME, date.getYear());
            updateEventAtPointerWithNameAndValue(event, ENTITY_DESCRIPTION_PUBLICATION_DATE_JSON_POINTER,
                EVENT_MONTH_NAME, date.getMonth());
            updateEventAtPointerWithNameAndValue(event, ENTITY_DESCRIPTION_PUBLICATION_DATE_JSON_POINTER,
                EVENT_DAY_NAME, date.getDay());
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
            ((ObjectNode) event.at(pointer)).put(name, (String) value);
        } else {
            ((ObjectNode) event.at(pointer)).put(name, (Integer) value);
        }
    }

    private void updateEventAtPointerWithNameAndArrayValue(ObjectNode event,
                                                           ArrayNode value) {
        ((ObjectNode) event.at(PublicationStreamRecordTestDataGenerator.CONTRIBUTOR_POINTER))
            .set(PublicationStreamRecordTestDataGenerator.EVENT_JSON_LIST_NAME, value);
    }

    public static final class Builder {

        private String eventId;
        private String eventName;
        private UUID id;
        private String instancetype;
        private String dynamoDbType;
        private String mainTitle;
        private List<Contributor> contributors;
        private PublicationDate date;
        private String status;

        public Builder() {

        }

        /**
         * Create a valid publication populated by the provided faker.
         * @param faker data provider to generate fake data.
         * @return Builder populated from faker.
         */
        public static Builder createValidPublication(Faker faker) {
            var localDate = Instant.ofEpochMilli(faker.date().birthday().getTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
            return new Builder()
                .withId(UUID.randomUUID())
                .withInstanceType(faker.options().nextElement(PublicationType.values()).toString())
                .withDate(new PublicationDate(
                    String.valueOf(localDate.getYear()),
                    String.valueOf(localDate.getMonth().getValue()),
                    String.valueOf(localDate.getDayOfMonth())))
                .withDynamoDbType("Publication")
                .withMainTitle(faker.book().title())
                .withEventId(UUID.randomUUID().toString())
                .withEventName(faker.options().nextElement(List.of("MODIFY")))
                .withStatus(faker.options().nextElement(List.of("Published", "Draft")))
                .withContributors(getContributors(faker, false));
        }

        private static List<Contributor> getContributors(Faker faker, boolean withoutName) {
            var contributors = new ArrayList<Contributor>();
            for (int i = 0; i < faker.random().nextInt(1, 10); i++) {
                var builder = new Contributor.Builder();
                builder.withId(faker.options().nextElement(getIdOptions()));
                builder.withName(withoutName ? null : faker.superhero().name());
                contributors.add(builder.build());
            }
            return contributors;
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

        public Builder withId(UUID id) {
            this.id = id;
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

        public Builder withContributors(List<Contributor> contributors) {
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

        public PublicationStreamRecordTestDataGenerator build() {
            return new PublicationStreamRecordTestDataGenerator(this);
        }
    }
}
