package cucumber;

import static cucumber.utils.transformers.CristinContributorAffiliationTransformer.parseContributorAffiliationsFromMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.nullValue;
import cucumber.utils.ContributorFlattenedDetails;
import cucumber.utils.exceptions.MisformattedScenarioException;
import cucumber.utils.transformers.CristinContributorTransformer;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.cristin.CristinDataGenerator;
import no.unit.nva.cristin.mapper.CristinContributor;
import no.unit.nva.cristin.mapper.CristinContributor.CristinContributorBuilder;
import no.unit.nva.cristin.mapper.CristinContributorRole;
import no.unit.nva.cristin.mapper.CristinContributorRoleCode;
import no.unit.nva.cristin.mapper.CristinContributorsAffiliation;
import no.unit.nva.cristin.mapper.CristinHrcsCategoriesAndActivities;
import no.unit.nva.cristin.mapper.CristinPresentationalWork;
import no.unit.nva.cristin.mapper.CristinTags;
import no.unit.nva.cristin.mapper.CristinTitle;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Project;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.ResearchProject;
import nva.commons.core.SingletonCollector;

public class GeneralMappingRules {

    public static final int SKIP_HEADERS = 1;
    public static final String ERROR_MESSAGE_FOR_MISMATCH_BETWEEN_ROLES_AND_AFFILIATIONS =
        "The number of contributor and the number of affiliations do not match";
    public static final String WRONG_NUMBER_OF_CONTRIBUTORS_OR_AFFILIATIONS_SET_BY_SCENARIO =
        "Scenario is expected to set one contributor with one affiliation in the Cristin Result";
    public static final int FIRST_AUTHOR = 1;
    private final ScenarioContext scenarioContext;

    public GeneralMappingRules(ScenarioContext scenarioContext) {
        this.scenarioContext = scenarioContext;
    }

    @Given("a valid Cristin Result")
    public void validCristinEntry() {
        this.scenarioContext.newCristinEntry();
    }

    @Given("the Cristin Result has id equal to {int}")
    public void theCristinEntryHasIdEqualTo(int id) {

        this.scenarioContext.getCristinEntry().setId(id);
    }

    @Given("the Cristin Result has an non null array of CristinTitles")
    public void theCristinEntryHasAnNonNullArrayOfCristinTitles() {
        scenarioContext.addEmptyCristinTitle();
    }

    @Given("the CristinTitle array has an entry")
    public void theCristinTitleArrayHasASingleEntry() {
        scenarioContext.addCristinTitle();
    }

    @Given("the CristinTitle entry has title text equal to {string}")
    public void theCristinTitleEntryHasTitleTextEqualTo(String titleText) {
        scenarioContext.getLatestCristinTitle().setTitle(titleText);
    }

    @Given("the CristinTitle entry has original status annotation {string}")
    public void theCristinTitleEntryHasOriginalStatusAnnotation(String statusOriginal) {
        scenarioContext.getLatestCristinTitle().setStatusOriginal(statusOriginal);
    }

    @Given("the Cristin Result has publication year {int}")
    public void theCristinEntryHasPublicationYear(int publicationYear) {
        scenarioContext.getCristinEntry().setPublicationYear(publicationYear);
    }

    @Given("that Cristin Result has created date equal to the local date {string}")
    public void thatCristinEntryHasCreatedDateEqualToTheLocalDate(String dateString) {
        LocalDate localDate = LocalDate.parse(dateString);
        scenarioContext.getCristinEntry().setEntryCreationDate(localDate);
    }

    @Given("that Cristin Result has modified date equal to the local date {string}")
    public void thatCristinResultHasModifiedDateEqualToTheLocalDate(String dateString) {
        LocalDate localDate = LocalDate.parse(dateString);
        scenarioContext.getCristinEntry().setEntryLastModifiedDate(localDate);
    }

    @When("the Cristin Result is converted to an NVA Resource")
    public void is_converted_to_an_nva_entry() {
        scenarioContext.convertToNvaEntry();
    }

