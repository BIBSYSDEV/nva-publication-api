package no.sikt.nva.brage.migration.lambda;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.testutils.RandomDataGenerator.randomIsbn13;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import no.sikt.nva.brage.migration.record.Customer;
import no.sikt.nva.brage.migration.record.Record;
import no.unit.nva.auth.uriretriever.UriRetriever;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.ImportSource;
import no.unit.nva.model.Publication;
import no.unit.nva.model.contexttypes.Anthology;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.instancetypes.book.BookAnthology;
import no.unit.nva.model.instancetypes.chapter.NonFictionChapter;
import no.unit.nva.publication.model.ResourceWithId;
import no.unit.nva.publication.model.SearchResourceApiResponse;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeS3Client;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.paths.UnixPath;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BragePatchEventConsumerTest extends ResourcesLocalTest {

    public static final String TIME_STAMP = randomString();
    public static final Context CONTEXT = mock(Context.class);
    private ResourceService resourceService;
    private UriRetriever uriRetriever;
    private BragePatchEventConsumer handler;
    private FakeS3Client s3Client;

    @BeforeEach
    public void init() {
        super.init();
        this.s3Client = new FakeS3Client();
        this.resourceService = getResourceServiceBuilder(client).build();
        this.uriRetriever = mock(UriRetriever.class);
        this.handler = new BragePatchEventConsumer(resourceService, s3Client, uriRetriever);
    }

    @Test
    void shouldUpdateChapterPartOfValueWhenSearchApiReturnsPublicationWithTheSameIsbn() throws NotFoundException {
        var isbn = randomIsbn13();
        var existingParentPublication = persistBookWithIsbn(isbn);
        var partOfReport = persistChildAndPartOfReportWithIsbn(isbn);
        var event = createSqsEvent(partOfReport.getLocation());

        mockSearchApiResponse(existingParentPublication, 1);
        handler.handleRequest(event, CONTEXT);

        var updatedChild = resourceService.getPublicationByIdentifier(partOfReport.getPublication().getIdentifier());
        var partOfValue = getPartOfValue(updatedChild);

        assertEquals(SortableIdentifier.fromUri(partOfValue), existingParentPublication.getIdentifier());
    }

    @Test
    void shouldNotUpdateChaptersPartOfValueWhenFetchedPublicationDoesNotContainTheSameIsbnAsChapter()
        throws NotFoundException {
        var existingParentPublication = persistBookWithIsbn(randomIsbn13());
        var partOfReport = persistChildAndPartOfReportWithIsbn(randomIsbn13());
        var event = createSqsEvent(partOfReport.getLocation());

        mockSearchApiResponse(existingParentPublication, 1);
        handler.handleRequest(event, CONTEXT);

        var notUpdatedChild = resourceService.getPublicationByIdentifier(partOfReport.getPublication().getIdentifier());

        assertNull(getPartOfValue(notUpdatedChild));
    }

    @Test
    void shouldNotUpdateChaptersPartOfValueWhenSearchApiReturnsMultiplePublications() throws NotFoundException {
        var existingParentPublication = persistBookWithIsbn(randomIsbn13());
        var partOfReport = persistChildAndPartOfReportWithIsbn(randomIsbn13());
        var event = createSqsEvent(partOfReport.getLocation());

        mockSearchApiResponse(existingParentPublication, 2);
        handler.handleRequest(event, CONTEXT);

        var notUpdatedChild = resourceService.getPublicationByIdentifier(partOfReport.getPublication().getIdentifier());

        assertNull(getPartOfValue(notUpdatedChild));
    }

    @Test
    void shouldPersistErrorReportWhenCouldNotUpdatePartOfValue() {
        var existingParentPublication = persistBookWithIsbn(randomIsbn13());
        var partOfReport = persistChildAndPartOfReportWithIsbn(randomIsbn13());
        var event = createSqsEvent(partOfReport.getLocation());

        mockSearchApiResponse(existingParentPublication, 2);
        handler.handleRequest(event, CONTEXT);

        var report = new S3Driver(s3Client, new Environment().readEnv("BRAGE_MIGRATION_ERROR_BUCKET_NAME"))
            .getFile(UnixPath.of("PART_OF_ERROR").addChild(partOfReport.getPublication().getIdentifier().toString()));

        assertTrue(report.contains("Multiple parents fetched for publication"));
    }

    private void mockSearchApiResponse(Publication publication, int hits) {
        var publicationId = UriWrapper.fromUri(randomUri()).addChild(publication.getIdentifier().toString()).getUri();
        var searchApiResponse = new SearchResourceApiResponse(hits, List.of(new ResourceWithId(publicationId)));
        when(uriRetriever.getRawContent(any(), any()))
            .thenReturn(Optional.of(searchApiResponse.toJsonString()));
    }

    private static URI getPartOfValue(Publication updatedChild) {
        return ((Anthology) updatedChild.getEntityDescription().getReference().getPublicationContext()).getId();
    }

    private PartOfReport persistChildAndPartOfReportWithIsbn(String isbn) {
        var publication = randomPublication(NonFictionChapter.class);
        publication.getEntityDescription().getReference().setPublicationContext(new Anthology());
        var persistedPublication =
            resourceService.createPublicationFromImportedEntry(publication,
                                                               ImportSource.fromBrageArchive(randomString()));
        var record = recordWithIsbn(isbn);
        var partOfReport = new PartOfReport(persistedPublication, record);
        partOfReport.persist(s3Client, TIME_STAMP);
        return partOfReport;
    }

    private static  Record recordWithIsbn(String isbn) {
        var record = new Record();
        record.setId(randomUri());
        record.setCustomer(new Customer("ntnu", null));
        var recordPublication = new no.sikt.nva.brage.migration.record.Publication();
        recordPublication.setIsbnList(List.of(isbn));
        record.setPublication(recordPublication);
        return record;
    }

    private Publication persistBookWithIsbn(String isbn) {
        var publication = randomPublication(BookAnthology.class);
        var context = (Book) publication.getEntityDescription().getReference().getPublicationContext();
        var book = new Book(context.getSeries(),
                            context.getSeriesNumber(),
                            context.getPublisher(), List.of(isbn),
                            context.getRevision());
        publication.getEntityDescription().getReference().setPublicationContext(book);
        return resourceService.createPublicationFromImportedEntry(publication,
                                                                  ImportSource.fromBrageArchive(randomString()));
    }

    private SQSEvent createSqsEvent(URI location) {
        var eventReference = new EventReference(randomString(), randomString(), location, Instant.now());
        var sqsEvent = new SQSEvent();
        var sqsMessage = new SQSMessage();
        sqsMessage.setBody(eventReference.toJsonString());
        sqsEvent.setRecords(List.of(sqsMessage));
        return sqsEvent;
    }
}