package no.sikt.nva.scopus.conversion;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_OK;
import static no.sikt.nva.scopus.conversion.ContributorExtractor.SCOPUS_AUID;
import static no.sikt.nva.scopus.utils.CristinGenerator.generateCristinOrganizationWithCountry;
import static no.sikt.nva.scopus.utils.ScopusGenerator.randomPersonalnameType;
import static no.sikt.nva.scopus.utils.ScopusTestUtils.randomCustomer;
import static no.unit.nva.publication.testing.http.RandomPersonServiceResponse.randomUri;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import no.scopus.generated.AffiliationTp;
import no.scopus.generated.AuthorGroupTp;
import no.scopus.generated.AuthorTp;
import no.scopus.generated.CollaborationTp;
import no.scopus.generated.CorrespondenceTp;
import no.scopus.generated.DocTp;
import no.sikt.nva.scopus.conversion.model.cristin.Affiliation;
import no.sikt.nva.scopus.conversion.model.cristin.CristinPerson;
import no.sikt.nva.scopus.conversion.model.cristin.SearchOrganizationResponse;
import no.sikt.nva.scopus.conversion.model.cristin.TypedValue;
import no.sikt.nva.scopus.utils.CristinGenerator;
import no.sikt.nva.scopus.utils.PiaResponseGenerator;
import no.sikt.nva.scopus.utils.ScopusGenerator;
import no.unit.nva.clients.CustomerList;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.expansion.model.cristin.CristinOrganization;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.ContributorVerificationStatus;
import no.unit.nva.model.Organization;
import no.unit.nva.model.additionalidentifiers.AdditionalIdentifier;
import no.unit.nva.stubs.FakeSecretsManagerClient;
import no.unit.nva.stubs.WiremockHttpClient;
import nva.commons.core.Environment;
import nva.commons.core.SingletonCollector;
import nva.commons.core.StringUtils;
import nva.commons.core.paths.UriWrapper;
import nva.commons.logutils.LogUtils;
import nva.commons.secrets.SecretsReader;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

@WireMockTest(httpsEnabled = true)
public class ContributorExtractorTest {

    public static final String ORCID_HOST_NAME = "orcid.org";
    private static final String PIA_SECRET_NAME = "someSecretName";
    private static final String PIA_USERNAME_SECRET_KEY = "someUserNameKey";
    private static final String PIA_PASSWORD_SECRET_KEY = "somePasswordNameKey";

    private CristinConnection cristinConnection;
    private PiaConnection piaConnection;
    private URI cristinOrganisationIdFromFetchOrganisationResponse;

    @BeforeEach
    void init(WireMockRuntimeInfo wireMockRuntimeInfo) {
        cristinOrganisationIdFromFetchOrganisationResponse = randomUri();

        var httpClient = WiremockHttpClient.create();
        var environment = createEnvironment(wireMockRuntimeInfo);

        var fakeSecretsManager = createFakeSecretsManager();
        var secretsReader = new SecretsReader(fakeSecretsManager);

        cristinConnection = new CristinConnection(httpClient, environment);
        piaConnection = new PiaConnection(httpClient, secretsReader, environment);
    }

    @Test
    void shouldCreateContributorWithoutAffiliationWhenAuthorTpIsNorwegianContributorWithoutAffiliation() {
        var generator = ScopusGenerator.createWithNumberOfContributorsFromAuthorTp(1);
        var document = generator.getDocument();
        var authorTp = getFirstAuthor(document);

        mockCristinPersonWithoutAffiliation(authorTp);

        var contributor = contributorExtractorFromDocument().generateContributors(document)
                              .contributors()
                              .stream()
                              .collect(SingletonCollector.collect());

        assertThat(contributor.getAffiliations(), is(emptyIterable()));
    }

    @Test
    void shouldCreateContributorWithoutAffiliationWhenAuthorTpIsNorwegianContributorWithOtherNorwegianInstitution()
        throws JsonProcessingException {
        var generator = ScopusGenerator.createWithNumberOfContributorsFromAuthorTp(1);
        var document = generator.getDocument();
        var authorTp = getFirstAuthor(document);
        var authorGroupTp = getAuthorGroup(document).getFirst();

        mockCristinPersonWithoutAffiliation(authorTp);
        mockOtherCristinOrganization(authorGroupTp);

        var contributor = contributorExtractorFromDocument().generateContributors(document)
                              .contributors()
                              .stream()
                              .collect(SingletonCollector.collect());

        assertThat(contributor.getAffiliations(), is(emptyIterable()));
    }

    @Test
    void shouldCreateContributorWithAffiliationWhenAuthorTpIsNorwegianContributorWithNorwegianInstitution()
        throws JsonProcessingException {
        var generator = ScopusGenerator.createWithNumberOfContributorsFromAuthorTp(1);
        var document = generator.getDocument();
        var authorTp = getFirstAuthor(document);
        var authorGroupTp = getAuthorGroup(document).getFirst();

        mockCristinPersonWithoutAffiliation(authorTp);
        mockNorwegianCristinOrganization(authorGroupTp);

        var contributor = contributorExtractorFromDocument().generateContributors(document)
                              .contributors()
                              .stream()
                              .collect(SingletonCollector.collect());

        assertThat(contributor.getAffiliations(), hasSize(1));
    }

