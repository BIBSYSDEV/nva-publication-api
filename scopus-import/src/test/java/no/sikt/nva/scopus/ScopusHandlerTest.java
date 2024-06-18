package no.sikt.nva.scopus;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.util.Objects.nonNull;
import static no.sikt.nva.brage.migration.lambda.BrageEntryEventConsumer.ERROR_BUCKET_PATH;
import static no.sikt.nva.brage.migration.lambda.BrageEntryEventConsumer.PATH_SEPERATOR;
import static no.sikt.nva.brage.migration.lambda.BrageEntryEventConsumer.YYYY_MM_DD_HH_FORMAT;
import static no.sikt.nva.scopus.ScopusConstants.ISSN_TYPE_ELECTRONIC;
import static no.sikt.nva.scopus.ScopusConstants.ISSN_TYPE_PRINT;
import static no.sikt.nva.scopus.ScopusConstants.ORCID_DOMAIN_URL;
import static no.sikt.nva.scopus.ScopusConstants.SCOPUS_IDENTIFIER;
import static no.sikt.nva.scopus.ScopusHandler.SCOPUS_IMPORT_BUCKET;
import static no.sikt.nva.scopus.ScopusHandler.SUCCESS_BUCKET_PATH;
import static no.sikt.nva.scopus.conversion.PiaConnection.API_HOST;
import static no.sikt.nva.scopus.conversion.PiaConnection.PIA_PASSWORD_KEY;
import static no.sikt.nva.scopus.conversion.PiaConnection.PIA_REST_API_ENV_KEY;
import static no.sikt.nva.scopus.conversion.PiaConnection.PIA_SECRETS_NAME_ENV_KEY;
import static no.sikt.nva.scopus.conversion.PiaConnection.PIA_USERNAME_KEY;
import static no.sikt.nva.scopus.conversion.PublicationContextCreator.UNSUPPORTED_SOURCE_TYPE;
import static no.sikt.nva.scopus.conversion.files.ScopusFileConverter.CROSSREF_URI_ENV_VAR_NAME;
import static no.sikt.nva.scopus.conversion.files.model.ContentVersion.VOR;
import static no.sikt.nva.scopus.utils.ScopusGenerator.createWithOneAuthorGroupAndAffiliation;
import static no.sikt.nva.scopus.utils.ScopusGenerator.randomYear;
import static no.unit.nva.language.LanguageConstants.BOKMAAL;
import static no.unit.nva.language.LanguageConstants.ENGLISH;
import static no.unit.nva.language.LanguageConstants.MISCELLANEOUS;
import static no.unit.nva.language.LanguageConstants.MULTIPLE;
import static no.unit.nva.language.LanguageConstants.NORWEGIAN;
import static no.unit.nva.language.LanguageConstants.UNDEFINED_LANGUAGE;
import static no.unit.nva.testutils.RandomDataGenerator.randomDoi;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomIsbn13;
import static no.unit.nva.testutils.RandomDataGenerator.randomIssn;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.core.StringUtils.isNotBlank;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalToObject;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.RequestParametersEntity;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.ResponseElementsEntity;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3BucketEntity;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3Entity;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3EventNotificationRecord;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3ObjectEntity;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.UserIdentityEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import jakarta.xml.bind.JAXBElement;
import java.io.IOException;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.xml.namespace.QName;
import no.scopus.generated.AffiliationTp;
import no.scopus.generated.AuthorGroupTp;
import no.scopus.generated.AuthorTp;
import no.scopus.generated.BibrecordTp;
import no.scopus.generated.CitationTitleTp;
import no.scopus.generated.CitationtypeAtt;
import no.scopus.generated.CollaborationTp;
import no.scopus.generated.DocTp;
import no.scopus.generated.HeadTp;
import no.scopus.generated.InfTp;
import no.scopus.generated.ItemTp;
import no.scopus.generated.OpenAccessType;
import no.scopus.generated.OrganizationTp;
import no.scopus.generated.OrigItemTp;
import no.scopus.generated.SourcetypeAtt;
import no.scopus.generated.SupTp;
import no.scopus.generated.TitletextTp;
import no.scopus.generated.UpwOaLocationType;
import no.scopus.generated.UpwOaLocationsType;
import no.scopus.generated.UpwOpenAccessType;
import no.scopus.generated.YesnoAtt;
import no.sikt.nva.scopus.conversion.ContributorExtractor;
import no.sikt.nva.scopus.conversion.CristinConnection;
import no.sikt.nva.scopus.conversion.NvaCustomerConnection;
import no.sikt.nva.scopus.conversion.PiaConnection;
import no.sikt.nva.scopus.conversion.PublicationChannelConnection;
import no.sikt.nva.scopus.conversion.PublicationInstanceCreator;
import no.sikt.nva.scopus.conversion.files.ScopusFileConverter;
import no.sikt.nva.scopus.conversion.files.TikaUtils;
import no.sikt.nva.scopus.conversion.files.model.CrossrefResponse;
import no.sikt.nva.scopus.conversion.files.model.CrossrefResponse.CrossrefLink;
import no.sikt.nva.scopus.conversion.files.model.CrossrefResponse.License;
import no.sikt.nva.scopus.conversion.files.model.CrossrefResponse.Message;
import no.sikt.nva.scopus.conversion.files.model.CrossrefResponse.Primary;
import no.sikt.nva.scopus.conversion.files.model.CrossrefResponse.Resource;
import no.sikt.nva.scopus.conversion.files.model.CrossrefResponse.Start;
import no.sikt.nva.scopus.conversion.model.ImportCandidateSearchApiResponse;
import no.sikt.nva.scopus.conversion.model.PublicationChannelResponse;
import no.sikt.nva.scopus.conversion.model.PublicationChannelResponse.PublicationChannelHit;
import no.sikt.nva.scopus.conversion.model.cristin.Affiliation;
import no.sikt.nva.scopus.conversion.model.cristin.CristinPerson;
import no.sikt.nva.scopus.conversion.model.cristin.TypedValue;
import no.sikt.nva.scopus.conversion.model.pia.Author;
import no.sikt.nva.scopus.exception.UnsupportedCitationTypeException;
import no.sikt.nva.scopus.exception.UnsupportedSrcTypeException;
import no.sikt.nva.scopus.update.ScopusUpdater;
import no.sikt.nva.scopus.utils.ContentWrapper;
import no.sikt.nva.scopus.utils.CristinGenerator;
import no.sikt.nva.scopus.utils.LanguagesWrapper;
import no.sikt.nva.scopus.utils.PiaResponseGenerator;
import no.sikt.nva.scopus.utils.ScopusGenerator;
import no.unit.nva.auth.uriretriever.AuthorizedBackendUriRetriever;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.doi.models.Doi;
import no.unit.nva.expansion.model.ExpandedImportCandidate;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.language.Language;
import no.unit.nva.language.LanguageConstants;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.ContributorVerificationStatus;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Identity;
import no.unit.nva.model.ImportSource;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.ResearchProject;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.contexttypes.Anthology;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.contexttypes.Journal;
import no.unit.nva.model.contexttypes.Publisher;
import no.unit.nva.model.contexttypes.Report;
import no.unit.nva.model.contexttypes.Series;
import no.unit.nva.model.contexttypes.UnconfirmedJournal;
import no.unit.nva.model.contexttypes.UnconfirmedPublisher;
import no.unit.nva.model.contexttypes.UnconfirmedSeries;
import no.unit.nva.model.funding.FundingBuilder;
import no.unit.nva.model.instancetypes.book.BookMonograph;
import no.unit.nva.model.instancetypes.chapter.ChapterArticle;
import no.unit.nva.model.instancetypes.journal.JournalArticle;
import no.unit.nva.model.instancetypes.journal.JournalCorrigendum;
import no.unit.nva.model.instancetypes.journal.JournalLeader;
import no.unit.nva.model.instancetypes.journal.JournalLetter;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;
import no.unit.nva.publication.external.services.UriRetriever;
import no.unit.nva.publication.model.BackendClientCredentials;
import no.unit.nva.publication.model.business.importcandidate.ImportCandidate;
import no.unit.nva.publication.model.business.importcandidate.ImportStatusFactory;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeS3Client;
import no.unit.nva.stubs.FakeSecretsManagerClient;
import no.unit.nva.stubs.WiremockHttpClient;
import nva.commons.core.Environment;
import nva.commons.core.SingletonCollector;
import nva.commons.core.StringUtils;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.core.paths.UnixPath;
import nva.commons.core.paths.UriWrapper;
import nva.commons.logutils.LogUtils;
import nva.commons.logutils.TestAppender;
import nva.commons.secrets.SecretsReader;
import org.apache.tika.io.TikaInputStream;
import org.hamcrest.Matchers;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CopyObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

@WireMockTest(httpsEnabled = true)
class ScopusHandlerTest extends ResourcesLocalTest {

    public static final Context CONTEXT = null;
    public static final RequestParametersEntity EMPTY_REQUEST_PARAMETERS = null;
    public static final ResponseElementsEntity EMPTY_RESPONSE_ELEMENTS = null;
    public static final UserIdentityEntity EMPTY_USER_IDENTITY = null;
    public static final long SOME_FILE_SIZE = 100L;
    public static final String EXPECTED_CONTENT_STRING_TXT = "expectedContentString.txt";
    public static final String INF_CLASS_NAME = "inf";
    public static final String SUP_CLASS_NAME = "sup";
    public static final String INVALID_ISSN = "096042";
    public static final String VALID_ISSN = "0960-4286";
    public static final String LANGUAGE_ENG = "eng";
    public static final String RESOURCE_EXCEPTION_MESSAGE = "resourceExceptionMessage";
    private static final String EXPECTED_RESULTS_PATH = "expectedResults";
    private static final String HARDCODED_EXPECTED_KEYWORD_1 = "<sup>64</sup>Cu";
    private static final String HARDCODED_EXPECTED_KEYWORD_2 = "excretion";
    private static final String HARDCODED_EXPECTED_KEYWORD_3 = "sheep";
    private static final String SCOPUS_XML_0000469852 = "2-s2.0-0000469852.xml";
    private static final String PUBLICATION_DAY_FIELD_NAME = "day";
    private static final String PUBLICATION_MONTH_FIELD_NAME = "month";
    private static final String PUBLICATION_YEAR_FIELD_NAME = "year";
    private static final String FILENAME_EXPECTED_ABSTRACT_IN_0000469852 = "expectedAbstract.txt";
    private static final String PIA_SECRET_NAME = "someSecretName";
    private static final String PIA_USERNAME_SECRET_KEY = "someUserNameKey";
    private static final String PIA_PASSWORD_SECRET_KEY = "somePasswordNameKey";
    private FakeS3Client s3Client;
    private S3Driver s3Driver;
    private ScopusHandler scopusHandler;
    private PiaConnection piaConnection;
    private CristinConnection cristinConnection;
    private PublicationChannelConnection publicationChannelConnection;
    private NvaCustomerConnection nvaCustomerConnection;
    private ScopusGenerator scopusData;
    private ResourceService resourceService;
    private ScopusUpdater scopusUpdater;
    private ScopusFileConverter scopusFileConverter;
    private UriRetriever uriRetriever;
    private AuthorizedBackendUriRetriever authorizedBackendUriRetriever;

    private TestAppender appender;

    public static Stream<Arguments> providedLanguagesAndExpectedOutput() {
        return Stream.concat(LanguageConstants.ALL_LANGUAGES.stream().map(ScopusHandlerTest::createArguments),
                             addLanguageEdgeCases());
    }

