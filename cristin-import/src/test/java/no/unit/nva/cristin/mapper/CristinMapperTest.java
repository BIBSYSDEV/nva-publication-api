package no.unit.nva.cristin.mapper;

import static no.unit.nva.cristin.CristinDataGenerator.largeRandomNumber;
import static no.unit.nva.cristin.CristinDataGenerator.randomAffiliation;
import static no.unit.nva.cristin.mapper.CristinPresentationalWork.PROSJEKT;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.CHAPTER;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.JOURNAL_ARTICLE;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.TEXTBOOK;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.cristin.AbstractCristinImportTest;
import no.unit.nva.cristin.CristinDataGenerator;
import no.unit.nva.cristin.mapper.channelregistry.ChannelRegistryMapper;
import no.unit.nva.cristin.mapper.nva.NvaBookBuilder;
import no.unit.nva.cristin.mapper.nva.NvaBookSeriesBuilder;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.additionalidentifiers.AdditionalIdentifier;
import no.unit.nva.model.additionalidentifiers.AdditionalIdentifierBase;
import no.unit.nva.model.additionalidentifiers.CristinIdentifier;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.contexttypes.Journal;
import no.unit.nva.model.contexttypes.NullPublisher;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.contexttypes.Publisher;
import no.unit.nva.model.contexttypes.Series;
import no.unit.nva.model.contexttypes.UnconfirmedPublisher;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.book.BookAnthology;
import no.unit.nva.model.instancetypes.book.BookMonograph;
import no.unit.nva.model.instancetypes.journal.ProfessionalArticle;
import no.unit.nva.model.pages.MonographPages;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;
import no.unit.nva.publication.utils.CristinUnitsUtil;
import nva.commons.core.SingletonCollector;
import nva.commons.core.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import software.amazon.awssdk.services.s3.S3Client;

class CristinMapperTest extends AbstractCristinImportTest {

    public static final int NUMBER_OF_OBJECTS = 100;
    private List<CristinObject> cristinObjects;
    private CristinUnitsUtil cristinUnitsUtil;

    @Override
    @BeforeEach
    public void init() {
        super.init();
        this.cristinObjects = cristinObjects(NUMBER_OF_OBJECTS).toList();
        this.cristinUnitsUtil = mock(CristinUnitsUtil.class);
    }

    @ParameterizedTest(name = "map returns journal with DOI URI when input is Journal with DOI:{0}")
    @CsvSource({
        "http://dx.doi.org/10.5750/ejpch.v5i1.1209, https://doi.org/10.5750/ejpch.v5i1.1209",
        "https://doi.org/10.1016/j.toxlet.2020.09.006, https://doi.org/10.1016/j.toxlet.2020.09.006",
        "DOI:10.1080/02699052.2017.1312145, https://doi.org/10.1080/02699052.2017.1312145",
        "doi:10.1080/02699052.2017.1312145, https://doi.org/10.1080/02699052.2017.1312145",
        "10.1016/j.toxlet.2020.09.006, https://doi.org/10.1016/j.toxlet.2020.09.006"
    })
    void mapReturnsJournalWithDoiWhenInputIsJournalResultWithDoi(String inputDoi, String expectedDoiUri) {
        CristinObject cristinObject =
            CristinDataGenerator.randomObject(JOURNAL_ARTICLE.toString());
        cristinObject.getJournalPublication().setDoi(inputDoi);
        var publication = mapToPublication(cristinObject);
        assertThat(publication.getEntityDescription().getReference().getDoi(), is(equalTo(URI.create(expectedDoiUri))));
    }

    private Publication mapToPublication(CristinObject cristinObject) {
        return new CristinMapper(cristinObject, cristinUnitsUtil, mock(S3Client.class)).generatePublication();
    }

    @Test
    void mapReturnsJournalWithDoiUriWhenInputIsANonExistingDoiAndOnlineValidationIsDisabled() {
        String inputDoi = "10.1234/non.existin.doi";
        URI expectedDoiUri = URI.create("https://doi.org/" + inputDoi);
        CristinObject cristinObject =
            CristinDataGenerator.randomObject(JOURNAL_ARTICLE.toString());
        cristinObject.getJournalPublication().setDoi(inputDoi);
        var publication = mapToPublication(cristinObject);
        assertThat(publication.getEntityDescription().getReference().getDoi(), is(equalTo(expectedDoiUri)));
    }

