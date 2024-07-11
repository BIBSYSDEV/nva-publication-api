package no.unit.nva;

import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.instancetypes.artistic.literaryarts.manifestation.LiteraryArtsMonograph;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Deprecated
public class LiteraryArtsMonographMigrationTest {

    public static Stream<String> literaryArtsMonographProvider() {
        return Stream.of(
                generateOld(),
                generateNew(),
                regressed(),
                nullIsnbList()
        );
    }

    private static String generateNew() {
        return "{\n"
                + "          \"type\" : \"LiteraryArtsMonograph\",\n"
                + "          \"publisher\" : {\n"
                + "            \"type\" : \"UnconfirmedPublisher\",\n"
                + "            \"name\" : \"rCmadtTtK1WQtRkPEA\"\n"
                + "          },\n"
                + "          \"publicationDate\" : {\n"
                + "            \"type\" : \"PublicationDate\",\n"
                + "            \"year\" : \"tqrJ3RsfZm\",\n"
                + "            \"month\" : \"dmslbxWc3hmaUBQiv\",\n"
                + "            \"day\" : \"3syDklZHbnBwWUfV5\"\n"
                + "          },\n"
                + "          \"isbnList\" : [ \"9780099470434\" ],\n"
                + "          \"pages\" : {\n"
                + "            \"type\" : \"MonographPages\",\n"
                + "            \"introduction\" : {\n"
                + "              \"type\" : \"Range\",\n"
                + "              \"begin\" : \"wv29pYzXW46YMKR5HQ\",\n"
                + "              \"end\" : \"GMPD3cP5hFEIn4\"\n"
                + "            },\n"
                + "            \"pages\" : \"SSEXybb8sX4iARII5FQ\",\n"
                + "            \"illustrated\" : false\n"
                + "          }\n"
                + "        }";
    }

    private static String generateOld() {
        return "{\n"
                + "          \"type\" : \"LiteraryArtsMonograph\",\n"
                + "          \"publisher\" : {\n"
                + "            \"type\" : \"UnconfirmedPublisher\",\n"
                + "            \"name\" : \"rCmadtTtK1WQtRkPEA\"\n"
                + "          },\n"
                + "          \"publicationDate\" : {\n"
                + "            \"type\" : \"PublicationDate\",\n"
                + "            \"year\" : \"tqrJ3RsfZm\",\n"
                + "            \"month\" : \"dmslbxWc3hmaUBQiv\",\n"
                + "            \"day\" : \"3syDklZHbnBwWUfV5\"\n"
                + "          },\n"
                + "          \"isbn\" : \"9780099470434\",\n"
                + "          \"pages\" : {\n"
                + "            \"type\" : \"MonographPages\",\n"
                + "            \"introduction\" : {\n"
                + "              \"type\" : \"Range\",\n"
                + "              \"begin\" : \"wv29pYzXW46YMKR5HQ\",\n"
                + "              \"end\" : \"GMPD3cP5hFEIn4\"\n"
                + "            },\n"
                + "            \"pages\" : \"SSEXybb8sX4iARII5FQ\",\n"
                + "            \"illustrated\" : false\n"
                + "          }\n"
                + "        }";
    }

    private static String regressed() {
        return "{\n"
                + "       \"isbnList\": [\n"
                + "        \"9788234709159\"\n"
                + "       ],\n"
                + "       \"pages\": {\n"
                + "        \"illustrated\": false,\n"
                + "        \"type\": \"MonographPages\"\n"
                + "       },\n"
                + "       \"publicationDate\": {\n"
                + "        \"type\": \"PublicationDate\",\n"
                + "        \"year\": \"2020\"\n"
                + "       },\n"
                + "       \"publisher\": {\n"
                + "        \"name\": \"Utgiver\",\n"
                + "        \"type\": \"UnconfirmedPublisher\"\n"
                + "       },\n"
                + "       \"type\": \"LiteraryArtsMonograph\"\n"
                + "      }";
    }

    private static String nullIsnbList() {
        return "{\n"
                + "       \"pages\": {\n"
                + "        \"illustrated\": false,\n"
                + "        \"type\": \"MonographPages\"\n"
                + "       },\n"
                + "       \"publicationDate\": {\n"
                + "        \"type\": \"PublicationDate\",\n"
                + "        \"year\": \"2020\"\n"
                + "       },\n"
                + "       \"publisher\": {\n"
                + "        \"name\": \"Utgiver\",\n"
                + "        \"type\": \"UnconfirmedPublisher\"\n"
                + "       },\n"
                + "       \"type\": \"LiteraryArtsMonograph\"\n"
                + "      }";
    }

    @ParameterizedTest
    @MethodSource("literaryArtsMonographProvider")
    void shouldConstructObjectFromJson(String input) {
        assertDoesNotThrow(() -> JsonUtils.dtoObjectMapper.readValue(input, LiteraryArtsMonograph.class));
    }
}
