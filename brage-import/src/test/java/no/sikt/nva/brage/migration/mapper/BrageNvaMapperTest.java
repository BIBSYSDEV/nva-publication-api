package no.sikt.nva.brage.migration.mapper;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import no.sikt.nva.brage.migration.NvaType;
import no.sikt.nva.brage.migration.record.Contributor;
import no.sikt.nva.brage.migration.record.Customer;
import no.sikt.nva.brage.migration.record.Identity;
import no.sikt.nva.brage.migration.record.Type;
import no.sikt.nva.brage.migration.record.content.ContentFile;
import no.sikt.nva.brage.migration.record.content.ResourceContent;
import no.sikt.nva.brage.migration.record.content.ResourceContent.BundleType;
import no.sikt.nva.brage.migration.record.license.License;
import no.sikt.nva.brage.migration.record.license.NvaLicense;
import no.sikt.nva.brage.migration.testutils.NvaBrageMigrationDataGenerator;
import no.unit.nva.model.Publication;
import no.unit.nva.model.associatedartifacts.file.AdministrativeAgreement;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.ImportUploadDetails;
import no.unit.nva.model.exceptions.InvalidIsbnException;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.model.exceptions.InvalidUnconfirmedSeriesException;
import no.unit.nva.model.instancetypes.degree.DegreePhd;
import no.unit.nva.model.instancetypes.degree.UnconfirmedDocument;
import nva.commons.core.Environment;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

class BrageNvaMapperTest {

    private static final String API_HOST = new Environment().readEnv("API_HOST");
    public static final String RELATED_DOCUMENT_3 = "Paper 3: Prestmo A";
    public static final String RELATED_DOCUMENT_1 = "Paper 1: Saltvedt,";
    public static final String RELATED_DOCUMENT_2 = "Paper 2: Prestmo, ";
    public static final String NPOLAR_SHORT_NAME = "NPI";

    @Test
    void shouldMapContentFileWithBundleTypeLicenseToAdministrativeAgreement()
        throws InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
        var licenseContentFile = createRandomContentFileWithBundleType(BundleType.LICENSE);
        var generator =  new NvaBrageMigrationDataGenerator.Builder()
                             .withType(new Type(List.of(), NvaType.CHAPTER.getValue()))
                             .withResourceContent(new ResourceContent(List.of(licenseContentFile)))
                             .build();
        var file = BrageNvaMapper.toNvaPublication(generator.getBrageRecord(), API_HOST).getAssociatedArtifacts().getFirst();

