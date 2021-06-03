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
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import no.unit.nva.cristin.lambda.CristinEntryEventConsumer;
import no.unit.nva.cristin.lambda.constants.HardcodedValues;
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

public class CristinDataGenerator {

    public static final int SMALL_NUMBER = 10;
    public static final Random RANDOM = new Random(System.currentTimeMillis());
    public static final Faker FAKER = Faker.instance();
    public static final int FIRST_TITLE = 0;
    public static final ObjectMapper OBJECT_MAPPER = JsonUtils.objectMapperNoEmpty.configure(
        SerializationFeature.INDENT_OUTPUT, false);
    public static final int USE_WHOLE_ARRAY = -1;
    public static final String SOME_OWNER = "some@owner";
    public static final int NUMBER_OF_KNOWN_SECONDARY_CATEGORIES = 1;
    private static final List<String> LANGUAGE_CODES = List.of("nb", "no", "en");
    private static final int NUMBER_OF_KNOWN_MAIN_CATEGORIES = 1;
    public static final String ID_FIELD = "id";

    public Stream<CristinObject> randomObjects() {
        return IntStream.range(0, 100).boxed()
                   .map(this::newCristinObject);
    }

    public CristinObject randomObject() {
        return newCristinObject(largeRandomNumber());
    }

    public String randomDataAsString() {
        return randomObjects()
                   .map(this::toJsonString)
                   .collect(Collectors.joining(System.lineSeparator()));
    }

    public static String randomString() {
        return FAKER.lorem().sentence(smallRandomNumber());
    }

    public <T> AwsEventBridgeEvent<FileContentsEvent<JsonNode>> toAwsEvent(T inputData) {
        AwsEventBridgeEvent<FileContentsEvent<JsonNode>> event = new AwsEventBridgeEvent<>();
        JsonNode cristinData = convertToJsonNode(inputData);
        FileContentsEvent<JsonNode> eventDetail = new FileContentsEvent<>(randomUri(), cristinData);
        event.setDetailType(CristinEntryEventConsumer.EVENT_DETAIL_TYPE);
        event.setDetail(eventDetail);
        return event;
    }

    public CristinObject randomBookAnthology() {
        CristinObject cristinObject = CristinObject
                                          .builder()
                                          .withCristinTitles(List.of(randomCristinTitle(FIRST_TITLE)))
                                          .withEntryCreationDate(LocalDate.now())
                                          .withMainCategory(CristinMainCategory.BOOK)
                                          .withSecondaryCategory(CristinSecondaryCategory.ANTHOLOGY)
                                          .withId(largeRandomNumber())
                                          .withPublicationYear(randomYear())
                                          .withPublicationOwner(randomString())
                                          .withContributors(randomContributors())
                                          .build();
        assertThat(cristinObject, doesNotHaveEmptyValues());
        return cristinObject;
    }

    public String singleRandomObjectAsString() {
        return newCristinObject(0).toJsonString();
    }

    public JsonNode objectWithCustomMainCategory(String customMainCategory) throws JsonProcessingException {
        return cristinObjectWithUnexpectedValue(customMainCategory, MAIN_CATEGORY_FIELD);
    }

    public JsonNode objectWithCustomSecondaryCategory(String customSecondaryCategory) throws JsonProcessingException {
        return cristinObjectWithUnexpectedValue(customSecondaryCategory, SECONDARY_CATEGORY_FIELD);
    }

    public JsonNode objectWithoutId() throws JsonProcessingException {
        ObjectNode cristinObject = cristinObjectAsObjectNode();
        cristinObject.remove(ID_FIELD);
        return cristinObject;
    }

    private static <T> T randomElement(List<T> elements) {
        return elements.get(RANDOM.nextInt(elements.size()));
    }

    private static int smallRandomNumber() {
        return 1 + RANDOM.nextInt(SMALL_NUMBER);
    }

    private String toJsonString(CristinObject c) {
        return attempt(() -> OBJECT_MAPPER.writeValueAsString(c)).orElseThrow();
    }

    private CristinObject newCristinObject(Integer index) {
        return CristinObject.builder()
                   .withMainCategory(randomMainCategory())
                   .withSecondaryCategory(randomSecondaryCategory())
                   .withCristinTitles(randomTitles())
                   .withId(index)
                   .withEntryCreationDate(LocalDate.now())
                   .withPublicationYear(randomYear())
                   .withContributors(randomContributors())
                   .withPublicationOwner(HardcodedValues.HARDCODED_PUBLICATIONS_OWNER)
                   .build();
    }

    private JsonNode cristinObjectWithUnexpectedValue(String customSecondaryCategory, String secondaryCategoryField)
        throws JsonProcessingException {
        ObjectNode json = cristinObjectAsObjectNode();
        json.put(secondaryCategoryField, customSecondaryCategory);
        return json;
    }

