package no.unit.nva.publication.events.handlers.delete;

import static no.unit.nva.model.testing.ImportCandidateGenerator.randomImportCandidate;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.publication.events.bodies.ImportCandidateDeleteEvent.SCOPUS_IDENTIFIER;
import static no.unit.nva.publication.events.handlers.delete.DeleteImportCandidateEventConsumer.NO_IMPORT_CANDIDATE_FOUND;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import no.unit.nva.auth.uriretriever.RawContentRetriever;
import no.unit.nva.auth.uriretriever.UriRetriever;
import no.unit.nva.expansion.model.ExpandedImportCandidate;
import no.unit.nva.importcandidate.ImportCandidate;
import no.unit.nva.model.additionalidentifiers.AdditionalIdentifierBase;
import no.unit.nva.publication.events.bodies.ImportCandidateDeleteEvent;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.testutils.EventBridgeEventBuilder;
import no.unit.nva.testutils.RandomDataGenerator;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.paths.UriWrapper;
import nva.commons.logutils.LogUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DeleteImportCandidateEventConsumerTest extends ResourcesLocalTest {

    public static final Context CONTEXT = null;
    public static final int TWO_HITS = 2;
    public static final int SINGLE_HIT = 1;
    public static final int ZERO_HITS = 0;
    private ResourceService resourceService;
    private ByteArrayOutputStream output;
    private DeleteImportCandidateEventConsumer handler;
    private RawContentRetriever uriRetriever;

    @BeforeEach
    public void init() {
        var tableName = "import-candidates";
        super.init(tableName);
        this.output = new ByteArrayOutputStream();
        uriRetriever = mock(UriRetriever.class);
        resourceService = getResourceService(client, tableName);
        this.handler = new DeleteImportCandidateEventConsumer(resourceService, uriRetriever);
    }

    @Test
    void shouldDeleteImportCandidateSuccessfully() throws NotFoundException {
        final var appender = LogUtils.getTestingAppenderForRootLogger();
        var importCandidate = persistedImportCandidate();
        var event = emulateImportCandidateDeleteEvent(getScopusIdentifier(importCandidate));
        when(uriRetriever.getRawContent(any(), any())).thenReturn(createResponse(importCandidate, SINGLE_HIT));
        handler.handleRequest(event, output, CONTEXT);
        assertThrows(NotFoundException.class,
                     () -> resourceService.getImportCandidateByIdentifier(importCandidate.getIdentifier()));
        assertThat(appender.getMessages(), containsString("Import candidate has been deleted:"));
    }

    private Optional<String> createResponse(ImportCandidate importCandidate, int hits) {
        var expandedImportCandidate = new ExpandedImportCandidate();
        expandedImportCandidate.setIdentifier(UriWrapper.fromUri(randomUri()).addChild(importCandidate.getIdentifier().toString()).getUri());
        return toResponse(expandedImportCandidate, hits);
    }

    @Test
    void shouldThrowBadGatewayExceptionWhenMultipleHitsInResponseFetchingUniqueImportCandidate()
        throws NotFoundException {
        var importCandidate = persistedImportCandidate();
        var event = emulateImportCandidateDeleteEvent(getScopusIdentifier(importCandidate));
        when(uriRetriever.getRawContent(any(), any())).thenReturn(createResponse(importCandidate, TWO_HITS));
        assertThrows(RuntimeException.class, () -> handler.handleRequest(event, output, CONTEXT));
    }

    @Test
    void shouldLogScopusIdentifierWhenZeroHitsInResponseFetchingUniqueImportCandidate() {
        var appender = LogUtils.getTestingAppenderForRootLogger();
        var scopusIdentifier = randomString();
        var event = emulateImportCandidateDeleteEvent(scopusIdentifier);
        when(uriRetriever.getRawContent(any(), any())).thenReturn(emptyResponse());
        handler.handleRequest(event, output, CONTEXT);
        assertThat(appender.getMessages(), containsString(String.format(NO_IMPORT_CANDIDATE_FOUND, scopusIdentifier)));
    }

    private static Optional<String> toResponse(ExpandedImportCandidate importCandidate, int hits) {
        return Optional.of(new ImportCandidateSearchApiResponse(List.of(importCandidate), hits).toString());
    }

    private static Optional<String> emptyResponse() {
        return Optional.of(new ImportCandidateSearchApiResponse(List.of(), ZERO_HITS).toString());
    }

    private static boolean isScopus(AdditionalIdentifierBase identifier) {
        return identifier.sourceName().equals(SCOPUS_IDENTIFIER);
    }

    private String getScopusIdentifier(ImportCandidate importCandidate) {
        return importCandidate.getAdditionalIdentifiers().stream()
                   .filter(DeleteImportCandidateEventConsumerTest::isScopus)
                   .map(AdditionalIdentifierBase::value)
                   .findFirst()
                   .orElseGet(RandomDataGenerator::randomString);
    }

    private ImportCandidate persistedImportCandidate() throws NotFoundException {
        var importCandidate = resourceService.persistImportCandidate(randomImportCandidate());
        return resourceService.getImportCandidateByIdentifier(importCandidate.getIdentifier());
    }

    private InputStream emulateImportCandidateDeleteEvent(String scopusIdentifier) {
        var event = new ImportCandidateDeleteEvent("ImportCandidates.Scopus.Delete", scopusIdentifier);
        return EventBridgeEventBuilder.sampleEvent(event);
    }
}
