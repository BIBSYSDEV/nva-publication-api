package no.unit.nva.model.file;

import static java.time.temporal.ChronoUnit.DAYS;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFields;
import static no.unit.nva.model.associatedartifacts.RightsRetentionStrategyConfiguration.OVERRIDABLE_RIGHTS_RETENTION_STRATEGY;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomOpenFile;
import static no.unit.nva.testutils.RandomDataGenerator.randomBoolean;
import static no.unit.nva.testutils.RandomDataGenerator.randomInstant;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.Username;
import no.unit.nva.model.associatedartifacts.AssociatedArtifact;
import no.unit.nva.model.associatedartifacts.CustomerRightsRetentionStrategy;
import no.unit.nva.model.associatedartifacts.NullRightsRetentionStrategy;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.InternalFile;
import no.unit.nva.model.associatedartifacts.file.MissingLicenseException;
import no.unit.nva.model.associatedartifacts.file.OpenFile;
import no.unit.nva.model.associatedartifacts.file.PendingFile;
import no.unit.nva.model.associatedartifacts.file.PendingOpenFile;
import no.unit.nva.model.associatedartifacts.file.PublisherVersion;
import no.unit.nva.model.associatedartifacts.file.UploadDetails;
import no.unit.nva.model.associatedartifacts.file.UploadedFile;
import no.unit.nva.model.associatedartifacts.file.UserUploadDetails;
import no.unit.nva.model.testing.associatedartifacts.util.RightsRetentionStrategyGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.MethodSource;

public class FileModelTest {

    public static final URI LICENSE_URI = URI.create("http://creativecommons.org/licenses/by/4.0/");
    public static final String APPLICATION_PDF = "application/pdf";
    public static final String FIRST_FILE_TXT = "First_file.txt";
    public static final long SIZE = 200L;
    public static final ObjectMapper dataModelObjectMapper = JsonUtils.dtoObjectMapper;

    public static File randomPendingOpenFile() {
        return buildNonAdministrativeAgreement().buildPendingOpenFile();
    }

    public static Stream<File> notAdministrativeAgreements() {
        return Stream.of(randomPendingOpenFile(), internalFile());
    }

    public static File.Builder buildNonAdministrativeAgreement() {
        return File.builder()
                   .withName(randomString())
                   .withMimeType(randomString())
                   .withSize(randomInteger().longValue())
                   .withEmbargoDate(randomInstant())
                   .withLicense(LICENSE_URI)
                   .withIdentifier(UUID.randomUUID())
                   .withUploadDetails(randomInserted())
                   .withPublisherVersion(randomPublisherVersion());
    }

    @ParameterizedTest
    @ArgumentsSource(FileProvider.class)
    @DisplayName("Should round trip all file types")
    void shouldRoundTripAllFileTypes(AssociatedArtifact file) throws JsonProcessingException {
        var json = JsonUtils.dtoObjectMapper.writeValueAsString(file);
        var deserialized = JsonUtils.dtoObjectMapper.readValue(json, File.class);
        if (deserialized instanceof UploadedFile) {
            assertThat(deserialized,
                       doesNotHaveEmptyValuesIgnoringFields(Set.of(
                           ".rightsRetentionStrategy", "legalNote", ".publisherVersion", ".license")));
        } else {
            assertThat(deserialized, doesNotHaveEmptyValuesIgnoringFields(Set.of(".rightsRetentionStrategy", "legalNote")));
            assertThat(deserialized, is(equalTo(file)));
        }
    }

    @Test
    void shouldThrowMissingLicenseExceptionWhenFileIsNotAdministrativeAgreementAndLicenseIsMissing() {
        var file = getOpenFile();
        assertThrows(MissingLicenseException.class, file::validate);
    }

    @Test
    void shouldNotThrowMissingLicenseExceptionWhenFileIsAdministrativeAgreementAndLicenseIsPresent() {
        var file = getInternalFile(LICENSE_URI);
        assertDoesNotThrow(file::validate);
    }

    @Test
    void shouldNotThrowCcbyLicenseExceptionWhenNotCustomerRrs() throws JsonProcessingException {
        var file = JsonUtils.dtoObjectMapper.readValue(generateNewFile(), File.class);
        assertDoesNotThrow(file::validate);
    }

    @Test
    void shouldAssignDefaultStrategyWhenNoneProvided() throws JsonProcessingException {
        var file = JsonUtils.dtoObjectMapper.readValue(generateNewFile(), File.class);
        assertThat(file.getRightsRetentionStrategy(), instanceOf(NullRightsRetentionStrategy.class));
    }

    @Test
    void shouldSetNewRightsRetentionStrategy() {
        var file = getOpenFile();
        var rightsRetentionStrategy = CustomerRightsRetentionStrategy.create(OVERRIDABLE_RIGHTS_RETENTION_STRATEGY);
        file.setRightsRetentionStrategy(rightsRetentionStrategy);
        assertThat(file.getRightsRetentionStrategy(), is(equalTo(rightsRetentionStrategy)));
    }

    @ParameterizedTest(name = "should not throw MissingLicenseException when not administrative agreement")
    @MethodSource("notAdministrativeAgreements")
    void shouldNotThrowMissingLicenseExceptionWhenFileIsNotAdministrativeAgreementAndLicenseIsPresent(File file) {
        assertDoesNotThrow(file::validate);
    }

