package no.unit.nva.publication.validation;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.StringUtils.EMPTY_STRING;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

class ETagTest {

    private static final String ETAG_SEPARATOR = ":";

    @Test
    void shouldThrowIllegalArgumentExceptionWhenCreatingETagWithoutVersion() {
        assertThrows(IllegalArgumentException.class, () -> ETag.create(randomString(), null));
    }

    @Test
    void shouldCreateETagWithoutUsername() {
        assertDoesNotThrow(() -> ETag.create(null, randomString()));
    }

    @Test
    void shouldCreateETagWithVersionOnlyWhenUsernameIsMissing() {
        var version = randomString();

        var etag = ETag.create(null, version);

        assertEquals(version, etag.toString());
    }

    @Test
    void shouldCreateETagWithUsernameAndVersion() {
        var version = randomString();
        var username = randomString();

        var etag = ETag.create(username, version);
        var expectedETag = username + ETAG_SEPARATOR + version;

        assertEquals(expectedETag, etag.toString());
    }

    @Test
    void shouldCreateETagFromStringWhenUsernameIsMissing() {
        var version = randomString();

        var etag = ETag.fromString(version);
        var expectedETag = ETag.create(null, version);

        assertEquals(expectedETag, etag);
    }

    @Test
    void shouldCreateETagFromStringWhenUsernameAndVersionArePresent() {
        var version = randomString();
        var username = randomString();

        var etag = ETag.fromString(username + ":" + version);
        var expectedETag = ETag.create(username, version);

        assertEquals(expectedETag, etag);
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenCreatingETagFromNullValue() {
        assertThrows(IllegalArgumentException.class, () -> ETag.fromString(null));
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenCreatingETagFromEmptyString() {
        assertThrows(IllegalArgumentException.class, () -> ETag.fromString(EMPTY_STRING));
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenCreatingETagFromBlankString() {
        assertThrows(IllegalArgumentException.class, () -> ETag.fromString(" "));
    }
}