    @BeforeEach
    public void init(WireMockRuntimeInfo wireMockRuntimeInfo) throws IOException, URISyntaxException {
        super.init();
        appender = LogUtils.getTestingAppenderForRootLogger();
        appender.start();
        var fakeSecretsManagerClient = new FakeSecretsManagerClient();
        fakeSecretsManagerClient.putSecret(PIA_SECRET_NAME, PIA_USERNAME_SECRET_KEY, randomString());
        fakeSecretsManagerClient.putSecret(PIA_SECRET_NAME, PIA_PASSWORD_SECRET_KEY, randomString());
        fakeSecretsManagerClient.putPlainTextSecret("someSecret",
                                                    String.valueOf(new BackendClientCredentials("id", "secret")));
        var secretsReader = new SecretsReader(fakeSecretsManagerClient);
        s3Client = new FakeS3cClientWithHeadSupport();
        s3Driver = new S3Driver(s3Client, "ignoredValue");
        var httpClient = WiremockHttpClient.create();
        var piaEnvironment = createPiaConnectionEnvironment(wireMockRuntimeInfo);
        piaConnection = new PiaConnection(httpClient, secretsReader, piaEnvironment);
        cristinConnection = new CristinConnection(httpClient);
        authorizedBackendUriRetriever = mock(AuthorizedBackendUriRetriever.class);
        publicationChannelConnection = new PublicationChannelConnection(authorizedBackendUriRetriever);
        nvaCustomerConnection = mockCustomerConnection();
        resourceService = getResourceServiceBuilder().build();
        uriRetriever = mock(UriRetriever.class);
        scopusUpdater = new ScopusUpdater(resourceService, uriRetriever);
        var environment = mock(Environment.class);
        when(environment.readEnv(CROSSREF_URI_ENV_VAR_NAME)).thenReturn(wireMockRuntimeInfo.getHttpsBaseUrl());
        scopusFileConverter = new ScopusFileConverter(httpClient, s3Client, environment, mockedTikaUtils());
        scopusHandler = new ScopusHandler(s3Client, piaConnection, cristinConnection, publicationChannelConnection,
                                          nvaCustomerConnection, resourceService, scopusUpdater, scopusFileConverter);
        scopusData = new ScopusGenerator();
    }

    private TikaUtils mockedTikaUtils() throws IOException, URISyntaxException {
        var tikaUtils = mock(TikaUtils.class);
        var tikaInputStream = mock(TikaInputStream.class);
        when(tikaInputStream.getLength()).thenReturn(Long.parseLong(String.valueOf(randomInteger())));
        when(tikaInputStream.getPath()).thenReturn(
            Path.of(getClass().getClassLoader().getResource("2-s2.0-0000469852.xml").toURI()));
        when(tikaUtils.fetch(any())).thenReturn(tikaInputStream);
        when(tikaUtils.getMimeType(any())).thenReturn("application/pdf");
        return tikaUtils;
    }

    @AfterEach
    void tearDown() {
        appender.stop();
    }

    @Test
    void shouldLogExceptionMessageWhenExceptionOccurs() {
        createEmptyPiaMock();
        var s3Event = createS3Event(randomString());
        var expectedMessage = randomString();
        s3Client = new FakeS3ClientThrowingException(expectedMessage);
        scopusHandler = new ScopusHandler(s3Client, piaConnection, cristinConnection, publicationChannelConnection,
                                          nvaCustomerConnection, resourceService, scopusUpdater, scopusFileConverter);
        assertThrows(RuntimeException.class, () -> scopusHandler.handleRequest(s3Event, CONTEXT));
        assertThat(appender.getMessages(), containsString(expectedMessage));
    }

    @Test
    void shouldExtractOnlyScopusIdentifierIgnoreAllOtherIdentifiersAndStoreItInPublication() throws IOException {
        createEmptyPiaMock();
        var scopusIdentifiers = getEid();
        var expectedAdditionalIdentifier = new AdditionalIdentifier(SCOPUS_IDENTIFIER,
                                                                    scopusIdentifiers);
        var s3Event = createNewScopusPublicationEvent();
        var publication = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualAdditionalIdentifiers = publication.getAdditionalIdentifiers();
        assertThat(actualAdditionalIdentifiers, hasItem(expectedAdditionalIdentifier));
    }

    @Test
    void shouldReturnImportCandidateWithoutAssociatedArtifactsWhenExceptionOccursFetchingFiles(
        WireMockRuntimeInfo wireMockRuntimeInfo) throws IOException {
        scopusData.getDocument().getMeta().setOpenAccess(randomOpenAccess(wireMockRuntimeInfo));
        createEmptyPiaMock();
        mockBadRequestFetchingFilesFromXml(scopusData);
        var s3Event = createNewScopusPublicationEvent();
        var importCandidate = scopusHandler.handleRequest(s3Event, CONTEXT);

        assertThat(importCandidate.getAssociatedArtifacts(), is(emptyIterable()));
    }

    @Test
    void shouldFetchFileFromCrossRefDoiWhenScopusXmlDoesNotHaveFileReference(WireMockRuntimeInfo wireMockRuntimeInfo)
        throws IOException {
        scopusData.getDocument().getMeta().setOpenAccess(randomOpenAccess(wireMockRuntimeInfo));
        scopusData.getDocument().getMeta().setDoi(randomString());
        createEmptyPiaMock();
        mockBadRequestFetchingFilesFromXml(scopusData);
        var expectedFilename = mockFetchCrossrefDoiResponse(scopusData, wireMockRuntimeInfo);
        var s3Event = createNewScopusPublicationEvent();
        var importCandidate = scopusHandler.handleRequest(s3Event, CONTEXT);

        assertThat(((File) importCandidate.getAssociatedArtifacts().getFirst()).getName(),
                   is(equalTo(expectedFilename)));
    }

    @Test
    void shouldReturnImportCandidateWithoutAssociatedArtifactsWhenFailingFetchingFilesFromFilesFromXmlAndCrossref(
        WireMockRuntimeInfo wireMockRuntimeInfo) throws IOException {
        scopusData.getDocument().getMeta().setOpenAccess(randomOpenAccess(wireMockRuntimeInfo));
        scopusData.getDocument().getMeta().setDoi(randomString());
        createEmptyPiaMock();
        mockBadRequestFetchingFilesFromXml(scopusData);
        mockBadRequestCrossrefDoiResponse(scopusData);
        var s3Event = createNewScopusPublicationEvent();
        var importCandidate = scopusHandler.handleRequest(s3Event, CONTEXT);

        assertThat(importCandidate.getAssociatedArtifacts(), is(emptyIterable()));
    }

    @Test
    void shouldExtractDoiAndPlaceItInsideReferenceObject() throws IOException {
        createEmptyPiaMock();
        scopusData = ScopusGenerator.createScopusGeneratorWithSpecificDoi(randomDoi());
        var expectedURI = Doi.fromDoiIdentifier(scopusData.getDocument().getMeta().getDoi()).getUri();
        var s3Event = createNewScopusPublicationEvent();
        var publication = scopusHandler.handleRequest(s3Event, CONTEXT);
        assertThat(publication.getEntityDescription().getReference().getDoi(), equalToObject(expectedURI));
    }

    @Test
    void shouldReturnPublicationWithMainTitle() throws IOException {
        createEmptyPiaMock();
        var s3Event = createNewScopusPublicationEvent();
        var titleObject = extractTitle(scopusData);
        var expectedTitleString = expectedTitle(titleObject);
        var publication = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualMainTitle = publication.getEntityDescription().getMainTitle();
        assertThat(actualMainTitle, is(equalTo(expectedTitleString)));
    }

    @Test
    void shouldConvertSpecifiedSupAndInfContentToString() throws IOException {
        createEmptyPiaMock();
        scopusData = ScopusGenerator.createWithSpecifiedSupAndInfContent(createContentWithSupAndInfTags());
        var s3Event = createNewScopusPublicationEvent();
        var expectedTitleString = IoUtils.stringFromResources(
            Path.of(EXPECTED_RESULTS_PATH, EXPECTED_CONTENT_STRING_TXT));
        var publication = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualMainTitle = publication.getEntityDescription().getMainTitle();
        assertThat(actualMainTitle, is(equalTo(expectedTitleString)));
    }

    @Test
    void shouldExtractContributorsNamesAndSequenceNumberCorrectly() throws IOException {
        createEmptyPiaMock();
        var authors = keepOnlyTheAuthors();
        var collaborations = keepOnlyTheCollaborations();
        var s3Event = createNewScopusPublicationEvent();
        var publication = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualContributors = publication.getEntityDescription().getContributors();
        authors.forEach(author -> checkContributor(author, actualContributors));
        collaborations.forEach(collaboration -> checkCollaboration(collaboration, actualContributors));
    }

    @Test
    void shouldReturnPublicationWithUnconfirmedPublicationContextWhenEventWithS3UriThatPointsToScopusXml()
        throws IOException {
        createEmptyPiaMock();
        scopusData = ScopusGenerator.createWithSpecifiedSrcType(SourcetypeAtt.J);
        var s3Event = createNewScopusPublicationEvent();
        var publication = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualPublicationContext = publication.getEntityDescription().getReference().getPublicationContext();
        assertThat(actualPublicationContext, instanceOf(UnconfirmedJournal.class));
    }

    @Test
    void shouldReturnPublicationWithUnconfirmedPublicationContextWhenEventS3UriScopusXmlWithValidIssn()
        throws IOException {
        createEmptyPiaMock();
        scopusData = ScopusGenerator.createWithSpecifiedSrcType(SourcetypeAtt.J);
        scopusData.clearIssn();
        scopusData.addIssn(VALID_ISSN, ISSN_TYPE_PRINT);
        var s3Event = createNewScopusPublicationEvent();
        var publication = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualPublicationContext = publication.getEntityDescription().getReference().getPublicationContext();
        assertThat(actualPublicationContext, instanceOf(UnconfirmedJournal.class));
    }

    @Test
    void shouldReturnPublicationWithUnconfirmedPublicationContextWhenEventS3UriScopusXmlWithInvalidIssn()
        throws IOException {
        createEmptyPiaMock();
        scopusData = ScopusGenerator.createWithSpecifiedSrcType(SourcetypeAtt.J);
        scopusData.clearIssn();
        scopusData.addIssn(INVALID_ISSN, ISSN_TYPE_PRINT);
        var s3Event = createNewScopusPublicationEvent();
        Executable action = () -> scopusHandler.handleRequest(s3Event, CONTEXT);
        var exception = assertThrows(RuntimeException.class, action);
        var expectedMessage = "no.unit.nva.model.exceptions.InvalidIssnException: The ISSN";
        var actualMessage = exception.getMessage();
        assertThat(actualMessage, containsString(expectedMessage));
    }

    @Test
    void shouldReturnDefaultPublicationContextWhenEventWithS3UriThatPointsToScopusXmlWithoutPrintIssn()
        throws IOException {
        createEmptyPiaMock();
        scopusData = ScopusGenerator.createWithSpecifiedSrcType(SourcetypeAtt.J);
        var s3Event = createNewScopusPublicationEvent();
        var publication = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualPublicationContext = publication.getEntityDescription().getReference().getPublicationContext();
        assertThat(actualPublicationContext, instanceOf(UnconfirmedJournal.class));
    }

    @Test
    void shouldReturnPublicationContextBookWithUnconfirmedPublisherWhenEventWithS3UriThatPointsToScopusXmlWithSrcTypeB()
        throws IOException {
        createEmptyPiaMock();
        scopusData = ScopusGenerator.createWithSpecifiedSrcType(SourcetypeAtt.B);
        var s3Event = createNewScopusPublicationEvent();
        var publication = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualPublicationContext = publication.getEntityDescription().getReference().getPublicationContext();
        assertThat(actualPublicationContext, instanceOf(Book.class));
        var actualPublisher = ((Book) actualPublicationContext).getPublisher();
        var expectedPublisherName = scopusData.getDocument()
                                        .getItem()
                                        .getItem()
                                        .getBibrecord()
                                        .getHead()
                                        .getSource()
                                        .getPublisher()
                                        .getFirst()
                                        .getPublishername();
        //            var actualPublisherName = ((UnconfirmedPublisher) actualPublisher).getName();
        //            assertThat(actualPublisherName, is(expectedPublisherName));
    }

