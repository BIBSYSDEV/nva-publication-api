package no.unit.nva.publication.doi.dto;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import com.fasterxml.jackson.core.JsonPointer;
import com.github.javafaker.Faker;
import no.unit.nva.publication.doi.dto.PublicationStreamRecordTestDataGenerator.Builder;
import no.unit.nva.publication.doi.dynamodb.dao.DynamodbStreamRecordJsonPointers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PublicationStreamRecordTestDataGeneratorTest {

    public static final String ENTITY_DESCRIPTION = "entityDescription";
    public static final String ENTITYDESCRIPTION_REFERENCE = "reference";
    public static final String ENTITYDESCRIPTION_REFERENCE_PUBLICATION_INSTANCE = "publicationInstance";
    public static final String TYPE = "type";
    public static final String MAIN_TITLE = "mainTitle";
    public static final String CONTRIBUTORS = "contributors";
    public static final String DATE = "date";
    public static final String DATE_YEAR = "year";
    public static final String DATE_MONTH = "month";
    public static final String DATE_DAY = "day";
    public static final String STATUS = "status";
    public static final String IDENTIFIER = "identifier";
    public static final String DASH = "-";
    private static Builder validStreamRecordBuilder;

    private  static final DynamodbStreamRecordJsonPointers jsonPointers = new DynamodbStreamRecordJsonPointers("/dynamodb/newImage");

    @BeforeEach
    void setUp() {
        validStreamRecordBuilder = Builder.createValidPublication(new Faker(), jsonPointers);
    }

    @Test
    void asDynamoDbStreamRecord() {
        var streamRecord = validStreamRecordBuilder.build().asDynamoDbStreamRecord();
        assertThat(streamRecord.getEventID(), notNullValue());
        assertThat(streamRecord.getEventSourceARN(), notNullValue());
        assertThat(streamRecord.getEventID(), notNullValue());
        assertThat(streamRecord.getAwsRegion(), notNullValue());
        assertThat(streamRecord.getEventName(), notNullValue());
        assertThat(streamRecord.getEventVersion(), notNullValue());
        // TODO later when we need UserIdentity: assertThat(streamRecord.getUserIdentity(), notNullValue());

        var dynamodb = streamRecord.getDynamodb();
        assertThat(dynamodb.getNewImage().get(IDENTIFIER).getS(), containsString(DASH));
        var entityDescription = dynamodb.getNewImage()
            .get(ENTITY_DESCRIPTION)
            .getM();
        var entityReferencePublicationInstanceType = entityDescription
            .get(ENTITYDESCRIPTION_REFERENCE)
            .getM()
            .get(ENTITYDESCRIPTION_REFERENCE_PUBLICATION_INSTANCE)
            .getM()
            .get(TYPE)
            .getS();
        assertThat(entityReferencePublicationInstanceType, notNullValue());
        assertThat(entityDescription.get(MAIN_TITLE).getS(), notNullValue());
        assertThat(entityDescription.get(CONTRIBUTORS).getL(),
            hasSize(greaterThan(0)));
        var dateMap = entityDescription.get(DATE).getM();
        assertThat(dateMap.get(DATE_YEAR).getS(), notNullValue());
        assertThat(dateMap.get(DATE_MONTH).getS(), notNullValue());
        assertThat(dateMap.get(DATE_DAY).getS(), notNullValue());
        assertThat(dynamodb.getNewImage().get(STATUS).getS(), notNullValue());
    }

    @Test
    void asDynamodbStreamRecordDao() {
        var dao = validStreamRecordBuilder.build().asDynamodbStreamRecordDao();
        assertThat(dao.getIdentifier(), notNullValue());
        assertThat(dao.getDoi(), notNullValue());
        assertThat(dao.getPublisherId(), notNullValue());
        assertThat(dao.getMainTitle(), notNullValue());
        assertThat(dao.getPublicationReleaseDate(), notNullValue());
        assertThat(dao.getContributorIdentities(), hasSize(greaterThan(0)));
    }
}