    @Test
    void shouldCreateContributorWithoutAffiliationWhenAuthorTpIsNorwegianContributorWithoutAffiliationFromCristin() {
        var document = new ScopusGenerator().createWithNumberOfContributorsFromCollaborationTp(1);
        var authorGroupTp = getAuthorGroup(document).getFirst();

        mockCristinOrganizationBadRequest(authorGroupTp);

        var contributor = contributorExtractorFromDocument().generateContributors(document)
                              .contributors()
                              .stream()
                              .collect(SingletonCollector.collect());

        assertThat(contributor.getAffiliations(), is(emptyIterable()));
    }

    @Test
    void shouldCreateContributorWithAffiliationFromCristinWhenCreatingContributorFromCollaborationTp() {
        var document = new ScopusGenerator().createWithNumberOfContributorsFromCollaborationTp(1);
        var authorGroupTp = getAuthorGroup(document).getFirst();

        mockPiaAndCristinAffiliation(authorGroupTp);

        var contributor = contributorExtractorFromDocument().generateContributors(document)
                              .contributors()
                              .stream()
                              .collect(SingletonCollector.collect());

        var id = ((Organization) contributor.getAffiliations().getFirst()).getId();
        assertThat(id, is(equalTo(cristinOrganisationIdFromFetchOrganisationResponse)));
    }

    @RepeatedTest(100)
    void shouldCreateContributorByFetchingCristinPersonByOrcIdWhenNoResponseFromPia() {
        var document = ScopusGenerator.createWithNumberOfContributorsFromAuthorTp(1).getDocument();
        var orcId = getOrcidFromScopusDocument(document);
        var authorGroupTp = getAuthorGroup(document).getFirst();

        var expectedContributorName = mockCristinPersonByOrcId(orcId);
        mockPiaAndCristinAffiliation(authorGroupTp);

        var contributor = contributorExtractorFromDocument().generateContributors(document)
                              .contributors()
                              .stream()
                              .collect(SingletonCollector.collect());

        assertThat(contributor.getIdentity().getName(), is(equalTo(expectedContributorName)));
    }

    @Test
    void shouldPreserveAuidWhenConvertingToNvaContributor() {
        var auid = randomString();
        var expectedAdditionalIdentifier = new AdditionalIdentifier(SCOPUS_AUID, auid);
        var document = ScopusGenerator.createWithOneAuthorGroupAndAffiliation(createAuthorWithAuid(auid)).getDocument();
        var authorGroupTp = getAuthorGroup(document).getFirst();

        mockPiaAndCristinAffiliation(authorGroupTp);

        var nvaContributor = contributorExtractorFromDocument().generateContributors(document)
                                 .contributors()
                                 .getFirst();

        assertThat(nvaContributor.getIdentity().getAdditionalIdentifiers(), hasItem(expectedAdditionalIdentifier));
    }

    @Test
    void shouldPreserveAuidEvenWhenReplacingWithCristinContributor() {
        var auid = randomString();
        var expectedAdditionalIdentifier = new AdditionalIdentifier(SCOPUS_AUID, auid);
        var document = ScopusGenerator.createWithOneAuthorGroupAndAffiliation(createAuthorWithAuid(auid)).getDocument();
        var authorTp = getFirstAuthor(document);
        var authorGroupTp = getAuthorGroup(document).getFirst();

        mockCristinPersonWithoutAffiliation(authorTp);
        mockPiaAndCristinAffiliation(authorGroupTp);

        var nvaContributor = contributorExtractorFromDocument().generateContributors(document)
                                 .contributors()
                                 .getFirst();

        assertThat(nvaContributor.getIdentity().getAdditionalIdentifiers(), hasItem(expectedAdditionalIdentifier));
    }

    @Test
    void shouldCreateContributorFromAuthorTypeWhenBadResponseFromCristinAndNoOrcId() {
        var authorTp = createRandomAuthorTp();
        var document = ScopusGenerator.createWithSingleContributorFromAuthorTp(authorTp).getDocument();
        var authorGroupTp = getAuthorGroup(document).getFirst();

        mockCristinOrganizationBadRequest(authorGroupTp);

        var contributor = contributorExtractorFromDocument().generateContributors(document)
                              .contributors()
                              .stream()
                              .collect(SingletonCollector.collect());

        assertThat(contributor.getIdentity().getId(), is(nullValue()));
    }

