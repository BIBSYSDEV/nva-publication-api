package no.unit.nva.cristin.mapper;

import static no.unit.nva.cristin.CristinDataGenerator.largeRandomNumber;
import static no.unit.nva.cristin.CristinDataGenerator.randomAffiliation;
import static no.unit.nva.cristin.CristinDataGenerator.randomString;
import static no.unit.nva.cristin.lambda.constants.HardcodedValues.HARDCODED_SAMPLE_DOI;
import static no.unit.nva.cristin.lambda.constants.MappingConstants.CRISTIN_ORG_URI;
import static no.unit.nva.cristin.mapper.CristinObject.IDENTIFIER_ORIGIN;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.net.URI;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.cristin.AbstractCristinImportTest;
import no.unit.nva.cristin.CristinDataGenerator;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.Role;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.book.BookAnthology;
import no.unit.nva.model.instancetypes.book.BookMonograph;
import no.unit.nva.model.pages.MonographPages;
import nva.commons.core.SingletonCollector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class CristinMapperTest extends AbstractCristinImportTest {

    public static final String NAME_DELIMITER = ", ";

    @BeforeEach
    public void init() {
        super.init();
        testingData = CristinDataGenerator.randomDataAsString();
    }

    @Test
    public void mapReturnsResourceWithCristinIdStoredInAdditionalIdentifiers() {
        Set<Integer> expectedIds = cristinObjects().map(CristinObject::getId).collect(Collectors.toSet());

        Set<Integer> actualIds = cristinObjects()
            .map(cristinObject -> cristinObject.toPublication())
            .map(Publication::getAdditionalIdentifiers)
            .flatMap(Collection::stream)
            .map(AdditionalIdentifier::getValue)
            .map(Integer::parseInt)
            .collect(Collectors.toSet());

        assertThat(expectedIds.size(), is(equalTo(NUMBER_OF_LINES_IN_RESOURCES_FILE)));
        assertThat(actualIds, is(equalTo(expectedIds)));
    }

    @Test
    public void mapReturnsResourceWithMainTitleBeingTheTitleAnnotatedAsOriginalTitle() {

        List<CristinObject> cristinObjects = cristinObjects().collect(Collectors.toList());
        List<String> expectedTitles = cristinObjects.stream()
            .map(CristinObject::getCristinTitles)
            .map(this::mainTitle)
            .map(CristinTitle::getTitle)
            .collect(Collectors.toList());

        List<String> actualTitles = cristinObjects.stream()
                .map(cristinObject -> cristinObject.toPublication())
                .map(Publication::getEntityDescription)
                .map(EntityDescription::getMainTitle)
                .collect(Collectors.toList());

        assertThat(expectedTitles, is(not(empty())));
        assertThat(actualTitles, containsInAnyOrder(expectedTitles.toArray(String[]::new)));
        assertThat(actualTitles.size(), is(equalTo(cristinObjects.size())));
    }

    @Test
    @DisplayName("map returns resource with date equal to \"arstall\"")
    public void mapReturnsResourceWithDateEqualToArstall() {
        List<Integer> expectedPublicationYear = cristinObjects()
            .map(CristinObject::getPublicationYear)
            .collect(Collectors.toList());

        List<Integer> actualPublicationDates = cristinObjects().map(CristinObject::toPublication)
            .map(Publication::getEntityDescription)
            .map(EntityDescription::getDate)
            .map(PublicationDate::getYear)
            .map(Integer::parseInt)
            .collect(Collectors.toList());
        assertThat(expectedPublicationYear, is(not(empty())));
        assertThat(actualPublicationDates, containsInAnyOrder(expectedPublicationYear.toArray(Integer[]::new)));
    }

    @Test
    @DisplayName("map returns resource with createdDate equal to \"dato_opprettet\"")
    public void mapReturnsResourceWithCreatedDateEqualToCristinDateAssumedToBeUtcDate() {
        ZoneOffset utc = ZoneId.of("UTC").getRules().getOffset(Instant.now());
        List<Instant> expectedCreatedDates = cristinObjects()
            .map(CristinObject::getEntryCreationDate)
            .map(LocalDate::atStartOfDay)
            .map(time -> time.toInstant(utc))
            .collect(Collectors.toList());

        List<Instant> actualCreatedDates = cristinObjects().map(CristinObject::toPublication)
            .map(Publication::getCreatedDate)
            .collect(Collectors.toList());

        assertThat(actualCreatedDates, containsInAnyOrder(expectedCreatedDates.toArray(Instant[]::new)));
    }

    @Test
    public void mapReturnsBookAnthologyWhenInputHasMainTypeBookAndSecondaryTypeAnthology() {
        testingData = Stream.of(CristinDataGenerator.randomBookAnthology())
            .map(JsonSerializable::toJsonString)
            .collect(SingletonCollector.collect());

        Publication actualPublication = cristinObjects()
            .map(CristinObject::toPublication)
            .collect(SingletonCollector.collect());

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
    public void mapReturnsBookMonographWhenInputHasMainTypeBookAndSecondaryTypeMonograph() {
        testingData = Stream.of(CristinDataGenerator.randomBook(CristinSecondaryCategory.MONOGRAPH))
            .map(JsonSerializable::toJsonString)
            .collect(SingletonCollector.collect());

        Publication actualPublication = cristinObjects()
            .map(CristinObject::toPublication)
            .collect(SingletonCollector.collect());

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
    public void mapReturnsResourceWhereNvaContributorsNamesAreConcatenationsOfCristinFirstAndFamilyNames() {

        List<String> expectedContributorNames = cristinObjects()
            .map(CristinObject::getContributors)
            .flatMap(Collection::stream)
            .map(this::formatNameAccordingToNvaPattern)
            .collect(Collectors.toList());

        List<String> actualContributorNames = cristinObjects()
            .map(CristinObject::toPublication)
            .map(Publication::getEntityDescription)
            .map(EntityDescription::getContributors)
            .flatMap(Collection::stream)
            .map(Contributor::getIdentity)
            .map(Identity::getName)
            .collect(Collectors.toList());

        assertThat(actualContributorNames, containsInAnyOrder(expectedContributorNames.toArray(String[]::new)));
    }

    @Test
    public void mapReturnsResourceWhereNvaContributorSequenceIsEqualToCristinContributorSequence() {

        Set<ContributionReference> expectedContributions = cristinObjects()
            .map(this::extractContributions)
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());

        Set<ContributionReference> actualContributions = cristinObjects()
            .map(CristinObject::toPublication)
            .map(this::extractContributions)
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());

        assertThat(expectedContributions, is(equalTo(actualContributions)));
    }

    @Test
    public void mapReturnsResourceWhereNvaContributorHasAffiliationsWithUriCreatedBasedOnReferenceUriAndUnitNumbers() {

        List<URI> expectedAffiliations = cristinObjects()
            .flatMap(cristinEntries -> cristinEntries.getContributors().stream())
            .flatMap(contributor -> contributor.getAffiliations().stream())
            .map(this::explicitFormattingOfCristinAffiliationCode)
            .map(this::addCristinOrgHostPrefix)
            .map(URI::create)
            .collect(Collectors.toList());

        List<URI> actualAffiliations = cristinObjects().map(CristinObject::toPublication)
            .map(Publication::getEntityDescription)
            .map(EntityDescription::getContributors)
            .flatMap(Collection::stream)
            .map(Contributor::getAffiliations)
            .flatMap(Collection::stream)
            .map(Organization::getId)
            .collect(Collectors.toList());

        assertThat(actualAffiliations, containsInAnyOrder(expectedAffiliations.toArray(URI[]::new)));
    }

    @Test
    public void mapReturnsPublicationWithPublicationDateEqualToCristinPublicationYear() {
        List<PublicationDate> expectedPublicationDates = cristinObjects()
            .map(CristinObject::getPublicationYear)
            .map(this::yearToPublicationDate)
            .collect(Collectors.toList());
        List<PublicationDate> actualPublicationDates = cristinObjects()
            .map(CristinObject::toPublication)
            .map(Publication::getEntityDescription)
            .map(EntityDescription::getDate)
            .collect(Collectors.toList());

        assertThat(actualPublicationDates,
                   containsInAnyOrder(expectedPublicationDates.toArray(PublicationDate[]::new)));
    }

    @Test
    public void mapReturnsPublicationWithHardcodedLink() {
        Set<URI> actualLicenseIdentifiers = cristinObjects()
            .map(CristinObject::toPublication)
            .map(Publication::getLink)
            .collect(Collectors.toSet());

        Set<URI> expectedLinks = Set.of(HARDCODED_SAMPLE_DOI);

        assertThat(actualLicenseIdentifiers, is(equalTo(expectedLinks)));
    }

    @ParameterizedTest(name = "map returns NVA role {1} when Cristin role is {0}")
    @CsvSource({"REDAKTØR,EDITOR", "FORFATTER,CREATOR"})
    public void mapReturnsPublicationsWhereCristinRoleIsMappedToCorrectNvaRole(String cristinRole,
                                                                               String nvaRole) {
        CristinContributorRoleCode actualCristinRole = CristinContributorRoleCode.fromString(cristinRole);
        Role expectedNvaRole = Role.lookup(nvaRole);
        CristinObject objectWithEditor = createObjectWithRoleCode(actualCristinRole);

        Optional<Contributor> contributor = objectWithEditor.toPublication()
            .getEntityDescription()
            .getContributors()
            .stream()
            .filter(c -> c.getRole().equals(expectedNvaRole))
            .findAny();
        assertThat(contributor.isPresent(), is(true));
        assertThat(contributor.orElseThrow().getRole(), is(equalTo(expectedNvaRole)));
    }

    @Test
    public void mapReturnsPublicationWhereCristinTotalNumberOfPagesIsMappedToNvaPages() {
        CristinObject cristinImport = CristinDataGenerator.randomBook();

        String numberOfPages = cristinImport.getBookOrReportMetadata().getNumberOfPages();

        Publication actualPublication = cristinImport.toPublication();

        PublicationInstance<?> actualPublicationInstance = actualPublication
            .getEntityDescription()
            .getReference()
            .getPublicationInstance();

        MonographPages monographPages = (MonographPages) actualPublicationInstance.getPages();
        String actualNumberOfPages = monographPages.getPages();

        assertThat(actualNumberOfPages, is(equalTo(numberOfPages)));
    }

    @Test
    public void mapReturnsPublicationWhereCristinIsbnIsMappedToNvaIsbnList() {
        CristinObject cristinImport = CristinDataGenerator.randomBook();

        String isbn = cristinImport.getBookOrReportMetadata().getIsbn();

        Publication actualPublication = cristinImport.toPublication();

        PublicationContext actualPublicationContext = actualPublication
            .getEntityDescription()
            .getReference()
            .getPublicationContext();

        Book bookSubType = (Book) actualPublicationContext;
        List<String> actualIsbnList = bookSubType.getIsbnList();

        assertThat(actualIsbnList.get(0), is(equalTo(isbn)));
    }

    @Test
    public void mapSetsNameToNullWhenBothFamilyNameAndGivenNameAreMissing() {
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
        Executable action = cristinObjectWithContributorsWithoutRole::toPublication;
        MissingFieldsException error = assertThrows(MissingFieldsException.class, action);
        assertThat(error.getMessage(), containsString(".entityDescription.contributors[0].identity.name"));
    }

    @Test
    public void constructorThrowsExceptionWhenABookReportHasASubjectFieldButSubjectFieldCodeIsNull() {
        CristinObject cristinObject = CristinDataGenerator.randomBook();
        cristinObject.getBookOrReportMetadata().getSubjectField().setSubjectFieldCode(null);

        System.out.println(cristinObject);

        Executable action = cristinObject::toPublication;
        assertThrows(MissingFieldsException.class, action);
    }

    @Test
    public void mapThrowsMissingFieldsExceptionWhenNonIgnoredFieldIsMissing() {
        //re-use the test for the author's name
        mapSetsNameToNullWhenBothFamilyNameAndGivenNameAreMissing();
    }

    @ParameterizedTest(name = "map returns journal with DOI URI when input is Journal with DOI:{0}")
    @CsvSource({
        "http://dx.doi.org/10.5750/ejpch.v5i1.1209, https://doi.org/10.5750/ejpch.v5i1.1209",
        "https://doi.org/10.1016/j.toxlet.2020.09.006, https://doi.org/10.1016/j.toxlet.2020.09.006",
        "DOI:10.1080/02699052.2017.1312145, https://doi.org/10.1080/02699052.2017.1312145",
        "doi:10.1080/02699052.2017.1312145, https://doi.org/10.1080/02699052.2017.1312145",
        "10.1016/j.toxlet.2020.09.006, https://doi.org/10.1016/j.toxlet.2020.09.006"
    })
    public void mapReturnsJournalWithDoiWhenInputIsJournalResultWithDoi(String inputDoi, String expectedDoiUri) {
        CristinObject cristinObject =
            CristinDataGenerator.randomObject(CristinSecondaryCategory.JOURNAL_ARTICLE.toString());
        cristinObject.getJournalPublication().setDoi(inputDoi);
        Publication publication = cristinObject.toPublication();
        assertThat(publication.getEntityDescription().getReference().getDoi(), is(equalTo(URI.create(expectedDoiUri))));
    }

    @Test
    public void mapReturnsJournalWithDoiUriWhenInputIsANonExistingDoiAndOnlineValidationIsDisabled() {
        String inputDoi = "10.1234/non.existin.doi";
        URI expectedDoiUri = URI.create("https://doi.org/" + inputDoi);
        CristinObject cristinObject =
            CristinDataGenerator.randomObject(CristinSecondaryCategory.JOURNAL_ARTICLE.toString());
        cristinObject.getJournalPublication().setDoi(inputDoi);
        Publication publication = cristinObject.toPublication();
        assertThat(publication.getEntityDescription().getReference().getDoi(), is(equalTo(expectedDoiUri)));
    }

    @Test
    public void mapReturnsJournalWithoutDoiUriWhenInputIsAJournalArticleWihoutDoi() {
        CristinObject cristinObject =
            CristinDataGenerator.randomObject(CristinSecondaryCategory.JOURNAL_ARTICLE.toString());
        cristinObject.getJournalPublication().setDoi(null);
        Publication publication = cristinObject.toPublication();
        assertThat(publication.getEntityDescription().getReference().getDoi(), is(nullValue()));
    }

    private CristinObject createObjectWithRoleCode(CristinContributorRoleCode actualCristinRoleCode) {
        return CristinDataGenerator.newCristinObjectWithRoleCode(actualCristinRoleCode);
    }

    private CristinContributor contributorWithoutRoles() {
        CristinContributorsAffiliation affiliation = randomAffiliation()
            .copy()
            .withRoles(Collections.emptyList())
            .build();

        return CristinContributor.builder()
            .withFamilyName(randomString())
            .withIdentifier(largeRandomNumber())
            .withContributorOrder(1)
            .withGivenName(randomString())
            .withAffiliations(List.of((affiliation)))
            .build();
    }

    private PublicationDate yearToPublicationDate(Integer year) {
        return new PublicationDate.Builder().withYear(year.toString()).build();
    }

    //We do not use any more complex logic to make the tests fail if anything changes
    private String explicitFormattingOfCristinAffiliationCode(CristinContributorsAffiliation c) {
        return String.format("%s.%s.%s.%s", c.getInstitutionIdentifier(), c.getDepartmentIdentifier(),
                             c.getSubdepartmentIdentifier(), c.getGroupNumber());
    }

    //Hardcode Cristin ORG URIs for avoiding re-using the logic under test.
    private String addCristinOrgHostPrefix(String cristinAffiliationCode) {
        return CRISTIN_ORG_URI.toString() + cristinAffiliationCode;
    }

    private List<ContributionReference> extractContributions(Publication publication) {

        AdditionalIdentifier cristinIdentifier = publication.getAdditionalIdentifiers().stream()
            .filter(this::isCristinIdentifier)
            .collect(SingletonCollector.collect());
        Integer cristinIdentifierValue = Integer.parseInt(cristinIdentifier.getValue());

        return publication.getEntityDescription()
            .getContributors().stream()
            .map(contributor -> extractContributionReference(cristinIdentifierValue, contributor))
            .collect(Collectors.toList());
    }

    private List<ContributionReference> extractContributions(CristinObject cristinObject) {
        final Integer cristinResourceIdentifier = cristinObject.getId();
        return cristinObject.getContributors().stream()
            .map(c -> new ContributionReference(cristinResourceIdentifier, c.getIdentifier(),
                                                c.getContributorOrder()))
            .collect(Collectors.toList());
    }

    private boolean isCristinIdentifier(AdditionalIdentifier identifier) {
        return identifier.getSource().equals(IDENTIFIER_ORIGIN);
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
        return cristinContributor.getFamilyName() + NAME_DELIMITER + cristinContributor.getGivenName();
    }

    private CristinTitle mainTitle(List<CristinTitle> titles) {
        return titles.stream().filter(CristinTitle::isMainTitle).collect(SingletonCollector.collect());
    }
}