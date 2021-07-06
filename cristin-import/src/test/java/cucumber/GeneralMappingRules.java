package cucumber;

import static cucumber.CristinContributorAffiliationTransformer.parseContributorAffiliationsFromMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.nullValue;
import cucumber.utils.ContributorFlattenedDetails;
import cucumber.utils.exceptions.MisformattedScenarioException;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.cristin.mapper.CristinContributor;
import no.unit.nva.cristin.mapper.CristinContributor.CristinContributorBuilder;
import no.unit.nva.cristin.mapper.CristinContributorsAffiliation;
import no.unit.nva.cristin.mapper.CristinTitle;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.Identity;
import no.unit.nva.model.PublicationDate;

public class GeneralMappingRules {

    public static final int SKIP_HEADERS = 1;
    public static final String ERROR_MESSAGE_FOR_MISMATCH_BETWEEN_ROLES_AND_AFFILIATIONS = "The number of contributor"
                                                                                           + " and the number of "
                                                                                           + "affiliations do not "
                                                                                           + "match";
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

    @Given("the Cristin Result has publication year {string}")
    public void theCristinEntryHasPublicationYear(String publicationYear) {
        scenarioContext.getCristinEntry().setPublicationYear(publicationYear);
    }

    @Given("that Cristin Result has created date equal to the local date {string}")
    public void thatCristinEntryHasCreatedDateEqualToTheLocalDate(String dateString) {
        LocalDate localDate = LocalDate.parse(dateString);
        scenarioContext.getCristinEntry().setEntryCreationDate(localDate);
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

    @Then("the NVA Resource has a Publication Date with year equal to {string}, month equal to null and "
          + "day equal to null")
    public void theNvaResourceHasPublicationDateWithTheCristinYear(String expectedPublicationYear) {
        PublicationDate actualDate = scenarioContext.getNvaEntry().getEntityDescription().getDate();
        assertThat(actualDate.getYear(), is(equalTo(expectedPublicationYear)));
        assertThat(actualDate.getMonth(), is(nullValue()));
        assertThat(actualDate.getDay(), is(nullValue()));
    }

    @Then("the NVA Resource has a Creation Date equal to {string}")
    public void theNvaResourceHasACreationDateEqualTo(String expectedIsoInstant) {
        Instant expectedInstant = Instant.parse(expectedIsoInstant);
        assertThat(scenarioContext.getNvaEntry().getCreatedDate(), is(equalTo(expectedInstant)));
    }

    @Given("the Cristin Result has an  CristinTitles with values:")
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

    @Given("that the Cristin Result has the Contributors with names and sequence:")
    public void thatTheCristinResultHasTheContributorsWithNamesAndSequence(DataTable dataTable) {
        List<CristinContributor> contributors = dataTable.asMaps().stream()
                                                    .map(CristinContributorTransformer::toContributorWithOrdinalNumber)
                                                    .map(CristinContributorBuilder::build)
                                                    .collect(Collectors.toList());
        scenarioContext.getCristinEntry().setContributors(contributors);
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
                .map(ContributorFlattenedDetails::from)
                .collect(Collectors.toList());

        assertThat(actualContributors, containsInAnyOrder(expectedContributors.toArray(
            ContributorFlattenedDetails[]::new)));
    }

    @Given("the Contributors are affiliated with the following Cristin Institution respectively:")
    public void theContributorsAreAffiliatedWithTheFollowingCristinInstitutionRespectively(DataTable dataTable) {
        List<CristinContributorsAffiliation> desiredInjectedAffiliations =
            parseContributorAffiliationsFromMap(dataTable);
        List<CristinContributor> contributors = this.scenarioContext.getCristinEntry().getContributors();

        ensureTheContributorsAreAsManyAsTheInjectedAffiliations(desiredInjectedAffiliations, contributors);

        injectAffiliationsIntoContributors(desiredInjectedAffiliations, contributors);
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
                .map(ContributorFlattenedDetails::from)
                .collect(Collectors.toList())
                .toArray(ContributorFlattenedDetails[]::new);

        assertThat(actualDetails, containsInAnyOrder(expectedDetails));
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
}
