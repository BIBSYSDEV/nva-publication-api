package no.unit.nva.cristin;

import static no.unit.nva.cristin.mapper.CristinObject.MAIN_CATEGORY_FIELD;
import static no.unit.nva.cristin.mapper.CristinObject.PUBLICATION_OWNER_FIELD;
import static no.unit.nva.cristin.mapper.CristinObject.SECONDARY_CATEGORY_FIELD;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFields;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.javafaker.Faker;
import java.net.URI;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import no.unit.nva.cristin.lambda.CristinEntryEventConsumer;
import no.unit.nva.cristin.lambda.constants.HardcodedValues;
import no.unit.nva.cristin.mapper.CristinBookReport;
import no.unit.nva.cristin.mapper.CristinContributor;
import no.unit.nva.cristin.mapper.CristinContributorRole;
import no.unit.nva.cristin.mapper.CristinContributorRoleCode;
import no.unit.nva.cristin.mapper.CristinContributorsAffiliation;
import no.unit.nva.cristin.mapper.CristinMainCategory;
import no.unit.nva.cristin.mapper.CristinObject;
import no.unit.nva.cristin.mapper.CristinSecondaryCategory;
import no.unit.nva.cristin.mapper.CristinTitle;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.publication.s3imports.FileContentsEvent;
import nva.commons.core.JsonUtils;

public final class CristinDataGenerator {

    public static final int SMALL_NUMBER = 10;
    public static final Random RANDOM = new Random(System.currentTimeMillis());
    public static final Faker FAKER = Faker.instance();
    public static final int FIRST_TITLE = 0;
    public static final ObjectMapper OBJECT_MAPPER = JsonUtils.objectMapperSingleLine.configure(
        SerializationFeature.INDENT_OUTPUT, false);
    public static final int USE_WHOLE_ARRAY = -1;
    public static final int NUMBER_OF_KNOWN_SECONDARY_CATEGORIES = 1;
    public static final String ID_FIELD = "id";
    public static final String NULL_KEY = "null";
    private static final List<String> LANGUAGE_CODES = List.of("nb", "no", "en");
    private static final int NUMBER_OF_KNOWN_MAIN_CATEGORIES = 1;

    private CristinDataGenerator() {

    }

    public static CristinContributorsAffiliation randomAffiliation() {
        return creatCristinContributorsAffiliation(randomCristinContributorRoleCode());
    }

    public static CristinContributor randomContributor(Integer contributorIndex) {
        return CristinContributor.builder()
                   .withContributorOrder(contributorIndex)
                   .withIdentifier(contributorIndex)
                   .withGivenName(randomString())
                   .withFamilyName(randomString())
                   .withAffiliations(randomAffiliations())
                   .build();
    }

    public static List<CristinContributorsAffiliation> randomAffiliations() {
        return smallSample()
                   .map(ignored -> randomAffiliation())
                   .collect(Collectors.toList());
    }

    public static String randomString() {
        return FAKER.lorem().sentence(smallRandomNumber());
    }

    public static CristinContributorsAffiliation createAffiliation(CristinContributorRoleCode roleCode) {
        return creatCristinContributorsAffiliation(roleCode);
    }

    public static int smallRandomNumber() {
        return 1 + RANDOM.nextInt(SMALL_NUMBER);
    }

    public static int largeRandomNumber() {
        return 1 + RANDOM.nextInt(Integer.MAX_VALUE);
    }

    public static CristinObject randomObject() {
        return newCristinObject(largeRandomNumber());
    }

    public static CristinObject randomObject(String secondaryCategory) {
        CristinSecondaryCategory category = CristinSecondaryCategory.fromString(secondaryCategory);
        switch (category) {
            case MONOGRAPH:
                return randomBookMonograph();
            case ANTHOLOGY:
                return randomBookAnthology();
            case JOURNAL_ARTICLE:
                return randomJournalArticle();
            default:
                break;
        }
        throw new IllegalStateException(
            String.format("The secondary category %s is not covered", secondaryCategory));
    }

    public static <T> AwsEventBridgeEvent<FileContentsEvent<JsonNode>> toAwsEvent(T inputData) {
        AwsEventBridgeEvent<FileContentsEvent<JsonNode>> event = new AwsEventBridgeEvent<>();
        JsonNode cristinData = convertToJsonNode(inputData);
        FileContentsEvent<JsonNode> eventDetail = new FileContentsEvent<>(randomUri(), cristinData);
        event.setDetailType(CristinEntryEventConsumer.EVENT_DETAIL_TYPE);
        event.setDetail(eventDetail);
        return event;
    }

    private static CristinObject randomJournalArticle() {
        return createRandomJournalWithSpecifiedSecondaryCategory(CristinSecondaryCategory.JOURNAL_ARTICLE);
    }

    public static CristinObject randomBookAnthology() {
        return createRandomBookWithSpecifiedSecondaryCategory(CristinSecondaryCategory.ANTHOLOGY);
    }

    public static CristinObject randomBookMonograph() {
        return createRandomBookWithSpecifiedSecondaryCategory(CristinSecondaryCategory.MONOGRAPH);
    }

