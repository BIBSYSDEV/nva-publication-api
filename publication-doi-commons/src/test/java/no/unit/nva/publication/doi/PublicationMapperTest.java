package no.unit.nva.publication.doi;

import static no.unit.nva.publication.doi.dynamodb.dao.DynamodbStreamRecordDao.ERROR_MUST_BE_PUBLICATION_TYPE;
import static nva.commons.utils.JsonUtils.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.javafaker.Faker;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import no.unit.nva.publication.doi.dto.PublicationStreamRecordTestDataGenerator;
import no.unit.nva.publication.doi.dto.PublicationStreamRecordTestDataGenerator.Builder;
import no.unit.nva.publication.doi.dto.PublicationType;
import no.unit.nva.publication.doi.dynamodb.dao.DynamodbStreamRecordDao;
import no.unit.nva.publication.doi.dynamodb.dao.Identity;
import nva.commons.utils.JsonUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class PublicationMapperTest {

    private static final String EXAMPLE_NAMESPACE = "http://example.net/nva/";
    private static final String UNKNOWN_DYNAMODB_STREAMRECORD_TYPE = "UnknownType";

    private static final Faker FAKER = new Faker();

    private PublicationStreamRecordTestDataGenerator createDynamoDbStreamRecordWithoutContributorIdentityNames() {
        return PublicationStreamRecordTestDataGenerator.Builder.createValidPublication(FAKER)
            .withContributorIdentities(createContributorIdentities(true))
            .build();
    }

    private List<Identity> createContributorIdentities(boolean withoutName) {
        List<Identity> contributors = new ArrayList<>();
        for (int i = 0; i < FAKER.random().nextInt(1, 10); i++) {
            Identity.Builder builder = new Identity.Builder();
            builder.withArpId(FAKER.number().digits(10));
            builder.withOrcId(FAKER.number().digits(10));
            builder.withName(withoutName ? null : FAKER.superhero().name());
            contributors.add(builder.build());
        }
        return contributors;
    }

    private DynamodbStreamRecordDao.Builder createDaoBuilder(JsonNode rootNode) {
        return new DynamodbStreamRecordDao.Builder().withDynamodbStreamRecord(rootNode);
    }

    private Executable createPublicationMapperWithBadDao(ObjectNode rootNode) {
        return () ->
        {
            var dao = createDaoBuilder(rootNode).build();
            new PublicationMapper(EXAMPLE_NAMESPACE).fromDynamodbStreamRecordDao(dao);
        };
    }

    @Test
    void fromDynamodbStreamRecordDaoThenReturnFullyMappedPublicationDto() {
        Builder daoBuilder = Builder.createValidPublication(FAKER);
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
    }

    @Test
    void fromDynamodbStreamRecordThrowsIllegalArgumentExceptionWhenUnknownDynamodbTableType() throws IOException {
        var rootNode = objectMapper.createObjectNode();
        rootNode.putObject("detail")
            .putObject("dynamodb")
            .putObject("newImage")
            .putObject("type")
            .put("s", UNKNOWN_DYNAMODB_STREAMRECORD_TYPE);
        var actualException = assertThrows(IllegalArgumentException.class, createPublicationMapperWithBadDao(rootNode));
        assertThat(actualException.getMessage(), containsString(ERROR_MUST_BE_PUBLICATION_TYPE));
    }

    @Test
    void fromDynamodbStreamRecordWhenContributorWithoutNameThenIsSkipped() throws IOException {
        var dynamodbStreamRecord = createDynamoDbStreamRecordWithoutContributorIdentityNames()
            .asDynamoDbStreamRecord();
        var dao = createDaoBuilder(objectMapper.convertValue(dynamodbStreamRecord, JsonNode.class))
            .build();
        var publication = new PublicationMapper(EXAMPLE_NAMESPACE).fromDynamodbStreamRecordDao(
            dao);

        assertThat(publication.getContributor(), hasSize(0));
    }
}