    @Then("the NVA Resource has an additional identifier with key {string} and value {int}")
    public void theNvaEntryHasAnAdditionalIdentifierWithKeyAndValue(String cristinAdditionalIdentifierKey,
                                                                    int expectedCristinId) {
        Set<AdditionalIdentifier> actualAdditionalIdentifiers =
            scenarioContext.getNvaEntry().getAdditionalIdentifiers();
        AdditionalIdentifier expectedIdentifier =
            new AdditionalIdentifier(cristinAdditionalIdentifierKey, Integer.toString(expectedCristinId));

        assertThat(actualAdditionalIdentifiers, contains(expectedIdentifier));
    }

    @Then("the NVA Resource has an EntityDescription with mainTitle {string}")
    public void theNvaResourceHasAnEntityDescriptionWithMainTitle(String expectedTitle) {
        String actualTitle = scenarioContext.getNvaEntry().getEntityDescription().getMainTitle();
        assertThat(actualTitle, is(equalTo(expectedTitle)));
    }

    @Then("the NVA Resource has a Publication Date with year equal to {int}, month equal to null and "
          + "day equal to null")
    public void theNvaResourceHasPublicationDateWithTheCristinYear(Integer expectedPublicationYear) {
        PublicationDate actualDate = scenarioContext.getNvaEntry().getEntityDescription().getDate();
        assertThat(actualDate.getYear(), is(equalTo(expectedPublicationYear.toString())));
        assertThat(actualDate.getMonth(), is(nullValue()));
        assertThat(actualDate.getDay(), is(nullValue()));
    }

    @Then("the NVA Resource has a Creation Date equal to {string}")
    public void theNvaResourceHasACreationDateEqualTo(String expectedIsoInstant) {
        Instant expectedInstant = Instant.parse(expectedIsoInstant);
        assertThat(scenarioContext.getNvaEntry().getCreatedDate(), is(equalTo(expectedInstant)));
    }

    @Then("the NVA Resource has a Published Date equal to {string}")
    public void theNvaResourceHasAPublishedDateEqualTo(String expectedIsoInstant) {
        Instant expectedInstant = Instant.parse(expectedIsoInstant);
        assertThat(scenarioContext.getNvaEntry().getPublishedDate(), is(equalTo(expectedInstant)));
    }

    @Then("the NVA Resource has a Modified Date equal to {string}")
    public void theNvaResourceHasAModifiedDateEqualTo(String expectedIsoInstant) {
        Instant expectedInstant = Instant.parse(expectedIsoInstant);
        assertThat(scenarioContext.getNvaEntry().getModifiedDate(), is(equalTo(expectedInstant)));
    }

    @Given("the Cristin Result has an array of CristinTitles with values:")
    public void theCristinResultHasAnCristinTitlesWithValues(List<CristinTitle> cristinTitles) {
        scenarioContext.getCristinEntry().setCristinTitles(cristinTitles);
    }

    @Given("that the Cristin Result has Contributors with names:")
    public void thatCristinResultsHasTheContributors(DataTable nameTable) {
        List<CristinContributor> contributors = nameTable.asMaps()
                                                    .stream()
                                                    .map(CristinContributorTransformer::toContributor)
                                                    .map(CristinContributorBuilder::build)
                                                    .collect(Collectors.toList());

        scenarioContext.getCristinEntry().setContributors(contributors);
    }

    @Given("that the Cristin Result has the Contributor(s) with name(s) and sequence:")
    public void thatTheCristinResultHasTheContributorsWithNamesAndSequence(DataTable dataTable) {
        List<CristinContributor> contributors = dataTable.asMaps().stream()
                                                    .map(CristinContributorTransformer::toContributorWithOrdinalNumber)
                                                    .map(CristinContributorBuilder::build)
                                                    .collect(Collectors.toList());
        scenarioContext.getCristinEntry().setContributors(contributors);
    }

    @Given("the Contributor(s) is/are affiliated with the following Cristin Institution( respectively):")
    public void theContributorsAreAffiliatedWithTheFollowingCristinInstitutionRespectively(DataTable dataTable) {
        List<CristinContributorsAffiliation> desiredInjectedAffiliations =
            parseContributorAffiliationsFromMap(dataTable);
        List<CristinContributor> contributors = this.scenarioContext.getCristinEntry().getContributors();

        ensureTheContributorsAreAsManyAsTheInjectedAffiliations(desiredInjectedAffiliations, contributors);

        injectAffiliationsIntoContributors(desiredInjectedAffiliations, contributors);
    }

