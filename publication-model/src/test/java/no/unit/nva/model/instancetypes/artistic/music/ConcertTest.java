package no.unit.nva.model.instancetypes.artistic.music;

import static no.unit.nva.model.instancetypes.artistic.music.Concert.CONCERT_PROGRAMME_FIELD;
import static no.unit.nva.model.testing.PublicationInstanceBuilder.randomTime;
import static no.unit.nva.model.testing.PublicationInstanceBuilder.randomUnconfirmedPlace;
import static no.unit.nva.testutils.RandomDataGenerator.randomBoolean;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.commons.json.JsonUtils;
import org.junit.jupiter.api.Test;

class ConcertTest {

    //TODO: generalize this test for all types that have list fields
    @Test
    void shouldSerializeNullConcertProgrammeAsEmptyList() throws JsonProcessingException {
        var concert = new Concert(randomUnconfirmedPlace(),
                randomTime(),
                randomString(),
                null,
                randomString());
        var serialized = JsonUtils.dtoObjectMapper.writeValueAsString(concert);
        var json = JsonUtils.dtoObjectMapper.readTree(serialized);
        assertThat(json.get(CONCERT_PROGRAMME_FIELD).isArray(), is(true));
        assertThat(json.get(CONCERT_PROGRAMME_FIELD).isEmpty(), is(true));
    }
}