    @Test
    void mapReturnsJournalWithoutDoiUriWhenInputIsAJournalArticleWihoutDoi() {
        CristinObject cristinObject =
            CristinDataGenerator.randomObject(JOURNAL_ARTICLE.toString());
        cristinObject.getJournalPublication().setDoi(null);
        var publication = mapToPublication(cristinObject);
        assertThat(publication.getEntityDescription().getReference().getDoi(), is(nullValue()));
    }

    @Test
    void mapReturnsResourceWithCristinIdStoredInAdditionalIdentifiers() {
        Set<Integer> expectedIds = cristinObjects.stream().map(CristinObject::getId).collect(Collectors.toSet());

        Set<Integer> actualIds = cristinObjects.stream()
                                     .map(this::mapToPublication)
                                     .map(Publication::getAdditionalIdentifiers)
                                     .flatMap(Collection::stream)
                                     .filter(CristinIdentifier.class::isInstance)
                                     .map(CristinIdentifier.class::cast)
                                     .map(AdditionalIdentifierBase::value)
                                     .map(Integer::parseInt)
                                     .collect(Collectors.toSet());

        assertThat(expectedIds.size(), is(equalTo(NUMBER_OF_OBJECTS)));
        assertThat(actualIds, is(equalTo(expectedIds)));
    }

    @Test
    void shouldMapAllNonCristinIdentifiersToCristinObjectToAdditionalIdentifiers() {
        var expectedIds = cristinObjects.stream()
                              .map(CristinObject::getCristinSources)
                              .flatMap(Collection::stream)
                              .map(this::createExpectedAdditionalIdentifier)
                              .toList();

        var actualIds = cristinObjects.stream()
                            .map(this::mapToPublication)
                            .map(Publication::getAdditionalIdentifiers)
                            .flatMap(Collection::stream)
                            .filter(additionalIdentifierBase -> !(additionalIdentifierBase instanceof CristinIdentifier))
                            .toList();

        assertThat(actualIds, containsInAnyOrder(expectedIds.toArray()));
        assertThat(actualIds, hasSize(expectedIds.size()));
    }

    @Test
    void mapReturnsResourceWithSourceCodeAndSourceRecordIdentifierStoredInAdditionalIdentifiers() {
        var sourceCode = randomString();
        var sourceRecordIdentifier = randomString();
        var cristinObject = CristinDataGenerator.randomObject();
        cristinObject.setSourceCode(sourceCode);
        cristinObject.setSourceRecordIdentifier(sourceRecordIdentifier);
        var expectedAdditionalIdentifier = new AdditionalIdentifier(sourceCode, sourceRecordIdentifier);
        var actualAdditionalIdentifier = mapToPublication(cristinObject)
                                             .getAdditionalIdentifiers();
        assertThat(actualAdditionalIdentifier, hasItem(expectedAdditionalIdentifier));
    }

    @Test
    void mapPrioritizeCristinSourceOverSourceCode() {
        var collidingSourceCode = randomString();
        var sourceRecordIdentifierA = randomString();
        var sourceIdentifierB = randomString();
        var cristinObject = CristinDataGenerator.randomObject();
        cristinObject.setSourceCode(collidingSourceCode);
        cristinObject.setSourceRecordIdentifier(sourceRecordIdentifierA);
        cristinObject.setCristinSources(List.of(CristinSource.builder()
                                                    .withSourceCode(collidingSourceCode)
                                                    .withSourceIdentifier(sourceIdentifierB)
                                                    .build()));
        var expectedAdditionalIdentifier = new AdditionalIdentifier(collidingSourceCode, sourceIdentifierB);
        var expectedNotToExist = new AdditionalIdentifier(collidingSourceCode, sourceRecordIdentifierA);
        var actualAdditionalIdentifier = mapToPublication(cristinObject)
                                             .getAdditionalIdentifiers();
        assertThat(actualAdditionalIdentifier, hasItem(expectedAdditionalIdentifier));
        assertThat(actualAdditionalIdentifier, not(hasItem(expectedNotToExist)));
    }

    @Test
    void mapReturnsResourceWithMainTitleBeingTheTitleAnnotatedAsOriginalTitle() {

        List<String> expectedTitles = cristinObjects.stream()
                                          .map(CristinObject::getCristinTitles)
                                          .map(this::mainTitle)
                                          .map(CristinTitle::getTitle)
                                          .toList();

        List<String> actualTitles = cristinObjects.stream()
                                        .map(this::mapToPublication)
                                        .map(Publication::getEntityDescription)
                                        .map(EntityDescription::getMainTitle)
                                        .toList();

        assertThat(expectedTitles, is(not(empty())));
        assertThat(actualTitles, containsInAnyOrder(expectedTitles.toArray(String[]::new)));
        assertThat(actualTitles.size(), is(equalTo(cristinObjects.size())));
    }

