package no.unit.nva.model;

import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.net.URI;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.Test;

class OrganizationTest {

    private static final String UIO_LEGACY_IDENTIFIER = "185.0.0.0";
    private static final String UIO_IDENTIFIER = "185.90.0.0";

    @Test
    void shouldStoreLegacyUioIdentifierAsUioIdentifier() {
        var uri = UriWrapper.fromUri(randomUri()).addChild(UIO_LEGACY_IDENTIFIER).getUri();
        var organization = Organization.fromUri(uri);

        var expected = URI.create(uri.toString().replace(UIO_LEGACY_IDENTIFIER, UIO_IDENTIFIER));

        assertEquals(expected, organization.getId());
    }
}