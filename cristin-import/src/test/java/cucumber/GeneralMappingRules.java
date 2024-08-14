package cucumber;

import static cucumber.utils.transformers.CristinContributorAffiliationTransformer.parseContributorAffiliationsFromMap;
import static cucumber.utils.transformers.CristinSourceTransformer.parseCristinSourceFromMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
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
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.net.URI;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.cristin.CristinDataGenerator;
import no.unit.nva.cristin.mapper.CristinAssociatedUri;
import no.unit.nva.cristin.mapper.CristinContributor;
import no.unit.nva.cristin.mapper.CristinContributor.CristinContributorBuilder;
import no.unit.nva.cristin.mapper.CristinContributorRole;
import no.unit.nva.cristin.mapper.CristinContributorRoleCode;
import no.unit.nva.cristin.mapper.CristinContributorsAffiliation;
import no.unit.nva.cristin.mapper.CristinGrant;
import no.unit.nva.cristin.mapper.CristinHrcsCategoriesAndActivities;
import no.unit.nva.cristin.mapper.CristinLocale;
import no.unit.nva.cristin.mapper.CristinPresentationalWork;
import no.unit.nva.cristin.mapper.CristinTags;
import no.unit.nva.cristin.mapper.CristinTitle;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.AdditionalIdentifierBase;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.CristinIdentifier;
import no.unit.nva.model.Identity;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.PublicationNote;
import no.unit.nva.model.ResearchProject;
import no.unit.nva.model.SourceName;
import no.unit.nva.model.Username;
import no.unit.nva.model.funding.ConfirmedFunding;
import no.unit.nva.model.funding.Funding;
import no.unit.nva.model.role.RoleType;
import nva.commons.core.SingletonCollector;
import nva.commons.core.paths.UriWrapper;

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

    @Given("a cristin result with a single contributor that is not verified")
    public void cristinResultWithASingleContributorThatIsNotVerified() {
        var randomUnverifiedContributor = CristinDataGenerator.randomUnverifiedContributor(1);
        scenarioContext.getCristinEntry().setContributors(List.of(randomUnverifiedContributor));
    }

    @Then("the NVA contributor does not have an id")
    public void theNvaContributorDoesNotHaveAnId() {
        var contributor = getFirstContributor();
        assertThat(contributor.getIdentity().getId(), is(nullValue()));
    }

    @Given("a cristin result with a single contributor that is verified and has a cristin-id equal to {int}")
    public void cristinResultWithASingleContributorThatIsVerifiedAndHasACristinIdEqualTo(int cristinIdentifier) {
        var contributor = CristinDataGenerator.randomContributor(1);
        contributor.setIdentifier(cristinIdentifier);
        scenarioContext.getCristinEntry().setContributors(List.of(contributor));
    }

    @Then("the NVA contributor has an id equal to {string}")
    public void theNvaContributorHasAnIdEqualTo(String expectedId) {
        var contributor = getFirstContributor();
        assertThat(contributor.getIdentity().getId(),
                   is(UriWrapper.fromUri(expectedId).getUri()));
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

    @When("the Cristin Result is converted to an NVA Resource")
    public void is_converted_to_an_nva_entry() {
        scenarioContext.convertToNvaEntry();
    }

    @Then("the NVA Resource has an additional identifier with key {string} and value {int}")
    public void theNvaEntryHasAnAdditionalIdentifierWithKeyAndValue(String cristinAdditionalIdentifierKey,
                                                                    int expectedCristinId) {
        Set<AdditionalIdentifierBase> actualAdditionalIdentifiers =
            scenarioContext.getNvaEntry().getAdditionalIdentifiers();
        CristinIdentifier expectedIdentifier =
            new CristinIdentifier(SourceName.fromCristin(cristinAdditionalIdentifierKey.split("@")[1]),
                                  Integer.toString(expectedCristinId));

        assertThat(actualAdditionalIdentifiers, hasItem(expectedIdentifier));
    }

    @Then("the NVA Resource has an additional identifier with key {string} and value {string}")
    public void theNvaEntryHasAnAdditionalIdentifierWithKeyAndValue(String cristinAdditionalIdentifierKey,
                                                                    String expectedCristinId) {
        Set<AdditionalIdentifierBase> actualAdditionalIdentifiers =
            scenarioContext.getNvaEntry().getAdditionalIdentifiers();
        AdditionalIdentifier expectedIdentifier =
            new AdditionalIdentifier(cristinAdditionalIdentifierKey, expectedCristinId);

        assertThat(actualAdditionalIdentifiers, hasItem(expectedIdentifier));
    }

    @Then("the NVA Resource has an EntityDescription with mainTitle {string}")
    public void theNvaResourceHasAnEntityDescriptionWithMainTitle(String expectedTitle) {
        String actualTitle = scenarioContext.getNvaEntry().getEntityDescription().getMainTitle();
        assertThat(actualTitle, is(equalTo(expectedTitle)));
    }

    @Then("the NVA Resource has a Publication Date with year equal to {string}, month equal to {string} and "
          + "day equal to {string}")
    public void theNvaResourceHasPublicationDateWithTheCristinYear(String year,
                                                                   String month,
                                                                   String day) {
        PublicationDate actualDate = scenarioContext.getNvaEntry().getEntityDescription().getPublicationDate();
        assertThat(actualDate.getYear(), is(equalTo(year)));
        assertThat(actualDate.getMonth(), is(equalTo("null".equals(month) ? null : month)));
        assertThat(actualDate.getDay(), is("null".equals(day) ? null : day));
    }

    @Given("the Cristin Result has an array of CristinTitles with values:")
    public void theCristinResultHasAnCristinTitlesWithValues(List<CristinTitle> cristinTitles) {
        scenarioContext.getCristinEntry().setCristinTitles(cristinTitles);
    }

    @Given("the Cristin Result has a non null array of CristinSources with values:")
    public void theCristinResultHasCristinSourcesWithValues(DataTable dataTable) {
        var cristinSources = parseCristinSourceFromMap(dataTable);
        scenarioContext.getCristinEntry().setCristinSources(cristinSources);
    }

    @Given("that the Cristin Result has Contributors with names:")
    public void thatCristinResultsHasTheContributors(DataTable nameTable) {
        List<CristinContributor> contributors = nameTable.asMaps()
                                                    .stream()
                                                    .map(CristinContributorTransformer::toContributor)
                                                    .map(CristinContributorBuilder::build)
                                                    .toList();

        scenarioContext.getCristinEntry().setContributors(contributors);
    }

    @Given("that the Cristin Result has the Contributor(s) with name(s) and sequence:")
    public void thatTheCristinResultHasTheContributorsWithNamesAndSequence(DataTable dataTable) {
        List<CristinContributor> contributors = dataTable.asMaps().stream()
                                                    .map(CristinContributorTransformer::toContributorWithOrdinalNumber)
                                                    .map(CristinContributorBuilder::build)
                                                    .toList();
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
                                                  .toList();

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
                .toList();

        List<ContributorFlattenedDetails> expectedContributors =
            table.asMaps()
                .stream()
                .map(ContributorFlattenedDetails::fromDataTableMapEntry)
                .toList();

        assertThat(actualContributors, containsInAnyOrder(expectedContributors.toArray(
            ContributorFlattenedDetails[]::new)));
    }

    @Then("the NVA Resource Contributors have the following names, sequences and affiliation URIs")
    public void theNvaResourceContributorsHaveTheFollowingNamesSequencesAndAffiliationURIs(DataTable dataTable) {
        List<Contributor> contributors = this.scenarioContext.getNvaEntry().getEntityDescription().getContributors();

        List<ContributorFlattenedDetails> actualDetails =
            contributors.stream()
                .map(ContributorFlattenedDetails::extractNameSequenceAndAffiliationUri)
                .toList();
        ContributorFlattenedDetails[] expectedDetails =
            dataTable.asMaps()
                .stream()
                .map(ContributorFlattenedDetails::fromDataTableMapEntry)
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
                                .map(RoleType::getType)
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
        assertThat(actualLanguage, is(equalTo(expectedLanguageUri)));
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
        List<URI> expectedUriList = stringUriList.stream().map(URI::create).toList();
        List<URI> actualUriList = scenarioContext.getNvaEntry()
                                      .getProjects()
                                      .stream()
                                      .map(ResearchProject::getId)
                                      .toList();
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

    @And("the NVA Resource does not have an additional identifier with key {string} and value "
         + "{string}")
    public void theNvaResourceDoesNotHaveAnAdditionalIdentifierWithKeyAndValue(String key, String value) {
        Set<AdditionalIdentifierBase> actualAdditionalIdentifiers =
            scenarioContext.getNvaEntry().getAdditionalIdentifiers();
        AdditionalIdentifier expectedIdentifier =
            new AdditionalIdentifier(key, value);
        assertThat(actualAdditionalIdentifiers, not(hasItem(expectedIdentifier)));
    }

    @And("the Cristin Result has sourceCode equal to {string}")
    public void theCristinResultHasSourceCodeEqualTo(String sourceCode) {
        scenarioContext.getCristinEntry().setSourceCode(sourceCode);
    }

    @And("the Cristin Result has sourceRecordIdentifier equal to {string}")
    public void theCristinResultHasSourceRecordIdentifierEqualTo(String sourceRecordIdentifier) {
        scenarioContext.getCristinEntry().setSourceRecordIdentifier(sourceRecordIdentifier);
    }

    @Then("the publication should have a Confirmed Nva funding with identifier equal to {string} and id equal to "
          + "{string}")
    public void thePublicationShouldHaveAConfirmedNvaFundingWithIdentifierEqualToAndIdEqualTo(String identifier,
                                                                                              String id) {
        var nvaFundings = scenarioContext.getNvaEntry().getFundings();
        assertThat(nvaFundings, hasSize(1));
        assertThat(nvaFundings.get(0), allOf(is(instanceOf(ConfirmedFunding.class)),
                                             hasProperty("identifier", equalTo(identifier)),
                                             hasProperty("id", equalTo(UriWrapper.fromUri(id).getUri()))));
    }

    @Given("that Cristin Result has grants:")
    public void thatCristinResultHasGrants(List<CristinGrant> grants) {
        scenarioContext.getCristinEntry().setCristinGrants(grants);
    }

    @Then("publication should have a nva Fundings:")
    public void publicationShouldHaveANvaFundings(List<Funding> fundings) {
        var nvaFundings = scenarioContext.getNvaEntry().getFundings();
        assertThat(nvaFundings, containsInAnyOrder(fundings.toArray()));
    }

    @And("that the Cristin Result has published date equal to the local date {string}")
    public void thatTheCristinResultHasPublishedDateEqualToTheLocalDate(String publishedDate) {
        LocalDate localDate = LocalDate.parse(publishedDate);
        scenarioContext.getCristinEntry().setEntryPublishedDate(localDate);
    }

    @And("that the cristin Result has published date equal to null")
    public void thatTheCristinResultHasPublishedDateEqualToNull() {
        scenarioContext.getCristinEntry().setEntryPublishedDate(null);
    }

    @And("that the Cristin Result has a year set to {string}")
    public void thatTheCristinResultHasAYearSetTo(String year) {
        scenarioContext.getCristinEntry().setPublicationYear(Integer.parseInt(year));
    }

    @Given("that Cristin Result has eierkode_opprett {string}")
    public void thatCristinResultHasEierkodeOpprett(String eierkodeOpprettet) {
        scenarioContext.getCristinEntry().setOwnerCodeCreated(eierkodeOpprettet);
    }

    @And("the Cristin Result has vitenskapeligarbeid_lokal:")
    public void theCristinResultHasVitenskapeligarbeidLokal(List<CristinLocale> cristinLocales) {
        scenarioContext.getCristinEntry().setCristinLocales(cristinLocales);
    }

    @Then("the NVA Resource should have a owner {string} and ownerAffiliation: {string}")
    public void theNvaResourceShouldHaveAOwnerAndOwnerAffiliation(String owner, String ownerAffiliation) {
        var resourceOwner = scenarioContext.getNvaEntry().getResourceOwner();
        assertThat(resourceOwner, allOf(hasProperty("owner", equalTo(new Username(owner))),
                                        hasProperty("ownerAffiliation",
                                                    equalTo(UriWrapper.fromUri(ownerAffiliation).getUri()))));
    }

    @And("the cristin has institusjonsnr_opprettet equal to {string}, and avdnr, undavdnr and gruppenr equal to "
         + "{string}")
    public void theCristinHasInstitusjonsnrOpprettetEqualToAndAvdnrUndavdnrAndGruppenrEqualTo(
        String institutionIdentifierCreated,
        String partsIdentifier) {
        scenarioContext.getCristinEntry().setInstitutionIdentifierCreated(institutionIdentifierCreated);
        scenarioContext.getCristinEntry().setDepartmentIdentifierCreated(partsIdentifier);
        scenarioContext.getCristinEntry().setSubDepartmendIdentifierCreated(partsIdentifier);
        scenarioContext.getCristinEntry().setGroupIdentifierCreated(partsIdentifier);
    }

    @And("the cristin result has a note equal to {string}")
    public void theCristinResultHasANoteEqualTo(String note) {
        scenarioContext.getCristinEntry().setNote(note);
    }

    @Then("the NVA resource has a notes field equal to {string}")
    public void theNvaResourceHasANotesFieldEqualTo(String expectedNote) {
        var actualNotes = scenarioContext.getNvaEntry().getPublicationNotes();
        assertThat(actualNotes, hasSize(1));
        var publicationNote = (PublicationNote)actualNotes.getFirst();
        assertThat(publicationNote.getNote(), equalTo(expectedNote));
    }

    @Then("the NVA resource has a empty list as publicationNotes")
    public void theNvaResourceHasAEmptyListAsPublicationNotes() {
        var actualNotes = scenarioContext.getNvaEntry().getPublicationNotes();
        assertThat(actualNotes, hasSize(0));
    }

    @And("the contributor has a role {string} at the unknown affiliation")
    public void theContributorHasARoleAtTheUnknownAffiliation(String role) {
        var cristinRole = CristinContributorRole
                              .builder()
                              .withRoleCode(CristinContributorRoleCode.fromString(role))
                              .build();
        var contributors = scenarioContext.getCristinEntry().getContributors();
        assertThat(contributors, hasSize(1));
        var affiliations = contributors.get(0).getAffiliations();
        assertThat(affiliations, hasSize(1));
        affiliations.get(0).setRoles(List.of(cristinRole));
    }

    @Given("that Cristin Result has a grant with properties finansieringsreferanse {string} and sourceCode {string}:")
    public void thatCristinResultHasAGrantWithPropertiesFinansieringsreferanseAndSourceCode(String fundingReference,
                                                                                            String sourceCode) {
        scenarioContext.getCristinEntry()
            .setCristinGrants(
                List.of(CristinGrant.builder()
                            .withGrantReference(fundingReference)
                            .withSourceCode(sourceCode)
                            .build()));
    }

    @Given("the Cristin Result has the following varbeid_url present:")
    public void theCristinResultHasTheFollowingVarbeidUrlPresent(List<CristinAssociatedUri> cristinAssociatedUrls) {
        scenarioContext.getCristinEntry().setCristinAssociatedUris(cristinAssociatedUrls);
    }

    @Then("the NVA Resource should have the archive handle set to {string}")
    public void theNvaResourceShouldHaveTheArchiveHandleSet(String expectedHandleString) {
        var publication = scenarioContext.getNvaEntry();
        var actualHandle = publication.getHandle();
        var expectedHandle = UriWrapper.fromUri(expectedHandleString).getUri();
        assertThat(actualHandle, is(equalTo(expectedHandle)));
    }

    @Then("the NVA Resource should have the handle set to null")
    public void theNvaResourceShouldHaveTheHandleSetToNull() {
        var actualHandle = scenarioContext.getNvaEntry().getHandle();
        assertThat(actualHandle, is(nullValue()));
    }

    @Then("the NVA contributor has no affiliation")
    public void theNvaContributorHasNoAffiliation() {
        var contributors = scenarioContext.getNvaEntry().getEntityDescription().getContributors();
        assertThat(contributors, hasSize(1));
        var contributor = contributors.get(0);
        assertThat(contributor.getAffiliations(), hasSize(0));
    }

    @And("the contributor is missing affiliation")
    public void theContributorIsMissingAffiliation() {
        var contributors = scenarioContext.getCristinEntry().getContributors();
        assertThat(contributors, hasSize(1));
        contributors.get(0).setAffiliations(List.of());
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

    private Contributor getFirstContributor() {
        var contributors = scenarioContext.getNvaEntry().getEntityDescription().getContributors();
        assertThat(contributors, hasSize(1));
        return contributors.get(0);
    }
}
