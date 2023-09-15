package no.unit.nva.publication.events.handlers.delete;

import static no.unit.nva.publication.create.CreatePublicationFromImportCandidateHandler.SCOPUS_IDENTIFIER;
import static no.unit.nva.publication.service.impl.ResourceService.IMPORT_CANDIDATE_HAS_BEEN_DELETED_MESSAGE;
import static no.unit.nva.testutils.RandomDataGenerator.randomDoi;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.expansion.model.ExpandedImportCandidate;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Organization;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.ResearchProject;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
import no.unit.nva.model.funding.FundingBuilder;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;
import no.unit.nva.publication.events.bodies.ImportCandidateDeleteEvent;
import no.unit.nva.publication.external.services.UriRetriever;
import no.unit.nva.publication.model.business.importcandidate.ImportCandidate;
import no.unit.nva.publication.model.business.importcandidate.ImportStatusFactory;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeS3Client;
import no.unit.nva.testutils.EventBridgeEventBuilder;
import no.unit.nva.testutils.RandomDataGenerator;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.paths.UnixPath;
import nva.commons.logutils.LogUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DeleteImportCandidateEventConsumerTest extends ResourcesLocalTest {

    public static final Context CONTEXT = null;
    public static final int TWO_HITS = 2;
    public static final int SINGLE_HIT = 1;
    public static final int ZERO_HITS = 0;
    private ResourceService resourceService;
    private S3Driver s3Driver;
    private ByteArrayOutputStream output;
    private DeleteImportCandidateEventConsumer handler;
    private UriRetriever uriRetriever;

    @BeforeEach
    public void init() {
        super.init("import-candidates");
        this.output = new ByteArrayOutputStream();
        var eventsBucket = new FakeS3Client();
        uriRetriever = mock(UriRetriever.class);
        s3Driver = new S3Driver(eventsBucket, "eventsBucket");
        resourceService = new ResourceService(client, "import-candidates");
        this.handler = new DeleteImportCandidateEventConsumer(resourceService, uriRetriever);
    }

    @Test
    void shouldDeleteImportCandidateSuccessfully() throws NotFoundException, IOException {
        var appender = LogUtils.getTestingAppenderForRootLogger();
        var importCandidate = persistedImportCandidate();
        var event = emulateEventEmittedByImportCandidateUpdateHandler(getScopusIdentifier(importCandidate));
        when(uriRetriever.getRawContent(any(), any())).thenReturn(
            toResponse(ExpandedImportCandidate.fromImportCandidate(importCandidate, null),
                       SINGLE_HIT));
        handler.handleRequest(event, output, CONTEXT);
        assertThrows(NotFoundException.class,
                     () -> resourceService.getImportCandidateByIdentifier(importCandidate.getIdentifier()));
        assertThat(appender.getMessages(), containsString(IMPORT_CANDIDATE_HAS_BEEN_DELETED_MESSAGE));
    }

    @Test
    void shouldThrowBadGatewayExceptionWhenMultipleHitsInResponseFetchingUniqueImportCandidate()
        throws NotFoundException, IOException {
        var importCandidate = persistedImportCandidate();
        var event = emulateEventEmittedByImportCandidateUpdateHandler(getScopusIdentifier(importCandidate));
        when(uriRetriever.getRawContent(any(), any())).thenReturn(
            toResponse(ExpandedImportCandidate.fromImportCandidate(importCandidate, null),
                       TWO_HITS));
        assertThrows(RuntimeException.class, () -> handler.handleRequest(event, output, CONTEXT));
    }

    @Test
    void shouldThrowExceptionWhenEmptyZeroHitsInResponseFetchingUniqueImportCandidate()
        throws NotFoundException, IOException {
        var importCandidate = persistedImportCandidate();
        var event = emulateEventEmittedByImportCandidateUpdateHandler(getScopusIdentifier(importCandidate));
        when(uriRetriever.getRawContent(any(), any())).thenReturn(emptyResponse());
        assertThrows(RuntimeException.class, () -> handler.handleRequest(event, output, CONTEXT));
    }

    private static Optional<String> toResponse(ExpandedImportCandidate importCandidate, int hits) {
        return Optional.of(new ImportCandidateSearchApiResponse(List.of(importCandidate), hits).toString());
    }

    private static Optional<String> emptyResponse() {
        return Optional.of(new ImportCandidateSearchApiResponse(List.of(), ZERO_HITS).toString());
    }

    private static boolean isScopus(AdditionalIdentifier identifier) {
        return identifier.getSourceName().equals(SCOPUS_IDENTIFIER);
    }

    private String getScopusIdentifier(ImportCandidate importCandidate) {
        return importCandidate.getAdditionalIdentifiers().stream()
                   .filter(DeleteImportCandidateEventConsumerTest::isScopus)
                   .map(AdditionalIdentifier::getValue)
                   .findFirst()
                   .orElseGet(RandomDataGenerator::randomString);
    }

    private ImportCandidate persistedImportCandidate() throws NotFoundException {
        var importCandidate = resourceService.persistImportCandidate(createImportCandidate());
        return resourceService.getImportCandidateByIdentifier(importCandidate.getIdentifier());
    }

    private InputStream emulateEventEmittedByImportCandidateUpdateHandler(String scopusIdentifier)
        throws IOException {
        var blobUri = createSampleBlob(scopusIdentifier);
        var event = new EventReference("ImportCandidates.Resource.Update", blobUri);
        return EventBridgeEventBuilder.sampleLambdaDestinationsEvent(event);
    }

    private URI createSampleBlob(String scopusIdentifier) throws IOException {
        var event = new ImportCandidateDeleteEvent("ImportCandidates.Scopus.Delete", scopusIdentifier);
        var filePath = UnixPath.of(UUID.randomUUID().toString());
        return s3Driver.insertFile(filePath, event.toJsonString());
    }

    private ImportCandidate createImportCandidate() {
        return new ImportCandidate.Builder()
                   .withImportStatus(ImportStatusFactory.createNotImported())
                   .withEntityDescription(randomEntityDescription())
                   .withLink(randomUri())
                   .withDoi(randomDoi())
                   .withIndexedDate(Instant.now())
                   .withPublishedDate(Instant.now())
                   .withHandle(randomUri())
                   .withModifiedDate(Instant.now())
                   .withCreatedDate(Instant.now())
                   .withPublisher(new Organization.Builder().withId(randomUri()).build())
                   .withSubjects(List.of(randomUri()))
                   .withIdentifier(SortableIdentifier.next())
                   .withRightsHolder(randomString())
                   .withProjects(List.of(new ResearchProject.Builder().withId(randomUri()).build()))
                   .withFundings(List.of(new FundingBuilder().build()))
                   .withAdditionalIdentifiers(Set.of(new AdditionalIdentifier(SCOPUS_IDENTIFIER, randomString())))
                   .withResourceOwner(new ResourceOwner(new Username(randomString()), randomUri()))
                   .withAssociatedArtifacts(List.of())
                   .build();
    }

    private EntityDescription randomEntityDescription() {
        return new EntityDescription.Builder()
                   .withPublicationDate(new PublicationDate.Builder().withYear("2020").build())
                   .withAbstract(randomString())
                   .withDescription(randomString())
                   .withContributors(List.of(randomContributor()))
                   .withMainTitle(randomString())
                   .build();
    }

    private Contributor randomContributor() {
        return new Contributor.Builder()
                   .withIdentity(new Identity.Builder().withName(randomString()).build())
                   .withRole(new RoleType(Role.ACTOR))
                   .build();
    }
}