    @Test
    @DisplayName("map returns resource with date equal to \"arstall\"")
    void mapReturnsResourceWithDateEqualToArstall() {
        List<Integer> expectedPublicationYear = cristinObjects.stream()
                                                    .map(CristinObject::getPublicationYear)
                                                    .toList();

        var actualPublicationDates = cristinObjects.stream()
                                         .map(this::mapToPublication)
                                         .map(Publication::getEntityDescription)
                                         .map(EntityDescription::getPublicationDate)
                                         .map(PublicationDate::getYear)
                                         .map(Integer::parseInt)
                                         .toList();
        assertThat(expectedPublicationYear, is(not(empty())));
        assertThat(actualPublicationDates, containsInAnyOrder(expectedPublicationYear.toArray(Integer[]::new)));
    }

    @Test
    void mapReturnsBookAnthologyWhenInputHasMainTypeBookAndSecondaryTypeAnthology() {

        var actualPublication = mapToPublication(CristinDataGenerator.randomBookAnthology());

        PublicationInstance<?> actualPublicationInstance = actualPublication
                                                               .getEntityDescription()
                                                               .getReference()
                                                               .getPublicationInstance();
        PublicationContext actualPublicationContext = actualPublication
                                                          .getEntityDescription()
                                                          .getReference()
                                                          .getPublicationContext();

        assertThat(actualPublicationInstance, is(instanceOf(BookAnthology.class)));
        assertThat(actualPublicationContext, is(instanceOf(Book.class)));
    }

    @Test
    void mapReturnsBookMonographWhenInputHasMainTypeBookAndSecondaryTypeMonograph() {
        var actualPublication = mapToPublication(CristinDataGenerator.randomBook());

        PublicationInstance<?> actualPublicationInstance = actualPublication
                                                               .getEntityDescription()
                                                               .getReference()
                                                               .getPublicationInstance();
        PublicationContext actualPublicationContext = actualPublication
                                                          .getEntityDescription()
                                                          .getReference()
                                                          .getPublicationContext();

        assertThat(actualPublicationInstance, is(instanceOf(BookMonograph.class)));
        assertThat(actualPublicationContext, is(instanceOf(Book.class)));
    }

    @Test
    void mapReturnsResourceWhereNvaContributorsNamesAreConcatenationsOfCristinFirstAndFamilyNames() {

        List<String> expectedContributorNames = cristinObjects.stream()
                                                    .map(CristinObject::getContributors)
                                                    .flatMap(Collection::stream)
                                                    .map(this::formatNameAccordingToNvaPattern)
                                                    .toList();

        List<String> actualContributorNames = cristinObjects.stream()
                                                  .map(this::mapToPublication)
                                                  .map(Publication::getEntityDescription)
                                                  .map(EntityDescription::getContributors)
                                                  .flatMap(Collection::stream)
                                                  .map(Contributor::getIdentity)
                                                  .map(Identity::getName)
                                                  .toList();

        assertThat(actualContributorNames, containsInAnyOrder(expectedContributorNames.toArray(String[]::new)));
    }

    @Test
    void shouldMapCristinContributorsWithoutSequenceNumberToNvaContributorsWithUniqueSequenceNumber() {
        var singleCristinObject = cristinObjectWithSomeContributorsWithoutSeqNumber(getSingleCristinObject());
        List<Contributor> actualContributors = getContributors(singleCristinObject);
        List<Integer> actualSequenceNumbers = getSequnceNumberList(actualContributors);
        var sequenceNumberSet = getSequenceNumberSet(actualSequenceNumbers);
        assertThat(actualContributors.size(), is(equalTo(sequenceNumberSet.size())));
        assertThat(actualSequenceNumbers, is(equalTo(new ArrayList<>(sequenceNumberSet))));
        assertThat(actualContributors, not(hasItem(nullValue())));
    }

