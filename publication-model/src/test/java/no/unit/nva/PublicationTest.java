package no.unit.nva;

import static no.unit.nva.DatamodelConfig.dataModelObjectMapper;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFields;
import static no.unit.nva.model.PublicationStatus.DRAFT;
import static no.unit.nva.model.PublicationStatus.DRAFT_FOR_DELETION;
import static no.unit.nva.model.PublicationStatus.NEW;
import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.model.file.FileModelTest.buildAdministrativeAgreement;
import static no.unit.nva.model.file.FileModelTest.buildNonAdministrativeAgreement;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomAssociatedLink;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.AdditionalIdentifierBase;
import no.unit.nva.model.ImportDetail;
import no.unit.nva.model.ImportSource;
import no.unit.nva.model.ImportSource.Source;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.associatedartifacts.InvalidAssociatedArtifactsException;
import no.unit.nva.model.associatedartifacts.NullAssociatedArtifact;
import no.unit.nva.model.associatedartifacts.file.PublishedFile;
import no.unit.nva.model.associatedartifacts.file.UnpublishedFile;
import no.unit.nva.model.exceptions.InvalidPublicationStatusTransitionException;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.model.testing.PublicationInstanceBuilder;
import no.unit.nva.model.testing.associatedartifacts.AdministrativeAgreementGenerator;
import no.unit.nva.model.testing.associatedartifacts.AssociatedLinkGenerator;
import no.unit.nva.model.testing.associatedartifacts.PublishedFileGenerator;
import no.unit.nva.model.testing.associatedartifacts.UnpublishedFileGenerator;
import org.hamcrest.Matchers;
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.core.diff.Diff;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

public class PublicationTest {

    public static final String DOCUMENTATION_PATH_TEMPLATE = "../documentation/%s.json";
    public static final SortableIdentifier REPLACEMENT_IDENTIFIER_1 =
        new SortableIdentifier("c443030e-9d56-43d8-afd1-8c89105af555");
    public static final Javers JAVERS = JaversBuilder.javers().build();
    public static final Set<String> IGNORE_LIST =
        Set.of(".entityDescription.reference.publicationContext.revision", "importDetails");

    public static Stream<Class<?>> publicationInstanceProvider() {
        return PublicationInstanceBuilder.listPublicationInstanceTypes().stream();
    }

    public static Stream<Publication> publishablePublicationProvider() {

        var publication = randomPublication();
        publication.setStatus(DRAFT);

        return Stream.of(
            publicationWithOriginalDoi(),
            publicationWithAdministrativeAgreementAndUnpublishedFile(),
            publicationWithAdministrativeAgreementAndPublishedFile(),
            publicationWithAdministrativeAgreementAndLink()
        );
    }

    public static Stream<Publication> unpublishablePublicationProvider() {
        return Stream.of(
            randomDraftForDeletion(),
            publicationWithoutTitle(),
            publicationWithoutEntityDescription()
        );
    }

    public static Stream<Named<Publication>> importedPublicationProvider() {
        return Stream.of(Named.of("Brage", PublicationGenerator.createImportedPublication(Source.BRAGE)),
                         Named.of("Cristin", PublicationGenerator.createImportedPublication(Source.CRISTIN)),
                         Named.of("Scopus", PublicationGenerator.createImportedPublication(Source.SCOPUS)));
    }

    @ParameterizedTest(name = "Test that publication with InstanceType {0} can be round-tripped to and from JSON")
    @MethodSource("publicationInstanceProvider")
    void publicationReturnsValidPublicationWhenInputIsValid(Class<?> instanceType) throws Exception {
        Publication expected = randomPublication(instanceType);

        String publication = dataModelObjectMapper.writeValueAsString(expected);
        Publication roundTripped = dataModelObjectMapper.readValue(publication, Publication.class);
        Diff diff = JAVERS.compare(expected, roundTripped);
        assertThat(publication, doesNotHaveEmptyValuesIgnoringFields(IGNORE_LIST));
        assertThat(publication, doesNotHaveEmptyValuesIgnoringFields(IGNORE_LIST));
        assertEquals(expected, roundTripped);
        assertThat(diff.prettyPrint(), roundTripped, is(equalTo(expected)));

        writePublicationToFile(instanceType, expected);
    }

