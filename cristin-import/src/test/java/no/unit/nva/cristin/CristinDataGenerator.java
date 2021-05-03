package no.unit.nva.cristin;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.javafaker.Faker;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import no.unit.nva.cristin.mapper.CristinObject;
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
        return IntStream.range(0, 100)
                   .boxed()
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
                                          .withCristinTitles(List.of(newCristinTitle(FIRST_TITLE)))
                                          .withEntryCreationDate(LocalDate.now())
                                          .withMainCategory(MappingConstants.MAIN_CATEGORY_BOOK)
                                          .withSecondaryCategory(MappingConstants.SECONDARY_CATEGORY_ANTHOLOGY)
                                          .withId(randomString())
                                          .withPublicationYear(randomYear())
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
        CristinObject document = new CristinObject();
        document.setCristinTitles(randomTitles());
        document.setId(index.toString());
        document.setEntryCreationDate(LocalDate.now());
        document.setPublicationYear(randomYear());
        return document;
    }

    private String randomYear() {
        Date date = FAKER.date().birthday();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int year = calendar.get(Calendar.YEAR);
        return Integer.toString(year);
    }

    private List<CristinTitle> randomTitles() {
        return IntStream.range(0, smallRandomNumber())
                   .boxed()
                   .map(this::newCristinTitle)
                   .collect(Collectors.toList());
    }

    private CristinTitle newCristinTitle(int index) {
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

    private String randomLanguageCode() {
        return randomElement(LANGUAGE_CODES);
    }

    private String randomString() {
        return FAKER.lorem().sentence(smallRandomNumber());
    }

    private int smallRandomNumber() {
        return 1 + RANDOM.nextInt(SMALL_NUMBER);
    }
}