    @Test
    void shouldReturnPublicationContextBookWithUnconfirmedPublisherWhenPublisherIsNull() throws IOException {
        createEmptyPiaMock();
        scopusData = ScopusGenerator.createWithSpecifiedSrcType(SourcetypeAtt.B);
        removePublishers();
        var s3Event = createNewScopusPublicationEvent();
        var publication = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualPublicationContext = publication.getEntityDescription().getReference().getPublicationContext();
        assertThat(actualPublicationContext, instanceOf(Book.class));
        var publicationContext = (Book) publication.getEntityDescription().getReference().getPublicationContext();
        assertThat(publicationContext.getPublisher(), instanceOf(UnconfirmedPublisher.class));
    }

    @Test
    void shouldReturnPublicationContextBookWithConfirmedPublisherWhenScopusXmlHasSrcTypeBandIsNotAChapter()
        throws IOException {
        createEmptyPiaMock();
        scopusData = ScopusGenerator.createWithSpecifiedSrcType(SourcetypeAtt.B);
        var expectedPublisherName = randomString();
        scopusData.setPublishername(expectedPublisherName);
        var expectedIsbn13 = randomIsbn13();
        scopusData.addIsbn(expectedIsbn13, "13");
        var expectedYear = String.valueOf(randomYear());
        scopusData.setPublicationYear(expectedYear);
        var s3Event = createNewScopusPublicationEvent();
        var expectedPublisherId = randomUri();
        when(authorizedBackendUriRetriever.getRawContent(any(), any())).thenReturn(Optional.of(
            new PublicationChannelResponse(1, List.of(new PublicationChannelHit(expectedPublisherId))).toString()));
        var publication = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualPublicationContext = publication.getEntityDescription().getReference().getPublicationContext();
        assertThat(actualPublicationContext, instanceOf(Book.class));
        var actualPublisher = ((Book) actualPublicationContext).getPublisher();
        assertThat(actualPublisher, instanceOf(Publisher.class));
        var actualPublisherId = ((Publisher) actualPublisher).getId();
        assertThat(actualPublisherId, is(expectedPublisherId));
        var actualIsbnList = ((Book) actualPublicationContext).getIsbnList();
        assertThat(actualIsbnList.size(), is(1));
        assertThat(actualIsbnList, containsInAnyOrder(expectedIsbn13));
    }

    @Test
    void shouldReturnPublicationContextChapterWhenScopusXmlHasCitationTypeChEvenIfSrcTypeIsB() throws IOException {
        createEmptyPiaMock();
        scopusData = ScopusGenerator.create(CitationtypeAtt.CH);
        scopusData.setSrcType(SourcetypeAtt.B);
        var expectedPublisherName = randomString();
        scopusData.setPublishername(expectedPublisherName);
        var s3Event = createNewScopusPublicationEvent();
        var publication = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualPublicationContext = publication.getEntityDescription().getReference().getPublicationContext();
        assertThat(actualPublicationContext, instanceOf(Anthology.class));
        var actualContextUri = ((Anthology) actualPublicationContext).getId();
        assertThat(actualContextUri, is(ScopusConstants.DUMMY_URI));
    }

    @Test
    void shouldReturnPublicationContextReportWithConfirmedPublisherWhenEventWithS3UriThatPointsToScopusXmlWithSrcTypeR()
        throws IOException {
        createEmptyPiaMock();
        scopusData = ScopusGenerator.createWithSpecifiedSrcType(SourcetypeAtt.R);
        var expectedPublisherName = randomString();
        scopusData.setPublishername(expectedPublisherName);
        var s3Event = createNewScopusPublicationEvent();
        var expectedPublisherId = randomUri();
        when(authorizedBackendUriRetriever.getRawContent(any(), any())).thenReturn(Optional.of(
            new PublicationChannelResponse(1, List.of(new PublicationChannelHit(expectedPublisherId))).toString()));
        var publication = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualPublicationContext = publication.getEntityDescription().getReference().getPublicationContext();
        assertThat(actualPublicationContext, instanceOf(Book.class));
        var actualPublisher = ((Report) actualPublicationContext).getPublisher();
        assertThat(actualPublisher, instanceOf(Publisher.class));
        var actualPublisherId = ((Publisher) actualPublisher).getId();
        assertThat(actualPublisherId, is(expectedPublisherId));
    }

    @Test
    void shouldReturnPublicationContextUnconfirmedBookSeriesWhenEventWithS3UriThatPointsToScopusXmlWithSrcTypeK()
        throws IOException {
        createEmptyPiaMock();
        scopusData = ScopusGenerator.createWithSpecifiedSrcType(SourcetypeAtt.K);
        final var expectedYear = String.valueOf(randomYear());
        scopusData.setPublicationYear(expectedYear);
        scopusData.clearIssn();
        final var expectedIssn = randomIssn();
        scopusData.addIssn(expectedIssn, ISSN_TYPE_ELECTRONIC);
        var s3Event = createNewScopusPublicationEvent();
        var publication = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualPublicationContext = publication.getEntityDescription().getReference().getPublicationContext();
        assertThat(actualPublicationContext, instanceOf(Book.class));
        var actualSeries = ((Book) actualPublicationContext).getSeries();
        assertThat(actualSeries, instanceOf(UnconfirmedSeries.class));
        var actualIssn = ((UnconfirmedSeries) actualSeries).getOnlineIssn();
        assertThat(actualIssn, is(expectedIssn));
    }

    @Test
    void shouldReturnPublicationContextUnconfirmedJournalWhenSrcTypeIsPAndIssnExists() throws IOException {
        createEmptyPiaMock();
        scopusData = ScopusGenerator.createWithSpecifiedSrcType(SourcetypeAtt.P);
        final var expectedYear = String.valueOf(randomYear());
        scopusData.setPublicationYear(expectedYear);
        scopusData.clearIssn();
        final var expectedIssn = randomIssn();
        scopusData.addIssn(expectedIssn, ISSN_TYPE_ELECTRONIC);
        var uri = s3Driver.insertFile(UnixPath.of(randomString()), scopusData.toXml());
        var s3Event = createS3Event(uri);
        var publication = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualPublicationContext = publication.getEntityDescription().getReference().getPublicationContext();
        assertThat(actualPublicationContext, instanceOf(UnconfirmedJournal.class));
        var actualIssn = ((UnconfirmedJournal) actualPublicationContext).getOnlineIssn();
        assertThat(actualIssn, is(expectedIssn));
    }

    @Test
    void shouldReturnPublicationContextChapterWhenSrcTypeIsPAndIsbnExists() throws IOException {
        createEmptyPiaMock();
        scopusData = ScopusGenerator.createWithSpecifiedSrcType(SourcetypeAtt.P);
        final var expectedYear = String.valueOf(randomYear());
        scopusData.setPublicationYear(expectedYear);
        scopusData.clearIssn();
        final var expectedIsbn = randomIsbn13();
        scopusData.addIsbn(expectedIsbn, "13");
        var uri = s3Driver.insertFile(UnixPath.of(randomString()), scopusData.toXml());
        var s3Event = createS3Event(uri);
        var publication = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualPublicationContext = publication.getEntityDescription().getReference().getPublicationContext();
        assertThat(actualPublicationContext, instanceOf(Anthology.class));
        var actualContextUri = ((Anthology) actualPublicationContext).getId();
        assertThat(actualContextUri, is(ScopusConstants.DUMMY_URI));
    }

    @Test
    void shouldReturnPublicationContextConfirmedBookSeriesWhenEventWithS3UriThatPointsToScopusXmlWithSrcTypeK()
        throws IOException {
        createEmptyPiaMock();
        scopusData = ScopusGenerator.createWithSpecifiedSrcType(SourcetypeAtt.K);
        final var expectedYear = String.valueOf(randomYear());
        scopusData.setPublicationYear(expectedYear);
        scopusData.clearIssn();
        final var expectedIssn = randomIssn();
        scopusData.addIssn(expectedIssn, ISSN_TYPE_ELECTRONIC);
        var s3Event = createNewScopusPublicationEvent();
        var expectedSeriesId = randomUri();
        when(authorizedBackendUriRetriever.getRawContent(any(), any())).thenReturn(Optional.of(
            new PublicationChannelResponse(1, List.of(new PublicationChannelHit(expectedSeriesId))).toString()));
        var publication = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualPublicationContext = publication.getEntityDescription().getReference().getPublicationContext();
        assertThat(actualPublicationContext, instanceOf(Book.class));
        var actualSeries = ((Book) actualPublicationContext).getSeries();
        assertThat(actualSeries, instanceOf(Series.class));
        var actualUri = ((Series) actualSeries).getId();
        assertThat(actualUri, is(expectedSeriesId));
    }

    @Test
    void shouldReturnPublicationWithJournalWhenEventWithS3UriThatPointsToScopusXmlWhereSourceTitleIsInNsd()
        throws IOException {
        createEmptyPiaMock();
        scopusData = ScopusGenerator.createWithSpecifiedSrcType(SourcetypeAtt.J);
        var expectedYear = "2022";
        scopusData.setPublicationYear(expectedYear);
        scopusData.clearIssn();
        var expectedIssn = randomIssn();
        scopusData.addIssn(expectedIssn, ISSN_TYPE_ELECTRONIC);
        var s3Event = createNewScopusPublicationEvent();
        var expectedJournalId = randomUri();
        when(authorizedBackendUriRetriever.getRawContent(any(), any())).thenReturn(Optional.of(
            new PublicationChannelResponse(1, List.of(new PublicationChannelHit(expectedJournalId))).toString()));
        var publication = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualPublicationContext = publication.getEntityDescription().getReference().getPublicationContext();
        assertThat(actualPublicationContext, instanceOf(Journal.class));
        var actualJournalUri = ((Journal) actualPublicationContext).getId();
        assertThat(actualJournalUri, is(expectedJournalId));
    }

    @Test
    void shouldReturnUnconfirmedJournalWhenBadResponseFromPublicationChannelApi() throws IOException {
        createEmptyPiaMock();
        scopusData = ScopusGenerator.createWithSpecifiedSrcType(SourcetypeAtt.J);
        var expectedYear = "2022";
        scopusData.setPublicationYear(expectedYear);
        scopusData.clearIssn();
        var expectedIssn = randomIssn();
        scopusData.addIssn(expectedIssn, ISSN_TYPE_ELECTRONIC);
        var s3Event = createNewScopusPublicationEvent();
        when(authorizedBackendUriRetriever.getRawContent(any(), any())).thenReturn(Optional.of(randomString()));
        var publication = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualPublicationContext = publication.getEntityDescription().getReference().getPublicationContext();
        assertThat(actualPublicationContext, instanceOf(UnconfirmedJournal.class));
    }

    @Test
    void shouldThrowExceptionWhenSrcTypeIsNotSupported() throws IOException {
        createEmptyPiaMock();
        scopusData = ScopusGenerator.createWithSpecifiedSrcType(SourcetypeAtt.X);
        var expectedMessage = String.format(UNSUPPORTED_SOURCE_TYPE, getSrctype(), getEid());
        var s3Event = createNewScopusPublicationEvent();
        assertThrows(UnsupportedSrcTypeException.class, () -> scopusHandler.handleRequest(s3Event, CONTEXT));
        assertThat(appender.getMessages(), containsString(expectedMessage));
    }