    @Test
    void shouldNotBeVisibleForNonOwnersWhenFileIsAdministrativeAgreement() {
        var file = randomInternalFile();
        assertFalse(file.isVisibleForNonOwner());
    }

    @Test
    void shouldNotBeVisibleForNonOwnersWhenFileIsEmbargoed() {
        var embargoedFile = openFileWithActiveEmbargo();
        assertFalse(embargoedFile.isVisibleForNonOwner());
    }

    @Test
    void shouldNotBeVisibleForNonOwnerWhenUnpublished() throws JsonProcessingException {
        var file = randomPendingOpenFile();
        var mapped = dataModelObjectMapper.writeValueAsString(file);
        var unmapped = dataModelObjectMapper.readValue(mapped, File.class);

        assertThat(file.isVisibleForNonOwner(), equalTo(false));
        assertThat(unmapped.isVisibleForNonOwner(), equalTo(false));
    }

    /**
     * @deprecated remove when PublisherVersion no longer needs to parse boolean
     */
    @Deprecated
    @Test
    void objectMapperShouldSerializeAndDeserializePublishedVersion() throws JsonProcessingException {
        var file = new PendingOpenFile(UUID.randomUUID(), randomString(), randomString(), 10L, null,
                                       PublisherVersion.ACCEPTED_VERSION, null, null, randomString(), randomInserted());
        var fileAsString = file.toString();
        var roundTrippedFile = dataModelObjectMapper.readValue(fileAsString, PendingOpenFile.class);
        assertThat(roundTrippedFile.getPublisherVersion(), is(equalTo(PublisherVersion.ACCEPTED_VERSION)));
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenApprovingPendingFileWithoutLicense() {
        var fileWithoutLicense = randomPendingOpenFile().copy().withLicense(null).buildPendingOpenFile();
        var pendingFile = (PendingFile<?, ?>) fileWithoutLicense;

        assertThrows(IllegalStateException.class, pendingFile::approve,
                     "Cannot publish a file without a license: " + fileWithoutLicense.getIdentifier());
    }

    @Test
    void shouldKeepLicenseAsIs() {
        var license = URI.create("https://rightsstatements.org/vocab/InC/1.0/");
        var file = randomOpenFile().copy().withLicense(license).build(OpenFile.class);

        assertEquals(license, file.getLicense());
    }

    private static Username randomUsername() {
        return new Username(randomInteger().toString() + "@" + randomString());
    }

    private static UploadDetails randomInserted() {
        return new UserUploadDetails(randomUsername(), randomInstant());
    }

    private static PublisherVersion randomPublisherVersion() {
        return randomBoolean() ? PublisherVersion.PUBLISHED_VERSION : PublisherVersion.ACCEPTED_VERSION;
    }

    private static File internalFile() {
        return new InternalFile(UUID.randomUUID(), randomString(), randomString(), randomInteger().longValue(),
                                LICENSE_URI, PublisherVersion.ACCEPTED_VERSION, randomInstant(),
                                RightsRetentionStrategyGenerator.randomRightsRetentionStrategy(), randomString(),
                                randomInstant(), randomInserted());
    }

    private static String generateNewFile() {
        return """
            {
                "type" : "OpenFile",
                "identifier" : "d9fc5844-f1a3-491b-825a-5a4cabc12aa2",
                "name" : "Per Magne Østertun.pdf",
                "mimeType" : "application/pdf",
                "size" : 1025817,
                "license" : "https://creativecommons.org/licenses/by-nc/2.0/",
                "administrativeAgreement" : false,
                "publisherAuthority" : false,
                "publishedDate" : "2023-05-25T19:31:17.302914Z",
                "visibleForNonOwner" : true
            }""";
    }

    private InternalFile getInternalFile(URI license) {
        return File.builder()
                   .withIdentifier(UUID.randomUUID())
                   .withLicense(license)
                   .withName(FileModelTest.FIRST_FILE_TXT)
                   .buildInternalFile()
                   .toInternalFile();
    }

    private InternalFile randomInternalFile() {
        return new InternalFile(UUID.randomUUID(), randomString(), randomString(), randomInteger().longValue(),
                                LICENSE_URI, PublisherVersion.ACCEPTED_VERSION, randomInstant(),
                                RightsRetentionStrategyGenerator.randomRightsRetentionStrategy(), randomString(),
                                randomInstant(), randomInserted());
    }

    private OpenFile openFileWithActiveEmbargo() {
        return new OpenFile(UUID.randomUUID(), randomString(), randomString(), randomInteger().longValue(), LICENSE_URI,
                            PublisherVersion.ACCEPTED_VERSION, Instant.now().plus(1, DAYS),
                            RightsRetentionStrategyGenerator.randomRightsRetentionStrategy(), randomString(),
                            randomInstant(), randomInserted());
    }

    private File getOpenFile() {
        return File.builder()
                   .withEmbargoDate(null)
                   .withIdentifier(UUID.randomUUID())
                   .withLicense(null)
                   .withMimeType(APPLICATION_PDF)
                   .withName(FIRST_FILE_TXT)
                   .withPublisherVersion(PublisherVersion.PUBLISHED_VERSION)
                   .withSize(SIZE)
                   .buildOpenFile();
    }
}
