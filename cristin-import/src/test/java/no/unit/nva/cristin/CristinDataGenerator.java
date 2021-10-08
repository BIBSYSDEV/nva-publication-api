package no.unit.nva.cristin;

import static no.unit.nva.cristin.mapper.CristinObject.MAIN_CATEGORY_FIELD;
import static no.unit.nva.cristin.mapper.CristinObject.PUBLICATION_OWNER_FIELD;
import static no.unit.nva.cristin.mapper.CristinObject.SECONDARY_CATEGORY_FIELD;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.ENCYCLOPEDIA;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.MONOGRAPH;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.NON_FICTION_BOOK;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.POPULAR_BOOK;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.REFERENCE_MATERIAL;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.TEXTBOOK;
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
import no.unit.nva.cristin.mapper.CristinBookOrReportMetadata;
import no.unit.nva.cristin.mapper.CristinContributor;
import no.unit.nva.cristin.mapper.CristinContributorRole;
import no.unit.nva.cristin.mapper.CristinContributorRoleCode;
import no.unit.nva.cristin.mapper.CristinContributorsAffiliation;
import no.unit.nva.cristin.mapper.CristinJournalPublication;
import no.unit.nva.cristin.mapper.CristinJournalPublicationJournal;
import no.unit.nva.cristin.mapper.CristinMainCategory;
import no.unit.nva.cristin.mapper.CristinObject;
import no.unit.nva.cristin.mapper.CristinPresentationalWork;
import no.unit.nva.cristin.mapper.CristinPublisher;
import no.unit.nva.cristin.mapper.CristinSecondaryCategory;
import no.unit.nva.cristin.mapper.CristinSubjectField;
import no.unit.nva.cristin.mapper.CristinTitle;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.publication.s3imports.FileContentsEvent;
import nva.commons.core.JsonUtils;
import org.apache.commons.lang3.RandomStringUtils;

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
    public static final int MIN_DOI_PREFIX_SUBPART_LENGTH = 3;
    public static final int MAX_DOI_PREFIX_SUBPART_LENGTH = 10;
    public static final String DOI_SUBPART_DELIMITER = ".";
    public static final String DOI_PREFIX_SUFFIX_SEPARATOR = "/";
    public static final String DOI_PREFIX_FIRST_SUBPART = "10";
    public static final int MIN_SUFFIX_PARTS_NUMBER = 2;
    private static final List<String> LANGUAGE_CODES = List.of("nb", "no", "en");
    private static final int NUMBER_OF_KNOWN_MAIN_CATEGORIES = 1;
    private static final int MIDDLE_INDEX_OF_ISSN_STRING = 4;
    private static final String JOURNAL_PUBLICATION_FIELD = "journalPublication";
    private static final String CRISTIN_TAGS = "tags";
    private static final String CRISTIN_PRESENTATIONAL_WORK = "presentationalWork";
    private static final String CRISTIN_SUBJECT_FIELD = "bookReport.subjectField";
    private static final String BOOK_OR_REPORT_PART_METADATA = "bookOrReportPartMetadata";
    private static final String BOOK_OR_REPORT_METADATA_FIELD = "bookOrReportMetadata";
    private static final String HRCS_CATEGORIES_AND_ACTIVITIES = ".hrcsCategoriesAndActivities";
    private static final String CRISTIN_MODIFIED_DATE = "entryLastModifiedDate";
    private static final String LECTURE_OR_POSTER_METADATA = ".lectureOrPosterMetaData";

    private static final CristinSecondaryCategory[] BOOK_SECONDARY_CATEGORIES = new CristinSecondaryCategory[]{
        MONOGRAPH,
        TEXTBOOK,
        NON_FICTION_BOOK,
        ENCYCLOPEDIA,
        POPULAR_BOOK,
        REFERENCE_MATERIAL};

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

    public static CristinPresentationalWork randomPresentationalWork() {
        return CristinPresentationalWork.builder()
            .withPresentationType(randomWord())
            .withIdentifier(smallRandomNumber()).build();
    }

    public static List<CristinContributorsAffiliation> randomAffiliations() {
        return smallSample()
            .map(ignored -> randomAffiliation())
            .collect(Collectors.toList());
    }

    public static String randomString() {
        return FAKER.lorem().sentence(smallRandomNumber());
    }

    public static String randomWord() {
        return FAKER.lorem().word();
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
            case TEXTBOOK:
            case NON_FICTION_BOOK:
            case ENCYCLOPEDIA:
            case POPULAR_BOOK:
            case REFERENCE_MATERIAL:
                return randomBook(category);
            case ANTHOLOGY:
                return randomBookAnthology();
            case FEATURE_ARTICLE:
                return randomFeatureArticle();
            case JOURNAL_LETTER:
            case READER_OPINION:
                return randomJournalLetter(category);
            case JOURNAL_REVIEW:
                return randomJournalReview();
            case JOURNAL_LEADER:
                return randomJournalLeader();
            case JOURNAL_CORRIGENDUM:
                return randomJournalCorrigendum();
            case JOURNAL_ARTICLE:
            case ARTICLE:
            case POPULAR_ARTICLE:
            case ACADEMIC_REVIEW:
            case SHORT_COMMUNICATION:
                return randomJournalArticle(category);
            case RESEARCH_REPORT:
                return randomResearchReport();
            case DEGREE_PHD:
                return randomDegreePhd();
            case DEGREE_MASTER:
            case SECOND_DEGREE_THESIS:
            case MEDICAL_THESIS:
                return randomDegreeMaster(category);
            case CHAPTER_ACADEMIC:
            case CHAPTER:
            case POPULAR_CHAPTER_ARTICLE:
            case LEXICAL_IMPORT:
                return randomChapterArticle(category);
            case CONFERENCE_LECTURE:
            case CONFERENCE_POSTER:
            case POPULAR_SCIENTIFIC_LECTURE:
            case LECTURE:
                return randomEvent(category);
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

    public static CristinObject randomBookAnthology() {
        return createRandomBookWithSpecifiedSecondaryCategory(CristinSecondaryCategory.ANTHOLOGY);
    }

    public static CristinObject randomBook() {
        CristinSecondaryCategory category = randomArrayElement(BOOK_SECONDARY_CATEGORIES, USE_WHOLE_ARRAY);
        return randomBook(category);
    }

    public static CristinObject randomBook(CristinSecondaryCategory secondaryCategory) {
        return createRandomBookWithSpecifiedSecondaryCategory(secondaryCategory);
    }

    private static CristinObject randomEvent(CristinSecondaryCategory secondaryCategory) {
        return createRandomEventWithSpecifiedSecondaryCategory(secondaryCategory);
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

    public static JsonNode objectWithInvalidIsbn() throws JsonProcessingException {
        CristinObject cristinObject = randomBook();
        cristinObject.getBookOrReportMetadata().setIsbn("123");
        return cristinObjectAsObjectNode(cristinObject);
    }

    public static JsonNode objectWithInvalidIssn() throws JsonProcessingException {
        CristinObject cristinObject = randomBook();
        cristinObject.getBookOrReportMetadata().getBookSeries().setIssn(" ");
        cristinObject.getBookOrReportMetadata().getBookSeries().setNsdCode(null);
        return cristinObjectAsObjectNode(cristinObject);
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

    public static String randomIssn() {
        String issnMissingChecksum = random7DigitNumber();
        return createValidIssn(issnMissingChecksum);
    }

    public static CristinBookOrReportMetadata randomBookOrReportMetadata() {
        return CristinBookOrReportMetadata
            .builder()
            .withIsbn(randomIsbn13())
            .withPublisherName(randomString())
            .withNumberOfPages(randomString())
            .withSubjectField(randomSubjectField())
            .withBookSeries(randomBookSeries())
            .withIssue(randomString())
            .withVolume(randomString())
            .withCristinPublisher(randomPublisher())
            .build();
    }

    private static CristinPublisher randomPublisher() {
        return CristinPublisher.builder()
            .withPublisherName(randomString())
            .withNsdCode(largeRandomNumber())
            .build();
    }

    private static CristinObject randomChapterArticle(CristinSecondaryCategory secondaryCategory) {
        return createRandomChapterWithSpecifiedSecondaryCategory(secondaryCategory);
    }

    private static String random7DigitNumber() {
        return String.valueOf((int) (Math.random() * 9000000) + 1000000);
    }

    private static String createValidIssn(String issnMissingChecksum) {
        int issnDigitsSum = 0;
        for (int i = 0; i < issnMissingChecksum.length(); i++) {
            int number = Integer.parseInt(Character.toString(issnMissingChecksum.charAt(i)));
            issnDigitsSum += (8 - i) * number;
        }

        String issnWithChecksum = createIssnWithCheckSum(issnMissingChecksum, issnDigitsSum);
        return issnWithChecksum.substring(0, MIDDLE_INDEX_OF_ISSN_STRING)
               + "-"
               + issnWithChecksum.substring(MIDDLE_INDEX_OF_ISSN_STRING);
    }

    private static String createIssnWithCheckSum(String issnMissingChecksum, int totalSum) {
        int mod11 = totalSum % 11;
        int checksum = mod11 == 0 ? 0 : 11 - mod11;
        String issnWithChecksum = checksum == 10 ? issnMissingChecksum + "X" : issnMissingChecksum + checksum;
        return issnWithChecksum;
    }

    private static CristinObject randomFeatureArticle() {
        return createRandomJournalWithSpecifiedSecondaryCategory(CristinSecondaryCategory.FEATURE_ARTICLE);
    }

    private static CristinObject randomJournalLetter(CristinSecondaryCategory secondaryCategory) {
        return createRandomJournalWithSpecifiedSecondaryCategory(secondaryCategory);
    }

    private static CristinObject randomJournalReview() {
        return createRandomJournalWithSpecifiedSecondaryCategory(CristinSecondaryCategory.JOURNAL_REVIEW);
    }

    private static CristinObject randomJournalLeader() {
        return createRandomJournalWithSpecifiedSecondaryCategory(CristinSecondaryCategory.JOURNAL_LEADER);
    }

    private static CristinObject randomJournalCorrigendum() {
        return createRandomJournalWithSpecifiedSecondaryCategory(CristinSecondaryCategory.JOURNAL_CORRIGENDUM);
    }

    private static CristinObject randomJournalArticle(CristinSecondaryCategory secondaryCategory) {
        return createRandomJournalWithSpecifiedSecondaryCategory(secondaryCategory);
    }

    private static CristinObject randomResearchReport() {
        return createRandomReportWithSpecifiedSecondaryCategory(CristinSecondaryCategory.RESEARCH_REPORT);
    }

    private static CristinObject randomDegreePhd() {
        return createRandomReportWithSpecifiedSecondaryCategory(CristinSecondaryCategory.DEGREE_PHD);
    }

    private static CristinObject randomDegreeMaster(CristinSecondaryCategory secondaryCategory) {
        return createRandomReportWithSpecifiedSecondaryCategory(secondaryCategory);
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

    private static CristinJournalPublicationJournal randomBookSeries() {
        return CristinJournalPublicationJournal.builder()
            .withJournalTitle(randomString())
            .withNsdCode(largeRandomNumber())
            .withIssn(randomIssn())
            .withIssnOnline(randomIssn())

            .build();
    }

    private static CristinObject createRandomBookWithSpecifiedSecondaryCategory(
        CristinSecondaryCategory secondaryCategory) {
        return CristinObject.builder()
            .withYearReported(2001)
            .withCristinTitles(List.of(randomCristinTitle(FIRST_TITLE)))
            .withEntryCreationDate(LocalDate.now())
            .withMainCategory(CristinMainCategory.BOOK)
            .withSecondaryCategory(secondaryCategory)
            .withId(largeRandomNumber())
            .withPublicationYear(randomYear())
            .withPublicationOwner(randomString())
            .withContributors(randomContributors())
            .withBookOrReportMetadata(randomBookOrReportMetadata())
            .build();
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
            .withJournalPublication(randomJournalPublictaion())
            .build();
    }

    private static CristinObject createRandomBookWithBookReportValues() {
        return CristinObject.builder()
            .withCristinTitles(List.of(randomCristinTitle(FIRST_TITLE)))
            .withEntryCreationDate(LocalDate.now())
            .withMainCategory(CristinMainCategory.BOOK)
            .withSecondaryCategory(MONOGRAPH)
            .withId(largeRandomNumber())
            .withPublicationYear(randomYear())
            .withPublicationOwner(randomString())
            .withContributors(randomContributors())
            .withBookOrReportMetadata(randomBookOrReportMetadata())
            .build();
    }

    private static CristinObject createRandomChapterWithSpecifiedSecondaryCategory(
        CristinSecondaryCategory secondaryCategory) {
        return CristinObject.builder()
            .withCristinTitles(List.of(randomCristinTitle(FIRST_TITLE)))
            .withEntryCreationDate(LocalDate.now())
            .withMainCategory(CristinMainCategory.CHAPTER)
            .withSecondaryCategory(secondaryCategory)
            .withId(largeRandomNumber())
            .withPublicationYear(randomYear())
            .withPublicationOwner(randomString())
            .withContributors(randomContributors())
            .build();
    }

    private static CristinObject createRandomEventWithSpecifiedSecondaryCategory(
            CristinSecondaryCategory secondaryCategory) {
        return CristinObject.builder()
                .withCristinTitles(List.of(randomCristinTitle(FIRST_TITLE)))
                .withEntryCreationDate(LocalDate.now())
                .withMainCategory(CristinMainCategory.EVENT)
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

    private static CristinObject createRandomReportWithSpecifiedSecondaryCategory(
        CristinSecondaryCategory secondaryCategory) {
        return CristinObject.builder()
            .withCristinTitles(List.of(randomCristinTitle(FIRST_TITLE)))
            .withEntryCreationDate(LocalDate.now())
            .withMainCategory(CristinMainCategory.REPORT)
            .withSecondaryCategory(secondaryCategory)
            .withId(largeRandomNumber())
            .withPublicationYear(randomYear())
            .withPublicationOwner(randomString())
            .withContributors(randomContributors())
            .withBookOrReportMetadata(randomBookOrReportMetadata())
            .build();
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
            .withYearReported(randomYear())
            .withContributors(contributors)
            .withBookOrReportMetadata(randomBookOrReportMetadata())
            .withPublicationOwner(HardcodedValues.HARDCODED_PUBLICATIONS_OWNER)
            .build();
    }


    private static CristinSubjectField randomSubjectField() {
        return CristinSubjectField.builder()
            .withSubjectFieldCode(smallRandomNumber())
            .build();
    }

    private static CristinJournalPublication randomJournalPublictaion() {
        int pagesBegin = smallRandomNumber();
        return CristinJournalPublication.builder()
            .withJournal(randomCristinJournalPublicationJournal())
            .withPagesBegin(String.valueOf(pagesBegin))
            .withPagesEnd(String.valueOf(pagesBegin + smallRandomNumber()))
            .withVolume(String.valueOf(smallRandomNumber()))
            .withDoi(randomDoiString())
            .build();
    }

    private static CristinJournalPublicationJournal randomCristinJournalPublicationJournal() {
        return CristinJournalPublicationJournal.builder()
            .withIssn(randomIssn())
            .withIssnOnline(randomIssn())
            .withJournalTitle(randomString())
            .build();
    }

    private static String toJsonString(CristinObject c) {
        return attempt(() -> OBJECT_MAPPER.writeValueAsString(c))
            .orElseThrow();
    }

    private static ObjectNode cristinObjectAsObjectNode(CristinObject cristinObject) throws JsonProcessingException {
        assertThat(cristinObject, doesNotHaveEmptyValuesIgnoringFields(
            Set.of(PUBLICATION_OWNER_FIELD, JOURNAL_PUBLICATION_FIELD, CRISTIN_TAGS,
                   CRISTIN_PRESENTATIONAL_WORK, CRISTIN_SUBJECT_FIELD, BOOK_OR_REPORT_METADATA_FIELD,
                   BOOK_OR_REPORT_PART_METADATA, HRCS_CATEGORIES_AND_ACTIVITIES, CRISTIN_MODIFIED_DATE,
                    LECTURE_OR_POSTER_METADATA)));

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

    private static int randomYear() {
        Date date = FAKER.date().birthday();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.YEAR);
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
        title.setAbstractText((randomString()));
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

    private static String randomDoiString() {
        String prefixSecondPart = RandomStringUtils.randomAlphanumeric(MIN_DOI_PREFIX_SUBPART_LENGTH,
                                                                       MAX_DOI_PREFIX_SUBPART_LENGTH);
        String suffix = randomDoiSuffix();
        return DOI_PREFIX_FIRST_SUBPART
               + DOI_SUBPART_DELIMITER
               + prefixSecondPart
               + DOI_PREFIX_SUFFIX_SEPARATOR
               + suffix;
    }

    private static String randomDoiSuffix() {
        return IntStream.range(1, MIN_SUFFIX_PARTS_NUMBER + RANDOM.nextInt(4)).boxed()
            .map(ignored -> RandomStringUtils.randomAlphanumeric(MIN_DOI_PREFIX_SUBPART_LENGTH,
                                                                 MAX_DOI_PREFIX_SUBPART_LENGTH))
            .collect(Collectors.joining(DOI_SUBPART_DELIMITER));
    }
}