    @Test
    void shouldSaveErrorReportInS3ContainingTheOriginalFileName() throws IOException {
        createEmptyPiaMock();
        scopusData = ScopusGenerator.createWithSpecifiedSrcType(SourcetypeAtt.X);
        var s3Event = createNewScopusPublicationEvent();
        Executable action = () -> scopusHandler.handleRequest(s3Event, CONTEXT);
        var exception = assertThrows(UnsupportedSrcTypeException.class, action);
        var actualReport = extractActualReportFromS3Client(s3Event, exception);
        var input = actualReport.get("input").asText();
        assertThat(input, is(equalTo(scopusData.toXml())));
    }

    @Test
    void shouldSaveSuccessReportInS3() throws IOException {
        createEmptyPiaMock();
        scopusData = ScopusGenerator.createWithSpecifiedSrcType(SourcetypeAtt.J);
        var s3Event = createNewScopusPublicationEvent();
        var importCandidate = scopusHandler.handleRequest(s3Event, CONTEXT);
        var report = extractSuccessReport(s3Event, importCandidate);
        assertThat(getScopusIdentifier(importCandidate), is(equalTo(report)));
    }

    @Test
    void shouldExtractAuthorKeyWordsAsPlainText() throws IOException {
        createEmptyPiaMock();
        scopusData = ScopusGenerator.createWithSpecifiedSrcType(SourcetypeAtt.J);
        scopusData.clearAuthorKeywords();
        scopusData.addAuthorKeyword(HARDCODED_EXPECTED_KEYWORD_1, LANGUAGE_ENG);
        scopusData.addAuthorKeyword(HARDCODED_EXPECTED_KEYWORD_2, LANGUAGE_ENG);
        scopusData.addAuthorKeyword(HARDCODED_EXPECTED_KEYWORD_3, LANGUAGE_ENG);
        var s3Event = createNewScopusPublicationEvent();
        var publication = scopusHandler.handleRequest(s3Event, CONTEXT);
        var expectedKeywords = List.of(HARDCODED_EXPECTED_KEYWORD_1, HARDCODED_EXPECTED_KEYWORD_2,
                                       HARDCODED_EXPECTED_KEYWORD_3);
        var actualPlaintextKeyWords = publication.getEntityDescription().getTags();
        assertThat(actualPlaintextKeyWords, containsInAnyOrder(expectedKeywords.toArray()));
    }

    @Test
    void shouldHaveNoDuplicateContributors() throws IOException {
        createEmptyPiaMock();
        var s3Event = createNewScopusPublicationEvent();
        var publication = scopusHandler.handleRequest(s3Event, CONTEXT);
        var contributors = publication.getEntityDescription().getContributors();
        checkForDuplicateContributors(contributors);
    }

    @Test
    void shouldExtractPublicationDate() throws IOException {
        createEmptyPiaMock();
        scopusData = ScopusGenerator.createWithSpecifiedSrcType(SourcetypeAtt.J);
        var year = "1978";
        var month = "02";
        var day = "01";
        scopusData.setPublicationDate(year, month, day);
        var s3Event = createNewScopusPublicationEvent();
        var publication = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualPublicationDate = publication.getEntityDescription().getPublicationDate();
        assertThat(actualPublicationDate, allOf(hasProperty(PUBLICATION_DAY_FIELD_NAME, is(day)),
                                                hasProperty(PUBLICATION_MONTH_FIELD_NAME, is(month)),
                                                hasProperty(PUBLICATION_YEAR_FIELD_NAME, is(year))));
    }

    @Test
    void shouldExtractMainAbstract() throws IOException {
        createEmptyPiaMock();
        var scopusFile = IoUtils.stringFromResources(Path.of(SCOPUS_XML_0000469852));
        var uri = s3Driver.insertFile(randomS3Path(), scopusFile);
        var s3Event = createS3Event(uri);
        var publication = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualMainAbstract = publication.getEntityDescription().getAbstract();
        var expectedAbstract = IoUtils.stringFromResources(
            Path.of(EXPECTED_RESULTS_PATH, FILENAME_EXPECTED_ABSTRACT_IN_0000469852));
        assertThat(actualMainAbstract, is(equalTo(expectedAbstract)));
    }

    @Test
    void shouldNotThrowExceptionWhenScopusXmlDoesNotContainAbstract() throws IOException {
        createEmptyPiaMock();
        scopusData = ScopusGenerator.createWithSpecifiedAbstract(null);
        var event = createNewScopusPublicationEvent();
        assertDoesNotThrow(() -> scopusHandler.handleRequest(event, CONTEXT));
    }

    @Test
    void shouldExtractJournalArticleWhenScopusCitationTypeIsArticle() throws IOException {
        createEmptyPiaMock();
        scopusData = ScopusGenerator.create(CitationtypeAtt.AR);
        var s3Event = createNewScopusPublicationEvent();
        var publication = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualPublicationInstance = publication.getEntityDescription().getReference().getPublicationInstance();
        assertThat(actualPublicationInstance, isA(JournalArticle.class));
    }

    @Test
    void shouldExtractJournalArticleWhenScopusCitationTypeIsReview() throws IOException {
        createEmptyPiaMock();
        scopusData = ScopusGenerator.create(CitationtypeAtt.RE);
        var s3Event = createNewScopusPublicationEvent();
        var publication = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualPublicationInstance = publication.getEntityDescription().getReference().getPublicationInstance();
        assertThat(actualPublicationInstance, isA(JournalArticle.class));
    }

    @Test
    void shouldExtractJournalArticleWhenScopusCitationTypeIsEditorial() throws IOException {
        createEmptyPiaMock();
        scopusData = ScopusGenerator.create(CitationtypeAtt.ED);
        var expectedIssue = String.valueOf(randomInteger());
        var expectedVolume = randomString();
        var expectedPages = randomString();
        scopusData.setJournalInfo(expectedVolume, expectedIssue, expectedPages);
        var s3Event = createNewScopusPublicationEvent();
        var publication = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualPublicationInstance = publication.getEntityDescription().getReference().getPublicationInstance();
        assertThat(actualPublicationInstance, isA(JournalLeader.class));
        assertThat(expectedVolume, is(((JournalLeader) actualPublicationInstance).getVolume()));
        assertThat(expectedIssue, is(((JournalLeader) actualPublicationInstance).getIssue()));
    }

    @Test
    void shouldExtractJournalArticleWhenScopusCitationTypeIsErratum() throws IOException {
        createEmptyPiaMock();
        scopusData = ScopusGenerator.create(CitationtypeAtt.ER);
        var expectedIssue = String.valueOf(randomInteger());
        var expectedVolume = randomString();
        var expectedPages = randomString();
        scopusData.setJournalInfo(expectedVolume, expectedIssue, expectedPages);
        var s3Event = createNewScopusPublicationEvent();
        var publication = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualPublicationInstance = publication.getEntityDescription().getReference().getPublicationInstance();
        assertThat(actualPublicationInstance, isA(JournalCorrigendum.class));
        assertThat(ScopusConstants.DUMMY_URI, is(((JournalCorrigendum) actualPublicationInstance).getCorrigendumFor()));
    }

    @Test
    void shouldExtractJournalLetterWhenScopusCitationTypeIsLetter() throws IOException {
        createEmptyPiaMock();
        scopusData = ScopusGenerator.create(CitationtypeAtt.LE);
        var expectedIssue = String.valueOf(randomInteger());
        var expectedVolume = randomString();
        var expectedPages = randomString();
        scopusData.setJournalInfo(expectedVolume, expectedIssue, expectedPages);
        var s3Event = createNewScopusPublicationEvent();
        var publication = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualPublicationInstance = publication.getEntityDescription().getReference().getPublicationInstance();
        assertThat(actualPublicationInstance, isA(JournalLetter.class));
        assertThat(expectedIssue, is(((JournalLetter) actualPublicationInstance).getIssue()));
    }

    @Test
    void shouldExtractJournalArticleWhenScopusCitationTypeIsConferencePaperAndContextJournal() throws IOException {
        createEmptyPiaMock();
        scopusData = ScopusGenerator.create(CitationtypeAtt.CP);
        scopusData.setSrcType(SourcetypeAtt.P);
        scopusData.clearIssn();
        var expectedIssn = randomIssn();
        scopusData.addIssn(expectedIssn, ISSN_TYPE_PRINT);
        var expectedIssue = String.valueOf(randomInteger());
        var expectedVolume = randomString();
        var expectedPages = randomString();
        scopusData.setJournalInfo(expectedVolume, expectedIssue, expectedPages);
        var s3Event = createNewScopusPublicationEvent();
        var publication = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualPublicationInstance = publication.getEntityDescription().getReference().getPublicationInstance();
        assertThat(actualPublicationInstance, isA(JournalArticle.class));
        assertThat(expectedIssue, is(((JournalArticle) actualPublicationInstance).getIssue()));
    }

    @Test
    void shouldExtractChapterArticleWhenScopusCitationTypeIsConferencePaperAndContextChapter() throws IOException {
        createEmptyPiaMock();
        scopusData = ScopusGenerator.create(CitationtypeAtt.CP);
        scopusData.setSrcType(SourcetypeAtt.P);
        scopusData.clearIssn();
        var expectedIsbn13 = randomIsbn13();
        scopusData.addIsbn(expectedIsbn13, "13");
        var expectedIssue = String.valueOf(randomInteger());
        var expectedVolume = randomString();
        var expectedPagesEnd = randomString();
        scopusData.setJournalInfo(expectedVolume, expectedIssue, expectedPagesEnd);
        var s3Event = createNewScopusPublicationEvent();
        var publication = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualPublicationInstance = publication.getEntityDescription().getReference().getPublicationInstance();
        assertThat(actualPublicationInstance, isA(ChapterArticle.class));
        assertThat(expectedPagesEnd, is(((ChapterArticle) actualPublicationInstance).getPages().getEnd()));
    }

    @ParameterizedTest(name = "should not generate publication when CitationType is:{0}")
    @EnumSource(value = CitationtypeAtt.class, names = {"AR", "BK", "CH", "CP", "ED", "ER", "LE", "NO", "RE",
        "SH"}, mode = Mode.EXCLUDE)
    void shouldNotGenerateCreatePublicationFromUnsupportedPublicationTypes(CitationtypeAtt citationtypeAtt)
        throws IOException {
        createEmptyPiaMock();
        scopusData = ScopusGenerator.create(citationtypeAtt);
        // eid is chosen because it seems to match the file name in the bucket.
        var eid = getEid();
        var s3Event = createNewScopusPublicationEvent();
        var expectedMessage = String.format(
            PublicationInstanceCreator.UNSUPPORTED_CITATION_TYPE_MESSAGE,
            citationtypeAtt.value(), eid);
        assertThrows(UnsupportedCitationTypeException.class, () -> scopusHandler.handleRequest(s3Event, CONTEXT));
        assertThat(appender.getMessages(), containsString(expectedMessage));
    }

    @Test
    void shouldNotCreateImportCandidateWhenMissingPublicationType()
        throws IOException {
        createEmptyPiaMock();
        scopusData.getDocument().getItem().getItem().getBibrecord().getHead().getCitationInfo()
            .getCitationType().clear();
        var eid = getEid();
        var s3Event = createNewScopusPublicationEvent();
        var expectedMessage = String.format(PublicationInstanceCreator.MISSING_CITATION_TYPE_MESSAGE, eid);
        assertThrows(UnsupportedCitationTypeException.class, () -> scopusHandler.handleRequest(s3Event, CONTEXT));
        assertThat(appender.getMessages(), containsString(expectedMessage));
    }