        assertThat(file, is(instanceOf(AdministrativeAgreement.class)));
    }

    @Test
    void shouldMapContentFileWithBundleTypeIgnoredToAdministrativeAgreement()
        throws InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
        var licenseContentFile = createRandomContentFileWithBundleType(BundleType.IGNORED);
        var generator =  new NvaBrageMigrationDataGenerator.Builder()
                             .withType(new Type(List.of(), NvaType.CHAPTER.getValue()))
                             .withResourceContent(new ResourceContent(List.of(licenseContentFile)))
                             .build();
        var file = BrageNvaMapper.toNvaPublication(generator.getBrageRecord(), API_HOST).getAssociatedArtifacts().getFirst();

        assertThat(file, is(instanceOf(AdministrativeAgreement.class)));
    }

    @Test
    void shouldCreateDegreePhdWithRelatedDocumentsInTheSameOrderAsInBrageRecord()
        throws InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
        var generator =  new NvaBrageMigrationDataGenerator.Builder()
                             .withType(new Type(List.of(), NvaType.DOCTORAL_THESIS.getValue()))
                             .withHasPart(List.of("1", "2"))
                             .build();
        var publication = BrageNvaMapper.toNvaPublication(generator.getBrageRecord(), API_HOST);
        var degreePhd = (DegreePhd) publication.getEntityDescription().getReference().getPublicationInstance();
        var expectedRelatedDocuments = Set.of(new UnconfirmedDocument("1"), new UnconfirmedDocument("2"));
        assertThat(degreePhd.getRelated(), is(equalTo(expectedRelatedDocuments)));
    }

    @Test
    void shouldMapFirstAlternativeAbstractAsAbstractAndAllOthersAsAlternativeAbstracts()
        throws InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
        var firstAbstract = randomString();
        var secondAbstract = randomString();
        var thirdAbstract = randomString();
        var generator =  new NvaBrageMigrationDataGenerator.Builder()
                             .withType(new Type(List.of(), NvaType.DOCTORAL_THESIS.getValue()))
                             .withAbstracts(List.of(firstAbstract, secondAbstract, thirdAbstract))
                             .build();
        var publication = BrageNvaMapper.toNvaPublication(generator.getBrageRecord(), API_HOST);
        assertThat(publication.getEntityDescription().getAbstract(), is(equalTo(firstAbstract)));
        assertThat(publication.getEntityDescription().getAlternativeAbstracts().get("und"),
                   is(equalTo(secondAbstract + "\n\n" + thirdAbstract)));
    }

    @Test
    void shouldNotCreateAlternativeAbstractsWhenSingleAbstractInBrage()
        throws InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
        var generator =  new NvaBrageMigrationDataGenerator.Builder()
                             .withType(new Type(List.of(), NvaType.DOCTORAL_THESIS.getValue()))
                             .withAbstracts(List.of(randomString()))
                             .build();
        var publication = BrageNvaMapper.toNvaPublication(generator.getBrageRecord(), API_HOST);
        assertThat(publication.getEntityDescription().getAlternativeAbstracts(), is(anEmptyMap()));
    }

    @Test
    void shouldCreatePublicationWithRelatedDocumentsSortedAlphabeticallyWhenMappingDoctoralThesis()
        throws InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
        var generator =  new NvaBrageMigrationDataGenerator.Builder()
                             .withType(new Type(List.of(), NvaType.DOCTORAL_THESIS.getValue()))
                             .withHasPart(List.of(RELATED_DOCUMENT_3, RELATED_DOCUMENT_1, RELATED_DOCUMENT_2))
                             .build();
        var publication = BrageNvaMapper.toNvaPublication(generator.getBrageRecord(), API_HOST);
        var publicationInstance = (DegreePhd) publication.getEntityDescription()
                                                                       .getReference()
                                                                       .getPublicationInstance();
        var expectedDocuments = List.of(RELATED_DOCUMENT_1, RELATED_DOCUMENT_2, RELATED_DOCUMENT_3);
        var actualDocumentValues = getRelatedDocumentsValues(publicationInstance);

        assertThat(actualDocumentValues, is(equalTo(expectedDocuments)));
    }

    @Test
    void shouldCreatePublicationWithContributorWithOrcIdWhenBrageContributorHasOrcid()
        throws InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
        var contributor = randomContributorWithOrcId();
        var generator =  new NvaBrageMigrationDataGenerator.Builder()
                             .withType(new Type(List.of(), NvaType.DOCTORAL_THESIS.getValue()))
                             .withContributor(contributor)
                             .build();
        var publication = BrageNvaMapper.toNvaPublication(generator.getBrageRecord(), API_HOST);
        var actualContributor = publication.getEntityDescription().getContributors().getFirst();

        assertEquals(contributor.getIdentity().getOrcId(), actualContributor.getIdentity().getOrcId());
    }

    @Test
    void shouldImportFileFromBrageWithImportUploadDetailWithArchiveAsInstitutionShortName()
        throws InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
        var generator =  new NvaBrageMigrationDataGenerator.Builder()
                             .withType(new Type(List.of(), NvaType.DOCTORAL_THESIS.getValue()))
                             .withCustomer(new Customer("npolar", randomUri()))
                             .withResourceContent(new ResourceContent(List.of(createRandomContentFileWithBundleType(BundleType.ORIGINAL))))
                             .build();
        var publication = BrageNvaMapper.toNvaPublication(generator.getBrageRecord(), API_HOST);
        var file = getFirstFile(publication);
        var importUploadDetail = (ImportUploadDetails) file.getUploadDetails();
        assertEquals(importUploadDetail.archive(), NPOLAR_SHORT_NAME);
    }

    private static File getFirstFile(Publication publication) {
        return publication.getAssociatedArtifacts().stream()
                   .filter(File.class::isInstance)
                   .map(File.class::cast)
                   .findFirst()
                   .orElseThrow();
    }

    private static Contributor randomContributorWithOrcId() {
        return new Contributor(new Identity(randomString(), randomString(), randomString()),
                               "ACTOR",
                               randomString(), List.of());
    }

    private static List<String> getRelatedDocumentsValues(DegreePhd publicationInstance) {
        return publicationInstance.getRelated().stream()
                   .map(UnconfirmedDocument.class::cast)
                   .map(UnconfirmedDocument::text)
                   .toList();
    }

    private ContentFile createRandomContentFileWithBundleType(BundleType bundleType) {
        return new ContentFile(randomString(), bundleType, randomString(), UUID.randomUUID(),
                               new License(randomString(), new NvaLicense(randomUri())),
                               null);
    }
}