    @Test
    void additionalIdentifierShouldSupportSourceAlias() throws Exception {
        var payload = """
            {
              "type" : "Publication",
              "identifier" : "c443030e-9d56-43d8-afd1-8c89105af555",
              "status" : "PUBLISHED",
              "additionalIdentifiers" : [ {
                "type" : "AdditionalIdentifier",
                "source" : "sPUi0dHAUIqW8Bu4",
                "value" : "https://www.example.org/62408300-5cdb-4a05-be3f-063bc210cc30"
              }]
            }
            """;
        Publication publication = dataModelObjectMapper.readValue(payload, Publication.class);

        AdditionalIdentifierBase additionalIdentifier = publication.getAdditionalIdentifiers()
                                                                      .stream()
                                                                      .findFirst()
                                                                      .orElseThrow();
        assertEquals("sPUi0dHAUIqW8Bu4", additionalIdentifier.sourceName());
        assertEquals("https://www.example.org/62408300-5cdb-4a05-be3f-063bc210cc30", additionalIdentifier.value());
        assertEquals("AdditionalIdentifier", additionalIdentifier.getClass().getSimpleName());
    }

    @ParameterizedTest(name = "Test that publication with InstanceType {0} can be copied without loss of data")
    @MethodSource("publicationInstanceProvider")
    void copyReturnsBuilderWithAllDataOfAPublication(Class<?> referenceInstanceType) {
        Publication publication = randomPublication(referenceInstanceType);
        Publication copy = publication.copy().build();
        assertThat(publication, doesNotHaveEmptyValuesIgnoringFields(IGNORE_LIST));
        Diff diff = compareAsObjectNodes(publication, copy);
        assertThat(diff.prettyPrint(), copy, is(equalTo(publication)));
        assertThat(copy, is(not(sameInstance(publication))));
    }

    @ParameterizedTest(name = "Test projects are always lists when input is single element")
    @MethodSource("publicationInstanceProvider")
    void projectsAreSetAsListsWhenInputIsSingleProject(Class<?> instanceType) {
        Publication expected = randomPublication(instanceType);
        assertThat(expected.getProjects(), instanceOf(List.class));
    }

    @ParameterizedTest
    @EnumSource(value = PublicationStatus.class, names = {"DRAFT_FOR_DELETION", "PUBLISHED"})
    void updateStatusForDraftPublication(PublicationStatus target) throws Exception {
        Publication publication = randomPublication();
        publication.setStatus(DRAFT);
        publication.updateStatus(target);

        assertThat(publication.getStatus(), is(equalTo(target)));
    }

    @Test
    void updateStatusThrowsExceptionForInvalidStatusTransition() {
        Publication publication = randomPublication();
        publication.setStatus(NEW);

        InvalidPublicationStatusTransitionException exception =
            assertThrows(InvalidPublicationStatusTransitionException.class, () -> publication.updateStatus(PUBLISHED));

        String expectedError = String.format(InvalidPublicationStatusTransitionException.ERROR_MSG_TEMPLATE,
                                             NEW, PUBLISHED);
        assertThat(exception.getMessage(), is(equalTo(expectedError)));
    }

    @Test
    void shouldConvertPublishableArtifactToPublishedUponRequest() {
        var unpublishedFile = buildNonAdministrativeAgreement().buildUnpublishedFile();
        var publishedFile = buildNonAdministrativeAgreement().buildPublishedFile();
        assertThat(unpublishedFile.toPublishedFile(), is(instanceOf(PublishedFile.class)));
        assertThat(publishedFile.toPublishedFile(), is(instanceOf(PublishedFile.class)));
    }