    @Test
    void shouldExtractAuthorOrcidAndSequenceNumber() throws IOException {
        createEmptyPiaMock();
        var authors = keepOnlyTheAuthors();
        var s3Event = createNewScopusPublicationEvent();
        var publication = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualContributors = publication.getEntityDescription().getContributors();
        authors.forEach(author -> assertIsSameAuthor(author, actualContributors));
    }

    @Test
    void shouldExtractCitationTypesToBookMonographPublicationInstance() throws IOException {
        createEmptyPiaMock();
        scopusData = ScopusGenerator.create(CitationtypeAtt.BK);
        var s3Event = createNewScopusPublicationEvent();
        var publication = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualPublicationInstance = publication.getEntityDescription().getReference().getPublicationInstance();
        assertThat(actualPublicationInstance, isA(BookMonograph.class));
    }

    @Test
    void shouldExtractCitationTypesToChapterArticlePublicationInstance() throws IOException {
        createEmptyPiaMock();
        scopusData = ScopusGenerator.create(CitationtypeAtt.CH);
        var s3Event = createNewScopusPublicationEvent();
        var publication = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualPublicationInstance = publication.getEntityDescription().getReference().getPublicationInstance();
        assertThat(actualPublicationInstance, isA(ChapterArticle.class));
    }

    @Test
    void shouldNotThrowExceptionWhenDoiInScopusIsNull() throws IOException {
        createEmptyPiaMock();
        scopusData = ScopusGenerator.createScopusGeneratorWithSpecificDoi(null);
        var s3Event = createNewScopusPublicationEvent();
        assertDoesNotThrow(() -> scopusHandler.handleRequest(s3Event, CONTEXT));
    }

    @Test
    void shouldExtractVolumeIssueAndPageRange() throws IOException {
        createEmptyPiaMock();
        scopusData = ScopusGenerator.create(CitationtypeAtt.AR);
        var expectedIssue = String.valueOf(randomInteger());
        var expectedVolume = randomString();
        var expectedPages = randomString();
        scopusData.setJournalInfo(expectedVolume, expectedIssue, expectedPages);
        var s3Event = createNewScopusPublicationEvent();
        var publication = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualPublicationInstance = (JournalArticle) publication.getEntityDescription()
                                                             .getReference()
                                                             .getPublicationInstance();
        assertThat(actualPublicationInstance.getVolume(), is(expectedVolume));
        assertThat(actualPublicationInstance.getIssue(), is(expectedIssue));
        assertThat(actualPublicationInstance.getPages().getEnd(), is(expectedPages));
    }

    @Test
    void shouldExtractCorrespondingAuthor() throws IOException {
        createEmptyPiaMock();
        var authors = keepOnlyTheAuthors();
        var correspondingAuthorTp = authors.getFirst();
        scopusData.setCorrespondence(correspondingAuthorTp);
        var s3Event = createNewScopusPublicationEvent();
        var publication = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualPublicationContributors = publication.getEntityDescription().getContributors();
        var actualCorrespondingContributor = getCorrespondingContributor(actualPublicationContributors);
        assertThat(actualCorrespondingContributor.getIdentity().getName(),
                   startsWith(correspondingAuthorTp.getGivenName()));
    }

    @ParameterizedTest(name = "Should have entityDescription with language:{1}")
    @MethodSource("providedLanguagesAndExpectedOutput")
    void shouldExtractLanguage(List<Language> languageCodes, URI expectedLanguageUri) throws IOException {
        createEmptyPiaMock();
        scopusData = ScopusGenerator.createScopusGeneratorWithSpecificLanguage(new LanguagesWrapper(languageCodes));
        var s3Event = createNewScopusPublicationEvent();
        var publication = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualLanguageUri = publication.getEntityDescription().getLanguage();
        assertEquals(expectedLanguageUri, actualLanguageUri);
    }

    @Test
    void shouldReplaceContributorIdentityWithCristinDataVerifiedByAuthorId() throws IOException {
        var authorTypesMappableToCristin = keepOnlyAuthorsWithAuthorId();
        var piaCristinIdAndAuthors = new HashMap<CristinPerson, AuthorTp>();
        authorTypesMappableToCristin.forEach(authorTp -> piaCristinIdAndAuthors.put(randomCristinPerson(), authorTp));
        generatePiaResponseAndCristinPersons(piaCristinIdAndAuthors);
        var s3Event = createNewScopusPublicationEvent();
        var publication = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualContributorsWithScopusAuid =
            publication.getEntityDescription()
                .getContributors().stream()
                .filter(this::hasScopusAuid)
                .sorted(Comparator.comparingInt(Contributor::getSequence))
                .toList();
        assertThat(actualContributorsWithScopusAuid, hasSize(authorTypesMappableToCristin.size()));
        actualContributorsWithScopusAuid.forEach(
            contributor -> assertThatContributorHasCorrectCristinPersonData(contributor, piaCristinIdAndAuthors));
    }

    @Test
    void shouldCreateSequenceNumbersInOrder() throws IOException {
        var s3Event = createNewScopusPublicationEvent();
        var publication = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualContributorsSorted = publication
                                           .getEntityDescription()
                                           .getContributors()
                                           .stream()
                                           .sorted(Comparator.comparingInt(Contributor::getSequence))
                                           .toList();
        for (var contributorIndex = 0; contributorIndex < actualContributorsSorted.size(); contributorIndex++) {
            assertThat(actualContributorsSorted.get(contributorIndex).getSequence(),
                       is(Matchers.equalTo(contributorIndex + 1)));
        }
    }

    @Test
    void shouldFetchCristinPersonAndOrganization() throws IOException {
        scopusData = createWithOneAuthorGroupAndAffiliation(generateAuthorGroup());
        var piaCristinIdAndAuthors = new HashMap<Integer, AuthorTp>();
        var cristinAffiliationId = randomString();
        mockAffiliationResponse(cristinAffiliationId, getAuthorGroup());
        mockResponsesForAuthors(piaCristinIdAndAuthors, getAuthorGroup());
        var s3Event = createNewScopusPublicationEvent();
        var publication = scopusHandler.handleRequest(s3Event, CONTEXT);
        publication.getEntityDescription()
            .getContributors()
            .forEach(contributor -> hasBeenFetchedFromCristin(contributor, piaCristinIdAndAuthors.keySet()));
    }

    @Test
    void shouldFetchOrganizationFromPiaAndCristinAndAttachToContributorAffiliationList() throws IOException {
        scopusData = createWithOneAuthorGroupAndAffiliation(generateAuthorGroup());
        var cristinAffiliationId = randomString();
        mockAffiliationResponse(cristinAffiliationId, getAuthorGroup());
        var s3Event = createNewScopusPublicationEvent();
        var publication = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualContributors = publication.getEntityDescription().getContributors();
        actualContributors.forEach(contributor -> hasAffiliationWithId(contributor, cristinAffiliationId));
    }

    @Test
    void shouldReturnAffiliationWithLabelsOnlyWhenNoResponseFromPia() throws IOException {
        scopusData = generateScopusDataWithOneAffiliation();
        var authorGroupTpList = new ArrayList<>(keepOnlyAuthorGroups());
        var piaCristinAffiliationIdAndAuthors = new HashMap<String, AuthorGroupTp>();
        authorGroupTpList.forEach(group -> piaCristinAffiliationIdAndAuthors.put(randomString(), group));
        piaCristinAffiliationIdAndAuthors.forEach(
            (cristinOrganizationId, authorGroupTp) -> generatePiaAffiliationsResponse(new PiaResponseGenerator(),
                                                                                      authorGroupTp,
                                                                                      cristinOrganizationId));
        var s3Event = createNewScopusPublicationEvent();
        var publication = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualContributors = publication.getEntityDescription().getContributors();
        actualContributors.forEach(ScopusHandlerTest::hasNoAffiliationWithId);
    }

    @Test
    void shouldReturnAffiliationWithLabelsOnlyWhenNoResponseFromCristin() throws IOException {
        scopusData = generateScopusDataWithOneAffiliation();
        var authorGroupTpList = new ArrayList<>(keepOnlyAuthorGroups());
        var piaCristinAffiliationIdAndAuthors = new HashMap<String, AuthorGroupTp>();
        authorGroupTpList.forEach(group -> piaCristinAffiliationIdAndAuthors.put(randomString(), group));
        var s3Event = createNewScopusPublicationEvent();
        var publication = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualContributors = publication.getEntityDescription().getContributors();
        actualContributors.forEach(ScopusHandlerTest::hasNoAffiliationWithId);
    }

    @Test
    void shouldNotAddCristinOrganizationFromAuthorGroupWhenNoResponseFromCristin() throws IOException {
        scopusData = generateScopusDataWithOneAffiliation();
        var s3Event = createNewScopusPublicationEvent();
        var publication = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualContributors = publication.getEntityDescription().getContributors();

        var affiliationIds = actualContributors.stream()
                                 .map(Contributor::getAffiliations)
                                 .flatMap(List::stream)
                                 .filter(Organization.class::isInstance)
                                 .map(Organization.class::cast)
                                 .filter(org -> nonNull(org.getId()))
                                 .collect(Collectors.toList());
        assertThat(affiliationIds, is(equalTo(Collections.emptyList())));
    }

    @Test
    void shouldHandleCristinPersonBadRequest() throws IOException {
        var authorTypes = keepOnlyTheAuthors();
        var piaCristinIdAndAuthors = new HashMap<Integer, AuthorTp>();
        authorTypes.forEach(authorTp -> piaCristinIdAndAuthors.put(randomInteger(), authorTp));
        var authors = new ArrayList<List<Author>>();
        mockCristinPersonBadRequest();
        piaCristinIdAndAuthors.forEach(
            (cristinId, authorTp) -> generatePiaAuthorResponse(authors, cristinId,
                                                               authorTp));
        var s3Event = createNewScopusPublicationEvent();
        assertDoesNotThrow(() -> scopusHandler.handleRequest(s3Event, CONTEXT));
    }

    @Test
    void shouldIgnoreCristinNumberThatIsNull() throws IOException {
        var authorTypes = keepOnlyTheAuthors();
        var authors = new ArrayList<List<Author>>();
        authorTypes.forEach(
            (authorTp) -> authors.add(PiaResponseGenerator.generateAuthors(authorTp.getAuid(), 0)));
        authors.forEach(this::createPiaAuthorMock);
        var s3Event = createNewScopusPublicationEvent();
        var publication = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualContributors = publication.getEntityDescription().getContributors();
        actualContributors.stream()
            .filter(contributor -> isAuthor(contributor, authorTypes))
            .forEach(contributor -> assertNull(contributor.getIdentity().getId()));
    }

    @Test
    void shouldHandlePiaConnectionException() throws IOException {
        mockedPiaException();
        var s3Event = createNewScopusPublicationEvent();
        scopusHandler.handleRequest(s3Event, CONTEXT);
        assertThat(appender.getMessages(), containsString(PiaConnection.PIA_RESPONSE_ERROR));
    }

    @Test
    void shouldHandlePiaBadRequest() throws IOException {
        mockedPiaBadRequest();
        var s3Event = createNewScopusPublicationEvent();
        scopusHandler.handleRequest(s3Event, CONTEXT);
        assertThat(appender.getMessages(), containsString(PiaConnection.PIA_RESPONSE_ERROR));
    }