    public static CristinObject objectWithRandomBookReport() {
        return createRandomBookWithBookReportValues();
    }

    public static String singleRandomObjectAsString() {
        return newCristinObject(0).toJsonString();
    }

    public static JsonNode objectWithCustomMainCategory(String customMainCategory) throws JsonProcessingException {
        return cristinObjectWithUnexpectedValue(randomObject(), customMainCategory, MAIN_CATEGORY_FIELD);
    }

    public static JsonNode objectWithCustomSecondaryCategory(String customSecondaryCategory)
        throws JsonProcessingException {
        return cristinObjectWithUnexpectedValue(randomObject(), customSecondaryCategory, SECONDARY_CATEGORY_FIELD);
    }

    public static String randomDataAsString() {
        return randomObjects()
                   .map(CristinDataGenerator::toJsonString)
                   .collect(Collectors.joining(System.lineSeparator()));
    }

    public static Stream<CristinObject> randomObjects() {
        return IntStream.range(0, 100).boxed()
                   .map(CristinDataGenerator::newCristinObject);
    }

    public static JsonNode objectWithoutId() throws JsonProcessingException {
        ObjectNode cristinObject = cristinObjectAsObjectNode(randomObject());
        cristinObject.remove(ID_FIELD);
        return cristinObject;
    }

    public static CristinObject newCristinObjectWithRoleCode(CristinContributorRoleCode roleCode) {
        return createObjectWithCristinContributorRoleCode(0, createContributors(roleCode));
    }

    public static JsonNode injectCustomSecondaryCategoryIntoCristinObject(CristinObject cristinObject,
                                                                          String customSecondaryCategory)
        throws JsonProcessingException {
        return cristinObjectWithUnexpectedValue(cristinObject, customSecondaryCategory, SECONDARY_CATEGORY_FIELD);
    }

    public static String randomIsbn13() {
        return FAKER.code().isbn13();
    }

    private static <T> T randomElement(List<T> elements) {
        return elements.get(RANDOM.nextInt(elements.size()));
    }

    private static CristinContributorsAffiliation creatCristinContributorsAffiliation(
        CristinContributorRoleCode roleCode) {
        return CristinContributorsAffiliation.builder()
                   .withInstitutionIdentifier(threeDigitPositiveNumber())
                   .withDepartmentIdentifier(threeDigitPositiveNumber())
                   .withGroupNumber(threeDigitPositiveNumber())
                   .withSubdepartmentIdentifier(threeDigitPositiveNumber())
                   .withDepartmentIdentifier(largeRandomNumber())
                   .withRoles(List.of(createRole(roleCode)))
                   .build();
    }

    private static CristinContributorRole createRole(CristinContributorRoleCode roleCode) {
        return CristinContributorRole
                   .builder()
                   .withRoleCode(roleCode)
                   .build();
    }

    private static List<CristinContributor> randomContributors() {
        return smallSample()
                   .map(CristinDataGenerator::randomContributor)
                   .collect(Collectors.toList());
    }

    private static Stream<Integer> smallSample() {
        return IntStream.range(0, smallRandomNumber()).boxed();
    }

    private static <T> T randomArrayElement(T[] array, int subArrayEndIndex) {
        return subArrayEndIndex > 0
                   ? array[RANDOM.nextInt(subArrayEndIndex)]
                   : array[RANDOM.nextInt(array.length)];
    }

    private static CristinContributorRoleCode randomCristinContributorRoleCode() {
        return randomArrayElement(CristinContributorRoleCode.values(), USE_WHOLE_ARRAY);
    }

    private static int threeDigitPositiveNumber() {
        return RANDOM.nextInt(1000);
    }

    private static CristinObject createRandomBookWithSpecifiedSecondaryCategory(
        CristinSecondaryCategory secondaryCategory) {
        CristinObject cristinObject = CristinObject
                                          .builder()
                                          .withCristinTitles(List.of(randomCristinTitle(FIRST_TITLE)))
                                          .withEntryCreationDate(LocalDate.now())
                                          .withMainCategory(CristinMainCategory.BOOK)
                                          .withSecondaryCategory(secondaryCategory)
                                          .withId(largeRandomNumber())
                                          .withPublicationYear(randomYear())
                                          .withPublicationOwner(randomString())
                                          .withContributors(randomContributors())
                                          .withBookReport(randomBookReport())
                                          .build();
        assertThat(cristinObject, doesNotHaveEmptyValues());
        return cristinObject;
    }

    private static CristinObject createRandomBookWithBookReportValues() {
        CristinObject cristinObject = CristinObject
                                          .builder()
                                          .withCristinTitles(List.of(randomCristinTitle(FIRST_TITLE)))
                                          .withEntryCreationDate(LocalDate.now())
                                          .withMainCategory(CristinMainCategory.BOOK)
                                          .withSecondaryCategory(CristinSecondaryCategory.MONOGRAPH)
                                          .withId(largeRandomNumber())
                                          .withPublicationYear(randomYear())
                                          .withPublicationOwner(randomString())
                                          .withContributors(randomContributors())
                                          .withBookReport(randomBookReport())
                                          .build();
        assertThat(cristinObject, doesNotHaveEmptyValues());
        return cristinObject;
    }

