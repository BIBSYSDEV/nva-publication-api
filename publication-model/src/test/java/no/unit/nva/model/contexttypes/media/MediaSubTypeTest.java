package no.unit.nva.model.contexttypes.media;

import static no.unit.nva.model.contexttypes.media.MediaSubTypeEnum.OTHER;
import static no.unit.nva.model.contexttypes.media.MediaSubTypeEnum.RADIO;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test for demonstrating the necessity and the way to deserialize a SubType that can be both a String (legacy format)
 * and an object. Delete when all Subtypes have been migrated to objects.
 *
 */
class MediaSubTypeTest {
    
    public static Stream<Arguments> serializationVersionsProvider() {
        var notOther = Arguments.of("{\"type\": \"Radio\"}", RADIO);
        var other = Arguments.of("{\"type\":\"Other\", \"description\":\"Other media type\"}", OTHER);
        return Stream.of(notOther, other);
    }
    
    @Test
    void shouldDeserializeSubTypeFromString() throws JsonProcessingException {
        var type = "\"Radio\"";
        var subtype = JsonUtils.dtoObjectMapper.readValue(type, MediaSubType.class);
        assertThat(subtype.getType(), is(equalTo(RADIO)));
    }
    
    @ParameterizedTest
    @MethodSource("serializationVersionsProvider")
    void shouldDeserializeSubTypeFromObject(String jsonString, MediaSubTypeEnum expectedType)
        throws JsonProcessingException {
        var json  = JsonUtils.dtoObjectMapper.readTree(jsonString);
        var subtype = JsonUtils.dtoObjectMapper.readValue(jsonString, MediaSubType.class);
        assertThat(subtype.getType(), is(equalTo(expectedType)));
    }
}