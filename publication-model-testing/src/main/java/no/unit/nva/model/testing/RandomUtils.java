package no.unit.nva.model.testing;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;

import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.PublicationDate;

import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;

import java.net.URI;
import java.util.Map;

@JacocoGenerated
public class RandomUtils {

    private static final String PUBLICATION_ENDPOINT_NAME = "publication";

    public static Map<String, String> randomLabels() {
        return Map.of(RandomLanguageUtil.randomBcp47CompatibleLanguage(), randomString());
    }

    public static String randomLabel() {
        return randomString();
    }

    public static URI randomPublicationId() {
        var publicationId = SortableIdentifier.next().toString();
        return UriWrapper.fromUri(randomUri())
                .addChild(PUBLICATION_ENDPOINT_NAME)
                .addChild(publicationId)
                .getUri();
    }

    public static PublicationDate randomPublicationDate() {
        return new PublicationDate.Builder().withDay(randomString())
                                            .withMonth(randomString())
                                            .withYear(randomString())
                                            .build();
    }
}