    @Test
    void shouldExtractOrcIdFromXmlWhenCristinPersonIsPresentButIsMissingOrcId() {
        var authorTp = createRandomAuthorTp();
        var orcIdFromXml = randomString();
        authorTp.setOrcid(orcIdFromXml);
        var document = ScopusGenerator.createWithSingleContributorFromAuthorTp(authorTp).getDocument();

        mockCristinPersonWithoutOrcId(authorTp);

        var contributor = contributorExtractorFromDocument().generateContributors(document)
                              .contributors()
                              .stream()
                              .collect(SingletonCollector.collect());

        var expectedOrcId = UriWrapper.fromHost(ORCID_HOST_NAME).addChild(orcIdFromXml).toString();

        assertThat(contributor.getIdentity().getOrcId(), is(equalTo(expectedOrcId)));
    }

    @Test
    void shouldSetOrcIdToNullWhenMissingBothInXmlAndCristinPerson() {
        var authorTp = createRandomAuthorTp();
        var document = ScopusGenerator.createWithSingleContributorFromAuthorTp(authorTp).getDocument();

        mockCristinPersonWithoutOrcId(authorTp);

        var contributor = contributorExtractorFromDocument().generateContributors(document)
                              .contributors()
                              .stream()
                              .collect(SingletonCollector.collect());

        assertThat(contributor.getIdentity().getOrcId(), is(nullValue()));
    }

    @Test
    void shouldUseActiveAffiliationOnlyWhenCreatingOrganizationsForContributorWhenFetchingCristinPerson() {
        var document = ScopusGenerator.createWithNumberOfContributorsFromAuthorTp(1).getDocument();
        var authorTp = getFirstAuthor(document);
        var authorGroupTp = getAuthorGroup(document).getFirst();

        var cristinPerson = mockCristinPersonWithSingleActiveAffiliation(authorTp);
        mockPiaAndCristinAffiliation(authorGroupTp);

        var contributor = contributorExtractorFromDocument().generateContributors(document)
                              .contributors()
                              .stream()
                              .collect(SingletonCollector.collect());

        var expectedAffiliations = cristinPerson.getAffiliations().stream().filter(Affiliation::isActive).toList();
        var actualOrganizations = contributor.getAffiliations();

        var id = ((Organization) contributor.getAffiliations().getFirst()).getId();
        assertThat(actualOrganizations.size(), is(equalTo(expectedAffiliations.size())));
        assertThat(id, is(equalTo(expectedAffiliations.getFirst().getOrganization())));
    }

    @Test
    void shouldReplaceContributorIdentityWithCristinDataVerifiedByAuthorId() {
        var document = ScopusGenerator.createWithNumberOfContributorsFromAuthorTp(1).getDocument();
        var authorTp = getFirstAuthor(document);
        var authorGroupTp = getAuthorGroup(document).getFirst();

        var cristinPerson = mockCristinPersonWithSingleActiveAffiliation(authorTp);
        mockPiaAndCristinAffiliation(authorGroupTp);

        var contributor = contributorExtractorFromDocument().generateContributors(document).contributors().getFirst();

        assertThatContributorHasCorrectCristinPersonData(contributor, Map.of(cristinPerson, authorTp));
    }

    @Test
    void shouldExtractAuthorOrcidAndSequenceNumber() {
        var document = ScopusGenerator.createWithNumberOfContributorsFromAuthorTp(3).getDocument();
        var authors = getAuthorGroup(document).stream()
                          .flatMap(group -> group.getAuthorOrCollaboration().stream())
                          .filter(AuthorTp.class::isInstance)
                          .map(AuthorTp.class::cast)
                          .toList();

        getAuthorGroup(document).forEach(this::mockPiaAndCristinAffiliation);

        var contributors = contributorExtractorFromDocument().generateContributors(document).contributors();

        authors.forEach(author -> {
            var matchingContributor = contributors.stream()
                                          .filter(c -> c.getSequence() == Integer.parseInt(author.getSeq()))
                                          .findFirst()
                                          .orElseThrow();

            var orcid = getOrcid(author);
            if (StringUtils.isNotBlank(orcid)) {
                assertThat(matchingContributor.getIdentity().getOrcId(), is(equalTo(orcid)));
            }
            assertThat(matchingContributor.getSequence(), is(equalTo(Integer.parseInt(author.getSeq()))));
        });
    }

