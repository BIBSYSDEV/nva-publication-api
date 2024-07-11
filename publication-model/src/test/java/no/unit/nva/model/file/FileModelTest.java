package no.unit.nva.model.file;

import static java.time.temporal.ChronoUnit.DAYS;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFields;
import static no.unit.nva.model.associatedartifacts.RightsRetentionStrategyConfiguration.OVERRIDABLE_RIGHTS_RETENTION_STRATEGY;
import static no.unit.nva.testutils.RandomDataGenerator.randomBoolean;
import static no.unit.nva.testutils.RandomDataGenerator.randomInstant;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
import no.unit.nva.model.associatedartifacts.CustomerRightsRetentionStrategy;
import no.unit.nva.model.associatedartifacts.NullRightsRetentionStrategy;
import no.unit.nva.model.associatedartifacts.file.AdministrativeAgreement;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.License;
import no.unit.nva.model.associatedartifacts.file.MissingLicenseException;
import no.unit.nva.model.associatedartifacts.file.PublishedFile;
import no.unit.nva.model.associatedartifacts.file.PublisherVersion;
import no.unit.nva.model.associatedartifacts.file.UnpublishedFile;
import no.unit.nva.model.associatedartifacts.file.UploadDetails;
import no.unit.nva.model.testing.associatedartifacts.util.RightsRetentionStrategyGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class FileModelTest {

    public static final URI LICENSE_URI = URI.create("http://creativecommons.org/licenses/by/4.0/");
    public static final String APPLICATION_PDF = "application/pdf";
    public static final String FIRST_FILE_TXT = "First_file.txt";
    public static final String CC_BY = "CC-BY";
    public static final long SIZE = 200L;
    public static final ObjectMapper dataModelObjectMapper = JsonUtils.dtoObjectMapper;
    public static final boolean NOT_ADMINISTRATIVE_AGREEMENT = false;
    private static final boolean ADMINISTRATIVE_AGREEMENT = true;

    public static Stream<File> fileProvider() {
        var publishedFile = randomPublishedFile();
        var unpublishedFile = randomUnpublishedFile();
        var unpublishableFile = randomUnpublishableFile();
        return Stream.of(publishedFile, unpublishedFile, unpublishableFile);
    }

    public static Stream<File> notAdministrativeAgreements() {
        return Stream.of(randomPublishedFile(), randomUnpublishedFile(),
            unpublishableNotAdministrativeAgreement());
    }

    @ParameterizedTest
    @MethodSource("fileProvider")
    void shouldRoundTripAllFileTypes(File file) throws JsonProcessingException {
        var json = JsonUtils.dtoObjectMapper.writeValueAsString(file);
        var deserialized = JsonUtils.dtoObjectMapper.readValue(json, File.class);
        assertThat(deserialized, doesNotHaveEmptyValuesIgnoringFields(Set.of(".rightsRetentionStrategy", "legalNote")));
        assertThat(deserialized, is(equalTo(file)));
    }

    @Test
    void shouldThrowMissingLicenseExceptionWhenFileIsNotAdministrativeAgreementAndLicenseIsMissing() {
        var file = getPublishedFile();
        assertThrows(MissingLicenseException.class, file::validate);
    }

    @Test
    void shouldNotThrowMissingLicenseExceptionWhenFileIsAdministrativeAgreementAndLicenseIsMissing() {
        var file = getAdministrativeAgreement(null);
        assertDoesNotThrow(file::validate);
    }

    @Test
    void shouldNotThrowMissingLicenseExceptionWhenFileIsAdministrativeAgreementAndLicenseIsPresent() {
        var file = getAdministrativeAgreement(LICENSE_URI);
        assertDoesNotThrow(file::validate);
    }

    @Test
    void shouldNotThrowCcbyLicenseExceptionWhenNotCustomerRrs() throws JsonProcessingException {
        var file = JsonUtils.dtoObjectMapper.readValue(generateNewFile(), File.class);
        assertDoesNotThrow(file::validate);
    }

    @Test
    public void shouldAssignDefaultStrategyWhenNoneProvided() throws JsonProcessingException {
        var file = JsonUtils.dtoObjectMapper.readValue(generateNewFile(), File.class);
        assertThat(file.getRightsRetentionStrategy(), instanceOf(NullRightsRetentionStrategy.class));
    }

    @Test
    public void shouldSetNewRightsRetentionStrategy() {
        var file = getPublishedFile();
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
    void shouldReturnEmptySetWhenLicenseLabelsAreNull() {
        var license = new License.Builder()
                          .withIdentifier(CC_BY)
                          .withLink(LICENSE_URI)
                          .withLabels(null)
                          .build();
        assertThat(license.getLabels(), is(anEmptyMap()));
    }

    @ParameterizedTest
    @MethodSource("fileProvider")
    void shouldReturnSerializedModel(File file) throws JsonProcessingException {
        var mapped = dataModelObjectMapper.writeValueAsString(file);
        var unmapped = dataModelObjectMapper.readValue(mapped, File.class);
        assertThat(file, equalTo(unmapped));
        assertThat(file, doesNotHaveEmptyValuesIgnoringFields(Set.of(".rightsRetentionStrategy", "legalNote")));
    }

    @Test
    void shouldNotBeVisibleForNonOwnersWhenFileIsAdministrativeAgreement() {
        var file = randomAdministrativeAgreement();
        assertFalse(file.isVisibleForNonOwner());
    }

    @Test
    void shouldNotBeVisibleForNonOwnersWhenFileIsEmbargoed() {
        var embargoedFile = publishedFileWithActiveEmbargo();
        assertFalse(embargoedFile.isVisibleForNonOwner());
    }

    @Test
    void shouldNotAllowPublishableFilesToBeAdministrativeAgreements() {
        assertThrows(IllegalStateException.class, this::illegalPublishedFile);
        assertThrows(IllegalStateException.class, this::illegalUnPublishedFile);
    }

    @Test
    void shouldNotBeVisibleForNonOwnerWhenUnpublished() throws JsonProcessingException {
        var file = randomUnpublishedFile();
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
        var unpublishedFile = new UnpublishedFile(UUID.randomUUID(), randomString(), randomString(), 10L, null,
                                                  false, PublisherVersion.ACCEPTED_VERSION, null, null, randomString(), randomInserted());
        var unpublishedFileString = unpublishedFile.toString();
        var publicationAfterRoundTrip = dataModelObjectMapper.readValue(unpublishedFileString, UnpublishedFile.class);
        assertThat(publicationAfterRoundTrip.getPublisherVersion(), is(equalTo(PublisherVersion.ACCEPTED_VERSION)));
    }

    private static UploadDetails randomInserted() {
        return new UploadDetails(randomUsername(), randomInstant());
    }

    public static File randomUnpublishableFile() {
        return new AdministrativeAgreement(UUID.randomUUID(), randomString(), randomString(),
            randomInteger().longValue(), LICENSE_URI, randomBoolean(), PublisherVersion.ACCEPTED_VERSION, randomInstant(),
                                           randomInserted());
    }

    private static Username randomUsername() {
        return new Username(randomString());
    }

    public static File randomUnpublishedFile() {
        return buildNonAdministrativeAgreement().buildUnpublishedFile();
    }

    public static File randomPublishedFile() {
        return buildNonAdministrativeAgreement().buildPublishedFile();
    }

    public static File.Builder buildNonAdministrativeAgreement() {
        return File.builder()
                   .withName(randomString())
                   .withAdministrativeAgreement(NOT_ADMINISTRATIVE_AGREEMENT)
                   .withMimeType(randomString())
                   .withSize(randomInteger().longValue())
                   .withEmbargoDate(randomInstant())
                   .withLicense(LICENSE_URI)
                   .withIdentifier(UUID.randomUUID())
                   .withUploadDetails(randomInserted())
                   .withPublisherVersion(randomPublisherVersion());
    }

    private static PublisherVersion randomPublisherVersion() {
        return randomBoolean() ? PublisherVersion.PUBLISHED_VERSION : PublisherVersion.ACCEPTED_VERSION;
    }

    public static File.Builder buildAdministrativeAgreement() {
        return File.builder()
                   .withName(randomString())
                   .withAdministrativeAgreement(ADMINISTRATIVE_AGREEMENT)
                   .withMimeType(randomString())
                   .withSize(randomInteger().longValue())
                   .withEmbargoDate(randomInstant())
                   .withRightsRetentionStrategy(RightsRetentionStrategyGenerator.randomRightsRetentionStrategy())
                   .withLicense(LICENSE_URI)
                   .withIdentifier(UUID.randomUUID())
                   .withPublisherVersion(randomPublisherVersion());
    }

    private static File unpublishableNotAdministrativeAgreement() {
        return new AdministrativeAgreement(UUID.randomUUID(),
            randomString(),
            randomString(),
            randomInteger().longValue(),
            LICENSE_URI,
            NOT_ADMINISTRATIVE_AGREEMENT,
            PublisherVersion.ACCEPTED_VERSION,
            randomInstant(),
            randomInserted());
    }

    private static File.Builder admAgreementBuilder() {
        return File.builder()
                   .withAdministrativeAgreement(true)
                   .withName(randomString());
    }

    private File getAdministrativeAgreement(URI license) {
        return File.builder()
                   .withIdentifier(UUID.randomUUID())
                   .withLicense(license)
                   .withName(FileModelTest.FIRST_FILE_TXT)
                   .withAdministrativeAgreement(true)
                   .buildUnpublishableFile();
    }

    private AdministrativeAgreement randomAdministrativeAgreement() {
        return new AdministrativeAgreement(UUID.randomUUID(), randomString(), randomString(),
            randomInteger().longValue(),
            LICENSE_URI, ADMINISTRATIVE_AGREEMENT, PublisherVersion.ACCEPTED_VERSION, randomInstant(), randomInserted());
    }

    private PublishedFile publishedFileWithActiveEmbargo() {
        return new PublishedFile(UUID.randomUUID(),
                                 randomString(),
                                 randomString(),
                                 randomInteger().longValue(),
                                 LICENSE_URI,
                                 NOT_ADMINISTRATIVE_AGREEMENT,
                                 PublisherVersion.ACCEPTED_VERSION,
                                 Instant.now().plus(1, DAYS),
                                 RightsRetentionStrategyGenerator.randomRightsRetentionStrategy(),
                                 randomString(), randomInstant(), randomInserted());
    }

    private void illegalPublishedFile() {
        admAgreementBuilder().buildPublishedFile();
    }

    private void illegalUnPublishedFile() {
        admAgreementBuilder().buildUnpublishedFile();
    }

    private File getPublishedFile() {
        return getPublishedFile(UUID.randomUUID());
    }

    private File getPublishedFile(UUID identifier) {
        return File.builder()
                   .withAdministrativeAgreement(NOT_ADMINISTRATIVE_AGREEMENT)
                   .withEmbargoDate(null)
                   .withIdentifier(identifier)
                   .withLicense(null)
                   .withMimeType(APPLICATION_PDF)
                   .withName(FIRST_FILE_TXT)
                   .withPublisherVersion(PublisherVersion.PUBLISHED_VERSION)
                   .withSize(SIZE)
                   .buildPublishedFile();
    }



    @Deprecated
    @Test
    void shouldMigrateLegacyFileToUnpublishedFile() throws JsonProcessingException {
        var fileJson = "{\n"
                       + "    \"type\" : \"File\",\n"
                       + "    \"identifier\" : \"df2be965-f628-43fb-914b-e16d6f136e05\",\n"
                       + "    \"name\" : \"2-s2.0-85143901828.xml\",\n"
                       + "    \"mimeType\" : \"text/xml\",\n"
                       + "    \"size\" : 180088,\n"
                       + "    \"license\" : {\n"
                       + "      \"type\" : \"License\",\n"
                       + "      \"identifier\" : \"RightsReserved\",\n"
                       + "      \"labels\" : {\n"
                       + "        \"nb\" : \"RightsReserved\"\n"
                       + "      }\n"
                       + "    },\n"
                       + "    \"administrativeAgreement\" : false,\n"
                       + "    \"publisherAuthority\" : false,\n"
                       + "    \"visibleForNonOwner\" : true\n"
                       + "  }";
        var file = JsonUtils.dtoObjectMapper.readValue(fileJson, File.class);
        assertThat(file, instanceOf(UnpublishedFile.class));
    }

    private static String generateNewFile() {
        return "{\n"
               + "    \"type\" : \"PublishedFile\",\n"
               + "    \"identifier\" : \"d9fc5844-f1a3-491b-825a-5a4cabc12aa2\",\n"
               + "    \"name\" : \"Per Magne Østertun.pdf\",\n"
               + "    \"mimeType\" : \"application/pdf\",\n"
               + "    \"size\" : 1025817,\n"
               + "    \"license\" : \"https://creativecommons.org/licenses/by-nc/2.0/\",\n"
               + "    \"administrativeAgreement\" : false,\n"
               + "    \"publisherAuthority\" : false,\n"
               + "    \"publishedDate\" : \"2023-05-25T19:31:17.302914Z\",\n"
               + "    \"visibleForNonOwner\" : true\n"
               + "  }";
    }
}