    @Test
    void shouldTryToPersistPublicationInDatabaseSeveralTimesWhenResourceServiceIsThrowingException()
        throws IOException {
        var fakeResourceServiceThrowingException = resourceServiceThrowingExceptionWhenSavingResource();
        var s3Event = createNewScopusPublicationEvent();
        var handler = new ScopusHandler(this.s3Client, this.piaConnection, this.cristinConnection,
                                        this.publicationChannelConnection, nvaCustomerConnection,
                                        fakeResourceServiceThrowingException,
                                        scopusUpdater, scopusFileConverter);
        assertThrows(RuntimeException.class, () -> handler.handleRequest(s3Event, CONTEXT));
    }

    @Test
    void shouldMergeIncomingImportCandidateIntoExistingOneWhenScopusIdsMatch() throws IOException {
        var existingImportCandidate = createPersistedImportCandidate();
        createEmptyPiaMock();
        when(uriRetriever.getRawContent(any(), any())).thenReturn(toResponse(existingImportCandidate));
        scopusData = ScopusGenerator.create(CitationtypeAtt.LE);
        var expectedIssue = String.valueOf(randomInteger());
        var expectedVolume = randomString();
        var expectedPages = randomString();
        scopusData.setJournalInfo(expectedVolume, expectedIssue, expectedPages);
        var s3Event = createNewScopusPublicationEvent();
        var importCandidate = scopusHandler.handleRequest(s3Event, CONTEXT);

        assertThat(importCandidate.getIdentifier(), is(equalTo(existingImportCandidate.getIdentifier())));
        assertThat(importCandidate.getImportStatus(), is(equalTo(existingImportCandidate.getImportStatus())));
        assertThat(importCandidate.getEntityDescription(),
                   is(not(equalTo(existingImportCandidate.getEntityDescription()))));
    }

    void hasBeenFetchedFromCristin(Contributor contributor, Set<Integer> cristinIds) {
        var contributorId = contributor.getIdentity().getId();
        if (nonNull(contributorId)) {
            assertThat(cristinIds, hasItem(toCristinIdentifier(contributorId)));
        }
    }

    private static NvaCustomerConnection mockCustomerConnection() {
        var customerConnection = mock(NvaCustomerConnection.class);
        when(customerConnection.atLeastOneNvaCustomerPresent(any())).thenReturn(true);
        return customerConnection;
    }

    private static String toCrossrefResponse(UriWrapper downloadUrl) {
        return new CrossrefResponse(new Message(List.of(new CrossrefLink(downloadUrl.getUri(), "application/pdf", VOR)),
                                                List.of(new License(
                                                    URI.create("http://creativecommons.org/" + randomString()), 0,
                                                    new Start(List.of(List.of(2023, 01, 25))), VOR)),
                                                new Resource(new Primary(randomUri())))).toString();
    }

    private static void mockBadRequestFileResponse(UpwOaLocationType locationType) {
        var testUrl = "/" + UriWrapper.fromUri(locationType.getUpwUrlForPdf()).getLastPathElement();
        stubFor(WireMock.get(urlPathEqualTo(testUrl))
                    .willReturn(aResponse().withStatus(HttpURLConnection.HTTP_BAD_REQUEST)));
    }

    private static void hasNoAffiliationWithId(Contributor contributor) {
        contributor.getAffiliations()
            .stream()
            .filter(Objects::nonNull)
            .filter(Organization.class::isInstance)
            .map(Organization.class::cast)
            .forEach(affiliation -> assertThat(affiliation.getId(), is(equalTo(null))));
    }

    private static List<Serializable> contentWithSupInftagsScopus14244261628() {
        // This is an actual title from doi: 10.1016/j.nuclphysbps.2005.01.029
        return List.of("Non-factorizable contributions to B", generateInf("d"), generateSup("0"), " - D",
                       generateInf("s"), "(*) D", generateInf("s"), "(*)");
    }

    private static JAXBElement<InfTp> generateInf(Serializable content) {
        InfTp infTp = new InfTp();
        infTp.getContent().add(content);
        return new JAXBElement<>(new QName(INF_CLASS_NAME), InfTp.class, infTp);
    }

    private static JAXBElement<SupTp> generateSup(Serializable content) {
        SupTp supTp = new SupTp();
        supTp.getContent().add(content);
        return new JAXBElement<>(new QName(SUP_CLASS_NAME), SupTp.class, supTp);
    }

    private static Stream<Arguments> addLanguageEdgeCases() {
        return Stream.of(Arguments.of(null, UNDEFINED_LANGUAGE.getLexvoUri()),
                         Arguments.of(List.of(ENGLISH, NORWEGIAN), MULTIPLE.getLexvoUri()));
    }

    private static Arguments createArguments(Language language) {
        var languageUri = language.getLexvoUri();
        if (MISCELLANEOUS.equals(language)) {
            languageUri = MULTIPLE.getLexvoUri();
        }
        if (NORWEGIAN.equals(language)) {
            languageUri = BOKMAAL.getLexvoUri();
        }
        return Arguments.of(List.of(language), languageUri);
    }

    private static String getScopusIdentifier(Publication publication) {
        return publication.getAdditionalIdentifiers()
                   .stream()
                   .filter(id -> SCOPUS_IDENTIFIER.equals(id.getSourceName()))
                   .findFirst()
                   .map(AdditionalIdentifier::getValue)
                   .orElse(null);
    }

    private static Optional<String> toResponse(ImportCandidate importCandidate) {
        return Optional.of(String.valueOf(new ImportCandidateSearchApiResponse(
            List.of(ExpandedImportCandidate
                        .fromImportCandidate(importCandidate, mock(
                            no.unit.nva.publication.external.services.AuthorizedBackendUriRetriever.class))), 1)));
    }

    private static List<Affiliation> getActiveAffiliations(CristinPerson expectedCristinPerson) {
        return expectedCristinPerson.getAffiliations().stream().filter(Affiliation::isActive).toList();
    }

    private boolean hasScopusAuid(Contributor contributor) {
        return contributor.getIdentity()
                   .getAdditionalIdentifiers()
                   .stream()
                   .anyMatch(additionalIdentifier ->
                                 "scopus-auid".equals(additionalIdentifier.getSourceName()));
    }

    private String getEid() {
        return scopusData.getDocument().getMeta().getEid();
    }

    private String getSrctype() {
        return scopusData.getDocument().getMeta().getSrctype();
    }

    private String mockFetchCrossrefDoiResponse(ScopusGenerator scopusData, WireMockRuntimeInfo wireMockRuntimeInfo) {
        var downloadUrl = UriWrapper.fromUri(wireMockRuntimeInfo.getHttpBaseUrl()).addChild(randomString());
        stubFor(WireMock.get(urlPathEqualTo("/" + scopusData.getDocument().getMeta().getDoi()))
                    .willReturn(
                        aResponse().withBody(toCrossrefResponse(downloadUrl)).withStatus(HttpURLConnection.HTTP_OK)));
        var filename = randomString() + ".pdf";
        var testUrl = "/" + UriWrapper.fromUri(downloadUrl.getLastPathElement()).getLastPathElement();
        stubFor(WireMock.get(urlPathEqualTo(testUrl))
                    .willReturn(aResponse().withBody("abcde")
                                    .withHeader("Content-Type", "application/pdf;charset=UTF-8")
                                    .withHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                                    .withStatus(HttpURLConnection.HTTP_OK)));
        return filename;
    }

    private void mockBadRequestCrossrefDoiResponse(ScopusGenerator scopusData) {
        stubFor(WireMock.get(urlPathEqualTo("/" + scopusData.getDocument().getMeta().getDoi()))
                    .willReturn(aResponse().withBody(randomString()).withStatus(HttpURLConnection.HTTP_BAD_REQUEST)));
    }

    private void mockBadRequestFetchingFilesFromXml(ScopusGenerator scopusData) {
        scopusData.getDocument()
            .getMeta()
            .getOpenAccess()
            .getUpwOpenAccess()
            .getUpwOaLocations()
            .getUpwOaLocation()
            .forEach(ScopusHandlerTest::mockBadRequestFileResponse);
    }

    private OpenAccessType randomOpenAccess(WireMockRuntimeInfo wireMockRuntimeInfo) {
        var openAccess = new OpenAccessType();
        var upwOpenAccess = new UpwOpenAccessType();
        var locations = new UpwOaLocationsType();
        var locationList = IntStream.range(0, 3).mapToObj(i -> randomLocation(wireMockRuntimeInfo)).toList();
        locations.getUpwOaLocation().addAll(locationList);
        upwOpenAccess.setUpwOaLocations(locations);
        openAccess.setUpwOpenAccess(upwOpenAccess);
        return openAccess;
    }

    private UpwOaLocationType randomLocation(WireMockRuntimeInfo wireMockRuntimeInfo) {
        var location = new UpwOaLocationType();
        var string = UriWrapper.fromUri(wireMockRuntimeInfo.getHttpsBaseUrl()).addChild(randomString()).toString();
        location.setUpwUrlForPdf(string);
        return location;
    }

    private ImportCandidate createPersistedImportCandidate() {
        var importCandidate = randomImportCandidate();
        return resourceService.persistImportCandidate(importCandidate);
    }

    private ImportCandidate randomImportCandidate() {
        return new ImportCandidate.Builder().withImportStatus(ImportStatusFactory.createNotImported())
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
                   .withAdditionalIdentifiers(Set.of(new AdditionalIdentifier("Scopus", randomString())))
                   .withResourceOwner(new ResourceOwner(new Username(randomString()), randomUri()))
                   .withAssociatedArtifacts(List.of())
                   .build();
    }

    private EntityDescription randomEntityDescription() {
        return new EntityDescription.Builder().withPublicationDate(
                new PublicationDate.Builder().withYear("2020").build())
                   .withAbstract(randomString())
                   .withDescription(randomString())
                   .withContributors(List.of(randomContributor()))
                   .withMainTitle(randomString())
                   .build();
    }

    private Contributor randomContributor() {
        return new Contributor.Builder().withIdentity(new Identity.Builder().withName(randomString()).build())
                   .withRole(new RoleType(Role.ACTOR))
                   .build();
    }

    private void removePublishers() {
        var publishers = scopusData.getDocument()
                             .getItem()
                             .getItem()
                             .getBibrecord()
                             .getHead()
                             .getSource()
                             .getPublisher();
        publishers.removeAll(publishers);
    }

    private void mockAffiliationResponse(String cristinOrganizationId, AuthorGroupTp authorGroupTp) {
        generatePiaAndCristinAffiliationResponse(authorGroupTp, cristinOrganizationId);
    }

    private void hasAffiliationWithId(Contributor contributor, String cristinAffiliationId) {
        var affiliationIdList = contributor.getAffiliations()
                                    .stream()
                                    .filter(Organization.class::isInstance)
                                    .map(Organization.class::cast)
                                    .map(Organization::getId)
                                    .collect(Collectors.toSet());
        assertThat(affiliationIdList.toString(), containsString("cristin/organization/" + cristinAffiliationId));
    }

    private AuthorGroupTp generateAuthorGroup() {
        var authorGroup = new AuthorGroupTp();
        authorGroup.setAffiliation(randomAffiliation());
        authorGroup.getAuthorOrCollaboration().addAll(randomAuthorTpList());
        return authorGroup;
    }

    private List<Object> randomAuthorTpList() {
        return IntStream.range(1, 6).boxed().map(i -> scopusData.randomAuthorTp(i)).collect(Collectors.toList());
    }

    private AffiliationTp randomAffiliation() {
        var affiliation = new AffiliationTp();
        affiliation.setCountry("Norway");
        affiliation.setAfid(randomString());
        affiliation.setDptid(randomString());
        return affiliation;
    }

    private void generatePiaAffiliationsResponse(PiaResponseGenerator piaResponseGenerator, AuthorGroupTp authorGroupTp,
                                                 String cristinOrganizationId) {
        var affiliationList = piaResponseGenerator.generateAffiliations(cristinOrganizationId);
        createPiaAffiliationMock(affiliationList, authorGroupTp.getAffiliation().getAfid());
    }