    @Test
    void shouldExtractContributorsNamesAndSequenceNumberCorrectly() {
        var generator = new ScopusGenerator();
        generator.createWithNumberOfContributorsFromCollaborationTp(2);
        var document = generator.getDocument();

        var authors = getAuthorGroup(document).stream()
                          .flatMap(group -> group.getAuthorOrCollaboration().stream())
                          .filter(AuthorTp.class::isInstance)
                          .map(AuthorTp.class::cast)
                          .toList();

        var collaborations = getAuthorGroup(document).stream()
                                 .flatMap(group -> group.getAuthorOrCollaboration().stream())
                                 .filter(CollaborationTp.class::isInstance)
                                 .map(CollaborationTp.class::cast)
                                 .toList();

        // Mock empty PIA responses (no Cristin matches)
        authors.forEach(author -> mockPiaAuthorEmptyResponse(author.getAuid()));
        getAuthorGroup(document).forEach(this::mockPiaAndCristinAffiliation);

        var contributors = contributorExtractorFromDocument().generateContributors(document).contributors();

        // Verify authors
        authors.forEach(author -> {
            var contributor = findContributorBySequence(contributors, Integer.parseInt(author.getSeq()));
            var expectedName = determineAuthorName(author);
            assertThat(contributor.getIdentity().getName(), is(equalTo(expectedName)));
            assertThat(contributor.getSequence(), is(equalTo(Integer.parseInt(author.getSeq()))));
        });

        // Verify collaborations
        collaborations.forEach(collaboration -> {
            var contributor = findContributorBySequence(contributors, Integer.parseInt(collaboration.getSeq()));
            assertThat(contributor.getIdentity().getName(), is(equalTo(collaboration.getIndexedName())));
            assertThat(contributor.getSequence(), is(equalTo(Integer.parseInt(collaboration.getSeq()))));
        });
    }

    @Test
    void shouldExtractCorrespondingAuthor() {
        var generator = new ScopusGenerator();
        var authors = keepOnlyTheAuthors(generator);
        var correspondingAuthorTp = authors.getFirst();
        generator.setCorrespondence(correspondingAuthorTp);

        var document = generator.getDocument();

        createEmptyPiaMock();
        getAuthorGroup(document).forEach(this::mockPiaAndCristinAffiliation);

        var contributors = contributorExtractorFromDocument().generateContributors(document).contributors();

        var correspondingContributor = contributors.stream()
                                           .filter(Contributor::isCorrespondingAuthor)
                                           .findFirst()
                                           .orElseThrow(() -> new AssertionError("No corresponding author found"));

        assertThat(correspondingContributor.getIdentity().getName(), startsWith(correspondingAuthorTp.getGivenName()));
        assertThat(correspondingContributor.isCorrespondingAuthor(), is(true));
    }

    @Test
    void shouldHandlePiaConnectionException() {
        var appender = LogUtils.getTestingAppenderForRootLogger();
        var document = ScopusGenerator.createWithNumberOfContributorsFromAuthorTp(1).getDocument();
        var authorTp = getFirstAuthor(document);

        stubFor(WireMock.get(urlPathEqualTo("/sentralimport/authors"))
                    .withQueryParam("author_id", WireMock.equalTo("SCOPUS:" + authorTp.getAuid()))
                    .willReturn(aResponse().withStatus(HTTP_INTERNAL_ERROR)));

        getAuthorGroup(document).forEach(this::mockPiaAndCristinAffiliation);

        contributorExtractorFromDocument().generateContributors(document);

        assertThat(appender.getMessages(), containsString(PiaConnection.PIA_RESPONSE_ERROR));
    }

    @Test
    void shouldHandlePiaBadRequest() {
        var appender = LogUtils.getTestingAppenderForRootLogger();
        var document = ScopusGenerator.createWithNumberOfContributorsFromAuthorTp(1).getDocument();
        var authorTp = getFirstAuthor(document);

        stubFor(WireMock.get(urlPathEqualTo("/sentralimport/authors"))
                    .withQueryParam("author_id", WireMock.equalTo("SCOPUS:" + authorTp.getAuid()))
                    .willReturn(aResponse().withStatus(HTTP_BAD_REQUEST)));

        getAuthorGroup(document).forEach(this::mockPiaAndCristinAffiliation);

        contributorExtractorFromDocument().generateContributors(document);

        assertThat(appender.getMessages(), containsString(PiaConnection.PIA_RESPONSE_ERROR));
    }

    @Test
    void shouldCreateContributorAffiliationSearchingOrganizationNameWhenNoResponseFromPiaAndWhenCreatingFromAuthorTp() {
        var document = ScopusGenerator.createWithNumberOfContributorsFromAuthorTp(1).getDocument();
        var authorTp = getFirstAuthor(document);
        var authorGroupTp = getAuthorGroup(document).getFirst();
        var institutionName = getOrganizationName(document);

        mockPiaAffiliationEmptyResponse(authorGroupTp.getAffiliation().getAfid());
        mockPiaAuthorEmptyResponse(authorTp.getAuid());

        var organization = mockSearchOrganizationByNameResponse(institutionName);

        var contributor = contributorExtractorFromDocument().generateContributors(document)
                              .contributors()
                              .stream()
                              .collect(SingletonCollector.collect());

        var id = ((Organization) contributor.getAffiliations().getFirst()).getId();
        assertThat(id, is(equalTo(organization.id())));
    }

