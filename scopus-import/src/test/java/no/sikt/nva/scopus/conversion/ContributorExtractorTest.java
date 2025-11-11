package no.sikt.nva.scopus.conversion;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_OK;
import static no.sikt.nva.scopus.conversion.ContributorExtractor.SCOPUS_AUID;
import static no.sikt.nva.scopus.utils.CristinGenerator.generateCristinOrganizationWithCountry;
import static no.sikt.nva.scopus.utils.ScopusTestUtils.randomCustomer;
import static no.unit.nva.publication.testing.http.RandomPersonServiceResponse.randomUri;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.CoreMatchers.startsWith;
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
import no.scopus.generated.CorrespondenceTp;
import no.scopus.generated.DocTp;
import no.scopus.generated.OrganizationTp;
import no.scopus.generated.PersonalnameType;
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
import no.unit.nva.importcandidate.ImportContributor;
import no.unit.nva.importcandidate.ImportOrganization;
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
import org.junit.jupiter.api.Test;

@WireMockTest(httpsEnabled = true)
public class ContributorExtractorTest {

    private static final String ORCID_HOST_NAME = "orcid.org";
    private static final String PIA_SECRET_NAME = "someSecretName";
    private static final String PIA_USERNAME_SECRET_KEY = "someUserNameKey";
    private static final String PIA_PASSWORD_SECRET_KEY = "somePasswordNameKey";
    private static final String USA = "United States of America";
    private static final String ENGLISH_ORGANIZATION_NAME = "Department of justice";
    private static final String CRISTIN_PERSON = "/cristin/person/";
    private static final String THREE_PARAMS_PATTERN = "/%s/%s/%s";
    private static final String APPLICATION_JSON = "application/json";
    private static final String QUERY = "query";
    private static final String CUSTOMERS = "/customers";
    private static final String CRISTIN = "cristin";
    private static final String ORGANIZATION = "organization";
    private static final String TWO_PARAMS_PATTERN = "/%s/%s";

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

        assertThat(contributor.affiliations(), is(emptyIterable()));
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

        assertThat(contributor.affiliations(), is(emptyIterable()));
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

        assertThat(contributor.affiliations(), hasSize(1));
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

        assertThat(contributor.affiliations(), is(emptyIterable()));
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