    @Test
    void shouldMapCristinContributorsWhenEveryContributorIsMissingSequenceNumber() {
        var singleCristinObject = cristinObjectWhereEveryContributorIsMissingSeqNumber(getSingleCristinObject());
        List<Contributor> actualContributors = getContributors(singleCristinObject);
        List<Integer> actualSequenceNumbers = getSequnceNumberList(actualContributors);
        var sequenceNumberSet = getSequenceNumberSet(actualSequenceNumbers);
        assertThat(actualContributors.size(), is(equalTo(sequenceNumberSet.size())));
        assertThat(actualSequenceNumbers, is(equalTo(new ArrayList<>(sequenceNumberSet))));
        assertThat(actualContributors, not(hasItem(nullValue())));
    }

    @Test
    void generatedSequenceNumberShouldBeLargerThatAllExistentSequenceNumbers() {
        var singleCristinObject = cristinObjectTwoContributors(getSingleCristinObject());
        List<Contributor> actualContributors = getContributors(singleCristinObject);
        List<Integer> actualSequenceNumbers = getSequnceNumberList(actualContributors);
        assertThat(actualSequenceNumbers, contains(1, 2));
    }

    private CristinObject cristinObjectTwoContributors(CristinObject singleCristinObject) {
        var contributor = singleCristinObject.getContributors().getFirst();
        var firstContributor = contributor.copy().withContributorOrder(1).build();
        var secondContributor = contributor.copy().withContributorOrder(null).build();
        singleCristinObject.getContributors().removeAll(singleCristinObject.getContributors());
        singleCristinObject.setContributors(List.of(firstContributor, secondContributor));
        return singleCristinObject;
    }

    @Test
    void mapReturnsResourceWhereNvaContributorSequenceIsEqualToCristinContributorSequence() {

        Set<ContributionReference> expectedContributions = cristinObjects.stream()
                                                               .map(this::extractContributions)
                                                               .flatMap(Collection::stream)
                                                               .collect(Collectors.toSet());

        Set<ContributionReference> actualContributions = cristinObjects.stream()
                                                             .map(this::mapToPublication)
                                                             .map(this::extractContributions)
                                                             .flatMap(Collection::stream)
                                                             .collect(Collectors.toSet());

        assertThat(expectedContributions, is(equalTo(actualContributions)));
    }

    @Test
    void mapReturnsPublicationWithPublicationDateEqualToCristinPublicationYear() {
        List<PublicationDate> expectedPublicationDates = cristinObjects.stream()
                                                             .map(CristinObject::getPublicationYear)
                                                             .map(this::yearToPublicationDate)
                                                             .toList();
        List<PublicationDate> actualPublicationDates = cristinObjects.stream()
                                                           .map(this::mapToPublication)
                                                           .map(Publication::getEntityDescription)
                                                           .map(EntityDescription::getPublicationDate)
                                                           .toList();

        assertThat(actualPublicationDates,
                   containsInAnyOrder(expectedPublicationDates.toArray(PublicationDate[]::new)));
    }

    @ParameterizedTest(name = "map returns NVA role {1} when Cristin role is {0}")
    @CsvSource({"REDAKTØR,EDITOR", "FORFATTER,CREATOR"})
    void mapReturnsPublicationsWhereCristinRoleIsMappedToCorrectNvaRole(String cristinRole,
                                                                        String nvaRole) {
        CristinContributorRoleCode actualCristinRole = CristinContributorRoleCode.fromString(cristinRole);
        RoleType expectedNvaRole = new RoleType(Role.parse(nvaRole));
        CristinObject objectWithEditor = createObjectWithRoleCode(actualCristinRole);

        Optional<Contributor> contributor = getContributors(objectWithEditor)
                                                .stream()
                                                .filter(c -> c.getRole().equals(expectedNvaRole))
                                                .findAny();
        assertThat(contributor.isPresent(), is(true));
        assertThat(contributor.orElseThrow().getRole(), is(equalTo(expectedNvaRole)));
    }

    @Test
    void mapReturnsPublicationWhereCristinTotalNumberOfPagesIsMappedToNvaPages() {
        CristinObject cristinImport = CristinDataGenerator.randomBook();

        String numberOfPages = cristinImport.getBookOrReportMetadata().getNumberOfPages();

        var actualPublication = mapToPublication(cristinImport);

        PublicationInstance<?> actualPublicationInstance = actualPublication
                                                               .getEntityDescription()
                                                               .getReference()
                                                               .getPublicationInstance();

        MonographPages monographPages = (MonographPages) actualPublicationInstance.getPages();
        String actualNumberOfPages = monographPages.getPages();

        assertThat(actualNumberOfPages, is(equalTo(numberOfPages)));
    }

