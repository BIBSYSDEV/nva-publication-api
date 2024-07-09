package no.unit.nva;

import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.instancetypes.artistic.film.realization.Broadcast;
import no.unit.nva.model.instancetypes.artistic.film.realization.OtherRelease;
import no.unit.nva.model.instancetypes.artistic.music.AudioVisualPublication;
import no.unit.nva.model.instancetypes.artistic.music.MusicScore;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Deprecated
public class MigrationTestingUnconfirmedPublisher {

    @ParameterizedTest
    @ValueSource(strings = {"{\n"
            + "  \"type\": \"Broadcast\",\n"
            + "  \"publisher\": \"My publisher\",\n"
            + "  \"date\": {\n"
            + "    \"type\": \"Instant\",\n"
            + "    \"value\": \"2022-03-23T00:00:00.000000Z\"\n"
            + "  },\n"
            + "  \"sequence\": \"1\"\n"
            + "}{\n\"type\": \"Broadcast\",\n"
            + "  \"publisher\": {\n"
            + "    \"type\": \"UnconfirmedPublisher\",\n"
            + "    \"name\": \"My publisher\"\n"
            + "  },\n"
            + "  \"date\": {\n"
            + "    \"type\": \"Instant\",\n"
            + "    \"value\": \"2022-03-23T00:00:00.000000Z\"\n"
            + "  },\n"
            + "  \"sequence\": \"1\"\n"
            + "}"})
    void shouldMigrateBroadcastPublishers(String value) {
        assertDoesNotThrow(() -> JsonUtils.dtoObjectMapper.readValue(value, Broadcast.class));

    }

    @ParameterizedTest
    @ValueSource(strings = {"{\n"
            + "  \"type\" : \"OtherRelease\",\n"
            + "  \"description\" : \"SmM4g4sYfz\",\n"
            + "  \"place\" : {\n"
            + "    \"type\" : \"UnconfirmedPlace\",\n"
            + "    \"label\" : \"dc6zWFBfBcmnzlwN1z\",\n"
            + "    \"country\" : \"LEQElC2GRpVi75IbqI\"\n"
            + "  },\n"
            + "  \"publisher\" : {\n"
            + "    \"type\" : \"UnconfirmedPublisher\",\n"
            + "    \"name\" : \"o3XnSxNZkyprTM7DWoQ\"\n"
            + "  },\n"
            + "  \"date\" : {\n"
            + "    \"type\" : \"Instant\",\n"
            + "    \"value\" : \"2016-01-09T00:59:20.264Z\"\n"
            + "  },\n"
            + "  \"sequence\" : 191312365\n"
            + "}", "{\n"
            + "  \"type\" : \"OtherRelease\",\n"
            + "  \"description\" : \"SmM4g4sYfz\",\n"
            + "  \"place\" : {\n"
            + "    \"type\" : \"UnconfirmedPlace\",\n"
            + "    \"label\" : \"dc6zWFBfBcmnzlwN1z\",\n"
            + "    \"country\" : \"LEQElC2GRpVi75IbqI\"\n"
            + "  },\n"
            + "  \"publisher\" : \"o3XnSxNZkyprTM7DWoQ\",\n"
            + "  \"date\" : {\n"
            + "    \"type\" : \"Instant\",\n"
            + "    \"value\" : \"2016-01-09T00:59:20.264Z\"\n"
            + "  },\n"
            + "  \"sequence\" : 191312365\n"
            + "}"})
    void shouldMigrateOtherRelease(String value) {
        assertDoesNotThrow(() -> JsonUtils.dtoObjectMapper.readValue(value, OtherRelease.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\n"
            + "          \"type\" : \"AudioVisualPublication\",\n"
            + "          \"mediaType\" : \"DigitalFile\",\n"
            + "          \"publisher\" : {\n"
            + "            \"type\" : \"UnconfirmedPublisher\",\n"
            + "            \"name\" : \"zqbVxPvPBPY\"\n"
            + "          },\n"
            + "          \"catalogueNumber\" : \"qVMAjQwGCTw\",\n"
            + "          \"trackList\" : [ {\n"
            + "            \"type\" : \"MusicTrack\",\n"
            + "            \"title\" : \"prvhN5ATlNDbcAAX0G\",\n"
            + "            \"composer\" : \"DtePAk2ZjLw8Lzr\",\n"
            + "            \"extent\" : \"GuJRYj8fAOMcQPQSMI\"\n"
            + "          }]}", "{\n"
            + "  \"type\": \"AudioVisualPublication\",\n"
            + "  \"mediaType\": \"DigitalFile\",\n"
            + "  \"publisher\": \"UnconfirmedPublisher\",\n"
            + "  \"catalogueNumber\": \"qVMAjQwGCTw\",\n"
            + "  \"trackList\": [\n"
            + "    {\n"
            + "      \"type\": \"MusicTrack\",\n"
            + "      \"title\": \"prvhN5ATlNDbcAAX0G\",\n"
            + "      \"composer\": \"DtePAk2ZjLw8Lzr\",\n"
            + "      \"extent\": \"GuJRYj8fAOMcQPQSMI\"\n"
            + "    }\n"
            + "  ]\n"
            + "}"})
    void shouldMigrateAudioVisualPublication(String value) {
        assertDoesNotThrow(() -> JsonUtils.dtoObjectMapper.readValue(value, AudioVisualPublication.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\n"
            + "          \"type\" : \"MusicScore\",\n"
            + "          \"ensemble\" : \"e2d9YoIg1eVxFiTZV\",\n"
            + "          \"movements\" : \"ID9aYllhE3N5SZ\",\n"
            + "          \"extent\" : \"26CJRjrfrUqOzP\",\n"
            + "          \"publisher\" : {\n"
            + "            \"type\" : \"UnconfirmedPublisher\",\n"
            + "            \"name\" : \"OVuxPTXnSLd7jcg\"\n"
            + "          },\n"
            + "          \"ismn\" : {\n"
            + "            \"type\" : \"Ismn\",\n"
            + "            \"value\" : \"M230671187\",\n"
            + "            \"formatted\" : \"M-2306-7118-7\"\n"
            + "          },\n"
            + "          \"isrc\" : {\n"
            + "            \"type\" : \"Isrc\",\n"
            + "            \"value\" : \"USRC17607839\"\n"
            + "          }\n"
            + "        }", "{\n"
            + "  \"type\" : \"MusicScore\",\n"
            + "  \"ensemble\" : \"e2d9YoIg1eVxFiTZV\",\n"
            + "  \"movements\" : \"ID9aYllhE3N5SZ\",\n"
            + "  \"extent\" : \"26CJRjrfrUqOzP\",\n"
            + "  \"publisher\" :  \"OVuxPTXnSLd7jcg\",\n"
            + "  \"ismn\" : {\n"
            + "    \"type\" : \"Ismn\",\n"
            + "    \"value\" : \"M230671187\",\n"
            + "    \"formatted\" : \"M-2306-7118-7\"\n"
            + "  },\n"
            + "  \"isrc\" : {\n"
            + "    \"type\" : \"Isrc\",\n"
            + "    \"value\" : \"USRC17607839\"\n"
            + "  }\n"
            + "}"})
    void shouldMigratePublisherInMusicScore(String value) {
        assertDoesNotThrow(() -> JsonUtils.dtoObjectMapper.readValue(value, MusicScore.class));
    }
}
