package no.unit.nva.cristin;

import static no.unit.nva.cristin.CristinImportConfig.eventHandlerObjectMapper;
import static no.unit.nva.cristin.mapper.CristinObject.MAIN_CATEGORY_FIELD;
import static no.unit.nva.cristin.mapper.CristinObject.PUBLICATION_OWNER_FIELD;
import static no.unit.nva.cristin.mapper.CristinObject.SECONDARY_CATEGORY_FIELD;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.ENCYCLOPEDIA;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.JOURNAL_ARTICLE;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.MONOGRAPH;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.MUSEUM;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.NON_FICTION_BOOK;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.POPULAR_BOOK;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.REFERENCE_MATERIAL;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.TEXTBOOK;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.WRITTEN_INTERVIEW;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFields;
import static no.unit.nva.testutils.RandomDataGenerator.randomBoolean;
import static no.unit.nva.testutils.RandomDataGenerator.randomDoi;
import static no.unit.nva.testutils.RandomDataGenerator.randomInstant;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.datafaker.providers.base.BaseFaker;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.cristin.lambda.constants.HardcodedValues;
import no.unit.nva.cristin.mapper.CristinAssociatedUri;
import no.unit.nva.cristin.mapper.CristinBookOrReportMetadata;
import no.unit.nva.cristin.mapper.CristinBookOrReportPartMetadata;
import no.unit.nva.cristin.mapper.CristinContributor;
import no.unit.nva.cristin.mapper.CristinContributorRole;
import no.unit.nva.cristin.mapper.CristinContributorRoleCode;
import no.unit.nva.cristin.mapper.CristinContributorsAffiliation;
import no.unit.nva.cristin.mapper.CristinGrant;
import no.unit.nva.cristin.mapper.CristinHrcsCategoriesAndActivities;
import no.unit.nva.cristin.mapper.CristinJournalPublication;
import no.unit.nva.cristin.mapper.CristinJournalPublicationJournal;
import no.unit.nva.cristin.mapper.CristinLectureOrPosterMetaData;
import no.unit.nva.cristin.mapper.CristinLocale;
import no.unit.nva.cristin.mapper.CristinMainCategory;
import no.unit.nva.cristin.mapper.CristinMediaContribution;
import no.unit.nva.cristin.mapper.CristinMediumType;
import no.unit.nva.cristin.mapper.CristinMediumTypeCode;
import no.unit.nva.cristin.mapper.CristinObject;
import no.unit.nva.cristin.mapper.CristinPresentationalWork;
import no.unit.nva.cristin.mapper.CristinPublisher;
import no.unit.nva.cristin.mapper.CristinSecondaryCategory;
import no.unit.nva.cristin.mapper.CristinSource;
import no.unit.nva.cristin.mapper.CristinSubjectField;
import no.unit.nva.cristin.mapper.CristinTags;
import no.unit.nva.cristin.mapper.CristinTitle;
import no.unit.nva.cristin.mapper.PresentationEvent;
import no.unit.nva.cristin.mapper.ScientificPerson;
import no.unit.nva.cristin.mapper.ScientificResource;
import no.unit.nva.cristin.mapper.VerificationStatus;
import no.unit.nva.cristin.mapper.artisticproduction.ArtisticGenre;
import no.unit.nva.cristin.mapper.artisticproduction.ArtisticProductionTimeUnit;
import no.unit.nva.cristin.mapper.artisticproduction.CristinArtisticProduction;
import no.unit.nva.cristin.mapper.artisticproduction.CristinProduct;
import no.unit.nva.cristin.mapper.exhibition.CristinExhibition;
import no.unit.nva.cristin.mapper.exhibition.ExhibitionEvent;
import no.unit.nva.cristin.mapper.exhibition.MuseumEventCategory;

public final class CristinDataGenerator {

