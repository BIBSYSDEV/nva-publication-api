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
import static org.hamcrest.CoreMatchers.hasItem;
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
import java.util.stream.Stream;
import no.unit.nva.cristin.mapper.NvaPublicationPartOf;
import no.unit.nva.cristin.mapper.NvaPublicationPartOfCristinPublication;
import no.unit.nva.cristin.patcher.exception.ChildPatchPublicationInstanceMismatchException;
import no.unit.nva.cristin.patcher.exception.NotFoundException;
import no.unit.nva.cristin.patcher.exception.ParentPatchPublicationInstanceMismatchException;
import no.unit.nva.cristin.patcher.exception.ParentPublicationException;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.contexttypes.Anthology;
import no.unit.nva.model.instancetypes.book.AcademicMonograph;
import no.unit.nva.model.instancetypes.book.BookAnthology;
import no.unit.nva.model.instancetypes.book.BookMonograph;
import no.unit.nva.model.instancetypes.book.Encyclopedia;
import no.unit.nva.model.instancetypes.book.ExhibitionCatalog;
import no.unit.nva.model.instancetypes.book.NonFictionMonograph;
import no.unit.nva.model.instancetypes.book.PopularScienceMonograph;
import no.unit.nva.model.instancetypes.book.Textbook;
import no.unit.nva.model.instancetypes.chapter.AcademicChapter;
import no.unit.nva.model.instancetypes.chapter.ChapterArticle;
import no.unit.nva.model.instancetypes.chapter.ChapterInReport;
import no.unit.nva.model.instancetypes.chapter.EncyclopediaChapter;
import no.unit.nva.model.instancetypes.chapter.Introduction;
import no.unit.nva.model.instancetypes.chapter.NonFictionChapter;
import no.unit.nva.model.instancetypes.chapter.PopularScienceChapter;
import no.unit.nva.model.instancetypes.chapter.TextbookChapter;
import no.unit.nva.model.instancetypes.degree.ConfirmedDocument;
import no.unit.nva.model.instancetypes.degree.DegreePhd;
import no.unit.nva.model.instancetypes.report.ConferenceReport;
import no.unit.nva.model.instancetypes.report.ReportPolicy;
import no.unit.nva.model.instancetypes.report.ReportResearch;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

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

    public static Stream<Arguments> childParentPublicationProvider() {
        return Stream.of(
            Arguments.of(AcademicChapter.class, BookAnthology.class),
            Arguments.of(AcademicChapter.class, NonFictionMonograph.class),
            Arguments.of(AcademicChapter.class, Textbook.class),
            Arguments.of(AcademicChapter.class, PopularScienceMonograph.class),
            Arguments.of(AcademicChapter.class, AcademicMonograph.class),
            Arguments.of(AcademicChapter.class, Encyclopedia.class),
            Arguments.of(AcademicChapter.class, ExhibitionCatalog.class),

            Arguments.of(NonFictionChapter.class, BookAnthology.class),
            Arguments.of(NonFictionChapter.class, Textbook.class),
            Arguments.of(NonFictionChapter.class, NonFictionMonograph.class),
            Arguments.of(NonFictionChapter.class, PopularScienceMonograph.class),
            Arguments.of(NonFictionChapter.class, Encyclopedia.class),
            Arguments.of(NonFictionChapter.class, ExhibitionCatalog.class),
            Arguments.of(NonFictionChapter.class, AcademicMonograph.class),

            Arguments.of(Introduction.class, BookAnthology.class),
            Arguments.of(Introduction.class, NonFictionMonograph.class),
            Arguments.of(Introduction.class, Introduction.class),
            Arguments.of(Introduction.class, Textbook.class),
            Arguments.of(Introduction.class, PopularScienceMonograph.class),
            Arguments.of(Introduction.class, AcademicMonograph.class),
            Arguments.of(Introduction.class, ExhibitionCatalog.class),
            Arguments.of(Introduction.class, Encyclopedia.class),

            Arguments.of(PopularScienceChapter.class, BookAnthology.class),
            Arguments.of(PopularScienceChapter.class, PopularScienceMonograph.class),
            Arguments.of(PopularScienceChapter.class, NonFictionMonograph.class),
            Arguments.of(PopularScienceChapter.class, Textbook.class),
            Arguments.of(PopularScienceChapter.class, ExhibitionCatalog.class),
            Arguments.of(PopularScienceChapter.class, Encyclopedia.class),
            Arguments.of(PopularScienceChapter.class, AcademicMonograph.class),

            Arguments.of(TextbookChapter.class, Textbook.class),

            Arguments.of(EncyclopediaChapter.class, Encyclopedia.class),
            Arguments.of(EncyclopediaChapter.class, BookAnthology.class),
            Arguments.of(EncyclopediaChapter.class, NonFictionMonograph.class),
            Arguments.of(EncyclopediaChapter.class, AcademicMonograph.class),
            Arguments.of(EncyclopediaChapter.class, Textbook.class),
            Arguments.of(EncyclopediaChapter.class, PopularScienceMonograph.class),
            Arguments.of(EncyclopediaChapter.class, ExhibitionCatalog.class),

            Arguments.of(ChapterInReport.class, ReportResearch.class),
            Arguments.of(ChapterInReport.class, ReportPolicy.class),
            Arguments.of(ChapterInReport.class, ConferenceReport.class),
            Arguments.of(ChapterInReport.class, ConferenceReport.class)
        );
    }

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

    @ParameterizedTest
    @MethodSource("childParentPublicationProvider")
    void shouldSetParentPublicationIdentifierAsPartOfChildPublicationWhenSuccess(Class<?> child,
                                                                                 Class<?> parent)
        throws ApiGatewayException, IOException {
        var childPublication =
            createPersistedPublicationWithStatusPublishedWithSpecifiedCristinId(randomString(),
                                                                                child);
        var partOfCristinId = randomString();
        var parentPublication =
            createPersistedPublicationWithStatusPublishedWithSpecifiedCristinId(partOfCristinId,
                                                                                parent);
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
    void shouldStoreErrorReportWhenParentAndChildPublicationDoesNotMatchWhenUpdatingChild()
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
                                                           ChildPatchPublicationInstanceMismatchException.class.getSimpleName(),
                                                           bookMonographChild.getIdentifier().toString());
        assertThat(actualReport.getInput().getChildPublication(), is(Matchers.equalTo(bookMonographChild)));
        assertThat(actualReport.getInput().getPartOf().getParentPublication(),
                   is(Matchers.equalTo(bookMonographParent)));
    }

    @Test
    void shouldAddChildPublicationIdentifierAsRelatedDocumentForParentPublicationWhenSuccess() throws ApiGatewayException,
                                                                                          IOException {
        var childPublication =
            createPersistedPublicationWithStatusPublishedWithSpecifiedCristinId(randomString(),
                                                                                NonFictionMonograph.class);
        var partOfCristinId = randomString();
        var parentPublication =
            createPersistedPublicationWithStatusPublishedWithSpecifiedCristinId(partOfCristinId,
                                                                                DegreePhd.class);
        var expectedRelatedDocumentId = createExpectedPartOfUri(childPublication.getIdentifier());
        var partOfEventReference = createPartOfEventReference(childPublication.getIdentifier().toString(),
                                                              partOfCristinId);
        var fileUri = s3Driver.insertFile(randomPath(), partOfEventReference);
        var eventReference = createInputEventForFile(fileUri);
        var sqsEvent = createSqsEvent(eventReference);
        handler.handleRequest(sqsEvent, CONTEXT);
        var actualUpdatedParentPublication =
            resourceService.getPublicationByIdentifier(parentPublication.getIdentifier());
        var updatedRelatedDocuments = ((DegreePhd) actualUpdatedParentPublication.getEntityDescription()
                                                    .getReference()
                                                    .getPublicationInstance()).getRelated();
        assertThat(updatedRelatedDocuments, hasItem(new ConfirmedDocument(expectedRelatedDocumentId)));

        var actualReport = extractSuccessReportFromS3Client(eventReference, parentPublication);
        assertThat(actualReport, containsString(parentPublication.getIdentifier().toString()));
    }

    @Test
    void shouldStoreErrorReportWhenParentAndChildPublicationDoesNotMatchWhenUpdatingParent()
        throws ApiGatewayException, IOException {
        var child =
            createPersistedPublicationWithStatusPublishedWithSpecifiedCristinId(randomString(), Textbook.class);
        var partOfCristinId = randomString();
        var parent =
            createPersistedPublicationWithStatusPublishedWithSpecifiedCristinId(partOfCristinId, DegreePhd.class);
        var partOfEventReference = createPartOfEventReference(child.getIdentifier().toString(),
                                                              partOfCristinId);
        var fileUri = s3Driver.insertFile(randomPath(), partOfEventReference);
        var eventReference = createInputEventForFile(fileUri);
        var sqsEvent = createSqsEvent(eventReference);
        handler.handleRequest(sqsEvent, CONTEXT);
        var childPatchReport = extractActualReportFromS3Client(eventReference,
                                                           ChildPatchPublicationInstanceMismatchException.class.getSimpleName(),
                                                           child.getIdentifier().toString());
        var parentPatchReport = extractActualReportFromS3Client(eventReference,
                                                               ParentPatchPublicationInstanceMismatchException.class.getSimpleName(),
                                                               child.getIdentifier().toString());
        assertThat(childPatchReport.getInput().getChildPublication(), is(Matchers.equalTo(child)));
        assertThat(parentPatchReport.getInput().getChildPublication(), is(Matchers.equalTo(child)));
        assertThat(childPatchReport.getInput().getPartOf().getParentPublication(),
                   is(Matchers.equalTo(parent)));
        assertThat(parentPatchReport.getInput().getPartOf().getParentPublication(),
                   is(Matchers.equalTo(parent)));
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