    private ResourceService resourceServiceThrowingExceptionWhenSavingResource() {
        var resourceService = spy(getResourceServiceBuilder().build());
        doThrow(new RuntimeException(RESOURCE_EXCEPTION_MESSAGE)).when(resourceService)
            .createPublicationFromImportedEntry(any());
        doThrow(new RuntimeException(RESOURCE_EXCEPTION_MESSAGE)).when(resourceService).persistImportCandidate(any());
        return resourceService;
    }

    @NotNull
    private ScopusGenerator generateScopusDataWithOneAffiliation() {
        return ScopusGenerator.createWithSpecifiedAffiliations(List.of(createAffiliation(List.of("Some Name"))));
    }

    private Integer toCristinIdentifier(URI contributorId) {
        return nonNull(contributorId) ? Integer.parseInt(contributorId.toString().split("/")[3]) : null;
    }

    private void mockResponsesForAuthors(HashMap<Integer, AuthorTp> piaCristinIdAndAuthors,
                                         AuthorGroupTp authorGroupTp) {
        authorGroupTp.getAuthorOrCollaboration()
            .stream()
            .filter(author -> author instanceof AuthorTp)
            .map(authorTp -> (AuthorTp) authorTp)
            .map(author -> attempt(() -> generatePersonResponse(piaCristinIdAndAuthors, author)).orElseThrow())
            .collect(Collectors.toList());
    }

    private AuthorTp generatePersonResponse(HashMap<Integer, AuthorTp> piaCristinIdAndAuthors, AuthorTp authorTp) {
        var cristinId = randomInteger();
        piaCristinIdAndAuthors.put(cristinId, authorTp);
        mockedPiaAuthorIdSearch(authorTp.getAuid(), PiaResponseGenerator.convertAuthorsToJson(
            PiaResponseGenerator.generateAuthors(authorTp.getAuid(), cristinId)));
        generateCristinPersonsResponse(new ArrayList<>(), cristinId);
        return authorTp;
    }

    private List<AuthorGroupTp> keepOnlyAuthorGroups() {
        return new ArrayList<>(scopusData.getDocument().getItem().getItem().getBibrecord().getHead().getAuthorGroup());
    }

    private AuthorGroupTp getAuthorGroup() {
        return scopusData.getDocument().getItem().getItem().getBibrecord().getHead().getAuthorGroup().getFirst();
    }

    private String extractSuccessReport(S3Event s3Event, ImportCandidate importCandidate) {
        UriWrapper handleReport = UriWrapper.fromUri(SUCCESS_BUCKET_PATH)
                                      .addChild(
                                          s3Event.getRecords().getFirst().getEventTime().toString(YYYY_MM_DD_HH_FORMAT))
                                      .addChild(importCandidate.getIdentifier().toString());
        S3Driver s3Driver = new S3Driver(s3Client, new Environment().readEnv(SCOPUS_IMPORT_BUCKET));
        return s3Driver.getFile(handleReport.toS3bucketPath());
    }

    private Environment createPiaConnectionEnvironment(WireMockRuntimeInfo wireMockRuntimeInfo) {
        var environment = mock(Environment.class);
        when(environment.readEnv(PIA_REST_API_ENV_KEY)).thenReturn(
            wireMockRuntimeInfo.getHttpsBaseUrl().replace("https://", ""));
        when(environment.readEnv(API_HOST)).thenReturn(wireMockRuntimeInfo.getHttpsBaseUrl().replace("https://", ""));
        when(environment.readEnv(PIA_USERNAME_KEY)).thenReturn(PIA_USERNAME_SECRET_KEY);
        when(environment.readEnv(PIA_PASSWORD_KEY)).thenReturn(PIA_USERNAME_SECRET_KEY);
        when(environment.readEnv(PIA_SECRETS_NAME_ENV_KEY)).thenReturn(PIA_SECRET_NAME);
        return environment;
    }

    private void generatePiaResponseAndCristinPersons(HashMap<CristinPerson, AuthorTp> piaCristinIdAndAuthors) {
        generatePiaAuthorResponse(piaCristinIdAndAuthors);
        generateCristinPersonsResponse(piaCristinIdAndAuthors.keySet());
    }

    private void generateCristinPersonsResponse(Collection<CristinPerson> cristinPersons) {
        cristinPersons.forEach(person -> mockCristinPerson(UriWrapper.fromUri(person.getId()).getLastPathElement(),
                                                           CristinGenerator.convertPersonToJson(person)));
    }

    private void generateCristinPersonsResponse(ArrayList<CristinPerson> cristinCristinPeople, Integer cristinId) {
        var cristinPerson = CristinGenerator.generateCristinPerson(
            UriWrapper.fromUri("/cristin/person/" + cristinId.toString()).getUri(), randomString(), randomString());
        cristinCristinPeople.add(cristinPerson);
        mockCristinPerson(cristinId.toString(), CristinGenerator.convertPersonToJson(cristinPerson));
    }

    private CristinPerson randomCristinPerson() {
        return CristinGenerator.generateCristinPerson(
            UriWrapper.fromUri("/cristin/person/" + randomInteger().toString()).getUri(),
            randomString(),
            randomString());
    }

    private void generatePiaAuthorResponse(HashMap<CristinPerson, AuthorTp> piaCristinIdAndAuthors) {

        piaCristinIdAndAuthors
            .entrySet()
            .stream()
            .map(entry -> PiaResponseGenerator.generateAuthors(entry.getValue().getAuid(),
                                                               getCristinIdentifier(entry.getKey())))
            .forEach(this::createPiaAuthorMock);
    }

    private int getCristinIdentifier(CristinPerson person) {
        return Integer.parseInt(UriWrapper.fromUri(person.getId()).getLastPathElement());
    }

    private void generatePiaAuthorResponse(ArrayList<List<Author>> authors,
                                           Integer cristinId, AuthorTp authorTp) {
        var authorList = PiaResponseGenerator.generateAuthors(authorTp.getAuid(), cristinId);
        authors.add(authorList);
        createPiaAuthorMock(authorList);
    }

    private void generatePiaAndCristinAffiliationResponse(AuthorGroupTp authorGroupTp, String cristinOrganizationId) {
        var affiliation = new PiaResponseGenerator().generateAffiliation(cristinOrganizationId);
        createPiaAffiliationMock(List.of(affiliation), authorGroupTp.getAffiliation().getAfid());
        generateCristinOrganizationResponse(affiliation.getUnitIdentifier());
    }

    private void generateCristinOrganizationResponse(String cristinOrganizationId) {
        URI cristinOrgUri = UriWrapper.fromUri("cristin/organization/" + cristinOrganizationId).getUri();
        var cristinOrganization = CristinGenerator.generateCristinOrganization(cristinOrgUri);
        var organization = attempt(() -> CristinGenerator.convertOrganizationToJson(cristinOrganization)).orElseThrow();
        mockCristinOrganization(cristinOrganizationId, organization);
    }

    private ContentWrapper createContentWithSupAndInfTags() {
        return new ContentWrapper(contentWithSupInftagsScopus14244261628());
    }

    private void checkForDuplicateContributors(List<Contributor> contributors) {
        List<Integer> sequenceNumbers = new ArrayList<>();
        List<String> orcids = new ArrayList<>();
        contributors.forEach(contributor -> isNotDuplicated(sequenceNumbers, orcids, contributor));
    }

    private void isNotDuplicated(List<Integer> sequenceNumbers, List<String> orcids, Contributor contributor) {
        assertThat(sequenceNumbers, not(hasItem(contributor.getSequence())));
        sequenceNumbers.add(contributor.getSequence());
        if (nonNull(contributor.getIdentity().getOrcId())) {
            assertThat(orcids, not(hasItem(contributor.getIdentity().getOrcId())));
            orcids.add(contributor.getIdentity().getOrcId());
        }
    }

    private Contributor getCorrespondingContributor(List<Contributor> actualPublicationContributors) {
        createEmptyPiaMock();
        return actualPublicationContributors.stream().filter(Contributor::isCorrespondingAuthor).findAny().orElse(null);
    }

    private boolean isAuthor(Contributor contributor, Collection<AuthorTp> authorTypes) {
        return authorTypes.stream().anyMatch(authorTp -> isEqualContributor(contributor, authorTp));
    }

    private boolean isEqualContributor(Contributor contributor, AuthorTp authorTp) {
        var authorId = contributor.getIdentity().getAdditionalIdentifiers()
                           .stream()
                           .filter(additionalIdentifier ->
                                       "scopus-auid".equalsIgnoreCase(additionalIdentifier.getSourceName()))
                           .map(AdditionalIdentifier::getValue)
                           .collect(SingletonCollector.collectOrElse(""));
        return authorTp.getAuid().equals(authorId);
    }

    private void assertThatContributorHasCorrectCristinPersonData(Contributor contributor,
                                                                  HashMap<CristinPerson, AuthorTp> piaCristinIdAndAuthors) {
        var actualCristinId = contributor.getIdentity().getId();
        assertThat(actualCristinId, hasProperty("path", containsString("/cristin/person")));
        var expectedCristinPerson = getPersonByCristinNumber(piaCristinIdAndAuthors.keySet(),
                                                             actualCristinId).orElseThrow();
        var expectedName = calculateExpectedNameFromCristinPerson(expectedCristinPerson);

        assertThat(contributor.getIdentity().getName(), is(equalTo(expectedName)));

        assertThat(contributor.getAffiliations(), hasSize(getActiveAffiliations(expectedCristinPerson).size()));

        assertThat(contributor.getIdentity().getVerificationStatus(),
                   anyOf(equalTo(ContributorVerificationStatus.VERIFIED),
                         equalTo(ContributorVerificationStatus.NOT_VERIFIED)));

        var actualOrganizationFromAffiliation = contributor.getAffiliations()
                                                    .stream()
                                                    .filter(Organization.class::isInstance)
                                                    .map(Organization.class::cast)
                                                    .map(Organization::getId)
                                                    .collect(Collectors.toList());
        var expectedOrganizationFromAffiliation = expectedCristinPerson.getAffiliations()
                                                      .stream()
                                                      .filter(Affiliation::isActive)
                                                      .map(Affiliation::getOrganization)
                                                      .toList();

        assertThat(actualOrganizationFromAffiliation, containsInAnyOrder(
            expectedOrganizationFromAffiliation.stream().map(Matchers::equalTo).collect(Collectors.toList())));
    }

    private String calculateExpectedNameFromCristinPerson(CristinPerson cristinPerson) {
        return cristinPerson.getNames()
                   .stream()
                   .filter(this::isFirstName)
                   .findFirst()
                   .map(TypedValue::getValue)
                   .orElse(StringUtils.EMPTY_STRING) + StringUtils.SPACE + cristinPerson.getNames()
                                                                               .stream()
                                                                               .filter(this::isSurname)
                                                                               .findFirst()
                                                                               .map(TypedValue::getValue)
                                                                               .orElse(StringUtils.EMPTY_STRING);
    }

    private boolean isFirstName(TypedValue typedValue) {
        return ContributorExtractor.FIRST_NAME_CRISTIN_FIELD_NAME.equals(typedValue.getType());
    }

    private boolean isSurname(TypedValue nameType) {
        return ContributorExtractor.LAST_NAME_CRISTIN_FIELD_NAME.equals(nameType.getType());
    }

    @NotNull
    private Optional<CristinPerson> getPersonByCristinNumber(Collection<CristinPerson> cristinCristinPeople,
                                                             URI cristinId) {
        return cristinCristinPeople.stream().filter(person -> cristinId.equals(person.getId())).findFirst();
    }

