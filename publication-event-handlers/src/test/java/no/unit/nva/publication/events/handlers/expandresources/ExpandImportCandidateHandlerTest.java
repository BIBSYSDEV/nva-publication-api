package no.unit.nva.publication.events.handlers.expandresources;

import static no.unit.nva.model.testing.ImportCandidateGenerator.randomImportCandidate;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomAssociatedArtifacts;
import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.objectMapper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import no.unit.nva.auth.uriretriever.UriRetriever;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.expansion.model.cristin.CristinOrganization;
import no.unit.nva.importcandidate.ImportCandidate;
import no.unit.nva.importcandidate.ImportContributor;
import no.unit.nva.importcandidate.Affiliation;
import no.unit.nva.model.Organization;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.publication.events.bodies.ImportCandidateDataEntryUpdate;
import no.unit.nva.publication.events.handlers.persistence.PersistedDocument;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeS3Client;
import no.unit.nva.testutils.EventBridgeEventBuilder;
import nva.commons.core.paths.UnixPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ExpandImportCandidateHandlerTest extends ResourcesLocalTest {

    private static final String EMPTY_EVENT_TOPIC = "Event.Empty";
    public static final Context CONTEXT = null;
    private ByteArrayOutputStream output;
    private ExpandImportCandidateHandler handler;
    private S3Driver s3Reader;
    private S3Driver s3Writer;
    private UriRetriever uriRetriever;

    @BeforeEach
    public void init() {
        super.init();
        this.output = new ByteArrayOutputStream();
        var eventsBucket = new FakeS3Client();
        var indexBucket = new FakeS3Client();
        s3Writer = new S3Driver(eventsBucket, "eventsBucket");
        s3Reader = new S3Driver(indexBucket, "indexBucket");
        uriRetriever = mock(UriRetriever.class);
        this.handler = new ExpandImportCandidateHandler(s3Writer, s3Reader, uriRetriever);
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
    void shouldNotProduceExpandedDataEntryWhenPublicationDateIsOlderThan2018() throws IOException {
        var oldImage = randomImportCandidate();
        var newImage = updatedVersionOfImportCandidateWithPublicationDate(oldImage);
        var request = emulateEventEmittedByImportCandidateUpdateHandler(oldImage, newImage);
        handler.handleRequest(request, output, CONTEXT);
        var eventReference = JsonUtils.dtoObjectMapper.readValue(output.toString(), EventReference.class);
        assertThat(eventReference, is(equalTo(emptyEvent(eventReference.getTimestamp()))));
    }

    @Test
    void shouldNotProduceAnExpandedDataEntryWhenInputHasNoNewImage() throws IOException {
        var oldImage = randomImportCandidate();
        var request = emulateEventEmittedByImportCandidateUpdateHandler(oldImage, null);
        handler.handleRequest(request, output, CONTEXT);
        var eventReference = JsonUtils.dtoObjectMapper.readValue(output.toString(), EventReference.class);
        assertThat(eventReference, is(equalTo(emptyEvent(eventReference.getTimestamp()))));
    }

    @Test
    void shouldProduceExpandedImportCandidateWithCollaboration() throws IOException {
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
    void shouldProduceExpandedImportCandidateWithExpandedOrganization() throws IOException {
        var oldImage = randomImportCandidate();
        var newImage = updatedVersionOfImportCandidate(oldImage);
        var request = emulateEventEmittedByImportCandidateUpdateHandler(oldImage, newImage);
        newImage.getEntityDescription().contributors().stream()
            .map(ImportContributor::affiliations)
            .flatMap(Collection::stream)
            .map(Affiliation::targetOrganization)
            .filter(Organization.class::isInstance)
            .map(Organization.class::cast)
            .forEach(this::mockOrganizations);

        handler.handleRequest(request, output, CONTEXT);
        var response = objectMapper.readValue(output.toString(), EventReference.class);
        var eventBlobStoredInS3 = s3Reader.readEvent(response.getUri());

        assertThatLabelsOfExpandedOrganizationsAreNotEmpty(eventBlobStoredInS3);
    }

    private static void assertThatLabelsOfExpandedOrganizationsAreNotEmpty(String eventBlobStoredInS3)
        throws JsonProcessingException {
        var rootNode = objectMapper.readTree(eventBlobStoredInS3);
        var organizations = rootNode.path("body").path("organizations");
        for (JsonNode organization : organizations) {
            var labels = organization.path("labels");
            assertFalse(labels.isEmpty());
        }
    }

    private void mockOrganizations(Organization org) {
        when(uriRetriever.getRawContent(org.getId(), "application/json"))
            .thenReturn(Optional.of(new CristinOrganization(org.getId(), null, null, null, null,
                                                            Map.of("no", "label")).toJsonString()));
    }

    private ImportCandidate updatedVersionOfImportCandidateWithPublicationDate(ImportCandidate importCandidate) {
        importCandidate.getEntityDescription().publicationDate().setYear("2015");
        return importCandidate;
    }

    private EventReference emptyEvent(Instant timestamp) {
        return new EventReference(EMPTY_EVENT_TOPIC, null, null, timestamp);
    }

    private ImportCandidate updatedVersionOfImportCandidate(ImportCandidate oldImage) {
        oldImage.setAssociatedArtifacts(new AssociatedArtifactList(randomAssociatedArtifacts()));
        return oldImage;
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
            new ImportCandidateDataEntryUpdate("ImportCandidates.Resource.Update",
                                               oldImage,
                                               newImage);
        var filePath = UnixPath.of(UUID.randomUUID().toString());
        return s3Writer.insertFile(filePath, dataEntryUpdateEvent.toJsonString());
    }
}