    @Test
    void shouldConvertPublishableArtifactToUnpublishedUponRequest() {
        var unpublishedFile = buildNonAdministrativeAgreement().buildUnpublishedFile();
        var publishedFile = buildNonAdministrativeAgreement().buildPublishedFile();
        assertThat(unpublishedFile.toUnpublishedFile(), is(instanceOf(UnpublishedFile.class)));
        assertThat(publishedFile.toUnpublishedFile(), is(instanceOf(UnpublishedFile.class)));
    }

    @Test
    void shouldNotConvertUnPublishableArtifactToPublishableArtifacts() {
        var unpublishableFile = buildAdministrativeAgreement().buildUnpublishableFile();
        assertThrows(IllegalStateException.class, unpublishableFile::toPublishedFile);
        assertThrows(IllegalStateException.class, unpublishableFile::toUnpublishedFile);
        assertThrows(IllegalStateException.class, unpublishableFile::toUnpublishedFile);
    }

    // This test is included because of a bizarre error.
    @Test
    void initializingPublicationShouldNotThrowException() {
        assertDoesNotThrow(Publication::new);
    }

    @Test
    void shouldThrowExceptionWhenCreatingAssociatedArtifactsWithNullArtifactsAndOtherArtifacts() {
        Executable executable = () -> new AssociatedArtifactList(randomAssociatedLink(),
                                                                 new NullAssociatedArtifact());
        assertThrows(InvalidAssociatedArtifactsException.class, executable);
    }

    @ParameterizedTest(name = "Publication can be published when basic data is OK and associated files is OK")
    @MethodSource("publishablePublicationProvider")
    void shouldMarkPublicationAsPublishableWhenPublicationHasRequiredData(Publication publication) {
        assertThat(publication.isPublishable(), is(equalTo(true)));
    }

    @ParameterizedTest(name = "Publication cannot be published when basic data is not in place")
    @MethodSource("unpublishablePublicationProvider")
    void shouldMarkPublicationAsWhenDataIsNotCompliantWithPublicationRequirements(Publication publication) {
        assertThat(publication.isPublishable(), is(equalTo(false)));
    }

    @Test
    void shouldReturnFalseForSatisfiesFindableDoiRequirementForPublicationMissingYear() {
        var publication = createSamplePublication();
        publication.getEntityDescription().getPublicationDate().setYear(null);
        var expectedResult = false;
        assertThat(publication.satisfiesFindableDoiRequirements(),
                   is(Matchers.equalTo(expectedResult)));
    }

    @Test
    void shouldReturnFalseForSatisfiesFindableDoiRequirementForPublicationMissingTitle() {
        var publication = createSamplePublication();
        publication.getEntityDescription().setMainTitle(null);
        var expectedResult = false;
        assertThat(publication.satisfiesFindableDoiRequirements(),
                   is(Matchers.equalTo(expectedResult)));
    }

    @Test
    void shouldReturnFalseForSatisfiesFindableDoiRequirementForPublicationMissingPublisherId() {
        var publication = createSamplePublication();
        publication.getPublisher().setId(null);
        var expectedResult = false;
        assertThat(publication.satisfiesFindableDoiRequirements(),
                   is(Matchers.equalTo(expectedResult)));
    }

    @Test
    void shouldReturnFalseForSatisfiesFindableDoiRequirementForDraftPublication() {
        var publication = createSamplePublication();
        publication.setStatus(DRAFT);
        var expectedResult = false;
        assertThat(publication.satisfiesFindableDoiRequirements(),
                   is(Matchers.equalTo(expectedResult)));
    }

    @Test
    void shouldReturnFalseForSatisfiesFindableDoiRequirementForPublicationMissingPublicationIdentifier() {
        var publication = createSamplePublication();
        publication.setIdentifier(null);
        var expectedResult = false;
        assertThat(publication.satisfiesFindableDoiRequirements(),
                   is(Matchers.equalTo(expectedResult)));
    }

