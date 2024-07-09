package no.unit.nva.model.instancetypes.artistic.architecture;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;

class ArchitectureSubtypeTest {

    @Test
    void shouldSerializeAndDeserializeWhenOtherIsDescribed() throws JsonProcessingException {
        var description = randomString();
        var expected = ArchitectureSubtype.fromJson(ArchitectureSubtypeEnum.OTHER, description);
        var json = dtoObjectMapper.writeValueAsString(expected);
        var deserialized = dtoObjectMapper.readValue(json, ArchitectureSubtype.class);
        assertThat(deserialized, is(equalTo(expected)));
    }
}