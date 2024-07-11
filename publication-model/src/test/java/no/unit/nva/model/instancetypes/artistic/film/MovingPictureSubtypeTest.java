package no.unit.nva.model.instancetypes.artistic.film;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;

class MovingPictureSubtypeTest {

    @Test
    void shouldSerializeAndDeserializeWhenOtherIsDescribed() throws JsonProcessingException {
        var description = randomString();
        var expected = MovingPictureSubtype.fromJson(MovingPictureSubtypeEnum.OTHER, description);
        var json = dtoObjectMapper.writeValueAsString(expected);
        var deserialized = dtoObjectMapper.readValue(json, MovingPictureSubtype.class);
        assertThat(deserialized, is(equalTo(expected)));
    }
}