package no.unit.nva.publication;

import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import java.util.Map;
import no.unit.nva.model.PublicationDate;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public class RandomUtils {

    public static Map<String, String> randomLabels() {
        return Map.of(randomString(), randomString());
    }

    public static String randomLabel() {
        return randomString();
    }

    public static boolean randomBoolean() {
        return randomElement(true, false);
    }

    public static PublicationDate randomPublicationDate() {
        return new PublicationDate.Builder()
            .withDay(randomString())
            .withMonth(randomString())
            .withYear(randomString())
            .build();
    }
}
