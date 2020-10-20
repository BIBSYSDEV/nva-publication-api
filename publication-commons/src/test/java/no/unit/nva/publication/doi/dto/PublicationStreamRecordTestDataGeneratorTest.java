package no.unit.nva.publication.doi.dto;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import com.github.javafaker.Faker;
import no.unit.nva.publication.doi.dto.PublicationStreamRecordTestDataGenerator.Builder;
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
    private static Builder validPublication;

    @BeforeEach
    void setUp() {
        validPublication = Builder.createValidPublication(new Faker());
    }

    @Test
    void asDynamoDbStreamRecord() {
        var streamRecord = validPublication.build().asDynamoDbStreamRecord();
        assertThat(streamRecord.getEventID(), notNullValue());
        assertThat(streamRecord.getEventSourceARN(), notNullValue());
        assertThat(streamRecord.getEventID(), notNullValue());
        assertThat(streamRecord.getAwsRegion(), notNullValue());
        assertThat(streamRecord.getEventName(), notNullValue());
        assertThat(streamRecord.getEventVersion(), notNullValue());
        //assertThat(streamRecord.getUserIdentity(), notNullValue());

        var dynamodb = streamRecord.getDynamodb();
        assertThat(dynamodb.getNewImage().get(IDENTIFIER).getS(), containsString(DASH));
        var entityDescription = dynamodb.getNewImage()
            .get(ENTITY_DESCRIPTION)
            .getM();
        assertThat(entityDescription
            .get(ENTITYDESCRIPTION_REFERENCE)
            .getM()
            .get(ENTITYDESCRIPTION_REFERENCE_PUBLICATION_INSTANCE)
            .getM()
            .get(TYPE)
            .getS(), notNullValue());
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
    void asPublicationDto() {
        var publication = validPublication.build().asPublicationDto();
        assertThat(publication.getId(), notNullValue());
        assertThat(publication.getDoi(), notNullValue());
        assertThat(publication.getInstitutionOwner(), notNullValue());
        assertThat(publication.getMainTitle(), notNullValue());
        assertThat(publication.getPublicationDate(), notNullValue());
        assertThat(publication.getContributor(), hasSize(greaterThan(0)));
    }
}