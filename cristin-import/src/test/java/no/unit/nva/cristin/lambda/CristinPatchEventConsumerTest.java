package no.unit.nva.cristin.lambda;

import static no.unit.nva.cristin.CristinImportConfig.eventHandlerObjectMapper;
import static no.unit.nva.cristin.lambda.CristinEntryEventConsumer.JSON;
import static no.unit.nva.cristin.lambda.CristinPatchEventConsumer.INVALID_PARENT_MESSAGE;
import static no.unit.nva.cristin.lambda.CristinPatchEventConsumer.PATCH_ERRORS_PATH;
import static no.unit.nva.cristin.lambda.CristinPatchEventConsumer.PATCH_SUCCESS;
import static no.unit.nva.cristin.lambda.constants.MappingConstants.NVA_API_DOMAIN;
import static no.unit.nva.publication.PublicationServiceConfig.PUBLICATION_PATH;
import static no.unit.nva.publication.s3imports.FileImportUtils.timestampToString;
import static no.unit.nva.publication.testing.http.RandomPersonServiceResponse.randomUri;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.StringContains.containsString;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import no.unit.nva.cristin.mapper.NvaPublicationPartOf;
import no.unit.nva.cristin.mapper.NvaPublicationPartOfCristinPublication;
import no.unit.nva.cristin.patcher.exception.NotFoundException;
import no.unit.nva.cristin.patcher.exception.ParentPublicationException;
import no.unit.nva.cristin.patcher.exception.PublicationInstanceMismatchException;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.contexttypes.Anthology;
import no.unit.nva.model.instancetypes.book.BookAnthology;
import no.unit.nva.model.instancetypes.book.BookMonograph;
import no.unit.nva.model.instancetypes.chapter.AcademicChapter;
import no.unit.nva.model.instancetypes.chapter.ChapterArticle;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.s3imports.FileContentsEvent;
import no.unit.nva.publication.s3imports.ImportResult;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeS3Client;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.paths.UnixPath;
import nva.commons.core.paths.UriWrapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CristinPatchEventConsumerTest extends ResourcesLocalTest {

    public static final Context CONTEXT = null;
    public static final String PUBLICATION_IDENTIFIER = "0189e3caa462-68be1d7f-fc6e-4aa1-97fd-38be735d9cac";
    public static final String CRISTIN_IDENTIFIER = "817503";
    public static final String S3_URI = "s3://cristin-import-750639270376/"
                                        + "PUBLICATIONS_THAT_ARE_PART_OF_OTHER_PUBLICATIONS/subset_august/"
                                        + PUBLICATION_IDENTIFIER;
    private static final String JSON_INPUT_TEMPLATE = """
        {
          "topic": "PublicationService.DataImport.DataEntry",
          "subtopic": "PublicationService.CristinData.PatchEntry",
          "fileUri": "%s",
          "timestamp": "2023-08-28T12:13:29.252151Z",
          "contents": {
            "nvapublicationidentifier": "%s",
            "partof": {
              "cristinid": "%s"
            }
          }
        }
        """;
    private static final String JSON_INPUT = String.format(JSON_INPUT_TEMPLATE, S3_URI, PUBLICATION_IDENTIFIER,
                                                           CRISTIN_IDENTIFIER);
    private FakeS3Client s3Client;
    private S3Driver s3Driver;

    private CristinPatchEventConsumer handler;

    private ResourceService resourceService;

    @BeforeEach
    public void init() {
        super.init();
        resourceService = new ResourceService(super.client, Clock.systemDefaultZone());
        s3Client = new FakeS3Client();
        s3Driver = new S3Driver(s3Client, "ignored");
        handler = new CristinPatchEventConsumer(resourceService, s3Client);
    }

    @Test
    void shouldParseFileContents() {
        var fileContents = FileContentsEvent.fromJson(JSON_INPUT, NvaPublicationPartOfCristinPublication.class);
        var contents = fileContents.getContents();
        assertThat(contents.getNvaPublicationIdentifier(), is(notNullValue()));
        assertThat(contents.getNvaPublicationIdentifier(), is(equalTo(PUBLICATION_IDENTIFIER)));
        assertThat(contents.getPartOf(), is(notNullValue()));
        assertThat(contents.getPartOf().getCristinId(), is(equalTo(CRISTIN_IDENTIFIER)));
    }

    @Test
    void shouldStoreErrorReportWhenChildPublicationCannotBeRetrieved() throws ApiGatewayException, IOException {
        var partOfCristinId = randomString();
        var childPublicationIdentifier = SortableIdentifier.next();
        createPersistedPublicationWithStatusPublishedWithSpecifiedCristinId(partOfCristinId, BookAnthology.class);
        var partOfEventReference = createPartOfEventReference(childPublicationIdentifier.toString(), partOfCristinId);
        var fileUri = s3Driver.insertFile(randomPath(), partOfEventReference);
        var eventReference = createInputEventForFile(fileUri);
        var sqsEvent = createSqsEvent(eventReference);
        handler.handleRequest(sqsEvent, CONTEXT);
        var actualReport = extractActualReportFromS3Client(eventReference,
                                                           NotFoundException.class.getSimpleName(),
                                                           childPublicationIdentifier.toString());
        assertThat(actualReport.getInput().getNvaPublicationIdentifier(),
                   is(equalTo(childPublicationIdentifier.toString())));
    }

    @Test
    void shouldStoreErrorReportWhenSearchingForNvaPublicationByCristinIdentifierReturnsMoreThanOnePublication()
        throws ApiGatewayException, IOException {
        var childPublication =
            createPersistedPublicationWithStatusPublishedWithSpecifiedCristinId(randomString(),
                                                                                ChapterArticle.class);
        var partOfCristinId = randomString();
        persistSeveralPublicationsWithTheSameCristinId(partOfCristinId);
        var partOfEventReference = createPartOfEventReference(childPublication.getIdentifier().toString(),
                                                              partOfCristinId);
        var fileUri = s3Driver.insertFile(randomPath(), partOfEventReference);
        var eventReference = createInputEventForFile(fileUri);
        var sqsEvent = createSqsEvent(eventReference);
        handler.handleRequest(sqsEvent, CONTEXT);

        var actualReport = extractActualReportFromS3Client(eventReference,
                                                           ParentPublicationException.class.getSimpleName(),
                                                           childPublication.getIdentifier().toString());
        assertThat(actualReport.getException(), containsString(INVALID_PARENT_MESSAGE));
    }

    @Test
    void shouldStoreErrorReportWhenSearchingForParentPublicationByCristinIdentifierReturnsNoPublication()
        throws ApiGatewayException, IOException {
        var childPublication =
            createPersistedPublicationWithStatusPublishedWithSpecifiedCristinId(randomString(),
                                                                                ChapterArticle.class);
        var partOfCristinId = randomString();
        var partOfEventReference = createPartOfEventReference(childPublication.getIdentifier().toString(),
                                                              partOfCristinId);
        var fileUri = s3Driver.insertFile(randomPath(), partOfEventReference);
        var eventReference = createInputEventForFile(fileUri);
        var sqsEvent = createSqsEvent(eventReference);
        handler.handleRequest(sqsEvent, CONTEXT);
        var actualReport = extractActualReportFromS3Client(eventReference,
                                                           ParentPublicationException.class.getSimpleName(),
                                                           childPublication.getIdentifier().toString());
        assertThat(actualReport.getException(), containsString(INVALID_PARENT_MESSAGE));
    }

    @Test
    void shouldSetParentPublicationIdentifierAsPartOfChildPublicationWhenSuccess() throws ApiGatewayException,
                                                                                          IOException {
        var childPublication =
            createPersistedPublicationWithStatusPublishedWithSpecifiedCristinId(randomString(),
                                                                                AcademicChapter.class);
        var partOfCristinId = randomString();
        var parentPublication =
            createPersistedPublicationWithStatusPublishedWithSpecifiedCristinId(partOfCristinId,
                                                                                BookAnthology.class);
        var expectedChildPartOfURI = createExpectedPartOfUri(parentPublication.getIdentifier());
        var partOfEventReference = createPartOfEventReference(childPublication.getIdentifier().toString(),
                                                              partOfCristinId);
        var fileUri = s3Driver.insertFile(randomPath(), partOfEventReference);
        var eventReference = createInputEventForFile(fileUri);
        var sqsEvent = createSqsEvent(eventReference);
        handler.handleRequest(sqsEvent, CONTEXT);
        var actualUpdatedChildPublication =
            resourceService.getPublicationByIdentifier(childPublication.getIdentifier());
        assertThat(actualUpdatedChildPublication.getEntityDescription().getReference().getPublicationContext(),
                   hasProperty("id", is(equalTo(expectedChildPartOfURI))));

        var actualReport = extractSuccessReportFromS3Client(eventReference, childPublication);
        assertThat(actualReport, containsString(parentPublication.getIdentifier().toString()));
    }


    @Test
    void shouldStoreErrorReportWhenParentAndChildPublicationDoesNotMatch()
        throws ApiGatewayException, IOException {
        var bookMonographChild =
            createPersistedPublicationWithStatusPublishedWithSpecifiedCristinId(randomString(),
                                                                                BookMonograph.class);
        var partOfCristinId = randomString();
        var bookMonographParent =
            createPersistedPublicationWithStatusPublishedWithSpecifiedCristinId(partOfCristinId, BookMonograph.class);
        var partOfEventReference = createPartOfEventReference(bookMonographChild.getIdentifier().toString(),
                                                              partOfCristinId);
        var fileUri = s3Driver.insertFile(randomPath(), partOfEventReference);
        var eventReference = createInputEventForFile(fileUri);
        var sqsEvent = createSqsEvent(eventReference);
        handler.handleRequest(sqsEvent, CONTEXT);
        var actualReport = extractActualReportFromS3Client(eventReference,
                                                           PublicationInstanceMismatchException.class.getSimpleName(),
                                                           bookMonographChild.getIdentifier().toString());
        assertThat(actualReport.getInput().getChildPublication(), is(Matchers.equalTo(bookMonographChild)));
        assertThat(actualReport.getInput().getPartOf().getParentPublication(),
                   is(Matchers.equalTo(bookMonographParent)));
    }

    private static void removePartOfInPublicationContext(Publication publication) {
        if (publication.getEntityDescription().getReference().getPublicationContext() instanceof Anthology) {
            publication.getEntityDescription().getReference().setPublicationContext(new Anthology.Builder().build());
        }
    }

    private SQSEvent createSqsEvent(EventReference eventReference) {
        var sqsEvent = new SQSEvent();
        var sqsMessage = new SQSMessage();
        sqsMessage.setBody(eventReference.toJsonString());
        sqsEvent.setRecords(List.of(sqsMessage));
        return sqsEvent;
    }

    private ImportResult<NvaPublicationPartOfCristinPublication> extractActualReportFromS3Client(
        EventReference eventBody,
        String exceptionName, String childPublicationIdentifier) throws JsonProcessingException {
        var errorFileUri = constructErrorFileUri(eventBody, exceptionName, childPublicationIdentifier);
        var s3Driver = new S3Driver(s3Client, errorFileUri.getUri().getHost());
        var content = s3Driver.getFile(errorFileUri.toS3bucketPath());
        return eventHandlerObjectMapper.readValue(content, new TypeReference<>() {
        });
    }

    private UriWrapper constructErrorFileUri(EventReference eventBody,
                                             String exceptionName, String childPublicationIdentifier) {

        var errorReportFilename = childPublicationIdentifier + JSON;
        var inputFile = UriWrapper.fromUri(eventBody.getUri());
        var timestamp = eventBody.getTimestamp();
        var bucket = inputFile.getHost();
        return bucket.addChild(PATCH_ERRORS_PATH)
                   .addChild(timestampToString(timestamp))
                   .addChild(exceptionName)
                   .addChild(inputFile.getPath())
                   .addChild(errorReportFilename);
    }

    private URI createExpectedPartOfUri(SortableIdentifier identifier) {
        return UriWrapper.fromUri(NVA_API_DOMAIN + PUBLICATION_PATH + "/" + identifier).getUri();
    }

    private void persistSeveralPublicationsWithTheSameCristinId(String cristinId) throws ApiGatewayException {
        createPersistedPublicationWithStatusPublishedWithSpecifiedCristinId(cristinId, BookAnthology.class);
        createPersistedPublicationWithStatusPublishedWithSpecifiedCristinId(cristinId, BookAnthology.class);
    }

    private String createPartOfEventReference(String childPublicationId, String partOfCristinId) {
        var partOf = NvaPublicationPartOfCristinPublication.builder()
                         .withNvaPublicationIdentifier(childPublicationId)
                         .withPartOf(NvaPublicationPartOf.builder().withCristinId(partOfCristinId).build())
                         .build();
        return createFileContentsEventReference(partOf);
    }

    private String createFileContentsEventReference(NvaPublicationPartOfCristinPublication partOf) {
        return new FileContentsEvent<>(randomString(),
                                       randomString(),
                                       randomUri(),
                                       Instant.now(),
                                       partOf).toJsonString();
    }

    private Publication createPersistedPublicationWithStatusPublishedWithSpecifiedCristinId(
        String cristinId,
        Class<?> publicationInstanceClass)
        throws ApiGatewayException {
        var publication = PublicationGenerator.randomPublication(publicationInstanceClass);
        publication.setAdditionalIdentifiers(createAdditionalIdentifiersWithCristinId(cristinId));
        removePartOfInPublicationContext(publication);
        var userInstance = UserInstance.fromPublication(publication);
        var publicationIdentifier = Resource.fromPublication(publication)
                                        .persistNew(resourceService, userInstance)
                                        .getIdentifier();
        return resourceService.getPublicationByIdentifier(publicationIdentifier);
    }

    private Set<AdditionalIdentifier> createAdditionalIdentifiersWithCristinId(String cristinId) {
        return Set.of(new AdditionalIdentifier("Cristin", cristinId));
    }

    private UnixPath randomPath() {
        return UnixPath.of(randomString(), randomString());
    }

    private EventReference createInputEventForFile(URI fileUri) {
        return new EventReference(randomString(), randomString(), fileUri, Instant.now());
    }

    private String extractSuccessReportFromS3Client(EventReference eventReference, Publication childPublication) {
        var successFileUri = constructSuccessFileUri(eventReference, childPublication);
        var s3Driver = new S3Driver(s3Client, successFileUri.getUri().getHost());
        return s3Driver.getFile(successFileUri.toS3bucketPath());
    }

    private UriWrapper constructSuccessFileUri(EventReference eventReference, Publication childPublication) {
        var successReportFilename = childPublication.getIdentifier() + JSON;
        var inputFile = UriWrapper.fromUri(eventReference.getUri());
        var timestamp = eventReference.getTimestamp();
        var bucket = inputFile.getHost();
        return bucket.addChild(PATCH_SUCCESS)
                   .addChild(timestampToString(timestamp))
                   .addChild(successReportFilename);
    }
}