    @Test
    void mapReturnsPublicationWhereCristinIsbnIsMappedToNvaIsbnList() {
        CristinObject cristinImport = CristinDataGenerator.randomBook();

        String isbn = cristinImport.getBookOrReportMetadata().getIsbn();

        var actualPublication = mapToPublication(cristinImport);

        PublicationContext actualPublicationContext = actualPublication
                                                          .getEntityDescription()
                                                          .getReference()
                                                          .getPublicationContext();

        Book bookSubType = (Book) actualPublicationContext;
        List<String> actualIsbnList = bookSubType.getIsbnList();

        assertThat(actualIsbnList.getFirst(), is(equalTo(isbn)));
    }

    @Test
    void mapSetsNameToNullWhenBothFamilyNameAndGivenNameAreMissing() {
        CristinContributor contributorWithMissingName = CristinContributor.builder()
                                                            .withIdentifier(largeRandomNumber())
                                                            .withContributorOrder(1)
                                                            .withAffiliations(List.of(randomAffiliation()))
                                                            .build();
        CristinObject cristinObjectWithContributorsWithoutRole =
            CristinDataGenerator.randomObject().copy()
                .withPublicationOwner(randomString())
                .withContributors(List.of(contributorWithMissingName))
                .build();
        var mappedContributor = mapToPublication(cristinObjectWithContributorsWithoutRole).getEntityDescription()
                              .getContributors()
                              .getFirst();

        assertNull(mappedContributor.getIdentity().getName());
    }

    @Test
    void shouldSetNullValueForProjectNameWhenGeneratingPublication() {
        var cristinObject = CristinDataGenerator.randomObject();
        cristinObject.setPresentationalWork(
            List.of(CristinDataGenerator.randomPresentationalWork(PROSJEKT))
        );

        var publication = mapToPublication(cristinObject);
        var projects = publication.getProjects();
        Assertions.assertFalse(projects.isEmpty());

        var projectName = projects.getFirst().getName();
        assertNull(projectName);
    }

    @Test
    void shouldLookUpForPublisherByNsdCodeWhenCristinPublisherIsMissingNsdCode() {
        var cristinObject = CristinDataGenerator.randomBook();
        cristinObject.getBookOrReportMetadata().getCristinPublisher().setNsdCode(5497);
        cristinObject.getBookOrReportMetadata().getCristinPublisher().setPublisherName("NORD UniverSITet");
        cristinObject.getBookOrReportMetadata().setPublisherName("NORD UniverSITet");
        var book = new NvaBookBuilder(cristinObject, ChannelRegistryMapper.getInstance(),
                                      mock(S3Client.class)).buildBookForPublicationContext();
        assertThat(((Publisher) book.getPublisher()).getId().toString(),
                   containsString("89938E70-AC80-48EE-BAAD-4C81514F6CA9"));
    }

    @Test
    void shouldLookUpForPublisherByPrimaryNameWhenCristinPublisherIsMissingNsdCode() {
        var cristinObject = CristinDataGenerator.randomBook();
        cristinObject.getBookOrReportMetadata().getCristinPublisher().setNsdCode(null);
        cristinObject.getBookOrReportMetadata().getCristinPublisher().setPublisherName("NORD UniverSITet");
        var book =
            new NvaBookBuilder(cristinObject,
                               ChannelRegistryMapper.getInstance(),
                               mock(S3Client.class)).buildBookForPublicationContext();
        assertThat(book.getPublisher(), is(instanceOf(Publisher.class)));
    }

    @Test
    void shouldLookUpForPublisherByAlternativeNameWhenCristinPublisherIsMissingNsdCodeAndPrimaryName() {
        var cristinObject = CristinDataGenerator.randomBook();
        cristinObject.getBookOrReportMetadata().getCristinPublisher().setNsdCode(null);
        cristinObject.getBookOrReportMetadata().getCristinPublisher().setPublisherName(null);
        cristinObject.getBookOrReportMetadata().setPublisherName("NORD UniverSITet");
        var book = new NvaBookBuilder(cristinObject, ChannelRegistryMapper.getInstance(), mock(S3Client.class))
                       .buildBookForPublicationContext();
        assertThat(book.getPublisher(), is(instanceOf(Publisher.class)));
    }

