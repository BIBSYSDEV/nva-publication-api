package no.unit.nva.publication.events.handlers.delete;

import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.EVENTS_BUCKET;
import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.objectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.amazonaws.services.lambda.runtime.Context;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Organization;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.ResearchProject;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
import no.unit.nva.model.additionalidentifiers.AdditionalIdentifier;
import no.unit.nva.model.funding.FundingBuilder;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;
import no.unit.nva.publication.events.bodies.DeleteImportCandidateEvent;
import no.unit.nva.publication.events.bodies.ImportCandidateDataEntryUpdate;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.importcandidate.ImportCandidate;
import no.unit.nva.publication.model.business.importcandidate.ImportStatusFactory;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeS3Client;
import no.unit.nva.testutils.EventBridgeEventBuilder;
import nva.commons.core.paths.UnixPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;

public class DeleteImportCandidateEventHandlerTest {

    public static final Context CONTEXT = null;
    private ByteArrayOutputStream output;
    private S3Client s3Client;
    private DeleteImportCandidateEventHandler handler;

    @BeforeEach
    public void init() {
        output = new ByteArrayOutputStream();
        s3Client = new FakeS3Client();
        this.handler = new DeleteImportCandidateEventHandler(s3Client);
    }

    @Test
    void shouldProduceAnExpandedDataEntryDeleteEvent() throws IOException {
        var oldImage = randomImportCandidate();
        var request = emulateEventEmittedByImportCandidateUpdateHandler(oldImage, null);
        handler.handleRequest(request, output, CONTEXT);
        var response = objectMapper.readValue(output.toString(), DeleteImportCandidateEvent.class);
        assertThat(oldImage.getIdentifier(), is(equalTo(response.getIdentifier())));
    }

    @Test
    void shouldTrowExceptionWhenBlobToDeleteIsEmpty() throws IOException {
        var request = emulateEventEmittedByImportCandidateUpdateHandler(null, null);

        assertThrows(IllegalStateException.class, () -> handler.handleRequest(request, output, CONTEXT));
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
                                               Resource.fromImportCandidate(oldImage),
                                               Resource.fromImportCandidate(newImage));
        var filePath = UnixPath.of(UUID.randomUUID().toString());
        var s3Writer = new S3Driver(s3Client, EVENTS_BUCKET);
        return s3Writer.insertFile(filePath, dataEntryUpdateEvent.toJsonString());
    }

    private ImportCandidate randomImportCandidate() {
        return new ImportCandidate.Builder()
                   .withImportStatus(ImportStatusFactory.createNotImported())
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
                   .withFundings(Set.of(new FundingBuilder().withId(randomUri()).build()))
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
}
