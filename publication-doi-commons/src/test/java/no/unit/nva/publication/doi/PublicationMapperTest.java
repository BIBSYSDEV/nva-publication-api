package no.unit.nva.publication.doi;

import static no.unit.nva.hamcrest.DoesNotHaveNullOrEmptyFields.doesNotHaveNullOrEmptyFields;
import static no.unit.nva.publication.doi.dynamodb.dao.DynamodbStreamRecordImageDao.ERROR_MUST_BE_PUBLICATION_TYPE;
import static no.unit.nva.publication.doi.dynamodb.dao.DynamodbStreamRecordJsonPointers.DynamodbImageType.NEW;
import static nva.commons.utils.JsonUtils.objectMapper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.StreamViewType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.javafaker.Faker;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import no.unit.nva.publication.doi.dto.PublicationStreamRecordTestDataGenerator;
import no.unit.nva.publication.doi.dto.PublicationStreamRecordTestDataGenerator.Builder;
import no.unit.nva.publication.doi.dto.PublicationType;
import no.unit.nva.publication.doi.dynamodb.dao.DynamodbStreamRecordImageDao;
import no.unit.nva.publication.doi.dynamodb.dao.DynamodbStreamRecordJsonPointers;
import no.unit.nva.publication.doi.dynamodb.dao.DynamodbStreamRecordJsonPointers.DynamodbImageType;
import no.unit.nva.publication.doi.dynamodb.dao.Identity;
import nva.commons.utils.IoUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class PublicationMapperTest {

    public static final String DYNAMO_STREAM_EVENT_WITH_OLD_IMAGE_JSON = "dynamoStream_event_with_old_image.json";
    private static final String EXAMPLE_NAMESPACE = "http://example.net/nva/";
    private static final String UNKNOWN_DYNAMODB_STREAMRECORD_TYPE = "UnknownType";
    private static final Faker FAKER = new Faker();

    @Test
    void fromDynamoStreamRecordNewImage() {
        DynamodbStreamRecordJsonPointers jsonPointers = new DynamodbStreamRecordJsonPointers(
            NEW);
        Builder daoBuilder = Builder.createValidPublication(FAKER, jsonPointers, StreamViewType.NEW_IMAGE.toString());
        var dynamodbStreamRecord = daoBuilder.build().asDynamoDbStreamRecord();
        PublicationMapper mapper = new PublicationMapper(EXAMPLE_NAMESPACE);
        var publicationMapping = mapper.fromDynamodbStreamRecord(dynamodbStreamRecord);

        assertTrue(publicationMapping.getOldPublication().isEmpty());
        assertTrue(publicationMapping.getNewPublication().isPresent());
    }

    @Test
    void fromDynamoStreamRecordOldImage() throws JsonProcessingException {
        String dynamoStreamRecordString = IoUtils.stringFromResources(
            Path.of(DYNAMO_STREAM_EVENT_WITH_OLD_IMAGE_JSON));
        DynamodbEvent.DynamodbStreamRecord recordWithOldImage = objectMapper.readValue(dynamoStreamRecordString,
            DynamodbEvent.DynamodbStreamRecord.class);
        PublicationMapper mapper = new PublicationMapper(EXAMPLE_NAMESPACE);
        var publicationMapping = mapper.fromDynamodbStreamRecord(recordWithOldImage);

        assertTrue(publicationMapping.getOldPublication().isPresent());
        assertTrue(publicationMapping.getNewPublication().isEmpty());
    }

    @Test
    void fromDynamodbStreamRecordDaoThenReturnFullyMappedPublicationDto() {
        Builder daoBuilder = validPublication(NEW);
        var dao = daoBuilder.build().asDynamodbStreamRecordDao();

        PublicationMapper mapper = new PublicationMapper(EXAMPLE_NAMESPACE);
        var dto = mapper.fromDynamodbStreamRecordDao(dao);

        assertThat(dto.getType(), is(equalTo(PublicationType.findByName(daoBuilder.getInstancetype()))));
        assertThat(dto.getPublicationDate(), is(equalTo(daoBuilder.getDate())));
        assertThat(dto.getDoi(), is(equalTo(URI.create(daoBuilder.getDoi()))));
        assertThat(dto.getInstitutionOwner(), is(equalTo(URI.create(daoBuilder.getPublisherId()))));
        assertThat(dto.getContributor(), hasSize(daoBuilder.getContributors().size()));
        assertThat(dto.getMainTitle(), is(equalTo(daoBuilder.getMainTitle())));
        assertThat(dto.getId(), is(equalTo(URI.create(mapper.namespacePublication + daoBuilder.getIdentifier()))));

        assertThat(dto, doesNotHaveNullOrEmptyFields());
    }

    @Test
    void fromDynamodbStreamRecordThrowsIllegalArgumentExceptionWhenUnknownDynamodbTableType() {
        var rootNode = objectMapper.createObjectNode();
        rootNode.putObject("detail")
            .putObject("dynamodb")
            .putObject("newImage")
            .putObject("type")
            .put("s", UNKNOWN_DYNAMODB_STREAMRECORD_TYPE);
        var actualException = assertThrows(IllegalArgumentException.class,
            createPublicationMapperWithBadDao(rootNode, NEW));
        assertThat(actualException.getMessage(), containsString(ERROR_MUST_BE_PUBLICATION_TYPE));
    }

    @Test
    void fromDynamodbStreamRecordWhenContributorWithoutNameThenIsSkipped() throws IOException {
        var dynamodbStreamRecord =
            createDynamoDbStreamRecordWithoutContributorIdentityNames(NEW).asDynamoDbStreamRecord();
        JsonNode dynamoStreamRecordJsonNode = objectMapper.convertValue(dynamodbStreamRecord, JsonNode.class);
        var dao = createDaoBuilder(dynamoStreamRecordJsonNode, NEW)
            .build();
        var publication = new PublicationMapper(EXAMPLE_NAMESPACE).fromDynamodbStreamRecordDao(
            dao);

        assertThat(publication.getContributor(), hasSize(0));
    }

    private Builder validPublication(DynamodbImageType imageType) {
        return Builder.createValidPublication(FAKER, new DynamodbStreamRecordJsonPointers(imageType));
    }

    private PublicationStreamRecordTestDataGenerator createDynamoDbStreamRecordWithoutContributorIdentityNames(
        DynamodbImageType imageType
    ) {
        return validPublication(imageType)
            .withContributorIdentities(createContributorIdentities(true, NEW))
            .build();
    }

    private List<Identity> createContributorIdentities(boolean withoutName, DynamodbImageType imageType) {
        DynamodbStreamRecordJsonPointers jsonPointers = new DynamodbStreamRecordJsonPointers(imageType);
        List<Identity> contributors = new ArrayList<>();
        for (int i = 0; i < FAKER.random().nextInt(1, 10); i++) {
            Identity.Builder builder = new Identity.Builder(jsonPointers);
            builder.withArpId(FAKER.number().digits(10));
            builder.withOrcId(FAKER.number().digits(10));
            builder.withName(withoutName ? null : FAKER.superhero().name());
            contributors.add(builder.build());
        }
        return contributors;
    }

    private DynamodbStreamRecordImageDao.Builder createDaoBuilder(JsonNode rootNode, DynamodbImageType imageType) {
        DynamodbStreamRecordJsonPointers jsonPointers = new DynamodbStreamRecordJsonPointers(imageType);

        return new DynamodbStreamRecordImageDao.Builder(jsonPointers).withDynamodbStreamRecordImage(rootNode);
    }

    private Executable createPublicationMapperWithBadDao(ObjectNode rootNode, DynamodbImageType imageType) {
        return () -> {
            var dao = createDaoBuilder(rootNode, imageType).build();
            new PublicationMapper(EXAMPLE_NAMESPACE).fromDynamodbStreamRecordDao(dao);
        };
    }
}