    @Test
    void shouldCreateContributorAffiliationSearchingAuthorGroupCountryWhenNoResponseFromPiaAndSearchingInstitutionByName() {
        var document = ScopusGenerator.createWithNumberOfContributorsFromAuthorTp(1).getDocument();
        var authorTp = getFirstAuthor(document);
        var authorGroupTp = getAuthorGroup(document).getFirst();
        var affiliationCountry = authorGroupTp.getAffiliation().getCountry();

        mockPiaAffiliationEmptyResponse(authorGroupTp.getAffiliation().getAfid());
        mockPiaAuthorEmptyResponse(authorTp.getAuid());

        var orgName = getOrganizationName(document);
        mockSearchOrganizationEmptyResponse(orgName);
        var organization = mockSearchOrganizationByNameResponse(affiliationCountry);

        var contributor = contributorExtractorFromDocument().generateContributors(document)
                              .contributors()
                              .stream()
                              .collect(SingletonCollector.collect());

        var id = ((Organization) contributor.getAffiliations().getFirst()).getId();
        assertThat(id, is(equalTo(organization.id())));
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenAuthorOrCollaborationIsUnknownType() {
        var document = ScopusGenerator.createWithNumberOfContributorsFromAuthorTp(1).getDocument();
        var authorGroupTp = getAuthorGroup(document).getFirst();

        authorGroupTp.getAuthorOrCollaboration().clear();
        authorGroupTp.getAuthorOrCollaboration().add(new Object());

        mockPiaAndCristinAffiliation(authorGroupTp);

        assertThrows(IllegalArgumentException.class,
                     () -> contributorExtractorFromDocument().generateContributors(getCorrespondence(document),
                                                                                   getAuthorGroup(document)));
    }

    private static String getOrcid(AuthorTp author) {
        return author.getOrcid().contains(ORCID_HOST_NAME) ? author.getOrcid()
                   : UriWrapper.fromHost(ORCID_HOST_NAME).addChild(author.getOrcid()).toString();
    }

    private static String getOrganizationName(DocTp document) {
        return document.getItem()
                   .getItem()
                   .getBibrecord()
                   .getHead()
                   .getAuthorGroup()
                   .getFirst()
                   .getAffiliation()
                   .getOrganization()
                   .getFirst()
                   .getContent()
                   .getFirst()
                   .toString();
    }

    private static List<Affiliation> getActiveAffiliations(CristinPerson expectedCristinPerson) {
        return expectedCristinPerson.getAffiliations().stream().filter(Affiliation::isActive).toList();
    }

    private static AuthorTp getFirstAuthor(DocTp document) {
        return (AuthorTp) document.getItem()
                              .getItem()
                              .getBibrecord()
                              .getHead()
                              .getAuthorGroup()
                              .getFirst()
                              .getAuthorOrCollaboration()
                              .getFirst();
    }

    private static String getOrcidFromScopusDocument(DocTp document) {
        return getFirstAuthor(document).getOrcid();
    }

    private static List<AuthorGroupTp> getAuthorGroup(DocTp document) {
        return document.getItem().getItem().getBibrecord().getHead().getAuthorGroup();
    }

    private static List<CorrespondenceTp> getCorrespondence(DocTp document) {
        return document.getItem().getItem().getBibrecord().getHead().getCorrespondence();
    }

    private static AuthorTp createRandomAuthorTp() {
        var authorTp = new AuthorTp();
        authorTp.setAuid(randomString());
        authorTp.setSeq(String.valueOf(1));
        var personalnameType = randomPersonalnameType();
        authorTp.setPreferredName(personalnameType);
        authorTp.setIndexedName(personalnameType.getIndexedName());
        authorTp.setGivenName(personalnameType.getGivenName());
        authorTp.setSurname(personalnameType.getSurname());
        return authorTp;
    }

    private void mockPiaAffiliationEmptyResponse(String afid) {
        var response = PiaResponseGenerator.convertAffiliationsToJson(List.of());
        stubFor(WireMock.get(urlPathEqualTo("/sentralimport/orgs/matches"))
                    .withQueryParam("affiliation_id", WireMock.equalTo("SCOPUS:" + afid))
                    .willReturn(aResponse().withBody(response).withStatus(HTTP_OK)));
    }

    private CristinOrganization mockSearchOrganizationByNameResponse(String organizationName) {
        var organization = new CristinOrganization(randomUri(), randomUri(), randomString(), List.of(), randomString(),
                                                   Map.of(randomString(), organizationName));
        var responseBody = new SearchOrganizationResponse(List.of(organization), 1).toJsonString();
        stubFor(WireMock.get(urlPathEqualTo("/cristin/organization"))
                    .withQueryParam("query", WireMock.equalTo(organizationName))
                    .willReturn(aResponse().withBody(responseBody).withStatus(HTTP_OK)));
        mockCustomerListResponse(organization.id());
        return organization;
    }

    private void mockSearchOrganizationEmptyResponse(String organizationName) {
        var responseBody = new SearchOrganizationResponse(List.of(), 0).toJsonString();
        stubFor(WireMock.get(urlPathEqualTo("/cristin/organization"))
                    .withQueryParam("query", WireMock.equalTo(organizationName))
                    .willReturn(aResponse().withBody(responseBody).withStatus(HTTP_OK)));
    }

    private void createEmptyPiaMock() {
        stubFor(WireMock.get(urlMatching("/sentralimport/authors"))
                    .willReturn(aResponse().withBody("[]").withStatus(HTTP_OK)));
    }

    private Contributor findContributorBySequence(List<Contributor> contributors, int sequence) {
        return contributors.stream()
                   .filter(c -> c.getSequence() == sequence)
                   .findFirst()
                   .orElseThrow(() -> new AssertionError("No contributor found with sequence: " + sequence));
    }

    private String determineAuthorName(AuthorTp author) {
        return author.getPreferredName() != null ? author.getPreferredName().getGivenName()
                                                   + StringUtils.SPACE
                                                   + author.getPreferredName().getSurname()
                   : author.getGivenName() + StringUtils.SPACE + author.getSurname();
    }

    private void mockPiaAuthorEmptyResponse(String auid) {
        var response = PiaResponseGenerator.convertAuthorsToJson(List.of());
        stubFor(WireMock.get(urlPathEqualTo("/sentralimport/authors"))
                    .withQueryParam("author_id", WireMock.equalTo("SCOPUS:" + auid))
                    .willReturn(aResponse().withBody(response).withStatus(HTTP_OK)));
    }

    private void assertThatContributorHasCorrectCristinPersonData(Contributor contributor,
                                                                  Map<CristinPerson, AuthorTp> piaCristinIdAndAuthors) {
        var actualCristinId = contributor.getIdentity().getId();
        assertThat(actualCristinId, hasProperty("path", containsString("/cristin/person")));
        var expectedCristinPerson = getPersonByCristinNumber(piaCristinIdAndAuthors.keySet(),
                                                             actualCristinId).orElseThrow();
        var expectedName = calculateExpectedNameFromCristinPerson(expectedCristinPerson);

        assertThat(contributor.getIdentity().getName(), is(IsEqual.equalTo(expectedName)));

        assertThat(contributor.getAffiliations(), hasSize(getActiveAffiliations(expectedCristinPerson).size()));

        assertThat(contributor.getIdentity().getVerificationStatus(),
                   anyOf(IsEqual.equalTo(ContributorVerificationStatus.VERIFIED),
                         IsEqual.equalTo(ContributorVerificationStatus.NOT_VERIFIED)));

        var actualOrganizationFromAffiliation = contributor.getAffiliations()
                                                    .stream()
                                                    .filter(Organization.class::isInstance)
                                                    .map(Organization.class::cast)
                                                    .map(Organization::getId)
                                                    .collect(Collectors.toList());
        var expectedOrganizationFromAffiliation = expectedCristinPerson.getAffiliations()
                                                      .stream()
                                                      .filter(Affiliation::isActive)
                                                      .map(Affiliation::getOrganization)
                                                      .toList();

        assertThat(actualOrganizationFromAffiliation, containsInAnyOrder(
            expectedOrganizationFromAffiliation.stream().map(Matchers::equalTo).collect(Collectors.toList())));
    }

    private String calculateExpectedNameFromCristinPerson(CristinPerson cristinPerson) {
        return cristinPerson.getNames()
                   .stream()
                   .filter(this::isFirstName)
                   .findFirst()
                   .map(TypedValue::getValue)
                   .orElse(StringUtils.EMPTY_STRING) + StringUtils.SPACE + cristinPerson.getNames()
                                                                               .stream()
                                                                               .filter(this::isSurname)
                                                                               .findFirst()
                                                                               .map(TypedValue::getValue)
                                                                               .orElse(StringUtils.EMPTY_STRING);
    }

    private boolean isFirstName(TypedValue typedValue) {
        return ContributorExtractor.FIRST_NAME_CRISTIN_FIELD_NAME.equals(typedValue.getType());
    }

    private List<AuthorTp> keepOnlyTheAuthors(ScopusGenerator generator) {
        return keepOnlyTheCollaborationsAndAuthors(generator).stream()
                   .filter(this::isAuthorTp)
                   .map(author -> (AuthorTp) author)
                   .collect(Collectors.toList());
    }

    private boolean isAuthorTp(Object object) {
        return object instanceof AuthorTp;
    }

    private List<Object> keepOnlyTheCollaborationsAndAuthors(ScopusGenerator generator) {
        return generator.getDocument()
                   .getItem()
                   .getItem()
                   .getBibrecord()
                   .getHead()
                   .getAuthorGroup()
                   .stream()
                   .map(AuthorGroupTp::getAuthorOrCollaboration)
                   .flatMap(Collection::stream)
                   .collect(Collectors.toList());
    }

    private boolean isSurname(TypedValue nameType) {
        return ContributorExtractor.LAST_NAME_CRISTIN_FIELD_NAME.equals(nameType.getType());
    }

    private Optional<CristinPerson> getPersonByCristinNumber(Collection<CristinPerson> cristinCristinPeople,
                                                             URI cristinId) {
        return cristinCristinPeople.stream().filter(person -> cristinId.equals(person.getId())).findFirst();
    }

    private FakeSecretsManagerClient createFakeSecretsManager() {
        var fakeSecretsManager = new FakeSecretsManagerClient();
        fakeSecretsManager.putSecret(PIA_SECRET_NAME, PIA_USERNAME_SECRET_KEY, randomString());
        fakeSecretsManager.putSecret(PIA_SECRET_NAME, PIA_PASSWORD_SECRET_KEY, randomString());
        return fakeSecretsManager;
    }

    private Environment createEnvironment(WireMockRuntimeInfo wireMockRuntimeInfo) {
        var environment = mock(Environment.class);
        var baseUrl = wireMockRuntimeInfo.getHttpsBaseUrl().replace("https://", "");
        when(environment.readEnv("API_HOST")).thenReturn(baseUrl);
        when(environment.readEnv("PIA_REST_API")).thenReturn(baseUrl);
        when(environment.readEnv("PIA_USERNAME_KEY")).thenReturn(PIA_USERNAME_SECRET_KEY);
        when(environment.readEnv("PIA_PASSWORD_KEY")).thenReturn(PIA_PASSWORD_SECRET_KEY);
        when(environment.readEnv("PIA_SECRETS_NAME")).thenReturn(PIA_SECRET_NAME);
        return environment;
    }

    private void mockCristinPersonWithoutAffiliation(AuthorTp authorTp) {
        var cristinId = randomInteger();
        var cristinPerson = CristinGenerator.generateCristinPersonWithoutAffiliations(
            UriWrapper.fromUri("/cristin/person/" + cristinId).getUri(), randomString(), randomString());

        mockPiaAuthorResponse(authorTp.getAuid(), cristinId);
        mockGetCristinPersonApiCall(cristinId.toString(), CristinGenerator.convertPersonToJson(cristinPerson));
    }

    private void mockCristinPersonWithoutOrcId(AuthorTp authorTp) {
        var cristinId = randomInteger();
        var cristinPerson = CristinGenerator.generateCristinPersonWithoutOrcId(
            UriWrapper.fromUri("/cristin/person/" + cristinId).getUri(), randomString(), randomString());

        mockPiaAuthorResponse(authorTp.getAuid(), cristinId);
        mockGetCristinPersonApiCall(cristinId.toString(), CristinGenerator.convertPersonToJson(cristinPerson));
    }

    private CristinPerson mockCristinPersonWithSingleActiveAffiliation(AuthorTp authorTp) {
        var cristinId = randomInteger();
        var cristinPerson = CristinGenerator.generateCristinPersonWithSingleActiveAffiliation(
            UriWrapper.fromUri("/cristin/person/" + cristinId).getUri(), randomString(), randomString());

        mockPiaAuthorResponse(authorTp.getAuid(), cristinId);
        mockGetCristinPersonApiCall(cristinId.toString(), CristinGenerator.convertPersonToJson(cristinPerson));

        return cristinPerson;
    }

    private String mockCristinPersonByOrcId(String orcId) {
        var firstname = randomString();
        var surname = randomString();
        var cristinPerson = CristinGenerator.generateCristinPerson(randomUri(), firstname, surname);

        stubFor(WireMock.get(urlPathEqualTo("/cristin/person/" + UriWrapper.fromUri(orcId).getLastPathElement()))
                    .willReturn(
                        aResponse().withBody(CristinGenerator.convertPersonToJson(cristinPerson)).withStatus(HTTP_OK)));

        return "%s %s".formatted(firstname, surname);
    }

    private void mockPiaAndCristinAffiliation(AuthorGroupTp authorGroupTp) {
        var cristinOrgId = randomInteger().toString();
        var affiliation = PiaResponseGenerator.generateAffiliation(cristinOrgId, List.of(1).iterator());

        createPiaAffiliationMock(List.of(affiliation), authorGroupTp.getAffiliation().getAfid());
        generateCristinOrganizationResponse(affiliation.getUnitIdentifier(), authorGroupTp);
    }

    private void mockNorwegianCristinOrganization(AuthorGroupTp authorGroupTp) throws JsonProcessingException {
        var affiliation = PiaResponseGenerator.generateAffiliation(randomInteger().toString(), List.of(1).iterator());
        var cristinOrgId = affiliation.getUnitIdentifier();
        createPiaAffiliationMock(List.of(affiliation), authorGroupTp.getAffiliation().getAfid());

        var cristinOrgUri = UriWrapper.fromUri("cristin/organization/" + cristinOrgId).getUri();
        var cristinOrg = generateCristinOrganizationWithCountry("NO");
        var organization = CristinGenerator.convertOrganizationToJson(cristinOrg);
        mockGetCristinOrganizationApiCall(cristinOrgId, organization);
        mockSearchCristinOrganizationApiCall(cristinOrg, organization);
        mockCustomerListResponse(cristinOrgUri);
    }

    private void mockOtherCristinOrganization(AuthorGroupTp authorGroupTp) throws JsonProcessingException {
        var cristinOrgId = randomInteger().toString();
        var affiliation = PiaResponseGenerator.generateAffiliation(cristinOrgId, List.of(1).iterator());

        createPiaAffiliationMock(List.of(affiliation), authorGroupTp.getAffiliation().getAfid());

        var cristinOrgUri = UriWrapper.fromUri("cristin/organization/" + cristinOrgId).getUri();
        var cristinOrg = CristinGenerator.generateOtherCristinOrganization(cristinOrgUri);
        mockGetCristinOrganizationApiCall(cristinOrgId, CristinGenerator.convertOrganizationToJson(cristinOrg));
        mockCustomerListResponse(cristinOrgUri);
    }

    private void mockCristinOrganizationBadRequest(AuthorGroupTp authorGroupTp) {
        var cristinOrgId = randomInteger();
        mockSearchPiaAuthorBadRequest(authorGroupTp.getAffiliation().getAfid());
        stubFor(WireMock.get(urlPathEqualTo("/cristin/organization/" + cristinOrgId))
                    .willReturn(aResponse().withStatus(HTTP_BAD_REQUEST)));
    }

    private void generateCristinOrganizationResponse(String cristinOrganizationId, AuthorGroupTp authorGroupTp) {
        URI cristinOrgUri = UriWrapper.fromUri("cristin/organization/" + cristinOrganizationId).getUri();
        cristinOrganisationIdFromFetchOrganisationResponse = cristinOrgUri;
        var country = authorGroupTp.getAffiliation().getCountry();
        var cristinOrganization = CristinGenerator.generateCristinOrganization(cristinOrgUri, country);
        var organization = attempt(() -> CristinGenerator.convertOrganizationToJson(cristinOrganization)).orElseThrow();
        mockGetCristinOrganizationApiCall(cristinOrganizationId, organization);
        mockSearchCristinOrganizationApiCall(cristinOrganization, country);
        mockCustomerListResponse(cristinOrgUri);
    }

    private void mockGetCristinOrganizationApiCall(String cristinId, String organization) {
        stubFor(WireMock.get(urlPathEqualTo("/cristin/organization/" + cristinId))
                    .willReturn(aResponse().withBody(organization).withStatus(HTTP_OK)));
    }

    private void mockSearchCristinOrganizationApiCall(CristinOrganization cristinOrganization, String countryName) {
        var responseBody = new SearchOrganizationResponse(List.of(cristinOrganization), 1).toJsonString();
        stubFor(WireMock.get(urlPathEqualTo("/cristin/organization"))
                    .withQueryParam("query", WireMock.equalTo(countryName))
                    .willReturn(aResponse().withBody(responseBody).withStatus(HTTP_OK)));
    }

    private void mockCustomerListResponse(URI cristinOrganizationId) {
        var customerList = new CustomerList(List.of(randomCustomer(cristinOrganizationId)));
        var responseBody = attempt(() -> JsonUtils.dtoObjectMapper.writeValueAsString(customerList)).orElseThrow();
        stubFor(WireMock.get(urlPathEqualTo("/customers"))
                    .willReturn(aResponse().withBody(responseBody).withStatus(HTTP_OK)));
    }

    private void mockPiaAuthorResponse(String auid, Integer cristinId) {
        var authorList = PiaResponseGenerator.generateAuthors(auid, cristinId);
        var response = PiaResponseGenerator.convertAuthorsToJson(authorList);
        stubFor(WireMock.get(urlPathEqualTo("/sentralimport/authors"))
                    .withQueryParam("author_id", WireMock.equalTo("SCOPUS:" + auid))
                    .willReturn(aResponse().withBody(response).withStatus(HTTP_OK)));
    }

    private void mockSearchPiaAuthorBadRequest(String afid) {
        stubFor(WireMock.get(urlPathEqualTo("/sentralimport/orgs/matches"))
                    .withQueryParam("affiliation_id", WireMock.equalTo("SCOPUS:" + afid))
                    .willReturn(aResponse().withStatus(HTTP_BAD_REQUEST)));
    }

    private void createPiaAffiliationMock(List<no.sikt.nva.scopus.conversion.model.pia.Affiliation> affiliations,
                                          String afid) {
        var response = PiaResponseGenerator.convertAffiliationsToJson(affiliations);
        stubFor(WireMock.get(urlPathEqualTo("/sentralimport/orgs/matches"))
                    .withQueryParam("affiliation_id", WireMock.equalTo("SCOPUS:" + afid))
                    .willReturn(aResponse().withBody(response).withStatus(HTTP_OK)));
    }

    private void mockGetCristinPersonApiCall(String cristinId, String response) {
        stubFor(WireMock.get(urlPathEqualTo("/cristin/person/" + cristinId))
                    .willReturn(aResponse().withBody(response).withStatus(HTTP_OK)));
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

    private ContributorExtractor contributorExtractorFromDocument() {
        return new ContributorExtractor(piaConnection, cristinConnection);
    }
}