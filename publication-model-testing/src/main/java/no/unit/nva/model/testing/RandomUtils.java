package no.unit.nva.model.testing;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import java.util.Map;
import no.unit.nva.model.PublicationDate;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public class RandomUtils {

    public static Map<String, String> randomLabels() {
        return Map.of(RandomLanguageUtil.randomBcp47CompatibleLanguage(), randomString());
    }

    public static String randomLabel() {
        return randomString();
    }


    public static PublicationDate randomPublicationDate() {
        return new PublicationDate.Builder()
            .withDay(randomString())
            .withMonth(randomString())
            .withYear(randomString())
            .build();
    }
}
