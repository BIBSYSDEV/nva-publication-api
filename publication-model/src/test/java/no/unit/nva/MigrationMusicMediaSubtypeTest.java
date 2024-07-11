package no.unit.nva;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.instancetypes.artistic.music.AudioVisualPublication;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class MigrationMusicMediaSubtypeTest {

    public static Stream<String> typeProvider() {
        return Stream.of(simpleType(), objectType());
    }

    @ParameterizedTest
    @MethodSource("typeProvider")
    void shouldAcceptBothSimpleSubtypeAndObjectSubtype(String type) {
        assertDoesNotThrow(() -> JsonUtils.dtoObjectMapper.readValue(type, AudioVisualPublication.class));
    }

    private static String objectType() {
        return "{\n"
               + "  \"type\": \"AudioVisualPublication\",\n"
               + "  \"mediaType\": {\n"
               + "    \"type\": \"Streaming\"\n"
               + "  },\n"
               + "  \"publisher\": {\n"
               + "    \"type\": \"UnconfirmedPublisher\",\n"
               + "    \"name\": \"tsuQdfyhHEHdDw\"\n"
               + "  },\n"
               + "  \"catalogueNumber\": \"rgDw2pPaziFB\",\n"
               + "  \"trackList\": [\n"
               + "    {\n"
               + "      \"type\": \"MusicTrack\",\n"
               + "      \"title\": \"ornLE22XXYUf13KxJp\",\n"
               + "      \"composer\": \"S9dg9UNjs4z3y\",\n"
               + "      \"extent\": \"EM3WDFYRMppR\"\n"
               + "    }\n"
               + "  ],\n"
               + "  \"isrc\": {\n"
               + "    \"type\": \"Isrc\",\n"
               + "    \"value\": \"USRC17607839\"\n"
               + "  }\n"
               + "}";
    }

    private static String simpleType() {
        return "{\n"
               + "  \"type\" : \"AudioVisualPublication\",\n"
               + "  \"mediaType\" : \"Streaming\",\n"
               + "  \"publisher\" : {\n"
               + "    \"type\" : \"UnconfirmedPublisher\",\n"
               + "    \"name\" : \"tsuQdfyhHEHdDw\"\n"
               + "  },\n"
               + "  \"catalogueNumber\" : \"rgDw2pPaziFB\",\n"
               + "  \"trackList\" : [ {\n"
               + "    \"type\" : \"MusicTrack\",\n"
               + "    \"title\" : \"ornLE22XXYUf13KxJp\",\n"
               + "    \"composer\" : \"S9dg9UNjs4z3y\",\n"
               + "    \"extent\" : \"EM3WDFYRMppR\"\n"
               + "  } ],\n"
               + "  \"isrc\" : {\n"
               + "    \"type\" : \"Isrc\",\n"
               + "    \"value\" : \"USRC17607839\"\n"
               + "  }\n"
               + "}";
    }
}
