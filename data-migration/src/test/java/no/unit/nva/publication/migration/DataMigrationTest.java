package no.unit.nva.publication.migration;

import static java.util.Objects.nonNull;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.extractOwner;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.DoiRequestMessage;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.service.impl.DoiRequestService;
import no.unit.nva.publication.service.impl.MessageService;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.storage.model.DoiRequest;
import no.unit.nva.publication.storage.model.Message;
import no.unit.nva.publication.storage.model.MessageStatus;
import no.unit.nva.publication.storage.model.UserInstance;
import no.unit.nva.s3.S3Driver;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.attempt.Try;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;

public class DataMigrationTest extends AbstractDataMigrationTest {

    public static final String EXPECTED_ERROR_MESSAGE = "expectedMessage";
    public static final String ISO_TIME_REGEX = "^.*[\\d]{4}-[\\d]{2}-[\\d]{2}T[\\d]{2}:[\\d]{2}:[\\d]{2}.*$";
    public static final String CURRENT_EXECUTIION_FOLDER_SYSTEM_PROPERTY = "user.dir";
    private static final Path EXISTING_DATA_PATH = Path.of("AWSDynamoDB",
        "01614701350715-b7f02d9e", "data/");
    private static final String EXISTING_REMOTE_BUCKET_NAME = "orestis-export";
    private static final Set<Publication> EXPECTED_IMPORTED_PUBLICATIONS_WITH_DOI_REQUESTS =
        constructExpectedPublicationsWithDoiRequests();
    private ResourceService resourceService;
    private FakeS3Driver fakeS3Client;
    private S3Driver remoteS3Client;
    private DoiRequestService doiRequestService;
    private MessageService messageService;
    private DataMigration dataMigration;

    @BeforeEach
    public void init() {
        super.init();
        AmazonDynamoDB dynamoClient = super.client;
        remoteS3Client = remoteS3Driver();
        fakeS3Client = new FakeS3Driver();
        resourceService = new ResourceService(dynamoClient, Clock.systemDefaultZone());
        doiRequestService = new DoiRequestService(dynamoClient, Clock.systemDefaultZone());
        messageService = new MessageService(dynamoClient, Clock.systemDefaultZone());
        dataMigration = newDataMigration(fakeS3Client);
    }

    @AfterEach
    public void deleteReportFiles() {
        File executionFolder = new File(System.getProperty(CURRENT_EXECUTIION_FOLDER_SYSTEM_PROPERTY));
        Arrays.stream(Objects.requireNonNull(executionFolder.listFiles()))
            .filter(f -> f.getName().matches(ISO_TIME_REGEX))
            .forEach(File::delete);
    }

    //TODO: Create a standalone running module: (NP-2297)
    @Test
    @Tag("RemoteTest")
    public void performMigration() throws IOException {
        var dataMigration = newDataMigration(remoteS3Client);
        List<ResourceUpdate> update = dataMigration.migrateData();
        assertThat(update, is(not(empty())));
    }


    @Test
    public void migrateDataInsertsPublicationsToDynamoDb() throws IOException {
        dataMigration.migrateData();
        Set<SortableIdentifier> expectedIdentifiers = testDataPublicationUniqueIdentifiers();

        List<Publication> savedPublications = fetchPublicationsFromDb(expectedIdentifiers);
        Set<SortableIdentifier> savedPublicationsIdentifiers = extractIdentifiers(savedPublications.stream());

        assertThat(savedPublicationsIdentifiers, is(equalTo(expectedIdentifiers)));
        assertThat(savedPublications.size(), is(equalTo(expectedIdentifiers.size())));
    }

    @Test
    public void migrateDataInsertsDoiRequestsToDynamoDb() throws IOException {
        dataMigration.migrateData();

        Stream<Publication> savedDoiRequests = fetchDoiRequestsFromService()
                                                   .stream()
                                                   .map(DoiRequest::toPublication);

        Set<SortableIdentifier> actualDoiRequestsResourceIdentifiers = extractIdentifiers(savedDoiRequests);

        Set<SortableIdentifier> expectedDoiRequestsResourceIdentifiers =
            extractIdentifiers(EXPECTED_IMPORTED_PUBLICATIONS_WITH_DOI_REQUESTS.stream());
        assertThat(actualDoiRequestsResourceIdentifiers, is(equalTo(expectedDoiRequestsResourceIdentifiers)));
    }

    @Test
    public void migrateDataInsertsMessagesToDynamoDb() throws IOException {
        dataMigration.migrateData();

        List<String> savedMessages = extractSavedMessages();
        String[] expectedMessages = extractExpectedMessages().toArray(new String[0]);

        assertThat(savedMessages, containsInAnyOrder(expectedMessages));
    }

    @Test
    public void migrateDataReportsFailureWhenThereIsNoVerificationThatPublicationWasInsertedSuccessfully()
        throws IOException, NotFoundException {
        ResourceService spiedResourceService = spy(resourceService);
        doThrow(new NotFoundException(EXPECTED_ERROR_MESSAGE))
            .when(spiedResourceService)
            .getPublication(any(Publication.class));

        dataMigration = new DataMigration(fakeS3Client,
            EXISTING_DATA_PATH,
            spiedResourceService,
            doiRequestService,
            messageService);

        assertThatUpdatesContainExpectedMessages();
    }

