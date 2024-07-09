package no.unit.nva;

import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.instancetypes.artistic.literaryarts.manifestation.LiteraryArtsAudioVisual;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Deprecated
public class LiteraryArtsAudioVisualMigrationTest {
    public static Stream<String> literaryArtsAudioVisualProvider() {
        return Stream.of(
                generateOld(),
                generateNew(),
                nullIsbns(),
                otherWithDescription(),
                otherWithoutDescription()
        );
    }

    private static String generateNew() {
        return "{\n"
                + "          \"type\" : \"LiteraryArtsAudioVisual\",\n"
                + "          \"subtype\" : \"RadioPlay\",\n"
                + "          \"publisher\" : {\n"
                + "            \"type\" : \"UnconfirmedPublisher\",\n"
                + "            \"name\" : \"rYgvX1EuiAFLb\"\n"
                + "          },\n"
                + "          \"publicationDate\" : {\n"
                + "            \"type\" : \"PublicationDate\",\n"
                + "            \"year\" : \"hJ9lbyvYn0VvE5oYHw\",\n"
                + "            \"month\" : \"agqDekENHtR2Gy\",\n"
                + "            \"day\" : \"rPHYfsH5VIzz\"\n"
                + "          },\n"
                + "          \"isbnList\" : [ \"9780099470434\" ],\n"
                + "          \"extent\" : 1040853105\n"
                + "        }";
    }

    private static String generateOld() {
        return "{\n"
                + "          \"type\" : \"LiteraryArtsAudioVisual\",\n"
                + "          \"subtype\" : \"RadioPlay\",\n"
                + "          \"publisher\" : {\n"
                + "            \"type\" : \"UnconfirmedPublisher\",\n"
                + "            \"name\" : \"rYgvX1EuiAFLb\"\n"
                + "          },\n"
                + "          \"publicationDate\" : {\n"
                + "            \"type\" : \"PublicationDate\",\n"
                + "            \"year\" : \"hJ9lbyvYn0VvE5oYHw\",\n"
                + "            \"month\" : \"agqDekENHtR2Gy\",\n"
                + "            \"day\" : \"rPHYfsH5VIzz\"\n"
                + "          },\n"
                + "          \"isbn\" : \"9780099470434\",\n"
                + "          \"extent\" : 1040853105\n"
                + "        }";
    }

    private static String nullIsbns() {
        return "{\n"
                + "          \"type\" : \"LiteraryArtsAudioVisual\",\n"
                + "          \"subtype\" : \"RadioPlay\",\n"
                + "          \"publisher\" : {\n"
                + "            \"type\" : \"UnconfirmedPublisher\",\n"
                + "            \"name\" : \"rYgvX1EuiAFLb\"\n"
                + "          },\n"
                + "          \"publicationDate\" : {\n"
                + "            \"type\" : \"PublicationDate\",\n"
                + "            \"year\" : \"hJ9lbyvYn0VvE5oYHw\",\n"
                + "            \"month\" : \"agqDekENHtR2Gy\",\n"
                + "            \"day\" : \"rPHYfsH5VIzz\"\n"
                + "          },\n"
                + "          \"extent\" : 1040853105\n"
                + "        }";
    }

    private static String otherWithDescription() {
        return "{\n"
                + "          \"type\" : \"LiteraryArtsAudioVisual\",\n"
                + "          \"subtype\" : {"
                + "            \"type\" : \"Other\",\n"
                + "            \"description\" : \"Some description\"\n"
                + "          },\n"
                + "          \"publisher\" : {\n"
                + "            \"type\" : \"UnconfirmedPublisher\",\n"
                + "            \"name\" : \"rYgvX1EuiAFLb\"\n"
                + "          },\n"
                + "          \"publicationDate\" : {\n"
                + "            \"type\" : \"PublicationDate\",\n"
                + "            \"year\" : \"hJ9lbyvYn0VvE5oYHw\",\n"
                + "            \"month\" : \"agqDekENHtR2Gy\",\n"
                + "            \"day\" : \"rPHYfsH5VIzz\"\n"
                + "          },\n"
                + "          \"extent\" : 1040853105\n"
                + "        }";
    }

    private static String otherWithoutDescription() {
        return "{\n"
                + "          \"type\" : \"LiteraryArtsAudioVisual\",\n"
                + "          \"subtype\" : {\n"
                + "            \"type\" : \"Other\"\n"
                + "          },\n"
                + "          \"publisher\" : {\n"
                + "            \"type\" : \"UnconfirmedPublisher\",\n"
                + "            \"name\" : \"rYgvX1EuiAFLb\"\n"
                + "          },\n"
                + "          \"publicationDate\" : {\n"
                + "            \"type\" : \"PublicationDate\",\n"
                + "            \"year\" : \"hJ9lbyvYn0VvE5oYHw\",\n"
                + "            \"month\" : \"agqDekENHtR2Gy\",\n"
                + "            \"day\" : \"rPHYfsH5VIzz\"\n"
                + "          },\n"
                + "          \"extent\" : 1040853105\n"
                + "        }";
    }

    @ParameterizedTest
    @MethodSource("literaryArtsAudioVisualProvider")
    void shouldConstructObjectFromJson(String input) {
        assertDoesNotThrow(() -> JsonUtils.dtoObjectMapper.readValue(input, LiteraryArtsAudioVisual.class));
    }
}
