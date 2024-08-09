package no.sikt.nva.scopus.conversion;

import static no.sikt.nva.scopus.conversion.ContributorExtractor.MISSING_CONTRIBUTORS_OF_NVA_CUSTOMERS_MESSAGE;
import static no.sikt.nva.scopus.conversion.ContributorExtractor.SCOPUS_AUID;
import static no.sikt.nva.scopus.utils.ScopusGenerator.randomPersonalnameType;
import static no.unit.nva.publication.testing.http.RandomPersonServiceResponse.randomUri;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import no.scopus.generated.AffiliationTp;
import no.scopus.generated.AuthorGroupTp;
import no.scopus.generated.AuthorTp;
import no.scopus.generated.CorrespondenceTp;
import no.scopus.generated.DocTp;
import no.sikt.nva.scopus.conversion.model.cristin.Affiliation;
import no.sikt.nva.scopus.conversion.model.cristin.CristinPerson;
import no.sikt.nva.scopus.conversion.model.cristin.SearchOrganizationResponse;
import no.sikt.nva.scopus.exception.MissingNvaContributorException;
import no.sikt.nva.scopus.utils.CristinGenerator;
import no.sikt.nva.scopus.utils.ScopusGenerator;
import no.unit.nva.expansion.model.cristin.CristinOrganization;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.Organization;
import nva.commons.core.SingletonCollector;
import nva.commons.core.StringUtils;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ContributorExtractorTest {

    public static final URI CRISTIN_ID = randomUri();
    public static final String ORCID_HOST_NAME = "orcid.org";
    public CristinConnection cristinConnection;
    private PiaConnection piaConnection;
    private NvaCustomerConnection nvaCustomerConnection;

    @BeforeEach
    void init() {
        piaConnection = mock(PiaConnection.class);
        cristinConnection = mock(CristinConnection.class);
        nvaCustomerConnection = mock(NvaCustomerConnection.class);
        when(nvaCustomerConnection.atLeastOneNvaCustomerPresent(any())).thenReturn(true);
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

        var id = ((Organization) contributor.getAffiliations().get(0)).getId();
        assertThat(id, is(equalTo(CRISTIN_ID)));
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

        var id = ((Organization) contributor.getAffiliations().get(0)).getId();
        assertThat(id, is(equalTo(organization.id())));
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

        var id = ((Organization) contributor.getAffiliations().get(0)).getId();
        assertThat(actualOrganizations.size(), is(equalTo(expectedAffiliations.size())));
        assertThat(id, is(equalTo(expectedAffiliations.get(0).getOrganization())));
    }

    @Test
    void shouldThrowMissingNvaContributorsExceptionWhenNoContributorsBelongingToNvaCustomer() {
        var document = ScopusGenerator.createWithNumberOfContributorsFromAuthorTp(1).getDocument();
        var orcId = getOrcidFromScopusDocument(document);
        mockCristinPersonByOrcIdResponse(orcId);
        mockRandomCristinOrgResponse();
        when(nvaCustomerConnection.atLeastOneNvaCustomerPresent(any())).thenReturn(false);

        assertThrows(MissingNvaContributorException.class,
                     () -> contributorExtractorFromDocument(document).generateContributors(),
                     MISSING_CONTRIBUTORS_OF_NVA_CUSTOMERS_MESSAGE);
    }

    @Test
    void shouldPreserveAuidWhenConvertingToNvaContributor() {
        var auid = randomString();
        var expectedAdditionalIdentifier = new AdditionalIdentifier(SCOPUS_AUID, auid);
        var document = ScopusGenerator.createWithOneAuthorGroupAndAffiliation(createAuthorWithAuid(auid)).getDocument();
        mockRandomCristinOrgResponse();
        var nvaContributor = contributorExtractorFromDocument(document).generateContributors().get(0);
        assertThat(nvaContributor.getIdentity().getAdditionalIdentifiers(), hasItem(expectedAdditionalIdentifier));
    }

    @Test
    void shouldPreserveAuidEvenWhenReplacingWithCristinContributor() {
        var auid = randomString();
        var expectedAdditionalIdentifier = new AdditionalIdentifier(SCOPUS_AUID, auid);
        var document = ScopusGenerator.createWithOneAuthorGroupAndAffiliation(createAuthorWithAuid(auid)).getDocument();
        mockCristinPersonWithoutAffiliationResponse();
        mockRandomCristinOrgResponse();
        var nvaContributor = contributorExtractorFromDocument(document).generateContributors().get(0);
        assertThat(nvaContributor.getIdentity().getAdditionalIdentifiers(), hasItem(expectedAdditionalIdentifier));
    }

    @Test
    void shouldCreateContributorFromAuthorTypeWhenBadResponseFromCristinAndNoOrcId() {
        var authorTp = new AuthorTp();
        authorTp.setAuid(randomString());
        authorTp.setSeq(String.valueOf(1));
        var personalnameType = randomPersonalnameType();
        authorTp.setPreferredName(personalnameType);
        authorTp.setIndexedName(personalnameType.getIndexedName());
        authorTp.setGivenName(personalnameType.getGivenName());
        authorTp.setSurname(personalnameType.getSurname());
        var document = ScopusGenerator.createWithSingleContributorFromAuthorTp(authorTp).getDocument();
        mockCristinPersonBadRequestResponse();
        var contributor = contributorExtractorFromDocument(document).generateContributors()
                              .stream()
                              .collect(SingletonCollector.collect());

        assertThat(contributor.getIdentity().getId(), is(nullValue()));
    }

    @Test
    void shouldExtractOrcIdFromXmlWhenCristinPersonIsPresentButIsMissingOrcId() {
        var authorTp = new AuthorTp();
        authorTp.setAuid(randomString());
        authorTp.setSeq(String.valueOf(1));
        var personalnameType = randomPersonalnameType();
        authorTp.setPreferredName(personalnameType);
        authorTp.setIndexedName(personalnameType.getIndexedName());
        authorTp.setGivenName(personalnameType.getGivenName());
        authorTp.setSurname(personalnameType.getSurname());
        var orcIdFromXml = randomString();
        authorTp.setOrcid(orcIdFromXml);
        var document = ScopusGenerator.createWithSingleContributorFromAuthorTp(authorTp).getDocument();
        mockCristinPersonWithoutOrcId();
        var contributor = contributorExtractorFromDocument(document).generateContributors()
                              .stream()
                              .collect(SingletonCollector.collect());
        var expectedOrcId = UriWrapper.fromHost(ORCID_HOST_NAME).addChild(orcIdFromXml).toString();

        assertThat(contributor.getIdentity().getOrcId(), is(equalTo(expectedOrcId)));
    }

    @Test
    void shouldSetOrcIdToNullWhenMissingBothInXmlAndCristinPerson() {
        var authorTp = new AuthorTp();
        authorTp.setAuid(randomString());
        authorTp.setSeq(String.valueOf(1));
        var personalnameType = randomPersonalnameType();
        authorTp.setPreferredName(personalnameType);
        authorTp.setIndexedName(personalnameType.getIndexedName());
        authorTp.setGivenName(personalnameType.getGivenName());
        authorTp.setSurname(personalnameType.getSurname());
        var document = ScopusGenerator.createWithSingleContributorFromAuthorTp(authorTp).getDocument();
        mockCristinPersonWithoutOrcId();
        var contributor = contributorExtractorFromDocument(document).generateContributors()
                              .stream()
                              .collect(SingletonCollector.collect());

        assertThat(contributor.getIdentity().getOrcId(), is(nullValue()));
    }

    private AuthorGroupTp createAuthorWithAuid(String auid) {
        var authorTp = new AuthorTp();
        authorTp.setAuid(auid);
        authorTp.setSurname(randomString());
        authorTp.setGivenName(randomString());
        authorTp.setIndexedName(randomString());
        authorTp.setSeq("1");
        var affiliationTp = new AffiliationTp();
        affiliationTp.setAfid(randomString());
        affiliationTp.setCountry("NO");
        var authorGp = new AuthorGroupTp();
        authorGp.setAffiliation(affiliationTp);
        authorGp.getAuthorOrCollaboration().add(authorTp);
        return authorGp;
    }

    private static String getOrcidFromScopusDocument(DocTp document) {
        return ((AuthorTp) document.getItem()
                               .getItem()
                               .getBibrecord()
                               .getHead()
                               .getAuthorGroup()
                               .getFirst()
                               .getAuthorOrCollaboration()
                               .getFirst()).getOrcid();
    }

    private static List<AuthorGroupTp> getAuthorGroup(DocTp document) {
        return document.getItem().getItem().getBibrecord().getHead().getAuthorGroup();
    }

    private static List<CorrespondenceTp> getCorrespondence(DocTp document) {
        return document.getItem().getItem().getBibrecord().getHead().getCorrespondence();
    }

    private CristinPerson mockCristinPersonWithSingleActiveAffiliationResponse() {
        var personCristinId = randomUri();
        when(piaConnection.getCristinPersonIdentifier(any())).thenReturn(Optional.of(personCristinId));
        var person = CristinGenerator.generateCristinPersonWithSingleActiveAffiliation(personCristinId, randomString(),
                                                                                       randomString());
        when(cristinConnection.getCristinPersonByCristinId(personCristinId)).thenReturn(Optional.of(person));
        return person;
    }

    private CristinPerson mockCristinPersonWithoutOrcId() {
        var personCristinId = randomUri();
        when(piaConnection.getCristinPersonIdentifier(any())).thenReturn(Optional.of(personCristinId));
        var person = CristinGenerator.generateCristinPersonWithoutOrcId(
            personCristinId, randomString(), randomString());
        when(cristinConnection.getCristinPersonByCristinId(personCristinId)).thenReturn(Optional.of(person));
        return person;
    }

    private CristinOrganization mockSearchOrganizationResponse() {
        var organization = new CristinOrganization(randomUri(), randomUri(), randomString(),
                                                   List.of(), randomString(), Map.of(randomString(), randomString()));
        when(cristinConnection.searchCristinOrganization(anyString())).thenReturn(
            Optional.of(new SearchOrganizationResponse(List.of(organization), 1)));
        return organization;
    }

    private String mockCristinPersonByOrcIdResponse(String orcId) {
        var firstname = randomString();
        var surname = randomString();
        when(cristinConnection.getCristinPersonByOrcId(eq(orcId))).thenReturn(
            Optional.of(CristinGenerator.generateCristinPerson(randomUri(), firstname, surname)));
        return firstname + StringUtils.SPACE + surname;
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
                                        cristinConnection, nvaCustomerConnection);
    }
}