        var id = ((Organization) contributor.affiliations().stream().toList().getFirst().corporation()).getId();
        assertThat(id, is(equalTo(cristinOrganisationIdFromFetchOrganisationResponse)));
    }

    @Test
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

        assertThat(contributor.identity().getName(), is(equalTo(expectedContributorName)));
    }

    @Test
    void shouldPreserveAuidWhenConvertingToNvaContributor() {
        var auid = randomString();
        var expectedAdditionalIdentifier = new AdditionalIdentifier(SCOPUS_AUID, auid);
        var document = ScopusGenerator.createWithOneAuthorGroupAndAffiliation(createAuthorWithAuid(auid, "NO")).getDocument();
        var authorGroupTp = getAuthorGroup(document).getFirst();

        mockPiaAndCristinAffiliation(authorGroupTp);

        var nvaContributor = contributorExtractorFromDocument().generateContributors(document)
                                 .contributors()
                                 .getFirst();

        assertThat(nvaContributor.identity().getAdditionalIdentifiers(), hasItem(expectedAdditionalIdentifier));
    }

    @Test
    void shouldPreserveAuidEvenWhenReplacingWithCristinContributor() {
        var auid = randomString();
        var expectedAdditionalIdentifier = new AdditionalIdentifier(SCOPUS_AUID, auid);
        var document =
            ScopusGenerator.createWithOneAuthorGroupAndAffiliation(createAuthorWithAuid(auid, USA)).getDocument();
        var authorTp = getFirstAuthor(document);
        var authorGroupTp = getAuthorGroup(document).getFirst();

        mockCristinPersonWithoutAffiliation(authorTp);
        mockPiaAndCristinAffiliation(authorGroupTp);

        var nvaContributor = contributorExtractorFromDocument().generateContributors(document)
                                 .contributors()
                                 .getFirst();

        assertThat(nvaContributor.identity().getAdditionalIdentifiers(), hasItem(expectedAdditionalIdentifier));
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

        assertThat(contributor.identity().getId(), is(nullValue()));
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

        assertThat(contributor.identity().getOrcId(), is(equalTo(expectedOrcId)));
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

        assertThat(contributor.identity().getOrcId(), is(nullValue()));
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
        var actualOrganizations = contributor.affiliations();

        var id = ((Organization) contributor.affiliations().stream().toList().getFirst().corporation()).getId();
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
                                          .filter(contributor -> contributor.sequence() == Integer.parseInt(author.getSeq()))
                                          .findFirst()
                                          .orElseThrow();

            var orcid = getOrcid(author);
            if (StringUtils.isNotBlank(orcid)) {
                assertThat(matchingContributor.identity().getOrcId(), is(equalTo(orcid)));
            }
            assertThat(matchingContributor.sequence(), is(equalTo(Integer.parseInt(author.getSeq()))));
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
                                           .filter(ImportContributor::correspondingAuthor)
                                           .findFirst()
                                           .orElseThrow(() -> new AssertionError("No corresponding author found"));

        assertThat(correspondingContributor.identity().getName(), startsWith(correspondingAuthorTp.getGivenName()));
        assertThat(correspondingContributor.correspondingAuthor(), is(true));
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

        var id = ((Organization) contributor.affiliations().stream().toList().getFirst().corporation()).getId();
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

        var id = ((Organization) contributor.affiliations().stream().toList().getFirst().corporation()).getId();
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

    @Test
    void shouldDeduplicateContributorsByOrcidAcrossMultipleAuthorGroups() {
        var sharedOrcid = randomString();
        var sharedAuid = randomString();

        var authorGroup1 = createAuthorGroupWithAuthor(sharedAuid, sharedOrcid, "NO");
        var authorGroup2 = createAuthorGroupWithAuthor(sharedAuid, sharedOrcid, "SE");

        mockPiaAuthorEmptyResponse(sharedAuid);
        mockPiaAndCristinAffiliation(authorGroup1);
        mockPiaAndCristinAffiliation(authorGroup2);

        var correspondenceTps = List.<CorrespondenceTp>of();
        var authorGroupTps = List.of(authorGroup1, authorGroup2);

        var result = contributorExtractorFromDocument().generateContributors(correspondenceTps, authorGroupTps);

        assertThat(result.contributors(), hasSize(1));

        var contributor = result.contributors().getFirst();
        assertThat(contributor.identity().getOrcId(), containsString(sharedOrcid));

        assertThat(contributor.affiliations().size(), is(2));
    }

    @Test
    void shouldDeduplicateContributorsBySequenceNumberWhenNoOrcid() {
        var sharedAuid = randomString();
        var sequenceNumber = "1";

        var author1 = createAuthorWithoutOrcid(sharedAuid, sequenceNumber);
        var author2 = createAuthorWithoutOrcid(sharedAuid, sequenceNumber);

        var authorGroup1 = createAuthorGroupWithCustomAuthor(author1, "NO");
        var authorGroup2 = createAuthorGroupWithCustomAuthor(author2, "SE");

        mockPiaAuthorEmptyResponse(sharedAuid);
        mockPiaAndCristinAffiliation(authorGroup1);
        mockPiaAndCristinAffiliation(authorGroup2);

        var correspondenceTps = List.<CorrespondenceTp>of();
        var authorGroupTps = List.of(authorGroup1, authorGroup2);

        var result = contributorExtractorFromDocument().generateContributors(correspondenceTps, authorGroupTps);

        assertThat(result.contributors(), hasSize(1));

        var contributor = result.contributors().getFirst();
        assertThat(contributor.sequence(), is(Integer.parseInt(sequenceNumber)));

        assertThat(contributor.affiliations().size(), is(2));
    }

    @Test
    void shouldNotMergeAffiliationsWhenContributorHasCristinId() {
        var sharedOrcid = randomString();
        var sharedAuid = randomString();

        var authorGroup1 = createAuthorGroupWithAuthor(sharedAuid, sharedOrcid, "NO");
        var authorGroup2 = createAuthorGroupWithAuthor(sharedAuid, sharedOrcid, "SE");

        var firstAuthor = (AuthorTp) authorGroup1.getAuthorOrCollaboration().getFirst();

        var cristinPerson = mockCristinPersonWithSingleActiveAffiliation(firstAuthor);
        mockPiaAndCristinAffiliation(authorGroup1);
        mockPiaAndCristinAffiliation(authorGroup2);

        var correspondenceTps = List.<CorrespondenceTp>of();
        var authorGroupTps = List.of(authorGroup1, authorGroup2);

        var result = contributorExtractorFromDocument().generateContributors(correspondenceTps, authorGroupTps);

        assertThat(result.contributors(), hasSize(1));

        var contributor = result.contributors().getFirst();
        assertThat(contributor.identity().getId(), is(equalTo(cristinPerson.getId())));

        assertThat(contributor.affiliations(), hasSize(1));
    }

    @Test
    void shouldMergeAffiliationsOnlyForNonCristinContributors() {
        var sharedAuid = randomString();
        var sequenceNumber = "1";

        var author1 = createAuthorWithoutOrcid(sharedAuid, sequenceNumber);
        var author2 = createAuthorWithoutOrcid(sharedAuid, sequenceNumber);

        var authorGroup1 = createAuthorGroupWithCustomAuthor(author1, "NO");
        var authorGroup2 = createAuthorGroupWithCustomAuthor(author2, "SE");

        mockPiaAuthorEmptyResponse(sharedAuid);
        mockPiaAndCristinAffiliation(authorGroup1);
        mockPiaAndCristinAffiliation(authorGroup2);

        var correspondenceTps = List.<CorrespondenceTp>of();
        var authorGroupTps = List.of(authorGroup1, authorGroup2);

        var result = contributorExtractorFromDocument().generateContributors(correspondenceTps, authorGroupTps);

        var contributor = result.contributors().getFirst();

        assertThat(contributor.identity().getId(), is(nullValue()));

        assertThat(contributor.affiliations().size(), is(2));
    }

    @Test
    void shouldDeduplicateContributorWhenContributorIsPresentInMultipleAuthorGroupsAndCorrespondence() {
        var contributorScopusIdentifier = randomString();

        var givenName = randomString();
        var surname = randomString();
        var orcId = randomString();
        var author1 = createAuthorWithNameAndId(contributorScopusIdentifier, givenName, surname);
        var author2 = createAuthorWithNameAndIdAndOrcId(contributorScopusIdentifier, orcId, givenName, surname);

        var authorGroup1 = createAuthorGroupWithCustomAuthor(author1, randomString());
        var authorGroup2 = createAuthorGroupWithCustomAuthor(author2, randomString());

        mockPiaAuthorEmptyResponse(contributorScopusIdentifier);
        mockPiaAndCristinAffiliation(authorGroup1);
        mockPiaAndCristinAffiliation(authorGroup2);

        var correspondence = new CorrespondenceTp();
        correspondence.setAffiliation(randomAffiliation(randomString()));
        correspondence.setPerson(personalnameType(givenName, surname));
        var correspondenceTps = List.of(correspondence);
        var authorGroupTps = List.of(authorGroup1, authorGroup2);

        var contributors = contributorExtractorFromDocument().generateContributors(correspondenceTps, authorGroupTps).contributors();

        assertThat(contributors.size(), is(1));
    }

    @Test
    void shouldKeepOrcIdWhenDeduplicatingContributorAndOnlyOneHasOrcId() {
        var contributorScopusIdentifier = randomString();

        var givenName = randomString();
        var surname = randomString();
        var orcId = randomString();
        var author1 = createAuthorWithNameAndId(contributorScopusIdentifier, givenName, surname);
        var author2 = createAuthorWithNameAndIdAndOrcId(contributorScopusIdentifier, orcId, givenName, surname);

        var authorGroup1 = createAuthorGroupWithCustomAuthor(author1, randomString());
        var authorGroup2 = createAuthorGroupWithCustomAuthor(author2, randomString());

        mockPiaAuthorEmptyResponse(contributorScopusIdentifier);
        mockPiaAndCristinAffiliation(authorGroup1);
        mockPiaAndCristinAffiliation(authorGroup2);

        var correspondence = new CorrespondenceTp();
        correspondence.setAffiliation(randomAffiliation(randomString()));
        correspondence.setPerson(personalnameType(givenName, surname));
        var correspondenceTps = List.of(correspondence);
        var authorGroupTps = List.of(authorGroup1, authorGroup2);

        var contributors = contributorExtractorFromDocument().generateContributors(correspondenceTps, authorGroupTps).contributors();

        assertThat(contributors.size(), is(1));
    }

    public static PersonalnameType personalnameType(String givenName, String surname) {
        var personalNameType = new PersonalnameType();
        personalNameType.setGivenName(givenName);
        personalNameType.setSurname(surname);
        return personalNameType;
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
        var personalnameType = personalnameType(randomString(), randomString());
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
                    .withHeader("Content-Type", WireMock.equalTo("application/json"))
                    .withQueryParam("query", WireMock.equalTo(organizationName))
                    .willReturn(aResponse().withBody(responseBody).withStatus(HTTP_OK)));
        mockCustomerListResponse(organization.id());
        return organization;
    }

    private void mockSearchOrganizationEmptyResponse(String organizationName) {
        var responseBody = new SearchOrganizationResponse(List.of(), 0).toJsonString();
        stubFor(WireMock.get(urlPathEqualTo("/cristin/organization"))
                    .withHeader("Content-Type", WireMock.equalTo("application/json"))
                    .withQueryParam("query", WireMock.equalTo(organizationName))
                    .willReturn(aResponse().withBody(responseBody).withStatus(HTTP_OK)));
    }

    private void createEmptyPiaMock() {
        stubFor(WireMock.get(urlMatching("/sentralimport/authors"))
                    .willReturn(aResponse().withBody("[]").withStatus(HTTP_OK)));
    }

    private void mockPiaAuthorEmptyResponse(String auid) {
        var response = PiaResponseGenerator.convertAuthorsToJson(List.of());
        stubFor(WireMock.get(urlPathEqualTo("/sentralimport/authors"))
                    .withQueryParam("author_id", WireMock.equalTo("SCOPUS:" + auid))
                    .willReturn(aResponse().withBody(response).withStatus(HTTP_OK)));
    }

    private void assertThatContributorHasCorrectCristinPersonData(ImportContributor contributor,
                                                                  Map<CristinPerson, AuthorTp> piaCristinIdAndAuthors) {
        var actualCristinId = contributor.identity().getId();
        assertThat(actualCristinId, hasProperty("path", containsString("/cristin/person")));
        var expectedCristinPerson = getPersonByCristinNumber(piaCristinIdAndAuthors.keySet(),
                                                             actualCristinId).orElseThrow();
        var expectedName = calculateExpectedNameFromCristinPerson(expectedCristinPerson);

        assertThat(contributor.identity().getName(), is(IsEqual.equalTo(expectedName)));

        assertThat(contributor.affiliations(), hasSize(getActiveAffiliations(expectedCristinPerson).size()));

        assertThat(contributor.identity().getVerificationStatus(),
                   anyOf(IsEqual.equalTo(ContributorVerificationStatus.VERIFIED),
                         IsEqual.equalTo(ContributorVerificationStatus.NOT_VERIFIED)));

        var actualOrganizationFromAffiliation = contributor.affiliations()
                                                    .stream()
                                                    .map(ImportOrganization::corporation)
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

        var cristinOrgUri =
            UriWrapper.fromUri(THREE_PARAMS_PATTERN.formatted(CRISTIN, ORGANIZATION, cristinOrgId)).getUri();
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

        var cristinOrgUri =
            UriWrapper.fromUri(THREE_PARAMS_PATTERN.formatted(CRISTIN, ORGANIZATION, cristinOrgId)).getUri();
        var cristinOrg = CristinGenerator.generateOtherCristinOrganization(cristinOrgUri);
        mockGetCristinOrganizationApiCall(cristinOrgId, CristinGenerator.convertOrganizationToJson(cristinOrg));
        mockCustomerListResponse(cristinOrgUri);
    }

    private void generateCristinOrganizationResponse(String cristinOrganizationId, AuthorGroupTp authorGroupTp) {
        var cristinOrgUri = UriWrapper.fromUri(THREE_PARAMS_PATTERN.formatted(CRISTIN, ORGANIZATION, cristinOrganizationId)).getUri();
        cristinOrganisationIdFromFetchOrganisationResponse = cristinOrgUri;
        var country = authorGroupTp.getAffiliation().getCountry();
        var cristinOrganization = CristinGenerator.generateCristinOrganization(cristinOrgUri, country);
        var organization = attempt(() -> CristinGenerator.convertOrganizationToJson(cristinOrganization)).orElseThrow();
        mockGetCristinOrganizationApiCall(cristinOrganizationId, organization);
        mockSearchCristinOrganizationApiCall(cristinOrganization, country);
        mockCustomerListResponse(cristinOrgUri);
    }

    private void mockGetCristinOrganizationApiCall(String cristinId, String organization) {
        stubFor(WireMock.get(urlPathEqualTo(THREE_PARAMS_PATTERN.formatted(CRISTIN, ORGANIZATION, cristinId)))
                    .withHeader(CONTENT_TYPE, WireMock.equalTo(APPLICATION_JSON))
                    .willReturn(aResponse().withBody(organization).withStatus(HTTP_OK)));
    }

    private void mockSearchCristinOrganizationApiCall(CristinOrganization cristinOrganization, String countryName) {
        var responseBody = new SearchOrganizationResponse(List.of(cristinOrganization), 1).toJsonString();
        stubFor(WireMock.get(urlPathEqualTo(TWO_PARAMS_PATTERN.formatted(CRISTIN, ORGANIZATION)))
                    .withQueryParam(QUERY, WireMock.equalTo(countryName))
                    .withHeader(CONTENT_TYPE, WireMock.equalTo(APPLICATION_JSON))
                    .willReturn(aResponse().withBody(responseBody).withStatus(HTTP_OK)));
    }

    private void mockCristinOrganizationBadRequest(AuthorGroupTp authorGroupTp) {
        var cristinOrgId = randomInteger();
        mockSearchPiaAuthorBadRequest(authorGroupTp.getAffiliation().getAfid());
        stubFor(WireMock.get(urlPathEqualTo(THREE_PARAMS_PATTERN.formatted(CRISTIN, ORGANIZATION, cristinOrgId)))
                    .withHeader(CONTENT_TYPE, WireMock.equalTo(APPLICATION_JSON))
                    .willReturn(aResponse().withStatus(HTTP_BAD_REQUEST)));
    }

    private void mockCustomerListResponse(URI cristinOrganizationId) {
        var customerList = new CustomerList(List.of(randomCustomer(cristinOrganizationId)));
        var responseBody = attempt(() -> JsonUtils.dtoObjectMapper.writeValueAsString(customerList)).orElseThrow();
        stubFor(WireMock.get(urlPathEqualTo(CUSTOMERS))
                    .withHeader(CONTENT_TYPE, WireMock.equalTo(APPLICATION_JSON))
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
        stubFor(WireMock.get(urlPathEqualTo(CRISTIN_PERSON + cristinId))
                    .withHeader(CONTENT_TYPE, WireMock.equalTo(APPLICATION_JSON))
                    .willReturn(aResponse().withBody(response).withStatus(HTTP_OK)));
    }

    private void mockCristinPersonWithoutAffiliation(AuthorTp authorTp) {
        var cristinId = randomInteger();
        var cristinPerson = CristinGenerator.generateCristinPersonWithoutAffiliations(
            UriWrapper.fromUri(CRISTIN_PERSON).addChild(cristinId.toString()).getUri(), randomString(), randomString());

        mockPiaAuthorResponse(authorTp.getAuid(), cristinId);
        mockGetCristinPersonApiCall(cristinId.toString(), CristinGenerator.convertPersonToJson(cristinPerson));
    }

    private void mockCristinPersonWithoutOrcId(AuthorTp authorTp) {
        var cristinId = randomInteger();
        var cristinPerson = CristinGenerator.generateCristinPersonWithoutOrcId(
            UriWrapper.fromUri(CRISTIN_PERSON).addChild(cristinId.toString()).getUri(), randomString(), randomString());

        mockPiaAuthorResponse(authorTp.getAuid(), cristinId);
        mockGetCristinPersonApiCall(cristinId.toString(), CristinGenerator.convertPersonToJson(cristinPerson));
    }

    private CristinPerson mockCristinPersonWithSingleActiveAffiliation(AuthorTp authorTp) {
        var cristinId = randomInteger();
        var cristinPerson = CristinGenerator.generateCristinPersonWithSingleActiveAffiliation(
            UriWrapper.fromUri(CRISTIN_PERSON).addChild(cristinId.toString()).getUri(), randomString(), randomString());

        mockPiaAuthorResponse(authorTp.getAuid(), cristinId);
        mockGetCristinPersonApiCall(cristinId.toString(), CristinGenerator.convertPersonToJson(cristinPerson));

        return cristinPerson;
    }

    private String mockCristinPersonByOrcId(String orcId) {
        var firstname = randomString();
        var surname = randomString();
        var cristinPerson = CristinGenerator.generateCristinPerson(randomUri(), firstname, surname);

        stubFor(WireMock.get(urlPathEqualTo(CRISTIN_PERSON + UriWrapper.fromUri(orcId).getLastPathElement()))
                    .withHeader(CONTENT_TYPE, WireMock.equalTo(APPLICATION_JSON))
                    .willReturn(
                        aResponse().withBody(CristinGenerator.convertPersonToJson(cristinPerson)).withStatus(HTTP_OK)));

        return "%s %s".formatted(firstname, surname);
    }

    private AuthorGroupTp createAuthorWithAuid(String auid, String country) {
        var authorTp = new AuthorTp();
        authorTp.setAuid(auid);
        authorTp.setSurname(randomString());
        authorTp.setGivenName(randomString());
        authorTp.setIndexedName(randomString());
        authorTp.setSeq("1");

        var affiliationTp = randomAffiliation(country);
        var organizationTp = new OrganizationTp();
        organizationTp.getContent().add(ENGLISH_ORGANIZATION_NAME);
        affiliationTp.getOrganization().add(organizationTp);

        var authorGp = new AuthorGroupTp();
        authorGp.setAffiliation(affiliationTp);
        authorGp.getAuthorOrCollaboration().add(authorTp);
        return authorGp;
    }

    private AuthorGroupTp createAuthorGroupWithAuthor(String auid, String orcid, String country) {
        var authorTp = new AuthorTp();
        authorTp.setAuid(auid);
        authorTp.setOrcid(orcid);
        authorTp.setSurname(randomString());
        authorTp.setGivenName(randomString());
        authorTp.setIndexedName(randomString());
        authorTp.setSeq("1");

        var affiliationTp = randomAffiliation(country);
        var organizationTp = new OrganizationTp();
        organizationTp.getContent().add(randomString());
        affiliationTp.getOrganization().add(organizationTp);

        var authorGp = new AuthorGroupTp();
        authorGp.setAffiliation(affiliationTp);
        authorGp.getAuthorOrCollaboration().add(authorTp);
        return authorGp;
    }

    private AuthorTp createAuthorWithoutOrcid(String auid, String sequence) {
        var authorTp = new AuthorTp();
        authorTp.setAuid(auid);
        authorTp.setOrcid(null);
        authorTp.setSurname(randomString());
        authorTp.setGivenName(randomString());
        authorTp.setIndexedName(randomString());
        authorTp.setSeq(sequence);
        return authorTp;
    }

    private AuthorTp createAuthorWithNameAndId(String auid, String givenName, String surname) {
        var authorTp = new AuthorTp();
        authorTp.setAuid(auid);
        authorTp.setOrcid(null);
        authorTp.setSurname(surname);
        authorTp.setIndexedName(randomString());
        authorTp.setGivenName(givenName);
        authorTp.setSeq("1");
        return authorTp;
    }

    private AuthorTp createAuthorWithNameAndIdAndOrcId(String auid, String orcId, String givenName, String surname) {
        var authorTp = new AuthorTp();
        authorTp.setAuid(auid);
        authorTp.setOrcid(orcId);
        authorTp.setSurname(surname);
        authorTp.setIndexedName(randomString());
        authorTp.setGivenName(givenName);
        authorTp.setSeq("1");
        return authorTp;
    }

    private AuthorGroupTp createAuthorGroupWithCustomAuthor(AuthorTp authorTp, String country) {
        var affiliationTp = randomAffiliation(country);
        var organizationTp = new OrganizationTp();
        organizationTp.getContent().add(randomString());
        affiliationTp.getOrganization().add(organizationTp);

        var authorGp = new AuthorGroupTp();
        authorGp.setAffiliation(affiliationTp);
        authorGp.getAuthorOrCollaboration().add(authorTp);
        return authorGp;
    }

    private static AffiliationTp randomAffiliation(String country) {
        var affiliationTp = new AffiliationTp();
        affiliationTp.setAfid(randomString());
        affiliationTp.setCountry(country);
        return affiliationTp;
    }

    private ContributorExtractor contributorExtractorFromDocument() {
        return new ContributorExtractor(piaConnection, cristinConnection);
    }
}