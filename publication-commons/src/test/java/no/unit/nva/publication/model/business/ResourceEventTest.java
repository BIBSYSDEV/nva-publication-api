package no.unit.nva.publication.model.business;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.assertEquals;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.Instant;
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.publication.model.business.publicationstate.CreatedResourceEvent;
import no.unit.nva.publication.model.business.publicationstate.DeletedResourceEvent;
import no.unit.nva.publication.model.business.publicationstate.PublishedResourceEvent;
import no.unit.nva.publication.model.business.publicationstate.ResourceEvent;
import no.unit.nva.publication.model.business.publicationstate.UnpublishedResourceEvent;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ResourceEventTest {

    public static Stream<Arguments> stateProvider() {
        return Stream.of(Arguments.of(new CreatedResourceEvent(Instant.now(), new User(randomString()), randomUri())),
                         Arguments.of(new UnpublishedResourceEvent(Instant.now(), new User(randomString()), randomUri())),
                         Arguments.of(new PublishedResourceEvent(Instant.now(), new User(randomString()), randomUri())),
                         Arguments.of(new DeletedResourceEvent(Instant.now(), new User(randomString()), randomUri())));
    }

    @ParameterizedTest
    @MethodSource("stateProvider")
    void shouldDoRoundTripWithoutLossOfData(ResourceEvent state) throws JsonProcessingException {
        var json = JsonUtils.dtoObjectMapper.writeValueAsString(state);
        var roundTrippedState = JsonUtils.dtoObjectMapper.readValue(json, ResourceEvent.class);

        assertEquals(state, roundTrippedState);
    }
}
