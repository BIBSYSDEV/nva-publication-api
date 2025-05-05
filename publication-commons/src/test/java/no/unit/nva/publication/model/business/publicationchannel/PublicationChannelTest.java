package no.unit.nva.publication.model.business.publicationchannel;

import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class PublicationChannelTest {

    public static Stream<Arguments> publicationChannelProvider() {
        return Stream.of(Arguments.of(randomNonClaimedPublicationChannel(), randomClaimedPublicationChannel()));
    }

    @ParameterizedTest
    @MethodSource("publicationChannelProvider")
    void shouldDoRoundTripWithoutLossOfInformation(PublicationChannel publicationChannel)
        throws JsonProcessingException {
        var json = JsonUtils.dtoObjectMapper.writeValueAsString(publicationChannel);
        var roundTripped = JsonUtils.dtoObjectMapper.readValue(json, PublicationChannel.class);

        assertEquals(publicationChannel, roundTripped);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Publisher", "SerialPublication"})
    void shouldConvertStringToChannelType(String value) {
        var channelType = ChannelType.fromValue(value);

        assertEquals(value, channelType.getValue());
    }

    @ParameterizedTest
    @ValueSource(strings = {"OwnerOnly", "Everyone"})
    void shouldConvertStringToChannelPolicy(String value) {
        var channelPolicy = ChannelPolicy.fromValue(value);

        assertEquals(value, channelPolicy.getValue());
    }

    private static ClaimedPublicationChannel randomClaimedPublicationChannel() {
        return new ClaimedPublicationChannel(randomUri(), randomUri(), randomUri(),
                                             new Constraint(ChannelPolicy.EVERYONE, ChannelPolicy.OWNER_ONLY,
                                                            List.of(randomString())), ChannelType.PUBLISHER,
                                             SortableIdentifier.next(), SortableIdentifier.next(), Instant.now(),
                                             Instant.now());
    }

    private static NonClaimedPublicationChannel randomNonClaimedPublicationChannel() {
        return new NonClaimedPublicationChannel(randomUri(), ChannelType.PUBLISHER, SortableIdentifier.next(),
                                                SortableIdentifier.next(), Instant.now(), Instant.now());
    }
}