    private ObjectNode cristinObjectAsObjectNode() throws JsonProcessingException {
        CristinObject cristinObject = newCristinObject(0);
        assertThat(cristinObject, doesNotHaveEmptyValuesIgnoringFields(Set.of(PUBLICATION_OWNER_FIELD)));
        return (ObjectNode) JsonUtils.objectMapperNoEmpty.readTree(cristinObject.toJsonString());
    }

    private <T> JsonNode convertToJsonNode(T inputData) {
        return inputData instanceof JsonNode
                   ? (JsonNode) inputData
                   : JsonUtils.objectMapperNoEmpty.convertValue(inputData, JsonNode.class);
    }

    private List<CristinContributor> randomContributors() {
        return smallSample()
                   .map(this::randomContributor)
                   .collect(Collectors.toList());
    }

    private CristinContributor randomContributor(Integer contributorIndex) {
        return CristinContributor.builder()
                   .withContributorOrder(contributorIndex)
                   .withIdentifier(contributorIndex)
                   .withGivenName(randomString())
                   .withFamilyName(randomString())
                   .withAffiliations(randomAffiliations())
                   .build();
    }

    private List<CristinContributorsAffiliation> randomAffiliations() {
        return smallSample()
                   .map(ignored -> randomAffiliation())
                   .collect(Collectors.toList());
    }

    public static CristinContributorsAffiliation randomAffiliation() {
        return CristinContributorsAffiliation.builder()
                   .withInstitutionIdentifier(threeDigitPositiveNumber())
                   .withDepartmentIdentifier(threeDigitPositiveNumber())
                   .withGroupNumber(threeDigitPositiveNumber())
                   .withSubdepartmentIdentifier(threeDigitPositiveNumber())
                   .withOriginalInsitutionCode(randomString())
                   .withOriginalInstitutionName(randomString())
                   .withOriginalPlaceName(randomString())
                   .withOriginalDepartmentName(randomString())
                   .withDepartmentIdentifier(largeRandomNumber())
                   .withRoles(randomAffiliationRoles())
                   .build();
    }

    public static int largeRandomNumber() {
        return 1 + RANDOM.nextInt(Integer.MAX_VALUE);
    }

    private CristinSecondaryCategory randomSecondaryCategory() {
        return randomArrayElement(CristinSecondaryCategory.values(), NUMBER_OF_KNOWN_SECONDARY_CATEGORIES);
    }

    private String randomYear() {
        Date date = FAKER.date().birthday();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int year = calendar.get(Calendar.YEAR);
        return Integer.toString(year);
    }

    private List<CristinTitle> randomTitles() {
        return smallSample()
                   .map(this::randomCristinTitle)
                   .collect(Collectors.toList());
    }

    private CristinTitle randomCristinTitle(int index) {
        CristinTitle title = new CristinTitle();
        title.setTitle(randomString());
        title.setLanguagecode(randomLanguageCode());
        title.setAbstractText(randomString());
        if (index == FIRST_TITLE) {
            title.setStatusOriginal(CristinTitle.ORIGINAL_TITLE);
        } else {
            title.setStatusOriginal(CristinTitle.NOT_ORIGINAL_TITLE);
        }
        return title;
    }

    private CristinMainCategory randomMainCategory() {
        return randomArrayElement(CristinMainCategory.values(), NUMBER_OF_KNOWN_MAIN_CATEGORIES);
    }

    private Stream<Integer> smallSample() {
        return IntStream.range(0, smallRandomNumber()).boxed();
    }

    private String randomLanguageCode() {
        return randomElement(LANGUAGE_CODES);
    }

    private static List<CristinContributorRole> randomAffiliationRoles() {
        CristinContributorRole role = CristinContributorRole
                                          .builder()
                                          .withRoleCode(randomCristinContributorRoleCode())
                                          .build();
        return Collections.singletonList(role);
    }

    private static CristinContributorRoleCode randomCristinContributorRoleCode() {
        return randomArrayElement(CristinContributorRoleCode.values(), USE_WHOLE_ARRAY);
    }

    private static <T> T randomArrayElement(T[] array, int subArrayEndIndex) {
        return subArrayEndIndex > 0
                   ? array[RANDOM.nextInt(subArrayEndIndex)]
                   : array[RANDOM.nextInt(array.length)];
    }

    private static int threeDigitPositiveNumber() {
        return RANDOM.nextInt(1000);
    }

    private URI randomUri() {
        String prefix = "https://www.example.org/";
        String suffix = FAKER.lorem().word() + FAKER.lorem().word();
        return URI.create(prefix + suffix);
    }
}