    @Given("the Contributor has the role {string}")
    public void theContributorHasTheRoleCristinRole(String roleCode) {
        List<CristinContributor> cristinContributors = this.scenarioContext.getCristinEntry().getContributors();
        if (cristinContributors.size() != 1 || cristinContributors.get(0).getAffiliations().size() != 1) {
            throw new IllegalStateException(WRONG_NUMBER_OF_CONTRIBUTORS_OR_AFFILIATIONS_SET_BY_SCENARIO);
        }
        CristinContributor contributor = cristinContributors.get(0);
        CristinContributorsAffiliation affiliation = contributor.getAffiliations().get(0);
        CristinContributorRoleCode injectedRoleCode = CristinContributorRoleCode.fromString(roleCode);
        CristinContributorRole injectedRole = CristinContributorRole.builder().withRoleCode(injectedRoleCode).build();
        affiliation.setRoles(List.of(injectedRole));
    }

    @Given("that the Cristin Result has a Contributor with role {string}")
    public void thatTheCristinResultHasAContributorWithRole(String roleCodeString) {
        CristinContributorRoleCode roleCode = CristinContributorRoleCode.fromString(roleCodeString);
        CristinContributorRole desiredRole = CristinContributorRole.builder().withRoleCode(roleCode).build();

        CristinContributorsAffiliation affiliation = CristinDataGenerator.randomAffiliation()
                                                         .copy()
                                                         .withRoles(List.of(desiredRole))
                                                         .build();

        CristinContributor newContributor = CristinDataGenerator.randomContributor(FIRST_AUTHOR).copy()
                                                .withAffiliations(List.of(affiliation))
                                                .build();
        this.scenarioContext.getCristinEntry().setContributors(List.of(newContributor));
    }

    @Given("that the Cristin Result has a Contributor with no role")
    public void thatTheCristinResultHasAContributorWithNoRole() {
        CristinContributorsAffiliation affiliation = CristinDataGenerator.randomAffiliation()
                                                         .copy()
                                                         .withRoles(Collections.emptyList())
                                                         .build();
        CristinContributor newContributor = CristinDataGenerator.randomContributor(FIRST_AUTHOR).copy()
                                                .withAffiliations(List.of(affiliation))
                                                .build();
        this.scenarioContext.getCristinEntry().setContributors(List.of(newContributor));
    }

    @Then("the NVA Resource has a List of NVA Contributors:")
    public void theNvaResourceHasAListOfNvaContributors(DataTable expectedContributors) {
        List<String> expectedContributorNames = expectedContributors.rows(SKIP_HEADERS).asList();
        List<String> actualContributorNames = scenarioContext.getNvaEntry().getEntityDescription().getContributors()
                                                  .stream()
                                                  .map(Contributor::getIdentity)
                                                  .map(Identity::getName)
                                                  .collect(Collectors.toList());

        assertThat(actualContributorNames, contains(expectedContributorNames.toArray(String[]::new)));
    }



    @Then("the NVA Resource has a List of NVA Contributors with the following sequences:")
    public void theNvaResourceHasAListOfNvaContributorsWithTheFollowingSequences(DataTable table) {
        List<ContributorFlattenedDetails> actualContributors =
            this.scenarioContext.getNvaEntry()
                .getEntityDescription()
                .getContributors()
                .stream()
                .map(ContributorFlattenedDetails::extractNameAndSequence)
                .collect(Collectors.toList());

        List<ContributorFlattenedDetails> expectedContributors =
            table.asMaps()
                .stream()
                .map(ContributorFlattenedDetails::fromDataTableMapEntry)
                .collect(Collectors.toList());

        assertThat(actualContributors, containsInAnyOrder(expectedContributors.toArray(
            ContributorFlattenedDetails[]::new)));
    }



