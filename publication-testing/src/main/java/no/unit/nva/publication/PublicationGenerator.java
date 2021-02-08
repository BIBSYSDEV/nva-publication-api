package no.unit.nva.publication;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.File;
import no.unit.nva.model.FileSet;
import no.unit.nva.model.License;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.Reference;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.journal.JournalArticle;
import no.unit.nva.model.pages.Pages;
import no.unit.nva.model.pages.Range;
import nva.commons.core.JacocoGenerated;

public final class PublicationGenerator {

    public static final String OWNER = "owner@example.org";
    public static final String PUBLISHER_ID = "http://example.org/123";

    @JacocoGenerated
    private PublicationGenerator() {

    }

    @JacocoGenerated
    public static Publication publicationWithIdentifier() {
        return generatePublication(SortableIdentifier.next());
    }

    @JacocoGenerated
    public static Publication publicationWithoutIdentifier() {
        return generatePublication(null);
    }

    /**
     * Generate a minimal Publication for testing.
     *
     * @param identifier Sortable identifier
     * @return  publication
     */
    @JacocoGenerated
    public static Publication generatePublication(SortableIdentifier identifier) {

        EntityDescription entityDescription = createSampleEntityDescription();

        Instant oneMinuteInThePast = Instant.now().minusSeconds(60L);

        return new Publication.Builder()
            .withIdentifier(identifier)
            .withCreatedDate(oneMinuteInThePast)
            .withModifiedDate(oneMinuteInThePast)
            .withOwner(OWNER)
            .withStatus(PublicationStatus.DRAFT)
            .withPublisher(samplePublisher())
            .withEntityDescription(entityDescription)
            .withFileSet(sampleFileSet())
            .build();
    }

    public static Publication generateEmptyPublication() {
        return new Publication.Builder()
            .withOwner(OWNER)
            .withPublisher(samplePublisher())
            .build();
    }

    private static Organization samplePublisher() {
        return new Organization.Builder()
            .withId(URI.create(PUBLISHER_ID))
            .build();
    }

    private static FileSet sampleFileSet() {
        return new FileSet.Builder()
            .withFiles(List.of(new File.Builder()
                .withIdentifier(UUID.randomUUID())
                .withLicense(new License.Builder()
                    .withIdentifier("licenseId")
                    .build())
                .build()))
            .build();
    }

    private static EntityDescription createSampleEntityDescription() {
        PublicationInstance<? extends Pages> publicationInstance = new JournalArticle.Builder()
            .withArticleNumber("1")
            .withIssue("2")
            .withVolume("Volume 1")
            .withPages(new Range("beginRange", "endRange"))
            .build();
        Reference reference = new Reference.Builder().withPublicationInstance(publicationInstance).build();

        return new EntityDescription.Builder()
            .withMainTitle("DynamoDB Local Testing")
            .withDate(new PublicationDate.Builder().withYear("2020").withMonth("2").withDay("31").build())
            .withReference(reference)
            .build();
    }
}