    @Test
    void shouldCreateUnconfirmedPublisherWhenPrimaryPublisherNameIsMissing() {
        var cristinObject = CristinDataGenerator.randomBook();
        cristinObject.getBookOrReportMetadata().getCristinPublisher().setNsdCode(null);
        cristinObject.getBookOrReportMetadata().getCristinPublisher().setPublisherName(null);
        var publisherName = randomString();
        cristinObject.getBookOrReportMetadata().setPublisherName(publisherName);
        var book = new NvaBookBuilder(cristinObject, ChannelRegistryMapper.getInstance(), mock(S3Client.class))
                       .buildBookForPublicationContext();
        assertThat(((UnconfirmedPublisher) book.getPublisher()).getName(), is(equalTo(publisherName)));
    }

    @Test
    void shouldCreateNullPublisherWhenAllPublisherDataIsMissing() {
        var cristinObject = CristinDataGenerator.randomBook();
        cristinObject.getBookOrReportMetadata().getCristinPublisher().setNsdCode(null);
        cristinObject.getBookOrReportMetadata().getCristinPublisher().setPublisherName(null);
        cristinObject.getBookOrReportMetadata().setPublisherName(null);
        var book = new NvaBookBuilder(cristinObject, ChannelRegistryMapper.getInstance(), mock(S3Client.class))
                       .buildBookForPublicationContext();
        assertThat(book.getPublisher(), is(instanceOf(NullPublisher.class)));
    }

    @Test
    void shouldLookupForJournalByNsdCode() {
        var cristinObject = CristinDataGenerator.randomObject(JOURNAL_ARTICLE.getValue());
        cristinObject.getJournalPublication().getJournal().setNsdCode(339700);
        var periodical = new PeriodicalBuilder(cristinObject, ChannelRegistryMapper.getInstance(),
                                      mock(S3Client.class)).buildPeriodicalForPublicationContext();
        assertThat(((Journal) periodical).getId().toString(),
                   containsString("48C55300-CAFE-45EE-8386-402EC6E791EF"));
    }

    @Test
    void shouldLookupForJournalByJournalTitleWhenNsdCodeIsMissing() {
        var cristinObject = CristinDataGenerator.randomObject(JOURNAL_ARTICLE.getValue());
        cristinObject.getJournalPublication().getJournal().setNsdCode(null);
        cristinObject.getJournalPublication().getJournal().setJournalTitle("Arctos: Acta Philologica Fennica");
        var periodical = new PeriodicalBuilder(cristinObject, ChannelRegistryMapper.getInstance(),
                                               mock(S3Client.class)).buildPeriodicalForPublicationContext();
        assertThat(((Journal) periodical).getId().toString(),
                   containsString("48C55300-CAFE-45EE-8386-402EC6E791EF"));
    }

    @Test
    void shouldLookupForJournalByPrintIssnWhenNsdCodeAndJournalTitleIsMissing() {
        var cristinObject = CristinDataGenerator.randomObject(JOURNAL_ARTICLE.getValue());
        cristinObject.getJournalPublication().getJournal().setNsdCode(null);
        cristinObject.getJournalPublication().getJournal().setJournalTitle(null);
        cristinObject.getJournalPublication().getJournal().setIssn("0570-734X");
        var periodical = new PeriodicalBuilder(cristinObject, ChannelRegistryMapper.getInstance(),
                                               mock(S3Client.class)).buildPeriodicalForPublicationContext();
        assertThat(((Journal) periodical).getId().toString(),
                   containsString("48C55300-CAFE-45EE-8386-402EC6E791EF"));
    }

    @Test
    void shouldLookupForJournalByOnlineIssnWhenNsdCodeAndPrintIssnAndJournalTitleIsMissing() {
        var cristinObject = CristinDataGenerator.randomObject(JOURNAL_ARTICLE.getValue());
        cristinObject.getJournalPublication().getJournal().setNsdCode(null);
        cristinObject.getJournalPublication().getJournal().setJournalTitle(null);
        cristinObject.getJournalPublication().getJournal().setIssn(null);
        cristinObject.getJournalPublication().getJournal().setIssnOnline("1940-3372");
        var periodical = new PeriodicalBuilder(cristinObject, ChannelRegistryMapper.getInstance(),
                                               mock(S3Client.class)).buildPeriodicalForPublicationContext();
        assertThat(((Journal) periodical).getId().toString(),
                   containsString("48D7E3B5-B05E-4834-B01C-65B2E14124B2"));
    }