    @Test
    public void migrateDataReportsFailureWhenThereIsNoVerificationThatMessageWasInsertedSuccessfully()
        throws IOException, NotFoundException {
        MessageService messageServiceThrowingError = spy(messageService);
        doThrow(new NotFoundException(EXPECTED_ERROR_MESSAGE))
            .when(messageServiceThrowingError)
            .getMessage(any(UserInstance.class), any(SortableIdentifier.class));

        dataMigration = new DataMigration(fakeS3Client, EXISTING_DATA_PATH, resourceService, doiRequestService,
            messageServiceThrowingError);

        assertThatUpdatesContainExpectedMessages();
    }

    public void assertThatUpdatesContainExpectedMessages() throws IOException {
        List<ResourceUpdate> resourcesUpdates = dataMigration.migrateData();
        List<String> failureMessages = resourcesUpdates.stream().filter(ResourceUpdate::isFailure)
                                           .map(ResourceUpdate::getExceptionString)
                                           .collect(Collectors.toList());

        assertThat(failureMessages, is(not(empty())));

        for (String failureMessage : failureMessages) {
            assertThat(failureMessage, containsString(EXPECTED_ERROR_MESSAGE));
        }
    }

    @Test
    public void migrateDataReportsFailureWhenThereIsNoVerificationThatDoiRequestWasInsertedSuccessfully()
        throws IOException, NotFoundException {
        DoiRequestService spiedDoiRequestService = spy(doiRequestService);
        doThrow(new NotFoundException(EXPECTED_ERROR_MESSAGE))
            .when(spiedDoiRequestService)
            .getDoiRequest(any(UserInstance.class), any(SortableIdentifier.class));

        dataMigration = new DataMigration(fakeS3Client, EXISTING_DATA_PATH, resourceService, spiedDoiRequestService,
            messageService);

        assertThatUpdatesContainExpectedMessages();
    }

    private static Set<Publication> constructExpectedPublicationsWithDoiRequests() {
        return constructExpectedPublications().stream()
                   .filter(p -> nonNull(p.getDoiRequest()))
                   .collect(Collectors.toSet());
    }

    private List<String> extractSavedMessages() {
        return fetchMessagesFromService()
                   .stream()
                   .map(Message::getText)
                   .collect(Collectors.toList());
    }

    private List<String> extractExpectedMessages() {
        return withoutDuplicates(FakeS3Driver.PUBLICATIONS_WITH_DOI_REQUESTS.stream())
                   .stream()
                   .map(Publication::getDoiRequest)
                   .flatMap(doiRequest -> doiRequest.getMessages().stream())
                   .map(DoiRequestMessage::getText)
                   .collect(Collectors.toList());
    }

    private List<Message> fetchMessagesFromService() {
        List<Message> unreadMessages = messageService.listMessages(FakeS3Driver.PUBLISHER_URI, MessageStatus.UNREAD);
        List<Message> readMessages = messageService.listMessages(FakeS3Driver.PUBLISHER_URI, MessageStatus.READ);
        List<Message> allMessages = new ArrayList<>();
        allMessages.addAll(unreadMessages);
        allMessages.addAll(readMessages);
        return allMessages;
    }

    private Set<SortableIdentifier> extractIdentifiers(Stream<Publication> stream) {
        return stream
                   .map(Publication::getIdentifier)
                   .collect(Collectors.toSet());
    }

    private List<DoiRequest> fetchDoiRequestsFromService() {
        return EXPECTED_IMPORTED_PUBLICATIONS_WITH_DOI_REQUESTS
                   .stream()
                   .map(this::fetchDoiRequest).collect(Collectors.toList());
    }

    private DoiRequest fetchDoiRequest(Publication pub) {
        return attempt(() -> fetchDoiRequestFromDb(pub)).orElseThrow();
    }

    private DoiRequest fetchDoiRequestFromDb(Publication pub) throws NotFoundException {
        UserInstance resourceOwner = extractOwner(pub);

        return doiRequestService.getDoiRequestByResourceIdentifier(resourceOwner, pub.getIdentifier());
    }

    private List<Publication> fetchPublicationsFromDb(Set<SortableIdentifier> expectedIdentifiers) {
        return expectedIdentifiers
                   .stream()
                   .map(attempt(id -> resourceService.getPublicationByIdentifier(id)))
                   .map(Try::orElseThrow)
                   .collect(Collectors.toList());
    }

    private DataMigration newDataMigration(S3Driver s3Client) {
        return new DataMigration(s3Client, EXISTING_DATA_PATH, resourceService,
            doiRequestService, messageService);
    }

    private Set<SortableIdentifier> testDataPublicationUniqueIdentifiers() {
        return extractIdentifiers(FakeS3Driver.allSamplePublications()
                                      .stream());
    }

    private List<Publication> fetchAllDoiRequestsDirectlyFromDb(List<ResourceUpdate> update) {
        return update.stream()
                   .filter(ResourceUpdate::isSuccess)
                   .flatMap(this::fetchDoiRequestFromService)
                   .map(DoiRequest::toPublication)
                   .collect(Collectors.toList());
    }

    private Stream<DoiRequest> fetchDoiRequestFromService(ResourceUpdate doiRequest) {
        var owner = extractOwner(doiRequest.getNewVersion());
        var publicationIdentifier = doiRequest.getNewVersion().getIdentifier();
        return attempt(() -> fetchDoiRequestByResourceIdentifier(owner, publicationIdentifier)).stream();
    }

    private DoiRequest fetchDoiRequestByResourceIdentifier(UserInstance owner, SortableIdentifier publicationIdentifier)
        throws NotFoundException {
        return doiRequestService.getDoiRequestByResourceIdentifier(owner, publicationIdentifier);
    }

    private S3Driver remoteS3Driver() {
        return attempt(() -> new S3Driver(S3Client.create(), EXISTING_REMOTE_BUCKET_NAME))
                   .orElse(fail -> null);
    }
}