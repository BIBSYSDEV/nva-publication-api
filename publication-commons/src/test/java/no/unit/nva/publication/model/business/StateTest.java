package no.unit.nva.publication.model.business;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.assertEquals;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.Instant;
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.publication.model.business.publicationstate.CreatedState;
import no.unit.nva.publication.model.business.publicationstate.DeletedState;
import no.unit.nva.publication.model.business.publicationstate.PublishedState;
import no.unit.nva.publication.model.business.publicationstate.State;
import no.unit.nva.publication.model.business.publicationstate.UnpublishedState;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class StateTest {

    public static Stream<Arguments> stateProvider() {
        return Stream.of(Arguments.of(new CreatedState(Instant.now(), new User(randomString()), randomUri())),
                         Arguments.of(new UnpublishedState(Instant.now(), new User(randomString()), randomUri())),
                         Arguments.of(new PublishedState(Instant.now(), new User(randomString()), randomUri())),
                         Arguments.of(new DeletedState(Instant.now(), new User(randomString()), randomUri())));
    }

    @ParameterizedTest
    @MethodSource("stateProvider")
    void shouldDoRoundTripWithoutLossOfData(State state) throws JsonProcessingException {
        var json = JsonUtils.dtoObjectMapper.writeValueAsString(state);
        var roundTrippedState = JsonUtils.dtoObjectMapper.readValue(json, State.class);

        assertEquals(state, roundTrippedState);
    }
}
