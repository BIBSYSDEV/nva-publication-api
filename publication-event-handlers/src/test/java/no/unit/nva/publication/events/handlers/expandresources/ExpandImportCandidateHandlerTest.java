package no.unit.nva.publication.events.handlers.expandresources;

import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.objectMapper;
import static no.unit.nva.publication.events.handlers.expandresources.ExpandDataEntriesHandler.EMPTY_EVENT_TOPIC;
import static no.unit.nva.testutils.RandomDataGenerator.randomDoi;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
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
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.expansion.model.ExpandedDataEntry;
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
import no.unit.nva.publication.events.bodies.ImportCandidateDataEntryUpdate;
import no.unit.nva.publication.events.handlers.persistence.PersistedDocument;
import no.unit.nva.publication.external.services.UriRetriever;
import no.unit.nva.publication.model.business.ImportCandidate;
import no.unit.nva.publication.model.business.ImportStatus;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeS3Client;
import no.unit.nva.testutils.EventBridgeEventBuilder;
import nva.commons.core.paths.UnixPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ExpandImportCandidateHandlerTest extends ResourcesLocalTest {

    public static final Context CONTEXT = mock(Context.class);
    private ByteArrayOutputStream output;
    private ExpandImportCandidateHandler handler;
    private S3Driver s3Reader;
    private S3Driver s3Writer;

    @BeforeEach
    public void init() {
        super.init();
        this.output = new ByteArrayOutputStream();
        var eventsBucket = new FakeS3Client();
        var indexBucket = new FakeS3Client();
        s3Writer = new S3Driver(eventsBucket, "eventsBucket");
        s3Reader = new S3Driver(indexBucket, "indexBucket");

        var mockUriRetriever = mock(UriRetriever.class);
        when(mockUriRetriever.getRawContent(any(), any())).thenReturn(Optional.empty());

        this.handler = new ExpandImportCandidateHandler(s3Writer, s3Reader);
    }

    @Test
    void shouldProduceAnExpandedDataEntryWhenInputHasNewImage() throws IOException {
        var oldImage = randomImportCandidate();
        var newImage = updatedVersionOfImportCandidate(oldImage);
        var request = emulateEventEmittedByImportCandidateUpdateHandler(oldImage, newImage);
        handler.handleRequest(request, output, CONTEXT);
        var response = objectMapper.readValue(output.toString(), EventReference.class);
        var eventBlobStoredInS3 = s3Reader.readEvent(response.getUri());
        var blobObject = JsonUtils.dtoObjectMapper.readValue(eventBlobStoredInS3, PersistedDocument.class);
        assertThat(blobObject.getBody().identifyExpandedEntry(), is(equalTo(newImage.getIdentifier())));
    }

    @Test
    void shouldNotProduceAnExpandedDataEntryWhenInputHasNoNewImage() throws IOException {
        var oldImage = randomImportCandidate();
        var request = emulateEventEmittedByImportCandidateUpdateHandler(oldImage, null);
        handler.handleRequest(request, output, CONTEXT);
        var eventReference = JsonUtils.dtoObjectMapper.readValue(output.toString(), EventReference.class);
        assertThat(eventReference, is(equalTo(emptyEvent(eventReference.getTimestamp()))));
    }

    private EventReference emptyEvent(Instant timestamp) {
        return new EventReference(EMPTY_EVENT_TOPIC, null, null, timestamp);
    }

    private ImportCandidate updatedVersionOfImportCandidate(ImportCandidate oldImage) {
        oldImage.setDoi(randomDoi());
        return oldImage;
    }

    private ImportCandidate randomImportCandidate() {
        return new ImportCandidate.Builder()
                   .withImportStatus(ImportStatus.NOT_IMPORTED)
                   .withEntityDescription(randomEntityDescription())
                   .withLink(randomUri())
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
                   .withFundings(List.of(new FundingBuilder().withId(randomUri()).build()))
                   .withAdditionalIdentifiers(Set.of(new AdditionalIdentifier(randomString(), randomString())))
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

    private InputStream emulateEventEmittedByImportCandidateUpdateHandler(ImportCandidate oldImage,
                                                                          ImportCandidate newImage)
        throws IOException {
        var blobUri = createSampleBlob(oldImage, newImage);
        var event = new EventReference("ImportCandidates.Resource.Update", blobUri);
        return EventBridgeEventBuilder.sampleLambdaDestinationsEvent(event);
    }

    private URI createSampleBlob(ImportCandidate oldImage, ImportCandidate newImage) throws IOException {
        var dataEntryUpdateEvent =
            new ImportCandidateDataEntryUpdate("ImportCandidates.Resource.Update", oldImage, newImage);
        var filePath = UnixPath.of(UUID.randomUUID().toString());
        return s3Writer.insertFile(filePath, dataEntryUpdateEvent.toJsonString());
    }
}