    @Test
    void shouldReturnFalseForSatisfiesFindableDoiRequirementForPublicationMissingModifiedDate() {
        var publication = createSamplePublication();
        publication.setModifiedDate(null);
        var expectedResult = false;
        assertThat(publication.satisfiesFindableDoiRequirements(),
                   is(Matchers.equalTo(expectedResult)));
    }

    @Test
    void shouldReturnFalseForSatisfiesFindableDoiRequirementForPublicationMissingInstanceType() {
        var publication = createSamplePublication();
        publication.getEntityDescription().getReference().setPublicationInstance(null);
        var expectedResult = false;
        assertThat(publication.satisfiesFindableDoiRequirements(),
                   is(Matchers.equalTo(expectedResult)));
    }

    @ParameterizedTest(name = "should return true for satisfiesFindable doi requirements for publication with all "
                              + "mandatory fields present")
    @EnumSource(value = PublicationStatus.class, names = {"PUBLISHED_METADATA", "PUBLISHED"})
    void shouldReturnTrueIfMandatoryFieldsArePresent(PublicationStatus validPublicationStatus) {
        var publication = createSamplePublication();
        publication.setStatus(validPublicationStatus);
        publication.setPublisher(
            new Organization.Builder().withId(randomUri()).build());
        publication.getEntityDescription().getPublicationDate().setYear("2014");
        publication.getEntityDescription().setMainTitle("some title");
        publication.setIdentifier(SortableIdentifier.next());
        publication.setModifiedDate(Instant.now());
        var expectedResult = true;
        assertThat(publication.satisfiesFindableDoiRequirements(),
                   is(Matchers.equalTo(expectedResult)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0001", "1", "999", "0999", "2099", "9999"})
    void shouldReturnFalseForSatisfiesFindableDoiRequirementForWrongPublicationYear(String year) {
        var publication = createSamplePublication();
        publication.setStatus(PUBLISHED);
        publication.getEntityDescription()
            .setPublicationDate(new PublicationDate.Builder().withYear(year).withDay("1").withMonth("1").build());
        assertFalse(publication.satisfiesFindableDoiRequirements());
    }

    @ParameterizedTest
    @ValueSource(strings = {"1000", "1950", "2024", "2028"})
    void shouldReturnTrueForSatisfiesFindableDoiRequirementForCorrectPublicationYear(String year) {
        var publication = createSamplePublication();
        publication.setStatus(PUBLISHED);
        publication.getEntityDescription()
            .setPublicationDate(new PublicationDate.Builder().withYear(year).withDay("1").withMonth("1").build());
        assertTrue(publication.satisfiesFindableDoiRequirements());
    }

    @ParameterizedTest
    @DisplayName("Should indicate Publication is imported")
    @MethodSource("importedPublicationProvider")
    void shouldIndicateThatPublicationIsImported(Publication publication) {
        assertFalse(publication.getImportDetails().isEmpty());
    }

    @Test
    @DisplayName("Should indicate Publication is not imported")
    void shouldMakeItClearThatANonImportedPublicationIsNotImported() {
        var publication = PublicationGenerator.randomPublication();
        assertTrue(publication.getImportDetails().isEmpty());
    }

    @Test
    @DisplayName("Should allow adding import details")
    void shouldAllowAddingImportDetails() {
        var publication = PublicationGenerator.randomPublication();
        assertTrue(publication.getImportDetails().isEmpty());
        assertDoesNotThrow(() -> publication.setImportDetails(List.of()));
        assertDoesNotThrow(() -> publication.addImportDetail(new ImportDetail(Instant.now(),
                                                                              ImportSource.fromBrageArchive(
                                                                                  randomString()))));
        assertDoesNotThrow(() -> publication.addImportDetail(ImportDetail.fromSource(Source.CRISTIN, Instant.now())));
    }

    @Test
    @DisplayName("Should throw exception when overriding previous import details")
    void shouldThrowExceptionWhenOverridingPreviousImportDetails() {
        var publication = PublicationGenerator.createImportedPublication(Source.BRAGE);
        assertThrows(IllegalArgumentException.class,
                     () -> publication.copy()
                               .withImportDetails(null)
                               .build());
        assertThrows(IllegalArgumentException.class,
                     () -> publication.copy()
                               .withImportDetails(List.of())
                               .build());
        assertThrows(IllegalArgumentException.class,
                     () -> publication.copy()
                               .withImportDetails(List.of(new ImportDetail(Instant.now(),
                                                                           ImportSource.fromBrageArchive(
                                                                               randomString()))))
                               .build());
    }

    @Test
    @DisplayName("Should allow copying import details")
    void shouldAllowCopyingImportDetails() {
        var publication = PublicationGenerator.createImportedPublication(Source.SCOPUS);
        var copy = assertDoesNotThrow(() -> publication.copy().build());
        assertThat(copy, is(equalTo(publication)));
        assertDoesNotThrow(() -> publication.setImportDetails(publication.getImportDetails()));
    }

    private static Publication publicationWithoutTitle() {
        var publication = PublicationGenerator.randomPublication();
        publication.setStatus(DRAFT);
        publication.getEntityDescription().setMainTitle(null);
        return publication;
    }

    private static Publication randomDraftForDeletion() {
        var publication = PublicationGenerator.randomPublication();
        publication.setStatus(DRAFT_FOR_DELETION);
        return publication;
    }

    private static Publication publicationWithoutEntityDescription() {
        var publication = publicationWithoutTitle();
        publication.setEntityDescription(null);
        return publication;
    }

    private static Publication publicationWithAdministrativeAgreementAndLink() {
        var administrativeAgreement = AdministrativeAgreementGenerator.random();
        var link = AssociatedLinkGenerator.random();
        return publicationWithAssociatedArtifact(new AssociatedArtifactList(List.of(administrativeAgreement, link)));
    }

    private static Publication publicationWithAdministrativeAgreementAndUnpublishedFile() {
        var unpublishedFile = UnpublishedFileGenerator.random();
        var administrativeAgreement = AdministrativeAgreementGenerator.random();
        return publicationWithAssociatedArtifact(new AssociatedArtifactList(List.of(administrativeAgreement,
                                                                                    unpublishedFile)));
    }

    private static Publication publicationWithAdministrativeAgreementAndPublishedFile() {
        var publishedFile = PublishedFileGenerator.random();
        var administrativeAgreement = AdministrativeAgreementGenerator.random();
        return publicationWithAssociatedArtifact(new AssociatedArtifactList(List.of(administrativeAgreement,
                                                                                    publishedFile)));
    }

    private static Publication publicationWithOriginalDoi() {
        var publication = randomPublication();
        publication.setStatus(DRAFT);
        publication.setAssociatedArtifacts(null);
        return publication;
    }

    private static Publication publicationWithAssociatedArtifact(AssociatedArtifactList associatedArtifacts) {
        var publication = randomPublication();
        publication.setStatus(DRAFT);
        publication.getEntityDescription().getReference().setDoi(null);
        publication.setAssociatedArtifacts(associatedArtifacts);
        return publication;
    }

    private Publication createSamplePublication() {
        return PublicationGenerator.publicationWithIdentifier();
    }

    private Diff compareAsObjectNodes(Publication publication, Publication copy) {
        var publicationObjectNode = dataModelObjectMapper.convertValue(publication, ObjectNode.class);
        var copyObjectNode = dataModelObjectMapper.convertValue(copy, ObjectNode.class);
        return JAVERS.compare(publicationObjectNode, copyObjectNode);
    }

    private void writePublicationToFile(Class<?> instanceType, Publication publication) throws IOException {
        publication.setIdentifier(REPLACEMENT_IDENTIFIER_1);
        String path = String.format(DOCUMENTATION_PATH_TEMPLATE, instanceType.getSimpleName());
        var publicationJson = dataModelObjectMapper.writeValueAsString(publication);
        Files.write(Paths.get(path), publicationJson.getBytes());
    }
}
