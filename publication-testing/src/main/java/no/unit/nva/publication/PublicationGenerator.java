package no.unit.nva.publication;

import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.File;
import no.unit.nva.model.FileSet;
import no.unit.nva.model.License;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import nva.commons.core.JacocoGenerated;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class PublicationGenerator {

    public static final String OWNER = "owner@example.org";
    public static final String PUBLISHER_ID = "http://example.org/123";

    @JacocoGenerated
    private PublicationGenerator() {

    }

    @JacocoGenerated
    public static Publication publicationWithIdentifier() {
        return generatePublication(UUID.randomUUID());
    }

    @JacocoGenerated
    public static Publication publicationWithoutIdentifier() {
        return generatePublication(null);
    }

    /**
     * Generate a minimal Publication for testing.
     *
     * @param uuid  uuid for identifier
     * @return  publication
     */
    @JacocoGenerated
    public static Publication generatePublication(UUID uuid) {
        Instant oneMinuteInThePast = Instant.now().minusSeconds(60L);
        return new Publication.Builder()
                .withIdentifier(uuid)
                .withCreatedDate(oneMinuteInThePast)
                .withModifiedDate(oneMinuteInThePast)
                .withOwner(OWNER)
                .withStatus(PublicationStatus.DRAFT)
                .withPublisher(new Organization.Builder()
                        .withId(URI.create(PUBLISHER_ID))
                        .build())
                .withEntityDescription(new EntityDescription.Builder()
                        .withMainTitle("DynamoDB Local Testing")
                        .build())
                .withFileSet(new FileSet.Builder()
                        .withFiles(List.of(new File.Builder()
                                .withIdentifier(UUID.randomUUID())
                                .withLicense(new License.Builder()
                                        .withIdentifier("licenseId")
                                        .build())
                                .build()))
                        .build())
                .build();
    }

}