    @Then("the NVA Resource Contributors have the following names, sequences and affiliation URIs")
    public void theNvaResourceContributorsHaveTheFollowingNamesSequencesAndAffiliationURIs(DataTable dataTable) {
        List<Contributor> contributors = this.scenarioContext.getNvaEntry().getEntityDescription().getContributors();

        List<ContributorFlattenedDetails> actualDetails =
            contributors.stream()
                .map(ContributorFlattenedDetails::extractNameSequenceAndAffiliationUri)
                .collect(Collectors.toList());
        ContributorFlattenedDetails[] expectedDetails =
            dataTable.asMaps()
                .stream()
                .map(ContributorFlattenedDetails::fromDataTableMapEntry)
                .collect(Collectors.toList())
                .toArray(ContributorFlattenedDetails[]::new);

        assertThat(actualDetails, containsInAnyOrder(expectedDetails));
    }

    @Then("the NVA Contributor has the role {string}")
    public void theNvaContributorHasTheRole(String expectedNvaRole) {
        List<Contributor> contributors = this.scenarioContext.getNvaEntry().getEntityDescription()
                                             .getContributors();
        assertThat(contributors.size(), is(equalTo(this.scenarioContext.getCristinEntry().getContributors().size())));
        String actualRole = contributors.stream()
                                .map(Contributor::getRole)
                                .map(Enum::toString)
                                .collect(SingletonCollector.collect());
        assertThat(actualRole, is(equalTo(expectedNvaRole)));
    }

    @Then("an error is reported.")
    public void anErrorIsReported() {
        assertThat(this.scenarioContext.mappingIsSuccessful(), is(false));
    }

    @Given("that the Cristin Result has a Contributor with no family and no given name")
    public void thatTheCristinResultHasAContributorWithNoFamilyAndNoGivenName() {
        CristinContributor contributorWithNoName = CristinDataGenerator.randomContributor(FIRST_AUTHOR).copy()
                                                       .withFamilyName(null)
                                                       .withGivenName(null)
                                                       .build();
        this.scenarioContext.getCristinEntry().setContributors(List.of(contributorWithNoName));
    }

    @Then("the NVA Resource has an EntityDescription with language {string}")
    public void theNvaResourceHasAnEntityDescriptionWithLanguage(String expectedLanguageUri) {
        String actualLanguage = this.scenarioContext.getNvaEntry().getEntityDescription().getLanguage().toString();
        assertThat(actualLanguage,is(equalTo(expectedLanguageUri)));
    }

    private void injectAffiliationsIntoContributors(List<CristinContributorsAffiliation> desiredInjectedAffiliations,
                                                    List<CristinContributor> contributors) {
        for (int contributorsIndex = 0; contributorsIndex < contributors.size(); contributorsIndex++) {
            CristinContributorsAffiliation desiredAffiliation = desiredInjectedAffiliations.get(contributorsIndex);
            contributors.get(contributorsIndex).setAffiliations(List.of(desiredAffiliation));
        }
    }

    private void ensureTheContributorsAreAsManyAsTheInjectedAffiliations(
        List<CristinContributorsAffiliation> desiredInjectedAffiliations,
        List<CristinContributor> contributors) {
        if (contributors.size() != desiredInjectedAffiliations.size()) {
            throw new MisformattedScenarioException(ERROR_MESSAGE_FOR_MISMATCH_BETWEEN_ROLES_AND_AFFILIATIONS);
        }
    }

    @Given("that the Cristin Result has a PresentationalWork object that is not null")
    public void thatTheCristinResultHasAPresentationalWorkObjectThatIsNotNull() {
        scenarioContext.getCristinEntry()
                .setPresentationalWork(List.of(CristinDataGenerator.randomPresentationalWork()));
    }


    @Then("the NVA Resource has the following abstract {string}")
    public void theNvaResourceHasTheFollowingAbstract(String expectedAbstract) {
        String actuallAbstract = scenarioContext
                                    .getNvaEntry()
                                    .getEntityDescription()
                                    .getAbstract();
        assertThat(actuallAbstract, is(equalTo(expectedAbstract)));
    }

    @Then("the NVA Resource has no abstract")
    public void theNvaResourceHasNoAbstract() {
        String actuallAbstract = scenarioContext
                .getNvaEntry()
                .getEntityDescription()
                .getAbstract();
        assertThat(actuallAbstract, is(equalTo(null)));
    }

    @Given("the cristin title abstract is sett to null")
    public void theCristinTitleAbstractIsSettToNull() {
        for (CristinTitle title : scenarioContext.getCristinEntry().getCristinTitles()) {
            title.setAbstractText(null);
        }
    }