    public static final int SMALL_NUMBER = 10;
    public static final Random RANDOM = new Random(System.currentTimeMillis());
    public static final BaseFaker FAKER = new BaseFaker();
    public static final int FIRST_TITLE = 0;
    public static final int USE_WHOLE_ARRAY = -1;
    public static final int NUMBER_OF_KNOWN_SECONDARY_CATEGORIES = 1;
    public static final String ID_FIELD = "id";
    public static final String YEAR_REPORTED = "yearReported";
    public static final String CRISTIN_SOURCES = "cristinSources";
    public static final String SOURCE_RECORD_IDENTIFIER = "sourceRecordIdentifier";
    public static final String SOURCE_CODE = "sourceCode";
    public static final String CRISTIN_GRANTS = "cristinGrants";
    public static final String SUB_DEPARTMEND_IDENTIFIER_CREATED = "subDepartmendIdentifierCreated";
    public static final String OWNER_CODE_CREATED = "ownerCodeCreated";
    public static final String INSTITUTION_IDENTIFIER_CREATED = "institutionIdentifierCreated";
    public static final String GROUP_IDENTIFIER_CREATED = "groupIdentifierCreated";
    public static final String DEPARTMENT_IDENTIFIER_CREATED = "departmentIdentifierCreated";
    public static final String CRISTIN_LOCALES = "cristinLocales";
    public static final String CRISTIN_ASSOCIATED_URIS = "cristinAssociatedUris";
    private static final String ENTRY_PUBLISHED_DATE = "entryPublishedDate";
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
    private static final String MEDIA_CONTRIBUTION = "mediaContribution";
    private static final CristinSecondaryCategory[] BOOK_SECONDARY_CATEGORIES = new CristinSecondaryCategory[]{
        MONOGRAPH, TEXTBOOK, NON_FICTION_BOOK, ENCYCLOPEDIA, POPULAR_BOOK, REFERENCE_MATERIAL};
    private static final Integer VALID_PUBLISHER_NSD_NUMBER = 5269;
    public static final String J = "J";
    public static final int VALID_SERIES_NSD_CODE = 339741;
    public static final String SCIENTIFIC_RESOURCES = "scientificResources";

    private CristinDataGenerator() {

    }

    public static CristinContributorsAffiliation randomAffiliation() {
        return creatCristinContributorsAffiliation(randomCristinContributorRoleCode());
    }

    public static CristinContributor randomContributor(Integer contributorIndex) {
        return CristinContributor.builder()
                   .withContributorOrder(contributorIndex + 1)
                   .withIdentifier(contributorIndex)
                   .withGivenName(randomString())
                   .withFamilyName(randomString())
                   .withVerificationStatus(VerificationStatus.VERIFIED)
                   .withAffiliations(randomAffiliations())
                   .build();
    }

    public static CristinContributor randomUnverifiedContributor(Integer contributorIndex) {
        return CristinContributor.builder()
                   .withContributorOrder(contributorIndex)
                   .withIdentifier(contributorIndex)
                   .withGivenName(randomString())
                   .withFamilyName(randomString())
                   .withVerificationStatus(VerificationStatus.NOT_VERIFIED)
                   .withAffiliations(randomAffiliations())
                   .build();
    }

    public static CristinPresentationalWork randomPresentationalWork() {
        return CristinPresentationalWork.builder()
                   .withPresentationType("PROSJEKT")
                   .withIdentifier(smallRandomNumber())
                   .build();
    }

    public static CristinPresentationalWork randomPresentationalWork(String presentationType) {
        return CristinPresentationalWork.builder()
                   .withPresentationType(presentationType)
                   .withIdentifier(smallRandomNumber())
                   .build();
    }