    private static CristinObject createRandomJournalWithSpecifiedSecondaryCategory(
        CristinSecondaryCategory secondaryCategory) {
        return CristinObject
                   .builder()
                   .withCristinTitles(List.of(randomCristinTitle(FIRST_TITLE)))
                   .withEntryCreationDate(LocalDate.now())
                   .withMainCategory(CristinMainCategory.JOURNAL)
                   .withSecondaryCategory(secondaryCategory)
                   .withId(largeRandomNumber())
                   .withPublicationYear(randomYear())
                   .withPublicationOwner(randomString())
                   .withContributors(randomContributors())
                   .build();
    }

    private static CristinObject newCristinObject(Integer index) {
        return createObjectWithCristinContributorRoleCode(index, randomContributors());
    }

    private static CristinObject createObjectWithCristinContributorRoleCode(Integer index,
                                                                            List<CristinContributor> contributors) {
        return CristinObject.builder()
                   .withMainCategory(randomMainCategory())
                   .withSecondaryCategory(randomSecondaryCategory())
                   .withCristinTitles(randomTitles())
                   .withId(index)
                   .withEntryCreationDate(LocalDate.now())
                   .withPublicationYear(randomYear())
                   .withContributors(contributors)
                   .withBookReport(randomBookReport())
                   .withPublicationOwner(HardcodedValues.HARDCODED_PUBLICATIONS_OWNER)
                   .build();
    }

    private static CristinBookReport randomBookReport() {
        return CristinBookReport
                   .builder()
                   .withIsbn(randomIsbn13())
                   .withPublisherName(randomString())
                   .withNumberOfPages(randomString())
                   .build();
    }

    private static String toJsonString(CristinObject c) {
        return attempt(() -> OBJECT_MAPPER.writeValueAsString(c))
                   .orElseThrow();
    }

    private static ObjectNode cristinObjectAsObjectNode(CristinObject cristinObject) throws JsonProcessingException {
        assertThat(cristinObject, doesNotHaveEmptyValuesIgnoringFields(Set.of(PUBLICATION_OWNER_FIELD)));
        return (ObjectNode) JsonUtils.objectMapperNoEmpty.readTree(cristinObject.toJsonString());
    }

    private static <T> JsonNode convertToJsonNode(T inputData) {
        return inputData instanceof JsonNode
                   ? (JsonNode) inputData
                   : JsonUtils.objectMapperNoEmpty.convertValue(inputData, JsonNode.class);
    }

    private static CristinSecondaryCategory randomSecondaryCategory() {
        return randomArrayElement(CristinSecondaryCategory.values(), NUMBER_OF_KNOWN_SECONDARY_CATEGORIES);
    }

    private static String randomYear() {
        Date date = FAKER.date().birthday();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int year = calendar.get(Calendar.YEAR);
        return Integer.toString(year);
    }

    private static JsonNode cristinObjectWithUnexpectedValue(CristinObject cristinObject,
                                                             String customSecondaryCategory,
                                                             String secondaryCategoryField)
        throws JsonProcessingException {
        ObjectNode json = cristinObjectAsObjectNode(cristinObject);
        json.put(secondaryCategoryField, customSecondaryCategory);
        return json;
    }

    private static CristinTitle randomCristinTitle(int index) {
        CristinTitle title = new CristinTitle();
        title.setTitle(randomString());
        title.setLanguagecode(randomLanguageCode());
        if (index == FIRST_TITLE) {
            title.setStatusOriginal(CristinTitle.ORIGINAL_TITLE);
        } else {
            title.setStatusOriginal(CristinTitle.NOT_ORIGINAL_TITLE);
        }
        return title;
    }

    private static CristinMainCategory randomMainCategory() {
        return randomArrayElement(CristinMainCategory.values(), NUMBER_OF_KNOWN_MAIN_CATEGORIES);
    }

    private static List<CristinContributor> createContributors(CristinContributorRoleCode roleCode) {
        return smallSample()
                   .map(contributorIndex -> creatContributor(contributorIndex, roleCode))
                   .collect(Collectors.toList());
    }

    private static String randomLanguageCode() {
        return randomElement(LANGUAGE_CODES);
    }

    private static List<CristinTitle> randomTitles() {
        return smallSample()
                   .map(CristinDataGenerator::randomCristinTitle)
                   .collect(Collectors.toList());
    }

    private static CristinContributor creatContributor(Integer contributorIndex, CristinContributorRoleCode roleCode) {
        return CristinContributor.builder()
                   .withContributorOrder(contributorIndex)
                   .withIdentifier(contributorIndex)
                   .withGivenName(randomString())
                   .withFamilyName(randomString())
                   .withAffiliations(List.of(createAffiliation(roleCode)))
                   .build();
    }

    private static URI randomUri() {
        String prefix = "https://www.example.org/";
        String suffix = FAKER.lorem().word() + FAKER.lorem().word();
        return URI.create(prefix + suffix);
    }
}
