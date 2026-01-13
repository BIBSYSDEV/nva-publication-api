package no.unit.nva.publication.s3imports;

import static java.util.UUID.randomUUID;
import static javax.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.core.ioutils.IoUtils.stringToStream;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import javax.xml.bind.JAXBContext;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.ImportUploadDetails;
import no.unit.nva.model.associatedartifacts.file.ImportUploadDetails.Source;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.stubs.FakeS3Client;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.Environment;
import nva.commons.core.paths.UnixPath;
import nva.commons.logutils.LogUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

class UpdatePublicationsFromBrageHandlerTest extends ResourcesLocalTest {

    private static final String PERSISTED_STORAGE_BUCKET_NAME = new Environment().readEnv(
        "NVA_PERSISTED_STORAGE_BUCKET_NAME");
    private static final String DUBLIN_CORE_XML_FILE_NAME = "dublin_core.xml";
    private S3Client s3Client;
    private ResourceService resourceService;
    private UpdatePublicationsFromBrageHandler handler;
    private ByteArrayOutputStream output;

    @BeforeEach
    void setUp() {
        super.init();
        resourceService = getResourceService(client);
        s3Client = new FakeS3Client();
        handler = new UpdatePublicationsFromBrageHandler(resourceService, s3Client, new Environment());
        output = new ByteArrayOutputStream();
    }

    @Test
    void shouldThrowExceptionWhenInputIsMissingMandatoryFields() {
        assertThrows(NullPointerException.class, () -> new UpdatePublicationsFromBrageRequest(null, null));
    }

    @Test
    void shouldThrowExceptionWhenFileForProvidedUriIsNotPresent() {
        var request = new UpdatePublicationsFromBrageRequest(randomUri(), randomString());
        var input = stringToStream(request.toJsonString());

        assertThrows(NoSuchKeyException.class, () -> handler.handleRequest(input, output, new FakeContext()));
    }

    @Test
    void shouldThrowExceptionWhenCsvFileIsNotParsable() throws IOException {
        var uri = randomUri();
        var request = new UpdatePublicationsFromBrageRequest(uri, randomString());
        var input = stringToStream(request.toJsonString());
        insertFile(uri, "identifier, asoidhjoeift\noiwheohomoaishdoihgfw");

        assertThrows(RuntimeException.class, () -> handler.handleRequest(input, output, new FakeContext()));
    }

    @Test
    void shouldNotThrowExceptionWhenPublicationWithIdentifierFromCsvDoesNotExist() throws IOException {
        var request = new UpdatePublicationsFromBrageRequest(randomUri(), randomString());
        var input = stringToStream(request.toJsonString());
        insertFile(request.uri(), randomCsvContent(SortableIdentifier.next()));

        assertDoesNotThrow(() -> handler.handleRequest(input, output, new FakeContext()));
    }

    @Test
    void shouldLogPublicationIdentifierWhichDoesNotExist() throws IOException {
        var request = new UpdatePublicationsFromBrageRequest(randomUri(), randomString());
        var publicationIdentifier = SortableIdentifier.next();
        insertFile(request.uri(), randomCsvContent(publicationIdentifier));
        var logAppender = LogUtils.getTestingAppender(UpdatePublicationsFromBrageHandler.class);

        handler.handleRequest(stringToStream(request.toJsonString()), output, new FakeContext());

        assertTrue(
            logAppender.getMessages().contains("Publication does not exist %s".formatted(publicationIdentifier)));
    }

    @Test
    void shouldNotThrowExceptionWhenDublinCoreDoesNotExist() throws IOException, BadRequestException {
        var request = new UpdatePublicationsFromBrageRequest(randomUri(), randomString());
        var publication = Resource.fromPublication(randomPublication()).persistNew(resourceService, userInstance());
        insertFile(request.uri(), randomCsvContent(publication.getIdentifier()));
        var logAppender = LogUtils.getTestingAppender(UpdatePublicationsFromBrageHandler.class);

        handler.handleRequest(stringToStream(request.toJsonString()), output, new FakeContext());

        assertTrue(logAppender.getMessages()
                       .contains(
                           ("Dublin core does not exist at publication %s").formatted(publication.getIdentifier())));
    }

    @Test
    void shouldLogSuccessWhenDublinCoreExists() throws IOException, BadRequestException {
        var archive = randomString();
        var request = new UpdatePublicationsFromBrageRequest(randomUri(), archive);
        var publication = persistPublicationWithDublinCoreFromArchive(archive);
        insertFile(request.uri(), randomCsvContent(publication.getIdentifier()));
        var logAppender = LogUtils.getTestingAppender(UpdatePublicationsFromBrageHandler.class);

        handler.handleRequest(stringToStream(request.toJsonString()), output, new FakeContext());

        assertTrue(logAppender.getMessages()
                       .contains(("Successfully parsed dublin core").formatted(publication.getIdentifier())));
    }

    private static UserInstance userInstance() {
        return UserInstance.create(randomString(), randomUri());
    }

    private Publication persistPublicationWithDublinCoreFromArchive(String archive)
        throws BadRequestException, IOException {
        var publication = randomPublication();
        var identifier = randomUUID();
        var dublinCore = File.builder()
                             .withIdentifier(identifier)
                             .withName(DUBLIN_CORE_XML_FILE_NAME)
                             .withUploadDetails(new ImportUploadDetails(Source.BRAGE, archive, Instant.now()))
                             .buildHiddenFile();
        var dublinCoreFile = new DublinCore(List.of(new DcValue(Element.IDENTIFIER, Qualifier.URI, randomString())));
        new S3Driver(s3Client, PERSISTED_STORAGE_BUCKET_NAME).insertFile(UnixPath.fromString(identifier.toString()),
                                                                         dublinCoreToXml(dublinCoreFile));
        publication.setAssociatedArtifacts(new AssociatedArtifactList(List.of(dublinCore)));
        return Resource.fromPublication(publication).persistNew(resourceService, userInstance());
    }

    private String randomCsvContent(SortableIdentifier identifier) {
        return """
            identifier
            %s
            """.formatted(identifier);
    }

    private String dublinCoreToXml(DublinCore dublinCore) {
        try {
            var marshaller = JAXBContext.newInstance(DublinCore.class).createMarshaller();
            marshaller.setProperty(JAXB_FORMATTED_OUTPUT, true);
            var stringWriter = new StringWriter();
            marshaller.marshal(dublinCore, stringWriter);
            return stringWriter.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize DublinCore to XML", e);
        }
    }

    private void insertFile(URI uri, String content) throws IOException {
        new S3Driver(s3Client, uri.getHost()).insertFile(UnixPath.of(uri.getPath()), content);
    }
}