    private void mockCristinPerson(String cristinPersonId, String response) {
        stubFor(WireMock.get(urlPathEqualTo("/cristin/person/" + cristinPersonId))
                    .willReturn(aResponse().withBody(response).withStatus(HttpURLConnection.HTTP_OK)));
    }

    private void mockCristinOrganization(String cristinId, String organization) {
        stubFor(WireMock.get(urlPathEqualTo("/cristin/organization/" + cristinId))
                    .willReturn(aResponse().withBody(organization).withStatus(HttpURLConnection.HTTP_OK)));
    }

    private void mockCristinPersonBadRequest() {
        stubFor(WireMock.get(urlMatching("/cristin/person/.*"))
                    .willReturn(aResponse().withStatus(HttpURLConnection.HTTP_BAD_REQUEST)));
    }

    private void createPiaAuthorMock(List<Author> author) {
        var scopusId = author.getFirst().getExternalId();
        var response = PiaResponseGenerator.convertAuthorsToJson(author);
        mockedPiaAuthorIdSearch(scopusId, response);
    }

    private void createPiaAffiliationMock(List<no.sikt.nva.scopus.conversion.model.pia.Affiliation> affiliationList,
                                          String affiliationId) {
        var response = PiaResponseGenerator.convertAffiliationsToJson(affiliationList);
        mockedPiaAffiliationIdSearch(affiliationId, response);
    }

    private void createEmptyPiaMock() {
        stubFor(WireMock.get(urlMatching("/sentralimport/authors"))
                    .willReturn(aResponse().withBody("[]").withStatus(HttpURLConnection.HTTP_OK)));
    }

    private void mockedPiaException() {
        stubFor(WireMock.get(urlPathEqualTo("/sentralimport/authors"))
                    .willReturn(aResponse().withFault(Fault.MALFORMED_RESPONSE_CHUNK)));
    }

    private void mockedPiaBadRequest() {
        stubFor(WireMock.get(urlPathEqualTo("/sentralimport/authors"))
                    .willReturn(aResponse().withStatus(HttpURLConnection.HTTP_BAD_REQUEST)));
    }

    private void mockedPiaAuthorIdSearch(String scopusId, String response) {
        stubFor(WireMock.get(urlPathEqualTo("/sentralimport/authors"))
                    .withQueryParam("author_id", WireMock.equalTo("SCOPUS:" + scopusId))
                    .willReturn(aResponse().withBody(response).withStatus(HttpURLConnection.HTTP_OK)));
    }

    private void mockedPiaAffiliationIdSearch(String affiliationId, String response) {

        stubFor(WireMock.get(urlPathEqualTo("/sentralimport/orgs/matches"))
                    .withQueryParam("affiliation_id", WireMock.equalTo("SCOPUS:" + affiliationId))
                    .willReturn(aResponse().withBody(response).withStatus(HttpURLConnection.HTTP_OK)));
    }

    private AffiliationTp createAffiliation(List<Serializable> organizationName) {
        var affiliation = new AffiliationTp();
        affiliation.setCountryAttribute(randomString());
        affiliation.setAffiliationInstanceId(randomString());
        affiliation.setAfid(randomString());
        affiliation.setCityGroup(randomString());
        affiliation.setDptid(randomString());
        affiliation.setCity(randomString());
        affiliation.setCountryAttribute(randomString());
        affiliation.getOrganization().add(createOrganization(organizationName));
        return affiliation;
    }

    private OrganizationTp createOrganization(List<Serializable> organizationName) {
        var organization = new OrganizationTp();
        organization.getContent().addAll(organizationName);
        return organization;
    }

    private void assertIsSameAuthor(AuthorTp authorTp, List<Contributor> contributors) {
        if (nonNull(authorTp.getOrcid())) {
            var orcidAsUriString = getOrcidAsUriString(authorTp);
            var optionalContributor = findContributorByOrcid(orcidAsUriString, contributors);
            assertTrue(optionalContributor.isPresent());
            var contributor = optionalContributor.get();
            assertThat(contributor.getIdentity().getName(), containsString(authorTp.getSurname()));
        }
    }

    private void checkContributor(AuthorTp authorTp, List<Contributor> contributors) {
        var contributor = findContributorByName(authorTp.getGivenName(), contributors);
        assertEquals(getExpectedFullAuthorName(authorTp), contributor.getIdentity().getName());
    }

    private Contributor findContributorByName(String givenName, List<Contributor> contributors) {
        return contributors.stream()
                   .filter(contributor -> contributor.getIdentity().getName().contains(givenName))
                   .findAny()
                   .orElseThrow();
    }

    private void checkCollaboration(CollaborationTp collaboration, List<Contributor> contributors) {
        var contributor = findContributorByName(collaboration.getIndexedName(), contributors);

        assertThat(contributor, is(notNullValue()));
    }

    private String getExpectedFullAuthorName(AuthorTp authorTp) {
        return authorTp.getPreferredName().getGivenName() + StringUtils.SPACE + authorTp.getPreferredName()
                                                                                    .getSurname();
    }

    private S3Event createNewScopusPublicationEvent() throws IOException {
        var uri = s3Driver.insertFile(randomS3Path(), scopusData.toXml());
        return createS3Event(uri);
    }

    private UnixPath randomS3Path() {
        return UnixPath.of(randomString());
    }

    private TitletextTp extractTitle(ScopusGenerator scopusData) {
        return Optional.of(scopusData.getDocument())
                   .map(DocTp::getItem)
                   .map(ItemTp::getItem)
                   .map(OrigItemTp::getBibrecord)
                   .map(BibrecordTp::getHead)
                   .map(HeadTp::getCitationTitle)
                   .map(CitationTitleTp::getTitletext)
                   .stream()
                   .flatMap(Collection::stream)
                   .filter(t -> YesnoAtt.Y.equals(t.getOriginal()))
                   .collect(SingletonCollector.collect());
    }

    private String expectedTitle(TitletextTp titleObject) {
        return ScopusConverter.extractContentAndPreserveXmlSupAndInfTags(titleObject.getContent());
    }

    private Optional<Contributor> findContributorByOrcid(String orcid, List<Contributor> contributors) {
        return contributors.stream()
                   .filter(contributor -> orcid.equals(contributor.getIdentity().getOrcId()))
                   .findFirst();
    }

    private String getOrcidAsUriString(AuthorTp authorTp) {
        return isNotBlank(authorTp.getOrcid()) ? craftOrcidUriString(authorTp.getOrcid()) : null;
    }

    private String craftOrcidUriString(String potentiallyMalformedOrcidString) {
        return potentiallyMalformedOrcidString.contains(ORCID_DOMAIN_URL) ? potentiallyMalformedOrcidString
                   : ORCID_DOMAIN_URL + potentiallyMalformedOrcidString;
    }

    private List<AuthorTp> keepOnlyTheAuthors() {
        return keepOnlyTheCollaborationsAndAuthors().stream()
                   .filter(this::isAuthorTp)
                   .map(author -> (AuthorTp) author)
                   .collect(Collectors.toList());
    }

    private Set<AuthorTp> keepOnlyAuthorsWithAuthorId() {
        return keepOnlyTheAuthors().stream()
                   .filter(authorTp -> StringUtils.isNotBlank(authorTp.getAuid()))

                   .collect(Collectors.toSet());
    }

    private List<CollaborationTp> keepOnlyTheCollaborations() {
        return keepOnlyTheCollaborationsAndAuthors().stream()
                   .filter(this::isCollaborationTp)
                   .map(collaboration -> (CollaborationTp) collaboration)
                   .collect(Collectors.toList());
    }

    private boolean isAuthorTp(Object object) {
        return object instanceof AuthorTp;
    }

    private boolean isCollaborationTp(Object object) {
        return object instanceof CollaborationTp;
    }

    private List<Object> keepOnlyTheCollaborationsAndAuthors() {
        return scopusData.getDocument()
                   .getItem()
                   .getItem()
                   .getBibrecord()
                   .getHead()
                   .getAuthorGroup()
                   .stream()
                   .map(AuthorGroupTp::getAuthorOrCollaboration)
                   .flatMap(Collection::stream)
                   .collect(Collectors.toList());
    }

    private S3Event createS3Event(String expectedObjectKey) {
        var eventNotification = new S3EventNotificationRecord(randomString(), randomString(), randomString(),
                                                              randomDate(), randomString(), EMPTY_REQUEST_PARAMETERS,
                                                              EMPTY_RESPONSE_ELEMENTS,
                                                              createS3Entity(expectedObjectKey), EMPTY_USER_IDENTITY);
        return new S3Event(List.of(eventNotification));
    }

    private S3Event createS3Event(URI uri) {
        return createS3Event(UriWrapper.fromUri(uri).toS3bucketPath().toString());
    }

    private String randomDate() {
        return Instant.now().toString();
    }

    private S3Entity createS3Entity(String expectedObjectKey) {
        var bucket = new S3BucketEntity(randomString(), EMPTY_USER_IDENTITY, randomString());
        var object = new S3ObjectEntity(expectedObjectKey, SOME_FILE_SIZE, randomString(), randomString(),
                                        randomString());
        var schemaVersion = randomString();
        return new S3Entity(randomString(), bucket, object, schemaVersion);
    }

    private JsonNode extractActualReportFromS3Client(S3Event s3Event, Exception exception)
        throws JsonProcessingException {
        UriWrapper errorFileUri = UriWrapper.fromUri(ERROR_BUCKET_PATH
                                                     + PATH_SEPERATOR
                                                     + s3Event.getRecords()
                                                           .getFirst()
                                                           .getEventTime()
                                                           .toString(YYYY_MM_DD_HH_FORMAT)
                                                     + PATH_SEPERATOR
                                                     + exception.getClass().getSimpleName()
                                                     + PATH_SEPERATOR
                                                     + UriWrapper.fromUri(
            s3Event.getRecords().getFirst().getS3().getObject().getKey()).getLastPathElement());
        S3Driver s3Driver = new S3Driver(s3Client, new Environment().readEnv(SCOPUS_IMPORT_BUCKET));
        String content = s3Driver.getFile(errorFileUri.toS3bucketPath());
        return JsonUtils.dtoObjectMapper.readTree(content);
    }

    private static class FakeS3ClientThrowingException extends FakeS3Client {

        private final String expectedErrorMessage;

        public FakeS3ClientThrowingException(String expectedErrorMessage) {
            super();
            this.expectedErrorMessage = expectedErrorMessage;
        }

        @Override
        public <ReturnT> ReturnT getObject(GetObjectRequest getObjectRequest,
                                           ResponseTransformer<GetObjectResponse, ReturnT> responseTransformer) {
            throw new RuntimeException(expectedErrorMessage);
        }
    }

    public static class FakeS3cClientWithHeadSupport extends FakeS3Client {

        public static final long SOME_CONTENT_LENGTH = 2932645L;
        public static final String APPLICATION_PDF_MIMETYPE = "application/pdf";
        private final List<CopyObjectRequest> copyObjectRequestList;

        public FakeS3cClientWithHeadSupport() {
            this.copyObjectRequestList = new ArrayList<>();
        }

        @Override
        public CopyObjectResponse copyObject(CopyObjectRequest copyObjectRequest) {
            copyObjectRequestList.add(copyObjectRequest);
            return CopyObjectResponse.builder().build();
        }

        @Override
        public HeadObjectResponse headObject(HeadObjectRequest headObjectRequest) {
            return HeadObjectResponse.builder()
                       .contentLength(SOME_CONTENT_LENGTH)
                       .contentType(APPLICATION_PDF_MIMETYPE)
                       .build();
        }
    }
}