    @Given("that the Cristin Result has a CristinTag object with the values:")
    public void thatTheCristinResultHasACristinTagObjectWithTheValues(List<CristinTags> cristinTags) {
        scenarioContext.getCristinEntry().setTags(cristinTags);
    }

    @Then("the NVA Resource has the tags:")
    public void theNvaResourceHasTheTags(List<String> expectedTags) {
        List<String> actualTags = this.scenarioContext.getNvaEntry().getEntityDescription().getTags();
        assertThat(actualTags, is(containsInAnyOrder(expectedTags.toArray())));
    }

    @Given("that the Cristin Result has a ResearchProject set to null")
    public void thatTheCristinResultHasAResearchProjectSetToNull() {
        scenarioContext.getCristinEntry().setPresentationalWork(null);
    }

    @Then("the NVA Resource has no projects")
    public void theNvaResourceHasNoProjects() {
        List<ResearchProject> actuallProjects = scenarioContext.getNvaEntry().getProjects();
        assertThat(actuallProjects, is(empty()));
    }

    @Given("that the Cristin Result has PresentationalWork objects with the values:")
    public void thatTheCristinResultHasPresentationalWorkObjectsWithTheValues(
            List<CristinPresentationalWork> presentationalWorks) {
        scenarioContext.getCristinEntry().setPresentationalWork(presentationalWorks);
    }

    @Then("the NVA Resource has Research projects with the id values:")
    public void theNvaResourceHasResearchProjectsWithTheIdValues(List<String> stringUriList) {
        List<URI> expectedUriList = stringUriList.stream().map(URI::create).collect(Collectors.toList());
        List<URI> actualUriList = scenarioContext.getNvaEntry()
            .getProjects()
            .stream()
            .map(Project::getId)
            .collect(Collectors.toList());
        assertThat(actualUriList, is(equalTo(expectedUriList)));
    }

    @Then("no error is reported.")
    public void noErrorIsReported() {
        assertThat(this.scenarioContext.mappingIsSuccessful(), is(true));
    }

    @Given("the Cristin Result has an valid ISBN with the value {string}")
    public void theCristinResultHasAnValidIsbnWithTheValue(String isbn) {
        this.scenarioContext.getCristinEntry().getBookOrReportMetadata().setIsbn(isbn);
    }

    @When("the Cristin Result has the HRCS values:")
    public void theCristinResultHasTheHrcsValues(List<CristinHrcsCategoriesAndActivities> hrcsCategoriesAndActivities) {
        this.scenarioContext.getCristinEntry().setHrcsCategoriesAndActivities(hrcsCategoriesAndActivities);
    }

    @Then("the NVA Resource has the following subjects:")
    public void theNvaResourceHasTheFollowingSubjects(List<String> stringUriList) {
        List<URI> expectedUriList = stringUriList.stream().map(URI::create).collect(Collectors.toList());
        List<URI> actualSubjectList = this.scenarioContext.getNvaEntry().getSubjects();
        assertThat(actualSubjectList, is(equalTo(expectedUriList)));
    }

    @When("that the Cristin Result has no last modified value.")
    public void thatTheCristinResultHasNoLastModifiedValue() {
        this.scenarioContext.getCristinEntry().setEntryLastModifiedDate(null);
    }

    @When("the Cristin Result has the HRCS values {string} and {string}")
    public void theCristinResultHasTheHrcsValuesAnd(String category, String activity) {
        CristinHrcsCategoriesAndActivities hrcsCategoriesAndActivities = CristinHrcsCategoriesAndActivities.builder()
                .withCategory(category)
                .withActivity(activity)
                .build();
        this.scenarioContext.getCristinEntry().setHrcsCategoriesAndActivities(List.of(hrcsCategoriesAndActivities));
    }

    @Then("the NVA Resource Publishers id is {string}")
    public void theNvaResourcePublishersIdIs(String expectedPublisherId) {
        URI actuallPublisherId = this.scenarioContext
                .getNvaEntry()
                .getPublisher()
                .getId();
        assertThat(actuallPublisherId.toString(), is(equalTo(expectedPublisherId)));
    }
}
