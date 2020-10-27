package no.unit.nva.publication.doi.dynamodb.dao;

import static no.unit.nva.hamcrest.DoesNotHaveNullOrEmptyFields.doesNotHaveNullOrEmptyFields;
import static no.unit.nva.publication.doi.dynamodb.dao.DynamodbStreamRecordJsonPointers.DYNAMODB_NEW_IMAGE_BASE;
import static nva.commons.utils.JsonUtils.objectMapper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.javafaker.Faker;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;
import no.unit.nva.publication.doi.dto.PublicationStreamRecordTestDataGenerator;
import no.unit.nva.publication.doi.dto.PublicationType;
import no.unit.nva.publication.doi.dynamodb.dao.DynamodbStreamRecordImageDao.Builder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DynamodbStreamRecordImageDaoTest {

    public static final String EXPECTED_PUBLICATION_TYPE = "Publication";
    private Faker faker;
    private Random random;
    private DynamodbStreamRecordJsonPointers jsonPointers;

    @BeforeEach
    void setUp() {
        random = new Random();
        faker = new Faker(pickRandomLocale());
        jsonPointers  = new DynamodbStreamRecordJsonPointers(DYNAMODB_NEW_IMAGE_BASE);
    }

    @Test
    void getId() {
        var randomIdentifier = UUID.randomUUID().toString();
        var daoIdentifier = getBuilder().withIdentifier(randomIdentifier).build().getIdentifier();
        assertThat(daoIdentifier, is(equalTo(randomIdentifier)));
    }

    @Test
    void getPublicationInstanceType() {
        var randomValidPublicationInstanceType = getRandomValidPublicationInstanceType();
        assertThat(getBuilder().withPublicationInstanceType(randomValidPublicationInstanceType)
            .build()
            .getPublicationInstanceType(), is(equalTo(randomValidPublicationInstanceType)));
    }

    @Test
    void getMainTitle() {
        var exampleTitle = faker.book().title();
        assertThat(getBuilder().withMainTitle(exampleTitle).build().getMainTitle(), is(equalTo(exampleTitle)));
    }

    @Test
    void getDynamodbStreamRecordType() {
        assertThat(getBuilder()
                .withDynamodbStreamRecordImageType(DynamodbStreamRecordImageDao.PUBLICATION_TYPE)
                .build()
                .getDynamodbStreamRecordType(),
            is(equalTo(EXPECTED_PUBLICATION_TYPE)));
    }

    @Test
    void getPublicationReleaseDate() {
        JsonNode actual = getBuilder().withPublicationReleaseDate(
            createPublicationReleaseDateJsonNode()).build().getPublicationReleaseDate();
        var actualDateMap = actual.get("date").get("m");
        assertThat(extractString(actualDateMap, "year"), is(equalTo("1999")));
        assertThat(extractString(actualDateMap, "month"), is(equalTo("07")));
        assertThat(extractString(actualDateMap, "day"), is(equalTo("09")));
    }

    @Test
    void getInstitutionOwner() {
        var institutionOwner = "https://example.net/nva/institution/1234";
        assertThat(getBuilder().withPublisherId(institutionOwner).build().getPublisherId(),
            is(equalTo(institutionOwner)));
    }

    @Test
    void getDoi() {
        var doi = "https://doi.net/prefix/suffix/documentid";
        assertThat(getBuilder().withDoi(doi).build().getDoi(), is(equalTo(doi)));
    }

    @Test
    void getContributorIdentities() {
        var contributorIdentities = createContributorIdentities();
        Identity firstIdentity = contributorIdentities.get(0);

        var actual = getBuilder().withContributorIdentities(contributorIdentities)
            .build()
            .getContributorIdentities();

        assertThat(actual, hasSize(1));
        assertThat(actual, hasItem(firstIdentity));
        assertThat(actual.get(0).getName(), is(equalTo(firstIdentity.getName())));
        assertThat(actual.get(0).getArpId(), is(equalTo(firstIdentity.getArpId())));
        assertThat(actual.get(0).getOrcId(), is(equalTo(firstIdentity.getOrcId())));
    }

    @Test
    void builderWithDynamodbStreamRecordJsonNodeReturnsFullyPopulatedDao() {
        PublicationStreamRecordTestDataGenerator.Builder validPublication =
            PublicationStreamRecordTestDataGenerator.Builder
                .createValidPublication(faker, jsonPointers);

        var dao = getBuilder()
            .withDynamodbStreamRecordImage(validPublication.build().asDynamoDbStreamRecordJsonNode())
            .build();

        assertThat(dao.getIdentifier(), is(equalTo(validPublication.getIdentifier())));
        assertThat(dao.getDynamodbStreamRecordType(), is(equalTo(DynamodbStreamRecordImageDao.PUBLICATION_TYPE)));
        assertThat(dao.getPublicationInstanceType(), is(equalTo(validPublication.getInstancetype())));
        assertThat(dao.getMainTitle(), is(equalTo(validPublication.getMainTitle())));
        assertThat(dao.getDoi(), is(equalTo(validPublication.getDoi())));
        assertThat(dao.getPublisherId(), is(equalTo(validPublication.getPublisherId())));
        assertThat(dao.getContributorIdentities(), is(equalTo(validPublication.getContributors())));

        JsonNode getDaoDateMap = dao.getPublicationReleaseDate().get("date").get("m");
        assertThat(getDaoDateMap.get("year").get("s").textValue(), is(equalTo(validPublication.getDate().getYear())));
        assertThat(getDaoDateMap.get("month").get("s").textValue(), is(equalTo(validPublication.getDate().getMonth())));
        assertThat(getDaoDateMap.get("day").get("s").textValue(), is(equalTo(validPublication.getDate().getDay())));
        assertThat(dao, doesNotHaveNullOrEmptyFields());
    }

    private ObjectNode createPublicationReleaseDateJsonNode() {
        var date = objectMapper.createObjectNode();
        var dateMap = date.putObject("date").putObject("m");
        dateMap.putObject("year").put("s", "1999");
        dateMap.putObject("month").put("s", "07");
        dateMap.putObject("day").put("s", "09");
        return date;
    }

    private Builder getBuilder() {
        return new DynamodbStreamRecordImageDao.Builder(jsonPointers);
    }

    private Locale pickRandomLocale() {
        var locales = Arrays.asList(Locale.ENGLISH, Locale.CHINA, Locale.FRANCE, Locale.GERMAN, Locale.KOREA);
        return locales.get(random.nextInt(locales.size()));
    }

    private String getRandomValidPublicationInstanceType() {
        return faker.options().nextElement(PublicationType.values()).toString();
    }

    private String extractString(JsonNode actualDateMap, String field) {
        return actualDateMap.get(field).get("s").textValue();
    }

    private List<Identity> createContributorIdentities() {
        Identity.Builder builder = new Identity.Builder(jsonPointers);
        builder.withArpId(faker.number().digits(10));
        builder.withOrcId(faker.number().digits(10));
        builder.withName(faker.superhero().name());
        return Collections.singletonList(builder.build());
    }
}