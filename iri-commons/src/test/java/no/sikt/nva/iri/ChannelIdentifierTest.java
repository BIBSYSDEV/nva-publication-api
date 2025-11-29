package no.sikt.nva.iri;

import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.commons.json.JsonUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ChannelIdentifierTest {


    @ParameterizedTest
    @ValueSource(strings = {
            "https://api.nva.unit.no/publication-channels-v2/serial-publication/360B8D2C-736F-450A-8D34-9596BFE28CB4/2025",
            "https://api.nva.unit.no/publication-channels-v2/publisher/360B8D2C-736F-450A-8D34-9596BFE28CB4/2025"
    })
    void shouldSerializeJsonString(String uriString) throws JsonProcessingException {
        var uri = URI.create(uriString);
        var channel = PublicationChannelId.from(uri);
        var expected = "\"%s\"".formatted(uriString.replace("https://api.nva.unit.no", "")
                .toLowerCase(Locale.getDefault()));
        assertEquals(expected, JsonUtils.dtoObjectMapper.writeValueAsString(channel));
    }


    @ParameterizedTest
    @ValueSource(strings = {
            "https://api.nva.unit.no/publication-channels-v2/serial-publication/360B8D2C-736F-450A-8D34-9596BFE28CB4/2025",
            "https://api.nva.unit.no/publication-channels-v2/publisher/360B8D2C-736F-450A-8D34-9596BFE28CB4/2025"
    })
    void shouldCreateString(String uriString) {
        var uri = URI.create(uriString);
        var channel = PublicationChannelId.from(uri);
        var expected = uriString.replace("https://api.nva.unit.no", "").toLowerCase(Locale.getDefault());
        assertEquals(expected, channel.value());
    }

    @Test
    void shouldThrowWhenInputUriIsNull() {
        var exception = assertThrows(IllegalArgumentException.class, () -> PublicationChannelId.from(null));
        assertEquals("Encountered URI that is not a valid publication channel id", exception.getMessage());
    }

    @Test
    void shouldThrowWhenCreatingUriWithHostThatIsNull() {
        var uri = URI.create("https://api.nva.unit.no/publication-channels-v2/serial-publication/FA676EF6-41D1-4244-9323-B7F3F18D394E/2025");
        var channel = PublicationChannelId.from(uri);
        var exception = assertThrows(IllegalArgumentException.class, () -> channel.uri(null));
        assertEquals("Provided host is invalid", exception.getMessage());
    }

    @Test
    public void serialPublicationIdExists() {
        var uriString = "https://api.nva.unit.no/publication-channels-v2/serial-publication/FA676EF6-41D1-4244-9323-B7F3F18D394E/2025";
        var uri = URI.create(uriString);
        var channel = PublicationChannelId.from(uri);
        var expected = URI.create(uriString.toLowerCase(Locale.ROOT));
        assertEquals(expected, channel.uri("api.nva.unit.no"));
    }

    @Test
    void publisherIdExists() {
        var uriString = "https://api.nva.unit.no/publication-channels-v2/publisher/360B8D2C-736F-450A-8D34-9596BFE28CB4/2025";
        var uri = URI.create(uriString);
        var publisherId = PublisherId.from(uri);
        var expected = URI.create(uriString.toLowerCase(Locale.ROOT));
        assertEquals(expected, publisherId.uri("api.nva.unit.no"));
    }

    @ParameterizedTest
    @NullAndEmptySource()
    void shouldThrowWhenPublisherIdUrlIsCalledIthInvalidHost(String host) {
        var uriString = "https://api.nva.unit.no/publication-channels-v2/publisher/360B8D2C-736F-450A-8D34-9596BFE28CB4/2025";
        var uri = URI.create(uriString);
        var publisherId = PublisherId.from(uri);
        var thrown = assertThrows(IllegalArgumentException.class, () -> publisherId.uri(host));
        assertEquals("Provided host is invalid", thrown.getMessage());
    }

    @ParameterizedTest
    @NullAndEmptySource()
    void shouldThrowWhenSerialPublicationIdUrlIsCalledIthInvalidHost(String host) {
        var uriString = "https://api.nva.unit.no/publication-channels-v2/serial-publication/360B8D2C-736F-450A-8D34-9596BFE28CB4/2025";
        var uri = URI.create(uriString);
        var publisherId = SerialPublicationId.from(uri);
        var thrown = assertThrows(IllegalArgumentException.class, () -> publisherId.uri(host));
        assertEquals("Provided host is invalid", thrown.getMessage());
    }

    @Test
    void shouldThrowWhenPublisherIdHasInvalidScheme() {
        var uri = URI.create("gopher://api.nva.unit.no/publication-channels-v2/publisher/360B8D2C-736F-450A-8D34-9596BFE28CB4/2025");
        var thrown = assertThrows(IllegalArgumentException.class, () -> PublisherId.from(uri));
        assertEquals("Channel URI should have https scheme", thrown.getMessage());
    }

    @Test
    void shouldThrowWhenSerialPublicationIdHasInvalidScheme() {
        var uri = URI.create("gopher://api.nva.unit.no/publication-channels-v2/serial-publication/360B8D2C-736F-450A-8D34-9596BFE28CB4/2025");
        var thrown = assertThrows(IllegalArgumentException.class, () -> SerialPublicationId.from(uri));
        assertEquals("Channel URI should have https scheme", thrown.getMessage());
    }

    @Test
    void shouldThrowWhenUriIsInvalid() {
        var uri = URI.create("invalid");
        var thrown = assertThrows(IllegalArgumentException.class, () -> PublicationChannelId.from(uri));
        assertEquals("Encountered URI that is not a valid publication channel id", thrown.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://api.nva.unit.no/publication-channels-v2/serial-publication/360B8D2C-736F-450A-8D34-9596BFE28CB4/2025",
            "https://api.nva.unit.no/publication-channels-v2/publisher/360B8D2C-736F-450A-8D34-9596BFE28CB4/2025"
    })
    void shouldReturnWhenUriIsInvalid(String uriString) {
        var uri = URI.create(uriString);
        var actual = PublicationChannelId.from(uri).uri("api.nva.unit.no");
        var expected = URI.create(uriString.toLowerCase(Locale.ROOT));
        assertEquals(expected, actual);
    }


    @ParameterizedTest
    @ValueSource(strings = {
            "https://api.nva.unit.no/publication-channels-v2/publisher/360B8D2C-736F-450A-8D34-9596BFE28CB4/",
            "https://api.nva.unit.no/publication-channels-v2/publisher/360B8D2C-736F-450A-8D34-9596BFE28CB4",
            "https://api.nva.unit.no/publication-channels-v2/publisher/",
            "https://api.nva.unit.no/publication-channels-v2/publisher",
            "https://api.nva.unit.no/",
            "https://api.nva.unit.no"
    })
    void shouldThrowWhenPublisherUriPathHasInvalidElements(String uriString) {
        var uri = URI.create(uriString);
        var thrown = assertThrows(IllegalArgumentException.class, () -> PublisherId.from(uri));
        assertEquals("publisher must have a path matching /{CHANNELS-PATH}/{TYPE}/{UUID}/{YYYY}", thrown.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://api.nva.unit.no/publication-channels-v2/serial-publication/360B8D2C-736F-450A-8D34-9596BFE28CB4/",
            "https://api.nva.unit.no/publication-channels-v2/serial-publication/360B8D2C-736F-450A-8D34-9596BFE28CB4",
            "https://api.nva.unit.no/publication-channels-v2/serial-publication/",
            "https://api.nva.unit.no/publication-channels-v2/serial-publication",
            "https://api.nva.unit.no/",
            "https://api.nva.unit.no"
    })
    void shouldThrowWhenSerialPublicationUriPathHasInvalidElements(String uriString) {
        var uri = URI.create(uriString);
        var thrown = assertThrows(IllegalArgumentException.class, () -> SerialPublicationId.from(uri));
        assertEquals("serial-publication must have a path matching /{CHANNELS-PATH}/{TYPE}/{UUID}/{YYYY}", thrown.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://api.nva.unit.no/publication-channels-v2/publisher/360B8D2C-736F-450A-8D34-9596BFE28CBK/2025",
            "https://api.nva.unit.no/publication-channels-v2/publisher/A360B8D2C-736F-450A-8D34-9596BFE28CB4/2025",
            "https://api.nva.unit.no/publication-channels-v2/publisher/horse/2025",
    })
    void shouldThrowWhenPublisherIdContainsInvalidUuid(String uriString) {
        var uri = URI.create(uriString);
        var thrown = assertThrows(IllegalArgumentException.class, () -> PublisherId.from(uri));
        assertEquals("publisher must contain a valid UUID", thrown.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://api.nva.unit.no/publication-channels-v2/serial-publication/360B8D2C-736F-450A-8D34-9596BFE28CBK/2025",
            "https://api.nva.unit.no/publication-channels-v2/serial-publication/A360B8D2C-736F-450A-8D34-9596BFE28CB4/2025",
            "https://api.nva.unit.no/publication-channels-v2/serial-publication/horse/2025",
    })
    void shouldThrowWhenSerialPublicationIdContainsInvalidUuid(String uriString) {
        var uri = URI.create(uriString);
        var thrown = assertThrows(IllegalArgumentException.class, () -> SerialPublicationId.from(uri));
        assertEquals("serial-publication must contain a valid UUID", thrown.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://api.nva.unit.no/publication-channels-v2/publishers/360B8D2C-736F-450A-8D34-9596BFE28CB4/2025",
            "https://api.nva.unit.no/publication-channels-v2/horse/360B8D2C-736F-450A-8D34-9596BFE28CB4/2025"
    })
    void shouldThrowWhenPublisherIdContainsInvalidType(String uriString) {
        var uri = URI.create(uriString);
        var thrown = assertThrows(IllegalArgumentException.class, () -> PublisherId.from(uri));
        assertEquals("publisher must contain a valid type", thrown.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://api.nva.unit.no/publication-channels-v2/serial-publications/360B8D2C-736F-450A-8D34-9596BFE28CB4/2025",
            "https://api.nva.unit.no/publication-channels-v2/horse/360B8D2C-736F-450A-8D34-9596BFE28CB4/2025"
    })
    void shouldThrowWhenSerialPublicationIdContainsInvalidType(String uriString) {
        var uri = URI.create(uriString);
        var thrown = assertThrows(IllegalArgumentException.class, () -> SerialPublicationId.from(uri));
        assertEquals("serial-publication must contain a valid type", thrown.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://api.nva.unit.no/publication-channel-v2/publisher/360B8D2C-736F-450A-8D34-9596BFE28CB4/2025",
            "https://api.nva.unit.no/publication-channels-v1/publisher/360B8D2C-736F-450A-8D34-9596BFE28CB4/2025"
    })
    void shouldThrowWhenPublisherIdContainsInvalidPathStart(String uriString) {
        var uri = URI.create(uriString);
        var thrown = assertThrows(IllegalArgumentException.class, () -> PublisherId.from(uri));
        assertEquals("publisher must have a path starting /{CHANNELS-PATH}", thrown.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://api.nva.unit.no/publication-channel-v2/serial-publication/360B8D2C-736F-450A-8D34-9596BFE28CB4/2025",
            "https://api.nva.unit.no/publication-channels-v1/serial-publication/360B8D2C-736F-450A-8D34-9596BFE28CB4/2025"
    })
    void shouldThrowWhenSerialPublicationIdContainsInvalidPathStart(String uriString) {
        var uri = URI.create(uriString);
        var thrown = assertThrows(IllegalArgumentException.class, () -> SerialPublicationId.from(uri));
        assertEquals("serial-publication must have a path starting /{CHANNELS-PATH}", thrown.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://api.nva.unit.no/publication-channels-v2/publisher/360B8D2C-736F-450A-8D34-9596BFE28CB4/2025A",
            "https://api.nva.unit.no/publication-channels-v2/publisher/360B8D2C-736F-450A-8D34-9596BFE28CB4/TOOT"
    })
    void shouldThrowWhenPublisherIdContainsInvalidYear(String uriString) {
        var uri = URI.create(uriString);
        var thrown = assertThrows(IllegalArgumentException.class, () -> PublisherId.from(uri));
        assertEquals("publisher must contain a valid year", thrown.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://api.nva.unit.no/publication-channels-v2/serial-publication/360B8D2C-736F-450A-8D34-9596BFE28CB4/2025A",
            "https://api.nva.unit.no/publication-channels-v2/serial-publication/360B8D2C-736F-450A-8D34-9596BFE28CB4/TOOT"
    })
    void shouldThrowWhenSerialPublicationIdContainsInvalidYear(String uriString) {
        var uri = URI.create(uriString);
        var thrown = assertThrows(IllegalArgumentException.class, () -> SerialPublicationId.from(uri));
        assertEquals("serial-publication must contain a valid year", thrown.getMessage());
    }

    @Test
    void shouldThrowWhenPublisherIdContainsTooEarlyYear() {
        var uri = URI.create("https://api.nva.unit.no/publication-channels-v2/publisher/360B8D2C-736F-450A-8D34-9596BFE28CB4/200");
        var thrown = assertThrows(IllegalArgumentException.class, () -> PublisherId.from(uri));
        assertEquals("publisher must have a year after 2004", thrown.getMessage());
    }

    @Test
    void shouldThrowWhenSerialPublicationIdContainsTooEarlyYear() {
        var uri = URI.create("https://api.nva.unit.no/publication-channels-v2/serial-publication/360B8D2C-736F-450A-8D34-9596BFE28CB4/200");
        var thrown = assertThrows(IllegalArgumentException.class, () -> SerialPublicationId.from(uri));
        assertEquals("serial-publication must have a year after 2004", thrown.getMessage());
    }
}
