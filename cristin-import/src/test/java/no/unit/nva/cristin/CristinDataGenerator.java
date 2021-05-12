package no.unit.nva.cristin;

import static no.unit.nva.cristin.mapper.CristinObject.PUBLICATION_OWNER_FIELD;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFields;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.javafaker.Faker;
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
import no.unit.nva.cristin.mapper.CristinContributor;
import no.unit.nva.cristin.mapper.CristinContributorRole;
import no.unit.nva.cristin.mapper.CristinContributorRoleCode;
import no.unit.nva.cristin.mapper.CristinContributorsAffiliation;
import no.unit.nva.cristin.mapper.CristinMainCategory;
import no.unit.nva.cristin.mapper.CristinObject;
import no.unit.nva.cristin.mapper.CristinSecondaryCategory;
import no.unit.nva.cristin.mapper.CristinTitle;
import nva.commons.core.JsonUtils;

public class CristinDataGenerator {

    public static final int SMALL_NUMBER = 10;
    public static final Random RANDOM = new Random(System.currentTimeMillis());
    public static final Faker FAKER = Faker.instance();
    public static final int FIRST_TITLE = 0;
    public static final ObjectMapper OBJECT_MAPPER = JsonUtils.objectMapperNoEmpty.configure(
        SerializationFeature.INDENT_OUTPUT, false);
    private static final List<String> LANGUAGE_CODES = List.of("nb", "no", "en");

    public Stream<CristinObject> randomObjects() {
        return IntStream.range(0, 100).boxed()
                   .map(this::newCristinObject);
    }

    public String randomDataAsString() {
        return randomObjects()
                   .map(this::toJsonString)
                   .collect(Collectors.joining(System.lineSeparator()));
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

    private static <T> T randomElement(List<T> elements) {
        return elements.get(RANDOM.nextInt(elements.size()));
    }

    private String toJsonString(CristinObject c) {
        return attempt(() -> OBJECT_MAPPER.writeValueAsString(c)).orElseThrow();
    }

    private CristinObject newCristinObject(Integer index) {
        CristinObject document = CristinObject.builder()
                                     .withMainCategory(randomMainCategory())
                                     .withSecondaryCategory(randomSecondaryCategory())
                                     .withCristinTitles(randomTitles())
                                     .withId(index)
                                     .withEntryCreationDate(LocalDate.now())
                                     .withPublicationYear(randomYear())
                                     .withContributors(randomContributors())
                                     .build();

        assertThat(document, doesNotHaveEmptyValuesIgnoringFields(Set.of(PUBLICATION_OWNER_FIELD)));

        return document;
    }

    private CristinSecondaryCategory randomSecondaryCategory() {
        return randomArrayElement(CristinSecondaryCategory.values());
    }

    private CristinMainCategory randomMainCategory() {
        return randomArrayElement(CristinMainCategory.values());
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

    private CristinContributorsAffiliation randomAffiliation() {
        return CristinContributorsAffiliation.builder()
                   .withInstitutionIdentifier(largeRandomNumber())
                   .withDepartmentIdentifier(largeRandomNumber())
                   .withGroupNumber(largeRandomNumber())
                   .withSubdepartmentIdentifier(largeRandomNumber())
                   .withOriginalInsitutionCode(randomString())
                   .withOriginalInstitutionName(randomString())
                   .withOriginalPlaceName(randomString())
                   .withOringalDepartmentName(randomString())
                   .withDepartmentIdentifier(largeRandomNumber())
                   .withRoles(randomAffiliationRoles())
                   .build();
    }

    private List<CristinContributorRole> randomAffiliationRoles() {
        CristinContributorRole role = CristinContributorRole
                                          .builder()
                                          .withRoleCode(randomCristinContributorRoleCode())
                                          .build();
        return Collections.singletonList(role);
    }

    private CristinContributorRoleCode randomCristinContributorRoleCode() {
        return randomArrayElement(CristinContributorRoleCode.values());
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

    private <T> T randomArrayElement(T[] array) {
        return array[RANDOM.nextInt(array.length)];
    }

    private Stream<Integer> smallSample() {
        return IntStream.range(0, smallRandomNumber()).boxed();
    }

    private String randomLanguageCode() {
        return randomElement(LANGUAGE_CODES);
    }

    private String randomString() {
        return FAKER.lorem().sentence(smallRandomNumber());
    }

    private int smallRandomNumber() {
        return 1 + RANDOM.nextInt(SMALL_NUMBER);
    }

    private int largeRandomNumber() {
        return 1 + RANDOM.nextInt();
    }
}
