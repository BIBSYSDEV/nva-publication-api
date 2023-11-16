package no.sikt.nva.scopus.conversion;

import static no.unit.nva.publication.testing.http.RandomPersonServiceResponse.randomUri;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import no.scopus.generated.AuthorGroupTp;
import no.scopus.generated.AuthorTp;
import no.scopus.generated.CorrespondenceTp;
import no.scopus.generated.DocTp;
import no.sikt.nva.scopus.conversion.model.cristin.Affiliation;
import no.sikt.nva.scopus.conversion.model.cristin.CristinPerson;
import no.sikt.nva.scopus.conversion.model.cristin.CristinOrganization;
import no.sikt.nva.scopus.conversion.model.cristin.SearchOrganizationResponse;
import no.sikt.nva.scopus.utils.CristinGenerator;
import no.sikt.nva.scopus.utils.ScopusGenerator;
import nva.commons.core.SingletonCollector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ContributorExtractorTest {

    public static final URI CRISTIN_ID = randomUri();
    public CristinConnection cristinConnection;
    private PiaConnection piaConnection;

    @BeforeEach
    void init() {
        piaConnection = mock(PiaConnection.class);
        cristinConnection = mock(CristinConnection.class);
    }

    @Test
    void shouldCreateContributorWithoutAffiliationWhenAuthorTpIsNorwegianContributorWithoutAffiliation() {
        var generator = ScopusGenerator.createWithNumberOfContributorsFromAuthorTp(1);
        mockCristinPersonWithoutAffiliationResponse();
        var contributor = contributorExtractorFromDocument(generator.getDocument()).generateContributors()
                              .stream()
                              .collect(SingletonCollector.collect());

        assertThat(contributor.getAffiliations(), is(emptyIterable()));
    }

    @Test
    void shouldCreateContributorWithoutAffiliationWhenAuthorTpIsNorwegianContributorWithOtherNorwegianInstitution() {
        var generator = ScopusGenerator.createWithNumberOfContributorsFromAuthorTp(1);
        mockCristinPersonWithoutAffiliationResponse();
        mockOtherCristinOrgResponseWithOtherOrganization();
        var contributor = contributorExtractorFromDocument(generator.getDocument()).generateContributors()
                              .stream()
                              .collect(SingletonCollector.collect());

        assertThat(contributor.getAffiliations(), is(emptyIterable()));
    }

    @Test
    void shouldCreateContributorWithAffiliationWhenAuthorTpIsNorwegianContributorWithNorwegianInstitution() {
        var generator = ScopusGenerator.createWithNumberOfContributorsFromAuthorTp(1);
        mockCristinPersonWithoutAffiliationResponse();
        mockCristinOrgResponseWithNorwegianInstitution();
        var contributor = contributorExtractorFromDocument(generator.getDocument()).generateContributors()
                              .stream()
                              .collect(SingletonCollector.collect());

        assertThat(contributor.getAffiliations(), hasSize(1));
    }

    @Test
    void shouldCreateContributorWithoutAffiliationWhenAuthorTpIsNorwegianContributorWithoutAffiliationFromCristin() {
        var document = new ScopusGenerator().createWithNumberOfContributorsFromCollaborationTp(1);
        mockCristinPersonBadRequestResponse();
        var contributor = contributorExtractorFromDocument(document).generateContributors()
                              .stream()
                              .collect(SingletonCollector.collect());

        assertThat(contributor.getAffiliations(), is(emptyIterable()));
    }

    @Test
    void shouldCreateContributorWithAffiliationFromCristinWhenCreatingContributorFromCollaborationTp() {
        var document = new ScopusGenerator().createWithNumberOfContributorsFromCollaborationTp(1);
        mockRandomCristinOrgResponse();

        var contributor = contributorExtractorFromDocument(document).generateContributors()
                              .stream()
                              .collect(SingletonCollector.collect());

        assertThat(contributor.getAffiliations().get(0).getId(), is(equalTo(CRISTIN_ID)));
    }

    @Test
    void shouldCreateContributorByFetchingCristinPersonByOrcIdWhenNoResponseFromPia() {
        var document = ScopusGenerator.createWithNumberOfContributorsFromAuthorTp(1).getDocument();
        var orcId = getOrcidFromScopusDocument(document);
        var expectedContributorName = mockCristinPersonByOrcIdResponse(orcId);
        mockRandomCristinOrgResponse();

        var contributor = contributorExtractorFromDocument(document).generateContributors()
                              .stream()
                              .collect(SingletonCollector.collect());

        assertThat(contributor.getIdentity().getName(), is(equalTo(expectedContributorName)));
    }

    @Test
    void shouldCreateContributorAffiliationSearchingOrganizationNameWhenNoResponseFromPiaAndWhenCreatingFromAuthorTp() {
        var document = ScopusGenerator.createWithNumberOfContributorsFromAuthorTp(1).getDocument();
        var organization = mockSearchOrganizationResponse();
        var contributor = contributorExtractorFromDocument(document).generateContributors()
                              .stream()
                              .collect(SingletonCollector.collect());

        assertThat(contributor.getAffiliations().get(0).getId(), is(equalTo(organization.getId())));
    }

    @Test
    void shouldUseActiveAffiliationOnlyWhenCreatingOrganizationsForContributorWhenFetchingCristinPerson() {
        var document = ScopusGenerator.createWithNumberOfContributorsFromAuthorTp(1).getDocument();
        var cristinPerson = mockCristinPersonWithSingleActiveAffiliationResponse();
        var contributor = contributorExtractorFromDocument(document).generateContributors()
                              .stream()
                              .collect(SingletonCollector.collect());

        var expectedAffiliations = cristinPerson.getAffiliations().stream().filter(Affiliation::isActive).toList();
        var actualOrganizations = contributor.getAffiliations();

        assertThat(actualOrganizations.size(), is(equalTo(expectedAffiliations.size())));
        assertThat(actualOrganizations.get(0).getId(), is(equalTo(expectedAffiliations.get(0).getOrganization())));
    }

    private CristinPerson mockCristinPersonWithSingleActiveAffiliationResponse() {
        var personCristinId = randomUri();
        when(piaConnection.getCristinPersonIdentifier(any())).thenReturn(Optional.of(personCristinId));
        var person = CristinGenerator.generateCristinPersonWithSingleActiveAffiliation(personCristinId,
                                                                                                randomString(),
                                                                                                randomString());
        when(cristinConnection.getCristinPersonByCristinId(personCristinId)).thenReturn(Optional.of(
            person));
        return person;
    }

    private static String getOrcidFromScopusDocument(DocTp document) {
        return ((AuthorTp) document.getItem()
                               .getItem()
                               .getBibrecord()
                               .getHead()
                               .getAuthorGroup()
                               .get(0)
                               .getAuthorOrCollaboration()
                               .get(0)).getOrcid();
    }

    private static List<AuthorGroupTp> getAuthorGroup(DocTp document) {
        return document.getItem().getItem().getBibrecord().getHead().getAuthorGroup();
    }

    private static List<CorrespondenceTp> getCorrespondence(DocTp document) {
        return document.getItem().getItem().getBibrecord().getHead().getCorrespondence();
    }

    private CristinOrganization mockSearchOrganizationResponse() {
        var organization = new CristinOrganization(randomUri(), Map.of(randomString(), randomString()), randomString());
        when(cristinConnection.searchCristinOrganization(anyString())).thenReturn(Optional.of(
            new SearchOrganizationResponse(List.of(organization), 1)));
        return organization;
    }

    private String mockCristinPersonByOrcIdResponse(String orcId) {
        var firstname = randomString();
        var surname = randomString();
        when(cristinConnection.getCristinPersonByOrcId(eq(orcId))).thenReturn(
            Optional.of(CristinGenerator.generateCristinPerson(randomUri(), firstname, surname)));
        return firstname + ", " + surname;
    }

    private void mockRandomCristinOrgResponse() {
        when(piaConnection.fetchCristinOrganizationIdentifier(any())).thenReturn(Optional.of(randomUri()));
        when(cristinConnection.fetchCristinOrganizationByCristinId(any())).thenReturn(
            CristinGenerator.generateCristinOrganization(CRISTIN_ID));
    }

    private void mockOtherCristinOrgResponseWithOtherOrganization() {
        var cristinOrgId = randomUri();
        when(piaConnection.fetchCristinOrganizationIdentifier(any())).thenReturn(Optional.of(cristinOrgId));
        when(cristinConnection.fetchCristinOrganizationByCristinId(cristinOrgId)).thenReturn(
            CristinGenerator.generateOtherCristinOrganization(cristinOrgId));
    }

    private void mockCristinPersonWithoutAffiliationResponse() {
        var personCristinId = randomUri();
        when(piaConnection.getCristinPersonIdentifier(any())).thenReturn(Optional.of(personCristinId));
        when(cristinConnection.getCristinPersonByCristinId(personCristinId)).thenReturn(Optional.of(
            CristinGenerator.generateCristinPersonWithoutAffiliations(personCristinId, randomString(),
                                                                      randomString())));
    }

    private void mockCristinOrgResponseWithNorwegianInstitution() {
        var cristinOrgId = randomUri();
        when(piaConnection.fetchCristinOrganizationIdentifier(any())).thenReturn(Optional.of(cristinOrgId));
        when(cristinConnection.fetchCristinOrganizationByCristinId(any())).thenReturn(
            CristinGenerator.generateCristinOrganizationWithCountry("NO"));
    }

    private void mockCristinPersonBadRequestResponse() {
        var cristinOrgId = randomUri();
        when(piaConnection.fetchCristinOrganizationIdentifier(any())).thenReturn(Optional.of(cristinOrgId));
        when(cristinConnection.fetchCristinOrganizationByCristinId(cristinOrgId)).thenReturn(null);
    }

    private ContributorExtractor contributorExtractorFromDocument(DocTp document) {
        return new ContributorExtractor(getCorrespondence(document), getAuthorGroup(document), piaConnection,
                                        cristinConnection);
    }
}