    public static List<CristinContributorsAffiliation> randomAffiliations() {
        return smallSample().map(ignored -> randomAffiliation()).collect(Collectors.toList());
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
        return switch (category) {
            case MONOGRAPH,
                 TEXTBOOK,
                 NON_FICTION_BOOK,
                 ENCYCLOPEDIA,
                 POPULAR_BOOK,
                 REFERENCE_MATERIAL,
                 ACADEMIC_COMMENTARY -> randomBook(category);
            case ANTHOLOGY -> randomBookAnthology();
            case FEATURE_ARTICLE -> randomFeatureArticle();
            case JOURNAL_LETTER,
                 READER_OPINION -> randomJournalLetter(category);
            case JOURNAL_REVIEW -> randomJournalReview();
            case JOURNAL_LEADER -> randomJournalLeader();
            case JOURNAL_CORRIGENDUM -> randomJournalCorrigendum();
            case JOURNAL_ARTICLE,
                 ARTICLE,
                 POPULAR_ARTICLE,
                 ACADEMIC_REVIEW,
                 SHORT_COMMUNICATION,
                 ABSTRACT -> randomJournalArticle(category);
            case RESEARCH_REPORT -> randomResearchReport();
            case DEGREE_PHD, MAGISTER_THESIS -> randomDegreePhd(category);
            case DEGREE_LICENTIATE -> randomDegreeLicentiate();
            case DEGREE_MASTER,
                 SECOND_DEGREE_THESIS,
                 MEDICAL_THESIS -> randomDegreeMaster(category);
            case CHAPTER_ACADEMIC,
                 CHAPTER,
                 POPULAR_CHAPTER_ARTICLE,
                 FOREWORD,
                 INTRODUCTION,
                 LEXICAL_IMPORT -> randomChapterArticle(category);
            case EXHIBITION_CATALOG -> randomExhibitCatalogue();
            case CONFERENCE_LECTURE,
                 CONFERENCE_POSTER,
                 POPULAR_SCIENTIFIC_LECTURE,
                 LECTURE,
                 OTHER_PRESENTATION,
                 INTERNET_EXHIBIT -> randomEvent(category);
            case INTERVIEW, PROGRAM_MANAGEMENT -> randomMedia(category);
            case PROGRAM_PARTICIPATION -> randomTvOrMedia(category);
            case WRITTEN_INTERVIEW -> randomWrittenInterview();
            case MUSICAL_PERFORMANCE,
                 FILM_PRODUCTION,
                 THEATRICAL_PRODUCTION,
                 MUSICAL_PIECE -> randomArtisticProduction(category);
            case VISUAL_ARTS,
                 ARCHITECT_DESIGN -> randomCristinProductWithSecondaryCategory(category);
            case MUSEUM -> randomCristinMuseum();
            default -> throw new IllegalStateException(String.format("The secondary category %s is not covered",
                                                                     secondaryCategory));
        };
    }

    public static CristinObject randomObjectWithReportedYear(int year) {
        var cristinObject = newCristinObject(largeRandomNumber());
        cristinObject.setYearReported(year);
        cristinObject.setCristinLocales(randomCristinLocales());
        cristinObject.setScientificResources(List.of(randomScientificResource()));
        return cristinObject;
    }

    private static ScientificResource randomScientificResource() {
        return ScientificResource.builder()
                   .withQualityCode(randomString())
                   .withScientificPeople(List.of(randomScientificPerson()))
                   .withReportedYear(randomString())
                   .build();
    }

