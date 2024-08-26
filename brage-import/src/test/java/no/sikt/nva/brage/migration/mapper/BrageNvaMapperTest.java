package no.sikt.nva.brage.migration.mapper;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.cucumber.java.hu.De;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import no.sikt.nva.brage.migration.NvaType;
import no.sikt.nva.brage.migration.record.Type;
import no.sikt.nva.brage.migration.record.content.ContentFile;
import no.sikt.nva.brage.migration.record.content.ResourceContent;
import no.sikt.nva.brage.migration.record.content.ResourceContent.BundleType;
import no.sikt.nva.brage.migration.testutils.NvaBrageMigrationDataGenerator;
import no.unit.nva.model.associatedartifacts.file.AdministrativeAgreement;
import no.unit.nva.model.exceptions.InvalidIsbnException;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.model.exceptions.InvalidUnconfirmedSeriesException;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.degree.DegreePhd;
import no.unit.nva.model.instancetypes.degree.UnconfirmedDocument;
import no.unit.nva.model.pages.Pages;
import nva.commons.core.Environment;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

class BrageNvaMapperTest {

    private static final String API_HOST = new Environment().readEnv("API_HOST");
    public static final String RELATED_DOCUMENT_3 = "Paper 3: Prestmo A";
    public static final String RELATED_DOCUMENT_1 = "Paper 1: Saltvedt,";
    public static final String RELATED_DOCUMENT_2 = "Paper 2: Prestmo, ";

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

    private static List<String> getRelatedDocumentsValues(DegreePhd publicationInstance) {
        return publicationInstance.getRelated().stream()
                   .map(UnconfirmedDocument.class::cast)
                   .map(UnconfirmedDocument::text)
                   .toList();
    }

    private ContentFile createRandomContentFileWithBundleType(BundleType bundleType) {
        return new ContentFile(randomString(), bundleType, randomString(), UUID.randomUUID(), null, null);
    }
}