    @Test
    void shouldLookupForSeriesByNsdCode() {
        var cristinObject = CristinDataGenerator.randomBook();
        cristinObject.getBookOrReportMetadata().getBookSeries().setNsdCode(487595);
        var periodical = new NvaBookSeriesBuilder(cristinObject, ChannelRegistryMapper.getInstance(),
                                                  mock(S3Client.class)).createBookSeries();
        assertThat(((Series) periodical).getId().toString(),
                   containsString("48D7E3B5-B05E-4834-B01C-65B2E14124B2"));
    }

    @Test
    void shouldLookupForSeriesByTitleWhenNsdCodeIsMissing() {
        var cristinObject = CristinDataGenerator.randomBook();
        cristinObject.getBookOrReportMetadata().getBookSeries().setNsdCode(null);
        cristinObject.getBookOrReportMetadata().getBookSeries()
            .setJournalTitle("Physical Review E. Statistical, Nonlinear, and Soft Matter Physics");
        var periodical = new NvaBookSeriesBuilder(cristinObject, ChannelRegistryMapper.getInstance(),
                                                  mock(S3Client.class)).createBookSeries();
        assertThat(((Series) periodical).getId().toString(),
                   containsString("0000CD61-4C96-48CC-A121-D8073A071EA5"));
    }

    @Test
    void shouldLookupForSeriesByPrintIssnWhenNsdCodeAndSeriesTitleIsMissing() {
        var cristinObject = CristinDataGenerator.randomBook();
        cristinObject.getBookOrReportMetadata().getBookSeries().setNsdCode(null);
        cristinObject.getBookOrReportMetadata().getBookSeries().setJournalTitle(null);
        cristinObject.getBookOrReportMetadata().getBookSeries().setIssn("1539-3755");
        var periodical = new NvaBookSeriesBuilder(cristinObject, ChannelRegistryMapper.getInstance(),
                                                  mock(S3Client.class)).createBookSeries();
        assertThat(((Series) periodical).getId().toString(),
                   containsString("0000CD61-4C96-48CC-A121-D8073A071EA5"));
    }

    @Test
    void shouldLookupForSeriesByOnlineIssnWhenNsdCodeAndSeriesTitleIsMissing() {
        var cristinObject = CristinDataGenerator.randomBook();
        cristinObject.getBookOrReportMetadata().getBookSeries().setNsdCode(null);
        cristinObject.getBookOrReportMetadata().getBookSeries().setJournalTitle(null);
        cristinObject.getBookOrReportMetadata().getBookSeries().setIssn(null);
        cristinObject.getBookOrReportMetadata().getBookSeries().setIssnOnline("2296-889X");
        var periodical = new NvaBookSeriesBuilder(cristinObject, ChannelRegistryMapper.getInstance(),
                                                  mock(S3Client.class)).createBookSeries();
        assertThat(((Series) periodical).getId().toString(),
                   containsString("0012291D-1927-4B27-A49B-1ADC08CA771A"));
    }

    @Test
    void shouldImportJournalWithoutVolumeAndIssueWhenJournalAndIssueAreMissing() {
        var cristinObject = CristinDataGenerator.randomObject(JOURNAL_ARTICLE.getValue());
        cristinObject.getJournalPublication().setVolume(null);
        cristinObject.getJournalPublication().setIssue(null);
        var journal = (ProfessionalArticle) new JournalBuilder(cristinObject, mock(S3Client.class)).build();

        assertNull(journal.getVolume());
        assertNull(journal.getIssue());
    }

    @Test
    void shouldImportPublicationWithoutNpiSubjectHeadingWhenSubjectFieldIsMissingAndPublicationTypeIsBook() {
        var cristinObject = CristinDataGenerator.randomObject(TEXTBOOK.getValue());
        cristinObject.setBookOrReportMetadata(cristinObject.getBookOrReportMetadata().copy()
                                                 .withSubjectField(null)
                                                 .build());
        var publication = new CristinMapper(cristinObject, mock(CristinUnitsUtil.class), mock(S3Client.class))
                         .generatePublication();

        assertNull(publication.getEntityDescription().getNpiSubjectHeading());
    }

    @Test
    void shouldImportPublicationWithoutNpiSubjectHeadingWhenSubjectFieldIsMissingAndPublicationTypeIsReport() {
        var cristinObject = CristinDataGenerator.randomObject(CHAPTER.getValue());
        cristinObject.setBookOrReportPartMetadata(cristinObject.getBookOrReportPartMetadata().copy()
                                                  .withSubjectField(null)
                                                  .build());
        var publication = new CristinMapper(cristinObject, mock(CristinUnitsUtil.class), mock(S3Client.class))
                              .generatePublication();

        assertNull(publication.getEntityDescription().getNpiSubjectHeading());
    }