    private static ScientificPerson randomScientificPerson() {
        return ScientificPerson.builder()
                   .withCristinPersonIdentifier(randomString())
                   .withDepartmentIdentifier(randomString())
                   .withGroupIdentifier(randomString())
                   .withInstitutionIdentifier(randomString())
                   .withSubDepartmentIdentifier(randomString())
                   .build();
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

    public static JsonNode objectWithCustomMainCategory(String customMainCategory) throws JsonProcessingException {
        return cristinObjectWithUnexpectedValue(randomObject(), customMainCategory, MAIN_CATEGORY_FIELD);
    }

    public static JsonNode objectWithCustomSecondaryCategory(String customSecondaryCategory)
        throws JsonProcessingException {
        return cristinObjectWithUnexpectedValue(randomObject(), customSecondaryCategory, SECONDARY_CATEGORY_FIELD);
    }

    public static JsonNode objectWithoutId() throws JsonProcessingException {
        ObjectNode cristinObject = cristinObjectAsObjectNode(randomObject());
        cristinObject.remove(ID_FIELD);
        return cristinObject;
    }

    public static CristinObject objectWithTags() {
        var cristingTagsList = randomTagList();
        var cristinObject = randomObject();
        cristinObject.setTags(cristingTagsList);
        return cristinObject;
    }

    private static List<CristinTags> randomTagList() {
        var tag = randomString();
        return List.of(CristinTags.builder()
                           .withBokmal(tag)
                           .withEnglish(tag)
                           .withNynorsk(tag)
                           .build());
    }

    public static JsonNode objectWithCristinHrcsCategoriesAndActivities() throws JsonProcessingException {
        var cristinhrcsCategoriesAndActivities = CristinHrcsCategoriesAndActivities.builder()
                                                     .withCategory("2")
                                                     .withActivity("1.1")
                                                     .build();
        var cristinObject = randomObject();
        cristinObject.setHrcsCategoriesAndActivities(List.of(cristinhrcsCategoriesAndActivities));
        return cristinObjectAsObjectNode(cristinObject);
    }

    public static JsonNode bookObjectWithInvalidIssn() throws JsonProcessingException {
        CristinObject cristinObject = randomBook();
        cristinObject.getBookOrReportMetadata().getBookSeries().setIssn("123-123-123-132-123");
        cristinObject.getBookOrReportMetadata().getBookSeries().setNsdCode(null);
        return cristinObjectAsObjectNode(cristinObject);
    }

    public static JsonNode journalObjectWithInvalidIssn() throws JsonProcessingException {
        CristinObject cristinObject = randomJournalArticle(JOURNAL_ARTICLE);
        cristinObject.getJournalPublication().getJournal().setIssn("123-123-123-132-123");
        cristinObject.getJournalPublication().getJournal().setIssnOnline("123-123-123-132-123");
        cristinObject.getJournalPublication().getJournal().setNsdCode(null);
        return cristinObjectAsObjectNode(cristinObject);
    }

    public static JsonNode objectWithoutContributors() throws JsonProcessingException {
        CristinObject cristinObject = randomBook();
        cristinObject.setContributors(null);
        return eventHandlerObjectMapper.readTree(cristinObject.toJsonString());
    }

    public static JsonNode objectWithContributorsWithoutAffiliation() throws JsonProcessingException {
        CristinObject cristinObject = randomBook();
        cristinObject.getContributors().get(0).setAffiliations(null);
        return eventHandlerObjectMapper.readTree(cristinObject.toJsonString());
    }

    public static JsonNode objectWithAffiliationWithoutRole() throws JsonProcessingException {
        CristinObject cristinObject = randomBook();
        cristinObject.getContributors()
            .stream()
            .flatMap(contributor -> contributor.getAffiliations().stream())
            .forEach(affiliation -> affiliation.setRoles(null));
        return eventHandlerObjectMapper.readTree(cristinObject.toJsonString());
    }

    public static CristinObject newCristinObjectWithRoleCode(CristinContributorRoleCode roleCode) {
        return createObjectWithCristinContributorRoleCode(0, createContributors(roleCode));
    }

    public static String randomIsbn13() {
        return FAKER.code().isbn13();
    }

    public static String randomIssn() {
        String issnMissingChecksum = random7DigitNumber();
        return createValidIssn(issnMissingChecksum);
    }

    public static CristinBookOrReportMetadata randomBookOrReportMetadata() {
        return CristinBookOrReportMetadata.builder()
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

    public static JsonNode objectWithUnknownProperty(String propertyName) {
        var object = randomObject();
        var json = JsonUtils.dtoObjectMapper.convertValue(object, ObjectNode.class);
        json.put(propertyName, randomString());
        return json;
    }

    private static CristinObject randomCristinMuseum() {
        return CristinObject.builder()
                   .withId(largeRandomNumber())
                   .withMainCategory(CristinMainCategory.EXHIBITION)
                   .withSecondaryCategory(MUSEUM)
                   .withCristinTitles(List.of(randomCristinTitle(FIRST_TITLE)))
                   .withEntryCreationDate(LocalDate.now())
                   .withPublicationYear(randomYear())
                   .withPublicationOwner(randomString())
                   .withContributors(randomContributors())
                   .withCristinExhibition(randomCristinExhibition())
                   .build();
    }

    private static CristinExhibition randomCristinExhibition() {
        return CristinExhibition.builder()
                   .withExhibitionEvent(randomExhibitionEvent())
                   .build();
    }

    private static ExhibitionEvent randomExhibitionEvent() {
        return ExhibitionEvent.builder()
                   .withMuseumEventCategory(randomMuseumCategory())
                   .withDateFrom(randomInstant().toString())
                   .build();
    }

    private static MuseumEventCategory randomMuseumCategory() {
        return MuseumEventCategory.builder()
                   .withEventCode(randomString())
                   .build();
    }

    private static CristinObject randomWrittenInterview() {
        return createRandomJournalWithSpecifiedSecondaryCategory(WRITTEN_INTERVIEW);
    }

    private static CristinObject randomTvOrMedia(CristinSecondaryCategory category) {
        var cristinObject = createRandomMediaWithSpecifiedSecondaryCategory(category);
        cristinObject.getMediaContribution()
            .setCristinMediumType(CristinMediumType.builder().withMediumTypeCode(CristinMediumTypeCode.TV).build());
        return cristinObject;
    }

    private static CristinObject randomMedia(CristinSecondaryCategory secondaryCategory) {
        return createRandomMediaWithSpecifiedSecondaryCategory(secondaryCategory);
    }

    public static CristinObject createRandomMediaWithSpecifiedSecondaryCategory(
        CristinSecondaryCategory secondaryCategory) {
        return CristinObject.builder()
                   .withYearReported(2001)
                   .withCristinTitles(List.of(randomCristinTitle(FIRST_TITLE)))
                   .withEntryCreationDate(LocalDate.now())
                   .withMainCategory(CristinMainCategory.MEDIA_CONTRIBUTION)
                   .withSecondaryCategory(secondaryCategory)
                   .withId(largeRandomNumber())
                   .withPublicationYear(randomYear())
                   .withPublicationOwner(randomString())
                   .withContributors(randomContributors())
                   .withMediaContribution(randomMediaContribution())
                   .withTags(randomTagList())
                   .withCristinAssociatedUris(List.of(new CristinAssociatedUri("DATA", randomUri().toString())))
                   .build();
    }

    public static CristinObject createObjectWithCategory(CristinMainCategory mainCategory,
        CristinSecondaryCategory secondaryCategory) {
        return CristinObject.builder()
                   .withYearReported(2001)
                   .withCristinTitles(List.of(randomCristinTitle(FIRST_TITLE)))
                   .withEntryCreationDate(LocalDate.now())
                   .withMainCategory(mainCategory)
                   .withSecondaryCategory(secondaryCategory)
                   .withId(largeRandomNumber())
                   .withPublicationYear(randomYear())
                   .withPublicationOwner(randomString())
                   .withContributors(randomContributors())
                   .withMediaContribution(randomMediaContribution())
                   .withTags(randomTagList())
                   .withCristinAssociatedUris(List.of(new CristinAssociatedUri("DATA", randomUri().toString())))
                   .withLectureOrPosterMetaData(randomLectureOrPosterMetaData())
                   .withBookOrReportPartMetadata(randomBookOrReportPartMetadata())
                   .withBookOrReportMetadata(randomBookOrReportMetadata())
                   .withJournalPublication(randomJournalPublication())
                   .build();
    }

    private static CristinBookOrReportPartMetadata randomBookOrReportPartMetadata() {
        return CristinBookOrReportPartMetadata.builder()
                   .withSubjectField(CristinSubjectField.builder().withSubjectFieldCode(randomInteger()).build())
                   .withPagesStart("1")
                   .withPagesEnd("10")
                   .withPartOf(randomString())
                   .build();
    }

    private static CristinMediaContribution randomMediaContribution() {
        return CristinMediaContribution.builder()
                   .withCristinMediumType(randomCristinMediumType())
                   .withMediaPlaceName(randomBoolean() ? randomString() : null)
                   .build();
    }

    private static CristinMediumType randomCristinMediumType() {
        return CristinMediumType.builder()
                   .withMediumTypeCode(CristinMediumTypeCode.TV)
                   .withMediumTypeNameNorwegianBokmaal(randomString())
                   .build();
    }

    private static CristinObject randomExhibitCatalogue() {
        return createRandomBookWithSpecifiedSecondaryCategory(CristinSecondaryCategory.EXHIBITION_CATALOG);
    }

    private static CristinObject randomDegreeLicentiate() {
        return createRandomReportWithSpecifiedSecondaryCategory(CristinSecondaryCategory.DEGREE_LICENTIATE);
    }

    private static CristinObject randomEvent(CristinSecondaryCategory secondaryCategory) {
        return createRandomEventWithSpecifiedSecondaryCategory(secondaryCategory);
    }

    private static CristinPublisher randomPublisher() {
        return CristinPublisher.builder()
                   .withPublisherName(randomString())
                   .withNsdCode(VALID_PUBLISHER_NSD_NUMBER)
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
        return issnWithChecksum.substring(0, MIDDLE_INDEX_OF_ISSN_STRING) + "-" + issnWithChecksum.substring(
            MIDDLE_INDEX_OF_ISSN_STRING);
    }

    private static String createIssnWithCheckSum(String issnMissingChecksum, int totalSum) {
        int mod11 = totalSum % 11;
        int checksum = mod11 == 0 ? 0 : 11 - mod11;
        return checksum == 10 ? issnMissingChecksum + "X" : issnMissingChecksum + checksum;
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

    private static CristinObject randomDegreePhd(CristinSecondaryCategory secondaryCategory) {
        return createRandomReportWithSpecifiedSecondaryCategory(secondaryCategory);
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
                   .withInstitutionIdentifier(threeDigitPositiveNumberKnownInstitution())
                   .withDepartmentIdentifier(threeDigitPositiveNumber())
                   .withGroupNumber(threeDigitPositiveNumber())
                   .withSubdepartmentIdentifier(threeDigitPositiveNumber())
                   .withDepartmentIdentifier(largeRandomNumber())
                   .withRoles(List.of(createRole(roleCode)))
                   .build();
    }

    private static CristinContributorRole createRole(CristinContributorRoleCode roleCode) {
        return CristinContributorRole.builder().withRoleCode(roleCode).build();
    }

    private static List<CristinContributor> randomContributors() {
        return smallSample().map(CristinDataGenerator::randomContributor).collect(Collectors.toList());
    }

    private static Stream<Integer> smallSample() {
        return IntStream.range(0, smallRandomNumber()).boxed();
    }

    private static <T> T randomArrayElement(T[] array, int subArrayEndIndex) {
        return subArrayEndIndex > 0 ? array[RANDOM.nextInt(subArrayEndIndex)] : array[RANDOM.nextInt(array.length)];
    }

    private static CristinContributorRoleCode randomCristinContributorRoleCode() {
        return randomArrayElement(CristinContributorRoleCode.values(), USE_WHOLE_ARRAY);
    }

    private static Integer threeDigitPositiveNumberKnownInstitution() {
        return RANDOM.nextInt(1, 1000);
    }

    private static int threeDigitPositiveNumber() {
        return RANDOM.nextInt(1000);
    }

    private static CristinJournalPublicationJournal randomBookSeries() {
        return CristinJournalPublicationJournal.builder()
                   .withJournalTitle(randomString())
                   .withNsdCode(VALID_SERIES_NSD_CODE)
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
        return CristinObject.builder()
                   .withCristinTitles(List.of(randomCristinTitle(FIRST_TITLE)))
                   .withEntryCreationDate(LocalDate.now())
                   .withMainCategory(CristinMainCategory.JOURNAL)
                   .withSecondaryCategory(secondaryCategory)
                   .withId(largeRandomNumber())
                   .withPublicationYear(randomYear())
                   .withPublicationOwner(randomString())
                   .withContributors(randomContributors())
                   .withJournalPublication(randomJournalPublication())
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
                   .withBookOrReportPartMetadata(randomBookOrReportPartMetadata())
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
                   .withLectureOrPosterMetaData(randomLectureOrPosterMetaData())
                   .build();
    }

    private static CristinLectureOrPosterMetaData randomLectureOrPosterMetaData() {
        return CristinLectureOrPosterMetaData.builder()
                   .withEvent(createRandomCristinEvent())
                   .build();
    }

    private static PresentationEvent createRandomCristinEvent() {
        return PresentationEvent.builder()
                   .withTitle(randomString())
                   .withAgent(randomString())
                   .withCountryCode(randomString())
                   .withPlace(randomString())
                   .withFrom("2023-11-28T00:00:00")
                   .withTo("2023-11-29T00:00:00")
                   .build();
    }

    public static CristinObject randomArtisticProduction(CristinSecondaryCategory secondaryCategory) {
        return CristinObject.builder()
                   .withId(largeRandomNumber())
                   .withMainCategory(CristinMainCategory.ARTISTIC_PRODUCTION)
                   .withSecondaryCategory(secondaryCategory)
                   .withCristinTitles(List.of(randomCristinTitle(FIRST_TITLE)))
                   .withEntryCreationDate(LocalDate.now())
                   .withPublicationYear(randomYear())
                   .withPublicationOwner(randomString())
                   .withContributors(randomContributors())
                   .withCristinArtisticProduction(randomCristinArtisticProduction())
                   .build();
    }

    private static CristinObject randomCristinProductWithSecondaryCategory(CristinSecondaryCategory category) {
        return CristinObject.builder()
                   .withId(largeRandomNumber())
                   .withMainCategory(CristinMainCategory.ARTISTIC_PRODUCTION)
                   .withSecondaryCategory(category)
                   .withCristinTitles(List.of(randomCristinTitle(FIRST_TITLE)))
                   .withEntryCreationDate(LocalDate.now())
                   .withPublicationYear(randomYear())
                   .withPublicationOwner(randomString())
                   .withContributors(randomContributors())
                   .withCristinProduct(randomCristinProduct())
                   .build();
    }

    private static CristinProduct randomCristinProduct() {
        return CristinProduct
                   .builder()
                   .withPublisherName(randomString())
                   .build();
    }

    private static CristinArtisticProduction randomCristinArtisticProduction() {
        return CristinArtisticProduction.builder()
                   .withArtisticGenre(ArtisticGenre.builder().withGenreCode(randomString()).build())
                   .withArtisticProductionTimeUnit(
                       ArtisticProductionTimeUnit.builder().withTimeUnitCode("MINUTT").build())
                   .withDuration("6")
                   .withOriginalComposer(randomString())
                   .withCoCreators(randomString())
                   .withCrew(randomString())
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

    private static CristinObject createObjectWithCristinContributorRoleCode(Integer cristinId,
                                                                            List<CristinContributor> contributors) {
        return CristinObject.builder()
                   .withMainCategory(randomMainCategory())
                   .withSecondaryCategory(randomSecondaryCategory())
                   .withCristinTitles(randomTitles())
                   .withId(cristinId)
                   .withCristinSources(randomCristinSources())
                   .withEntryCreationDate(LocalDate.now())
                   .withPublicationYear(randomYear())
                   .withYearReported(randomYear())
                   .withContributors(contributors)
                   .withBookOrReportMetadata(randomBookOrReportMetadata())
                   .withPublicationOwner(HardcodedValues.SIKT_OWNER)
                   .withPresentationalWork(List.of(randomPresentationalWork()))
                   .build();
    }

    private static CristinGrant randomCristinGrant() {
        return CristinGrant.builder()
                   .withSourceCode(randomString())
                   .withIdentifier(randomString())
                   .withYearFrom(2000)
                   .withYearTo(2001)
                   .build();
    }

    private static List<CristinLocale> randomCristinLocales() {
        return IntStream.range(0, 5).boxed().map(i -> randomLocale()).toList();
    }

    private static CristinLocale randomLocale() {
        return CristinLocale.builder()
                   .withInstitutionIdentifier(randomString())
                   .withDepartmentIdentifier(randomString())
                   .withOwnerCode(randomString())
                   .withSubDepartmentIdentifier(randomString())
                   .withGroupIdentifier(randomString())
                   .withControlledBy(randomString())
                   .withDateControlled(LocalDate.now())
                   .withControlStatus(J)
                   .build();
    }

    private static List<CristinSource> randomCristinSources() {
        return smallSample().map(ignored -> randomCristinSource()).collect(Collectors.toList());
    }

    private static CristinSource randomCristinSource() {
        return CristinSource.builder().withSourceCode(randomString()).withSourceIdentifier(randomString()).build();
    }

    private static CristinSubjectField randomSubjectField() {
        return CristinSubjectField.builder().withSubjectFieldCode(smallRandomNumber()).build();
    }

    private static CristinJournalPublication randomJournalPublication() {
        int pagesBegin = smallRandomNumber();
        return CristinJournalPublication.builder()
                   .withJournal(randomCristinJournalPublicationJournal())
                   .withPagesBegin(String.valueOf(pagesBegin))
                   .withPagesEnd(String.valueOf(pagesBegin + smallRandomNumber()))
                   .withVolume(String.valueOf(smallRandomNumber()))
                   .withDoi(randomDoiString())
                   .withArticleNumber(randomString())
                   .build();
    }

    private static CristinJournalPublicationJournal randomCristinJournalPublicationJournal() {
        return CristinJournalPublicationJournal.builder()
                   .withIssn(randomIssn())
                   .withIssnOnline(randomIssn())
                   .withJournalTitle(randomString())
                   .build();
    }

    private static ObjectNode cristinObjectAsObjectNode(CristinObject cristinObject) throws JsonProcessingException {
        assertThat(cristinObject, doesNotHaveEmptyValuesIgnoringFields(
            Set.of(PUBLICATION_OWNER_FIELD, ENTRY_PUBLISHED_DATE, JOURNAL_PUBLICATION_FIELD, CRISTIN_TAGS,
                   SOURCE_RECORD_IDENTIFIER, SUB_DEPARTMEND_IDENTIFIER_CREATED, OWNER_CODE_CREATED,
                   INSTITUTION_IDENTIFIER_CREATED, GROUP_IDENTIFIER_CREATED, DEPARTMENT_IDENTIFIER_CREATED,
                   CRISTIN_LOCALES, CRISTIN_ASSOCIATED_URIS, "cristinProduct", "note", "cristinArtisticProduction",
                   "cristinExhibition",
                   SOURCE_CODE, CRISTIN_PRESENTATIONAL_WORK, CRISTIN_SUBJECT_FIELD, BOOK_OR_REPORT_METADATA_FIELD,
                   BOOK_OR_REPORT_PART_METADATA, HRCS_CATEGORIES_AND_ACTIVITIES, CRISTIN_MODIFIED_DATE,
                   LECTURE_OR_POSTER_METADATA, YEAR_REPORTED, CRISTIN_GRANTS, CRISTIN_SOURCES, MEDIA_CONTRIBUTION,
                   SCIENTIFIC_RESOURCES)));

        return (ObjectNode) eventHandlerObjectMapper.readTree(cristinObject.toJsonString());
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
        return smallSample().map(contributorIndex -> creatContributor(contributorIndex, roleCode))
                   .collect(Collectors.toList());
    }

    private static String randomLanguageCode() {
        return randomElement(LANGUAGE_CODES);
    }

    private static List<CristinTitle> randomTitles() {
        return smallSample().map(CristinDataGenerator::randomCristinTitle).collect(Collectors.toList());
    }

    private static CristinContributor creatContributor(Integer contributorIndex, CristinContributorRoleCode roleCode) {
        return CristinContributor.builder()
                   .withContributorOrder(contributorIndex)
                   .withVerificationStatus(VerificationStatus.VERIFIED)
                   .withIdentifier(contributorIndex)
                   .withGivenName(randomString())
                   .withFamilyName(randomString())
                   .withAffiliations(List.of(createAffiliation(roleCode)))
                   .build();
    }

    private static String randomDoiString() {
        return randomDoi().toString().replace("https://doi.org/", "");
    }
}