    @DisplayName("Main title in Cristin has statusOriginal set to 'N', when missing original title, use first present" +
                 "title as main title")
    @Test
    void shouldImportPublicationWithFirstPresentTitleWhenMainTitleIsMissing() {
        var cristinObject = CristinDataGenerator.randomObject(CHAPTER.getValue());
        cristinObject.setCristinTitles(List.of(new CristinTitle(null, randomString(), null, null),
                                               new CristinTitle(null, randomString(), "N", null)));
        var publication = new CristinMapper(cristinObject, mock(CristinUnitsUtil.class), mock(S3Client.class))
                              .generatePublication();

        assertNotNull(publication.getEntityDescription().getMainTitle());
    }

    private List<Contributor> getContributors(CristinObject singleCristinObject) {
        return mapToPublication(singleCristinObject)
                   .getEntityDescription()
                   .getContributors();
    }

    private static List<Integer> getSequnceNumberList(List<Contributor> actualContributors) {
        return actualContributors.stream()
                   .map(Contributor::getSequence)
                   .sorted(Comparator.nullsLast(Comparator.naturalOrder()))
                   .toList();
    }

    private static List<Integer> getSequenceNumberSet(List<Integer> actualSequenceNumbers) {
        return new HashSet<>(actualSequenceNumbers).stream()
                   .sorted(Comparator.nullsLast(Comparator.naturalOrder()))
                   .toList();
    }

    private CristinObject getSingleCristinObject() {
        return cristinObjects(1).toList().getFirst();
    }

    private CristinObject cristinObjectWithSomeContributorsWithoutSeqNumber(CristinObject cristinObject) {
        cristinObject.getContributors().getLast().setContributorOrder(null);
        cristinObject.getContributors().getFirst().setContributorOrder(null);
        return cristinObject;
    }

    private CristinObject cristinObjectWhereEveryContributorIsMissingSeqNumber(CristinObject cristinObject) {
        cristinObject.getContributors().forEach(cristinContributor -> cristinContributor.setContributorOrder(null));
        return cristinObject;
    }

    private AdditionalIdentifier createExpectedAdditionalIdentifier(CristinSource cristinSource) {
        return new AdditionalIdentifier(cristinSource.getSourceCode(),
                                     cristinSource.getSourceIdentifier());
    }

    private CristinObject createObjectWithRoleCode(CristinContributorRoleCode actualCristinRoleCode) {
        return CristinDataGenerator.newCristinObjectWithRoleCode(actualCristinRoleCode);
    }

    private PublicationDate yearToPublicationDate(Integer year) {
        return new PublicationDate.Builder().withYear(year.toString()).build();
    }


    private List<ContributionReference> extractContributions(Publication publication) {

        AdditionalIdentifierBase cristinIdentifier = publication.getAdditionalIdentifiers().stream()
                                                     .filter(CristinIdentifier.class::isInstance)
                                                     .collect(SingletonCollector.collect());
        Integer cristinIdentifierValue = Integer.parseInt(cristinIdentifier.value());

        return publication.getEntityDescription()
                   .getContributors().stream()
                   .map(contributor -> extractContributionReference(cristinIdentifierValue, contributor))
                   .toList();
    }

    private List<ContributionReference> extractContributions(CristinObject cristinObject) {
        final Integer cristinResourceIdentifier = cristinObject.getId();
        return cristinObject.getContributors().stream()
                   .map(c -> new ContributionReference(cristinResourceIdentifier, c.getIdentifier(),
                                                       c.getContributorOrder()))
                   .toList();
    }

    private ContributionReference extractContributionReference(Integer cristinIdentifierValue,
                                                               Contributor contributor) {
        return new ContributionReference(cristinIdentifierValue, extractPersonId(contributor),
                                         contributor.getSequence());
    }

    private Integer extractPersonId(Contributor contributor) {
        String personIdentifier = Path.of(contributor.getIdentity().getId().getPath()).getFileName().toString();
        return Integer.parseInt(personIdentifier);
    }

    private String formatNameAccordingToNvaPattern(CristinContributor cristinContributor) {
        return cristinContributor.getGivenName() + StringUtils.SPACE + cristinContributor.getFamilyName();
    }

    private CristinTitle mainTitle(List<CristinTitle> titles) {
        return titles.stream().filter(CristinTitle::isMainTitle).collect(SingletonCollector.collect());
    }
}