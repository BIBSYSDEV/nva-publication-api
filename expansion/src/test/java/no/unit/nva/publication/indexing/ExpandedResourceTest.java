package no.unit.nva.publication.indexing;

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.Objects.isNull;
import static no.unit.nva.expansion.ExpansionConfig.objectMapper;
import static no.unit.nva.expansion.model.ExpandedResource.fromPublication;
import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.model.testing.PublicationGenerator.randomDoi;
import static no.unit.nva.model.testing.PublicationGenerator.randomOrganization;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.publication.indexing.PublicationChannelGenerator.getPublicationChannelSampleJournal;
import static no.unit.nva.publication.indexing.PublicationChannelGenerator.getPublicationChannelSamplePublisher;
import static no.unit.nva.publication.indexing.PublicationChannelGenerator.getPublicationChannelSampleSeries;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.apigateway.MediaTypes.APPLICATION_JSON_LD;
import static nva.commons.core.ioutils.IoUtils.stringFromResources;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsIterableContaining.hasItems;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import no.unit.nva.api.PublicationResponse;
import no.unit.nva.expansion.model.ExpandedResource;
import no.unit.nva.expansion.utils.PublicationJsonPointers;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.Corporation;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.associatedartifacts.AssociatedLink;
import no.unit.nva.model.contexttypes.Anthology;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.contexttypes.Journal;
import no.unit.nva.model.contexttypes.Publisher;
import no.unit.nva.model.contexttypes.Series;
import no.unit.nva.model.funding.Funding;
import no.unit.nva.model.funding.FundingBuilder;
import no.unit.nva.model.instancetypes.book.BookAnthology;
import no.unit.nva.model.instancetypes.book.BookMonograph;
import no.unit.nva.model.instancetypes.chapter.AcademicChapter;
import no.unit.nva.model.instancetypes.journal.FeatureArticle;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.model.testing.PublicationInstanceBuilder;
import no.unit.nva.publication.PublicationServiceConfig;
import no.unit.nva.publication.external.services.UriRetriever;
import nva.commons.core.paths.UriWrapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ExpandedResourceTest {

    public static final String COUNTRY_CODE_NO = "NO";
    public static final String JSON_PTR_TOP_LEVEL_ORGS = "/topLevelOrganizations";
    public static final String JSON_CONTRIBUTOR_ORGANIZATIONS = "/contributorOrganizations";
    public static final String JSON_PTR_ID = "/id";
    public static final String JSON_PTR_HAS_PART = "/hasPart";
    public static final String CRISTIN_ORG_JSON = "cristin_org.json";
    public static final String TOP_LEVEL_CRISTIN_ORG_JSON = "cristin_org_top_level.json";
    private static final String SERIES_LEVEL_JSON_PTR =
        "/entityDescription/reference/publicationContext/entityDescription/reference/publicationContext"
        + "/series/level";
    private static final String PUBLISHER_LEVEL_JSON_PTR = "/entityDescription/reference/publicationContext"
                                                           + "/entityDescription/reference/publicationContext"
                                                           + "/publisher/level";
    private static final String PUBLISHER_ID_JSON_PTR =
        "/entityDescription/reference/publicationContext/entityDescription/reference/publicationContext"
        + "/publisher/id";
    private static final String SERIES_ID_JSON_PTR =
        "/entityDescription/reference/publicationContext/entityDescription/reference/publicationContext"
        + "/series/id";
    private static final String PUBLISHER_NAME_JSON_PTR =
        "/entityDescription/reference/publicationContext/publisher/name";
    private static final String SERIES_NAME_JSON_PTR =
        "/entityDescription/reference/publicationContext/series/name";
    private static final String ID_NAMESPACE = System.getenv("ID_NAMESPACE");
    private static final URI HOST_URI = PublicationServiceConfig.PUBLICATION_HOST_URI;
    private UriRetriever uriRetriever;

    @BeforeEach
    void setup() {
        this.uriRetriever = mock(UriRetriever.class);
        when(uriRetriever.getRawContent(any(), any())).thenReturn(Optional.empty());
    }

    @Test
    void shouldReturnIndexDocumentWithValidReferenceData() throws Exception {

        final Publication publication = randomBookWithConfirmedPublisher();
        final URI seriesUri = extractSeriesId(publication);
        final URI publisherUri = extractPublisherId(publication);
        final String publisherName = randomString();
        final String seriesName = randomString();

        final UriRetriever mockUriRetriever = mock(UriRetriever.class);
        addPublicationChannelPublisherToMockUriRetriever(mockUriRetriever, seriesUri, seriesName, publisherUri,
                                                         publisherName);

        final ExpandedResource indexDocument = fromPublication(mockUriRetriever, publication);
        final JsonNode framedResultNode = indexDocument.asJsonNode();

        assertEquals(publisherUri.toString(),
                     framedResultNode.at(PublicationJsonPointers.PUBLISHER_ID_JSON_PTR).textValue());
        assertEquals(publisherName, framedResultNode.at(PUBLISHER_NAME_JSON_PTR).textValue());
        assertEquals(seriesUri.toString(), framedResultNode.at(PublicationJsonPointers.SERIES_ID_JSON_PTR).textValue());
        assertEquals(seriesName, framedResultNode.at(SERIES_NAME_JSON_PTR).textValue());
    }

    @Test
    void shouldReturnIndexDocumentWithValidContributorAffiliationCountryCode() throws Exception {
        var mockUriRetriever = mock(UriRetriever.class);
        var publication = randomPublication();
        mockCristinOrganizationRawContentResponse(mockUriRetriever, publication);

        var indexDocument = fromPublication(mockUriRetriever, publication);
        var framedResultNode = indexDocument.asJsonNode();

        var actualCountryCode = framedResultNode.at("/entityDescription/contributors/1/affiliations/0/countryCode")
                                    .textValue();
        assertThat(actualCountryCode, is(not(nullValue())));
    }

    @Test
    void shouldReturnIndexDocumentWithTopLevelOrganizationsWithTreeToRelevantAffiliation() throws Exception {
        final var publication = randomBookWithConfirmedPublisher();
        final var affiliationToBeExpanded = extractAffiliationsUris(publication).get(0);

        final var mockUriRetriever = mock(UriRetriever.class);
        mockOrganizationResponse(affiliationToBeExpanded, mockUriRetriever);

        var framedResultNode = fromPublication(mockUriRetriever, publication).asJsonNode();
        var topLevelNodes = (ArrayNode) framedResultNode.at(JSON_PTR_TOP_LEVEL_ORGS);
        var topLevelForExpandedAffiliation = getTopLevel(topLevelNodes, "194.0.0.0");

        assertThat(findDeepestNestedSubUnit(topLevelForExpandedAffiliation).at(JSON_PTR_ID).textValue(),
                   is(equalTo(affiliationToBeExpanded.toString())));
    }

    @Test
    void shouldReturnIndexDocumentWithContributorsOrganizations() throws Exception {
        var publication = randomPublication();
        var contributor1org = orgWithReadableId("contributor1org");
        var contributor1parentOrg = orgWithReadableId("contributor1parentOrg");
        var contributor2org = orgWithReadableId("contributor2org");
        var contributor2parentOrg = orgWithReadableId("contributor2parentOrg");
        var sharedGrandParent = orgWithReadableId("sharedGrandParent");

        var contributor1 = contributorWithOneAffiliation(contributor1org);
        var contributor2 = contributorWithOneAffiliation(contributor2org);
        publication.getEntityDescription().setContributors(List.of(contributor1, contributor2));

        final var mockUriRetriever = mock(UriRetriever.class);
        mockOrganizationResponseWithParents(mockUriRetriever,
                                            contributor1org,
                                            contributor1parentOrg,
                                            sharedGrandParent);
        mockOrganizationResponseWithParents(mockUriRetriever,
                                            contributor2org,
                                            contributor2parentOrg,
                                            sharedGrandParent);

        var framedResultNode = fromPublication(mockUriRetriever, publication).asJsonNode();
        var contributorOrganizationsNode = framedResultNode.at(JSON_CONTRIBUTOR_ORGANIZATIONS);
        var actualOrganizations =
            Lists.newArrayList(contributorOrganizationsNode.elements()).stream().map(JsonNode::textValue).toList();


        var expectedOrganizations = Stream.of(contributor1org, contributor2org, contributor1parentOrg,
                                              contributor2parentOrg, sharedGrandParent)
                                        .map(Organization::getId)
                                        .map(URI::toString)
                                        .toArray();


        assertThat(actualOrganizations, containsInAnyOrder(expectedOrganizations));
    }

    private static void mockOrganizationResponseWithParents(UriRetriever uriRetriever,
                                                            Organization org,
                                                            Organization parentOrg,
                                                            Organization grandParentOrg) {
        mockGetRawContentResponse(uriRetriever, org.getId(),
                                  getCristinResponseWithNestedPartOfsForOrganization(org.getId().toString(),
                                                                                      parentOrg.getId().toString(),
                                                                                      grandParentOrg
                                                                                          .getId().toString()));
    }

    private static Contributor contributorWithOneAffiliation(Organization contributor1org) {
        return new Contributor.Builder()
                   .withIdentity(new Identity.Builder().withName(randomString()).build())
                   .withRole(new RoleType(Role.ACTOR))
                   .withSequence(randomInteger(10000))
                   .withAffiliations(List.of(contributor1org))
                   .build();
    }

    private Organization orgWithReadableId(String readable) {
        return new Organization.Builder()
            .withId(URI.create(("https://example.org/" + readable)))
            .build();
    }

    @Test
    void shouldReturnIndexDocumentEvenIfParsingCristinOrgResponseFails() throws Exception {
        final var publication = randomBookWithConfirmedPublisher();
        final var affiliationToBeExpanded = extractAffiliationsUris(publication).get(0);

        final var mockUriRetriever = mock(UriRetriever.class);
        var nonParseableResponse = randomString();
        mockGetRawContentResponse(mockUriRetriever, affiliationToBeExpanded, nonParseableResponse);

        var framedResultNode = fromPublication(mockUriRetriever, publication).asJsonNode();
        assertThat(framedResultNode, is(not(nullValue())));
    }

    @Test
    void shouldReturnIndexDocumentWithTopLevelOrganizationWithoutHasPartsIfContributorAffiliatedWithTopLevel()
        throws Exception {
        final var publication = randomBookWithConfirmedPublisher();
        final var affiliationToBeExpanded = extractAffiliationsUris(publication).get(0);

        final var mockUriRetriever = mock(UriRetriever.class);
        mockOrganizationResponseForTopLevelAffiliation(affiliationToBeExpanded, mockUriRetriever);

        var framedResultNode = fromPublication(mockUriRetriever, publication).asJsonNode();
        var topLevelNodes = (ArrayNode) framedResultNode.at(JSON_PTR_TOP_LEVEL_ORGS);
        var topLevelForExpandedAffiliation = getTopLevel(topLevelNodes, affiliationToBeExpanded.toString());

        var deepestNestedSubUnit = findDeepestNestedSubUnit(topLevelForExpandedAffiliation);
        assertThat(deepestNestedSubUnit.at(JSON_PTR_ID).textValue(), is(equalTo(affiliationToBeExpanded.toString())));
    }

    @Test
    void shouldReturnIndexDocumentWithSortedContributorsByTheirSequence()
        throws Exception {
        final var publication = randomBookWithManyContributors();
        final var affiliationToBeExpanded = extractAffiliationsUris(publication).get(0);

        final var mockUriRetriever = mock(UriRetriever.class);
        mockOrganizationResponseForTopLevelAffiliation(affiliationToBeExpanded, mockUriRetriever);

        var framedResultNode = fromPublication(mockUriRetriever, publication).asJsonNode();
        var contributorsJson = framedResultNode.at("/entityDescription/contributors");

        List<Contributor> contributors = objectMapper.convertValue(contributorsJson,
                                                                   objectMapper.getTypeFactory()
                                                                       .constructCollectionType(List.class,
                                                                                                Contributor.class));
        var sortedContributors = contributors.stream().sorted(
            Comparator.comparing(Contributor::getSequence)).toList();

        assertThat(contributors, is(equalTo(sortedContributors)));
    }

    @Test
    void shouldReturnIndexDocumentWithIdWhenThereIsNoEntityDescription()
        throws Exception {
        final var publication = randomPublicationWithoutEntityDescription();

        final var mockUriRetriever = mock(UriRetriever.class);

        var framedResultNode = fromPublication(mockUriRetriever, publication).asJsonNode();
        var id = URI.create(framedResultNode.at(PublicationJsonPointers.ID_JSON_PTR).textValue());

        assertThat(id, is(not(nullValue())));
    }

    @Test
    @Deprecated
    void shouldReturnIndexDocumentWithValidFundingSource() throws Exception {

        final var publication = randomBookWithConfirmedPublisher();
        final var sourceUri0 = publication.getFundings().get(0).getSource();
        final var sourceUri1 = publication.getFundings().get(1).getSource();
        final var mockUriRetriever = mock(UriRetriever.class);

        var firstResponse = mockResponseWithStatusCodeAndBody(HTTP_OK, getPublicationSampleFundingSource(sourceUri0));

        var secondResponse = mockResponseWithStatusCodeAndBody(HTTP_OK, getPublicationSampleFundingSource(sourceUri1));
        when(mockUriRetriever.fetchResponse(sourceUri0, APPLICATION_JSON_LD.toString()))
            .thenReturn(Optional.of(firstResponse));
        when(mockUriRetriever.fetchResponse(sourceUri1, APPLICATION_JSON_LD.toString()))
            .thenReturn(Optional.of(secondResponse));

        final var framedResultNode = fromPublication(mockUriRetriever, publication).asJsonNode();
        final var extractedSourceId = extractSourceId(framedResultNode);

        assertThat(extractedSourceId, hasItems(sourceUri0, sourceUri1));
    }

    private static HttpResponse mockResponseWithStatusCodeAndBody(int statusCode, String body) {
        var firstResponse = mock(HttpResponse.class);
        when(firstResponse.statusCode()).thenReturn(statusCode);
        when(firstResponse.body()).thenReturn(body);
        return firstResponse;
    }

    @Test
    void shouldReturnIndexDocumentWithValidFundingSourceInsertingContextInFundingSource() throws Exception {

        final var publication = randomBookWithConfirmedPublisher();
        final var sourceUri0 = publication.getFundings().get(0).getSource();
        final var sourceUri1 = publication.getFundings().get(1).getSource();
        final var mockUriRetriever = mock(UriRetriever.class);
        var firstResponse = mockResponseWithStatusCodeAndBody(
            HTTP_OK, getPublicationSampleFundingSourceWithoutContext(sourceUri0));
        var secondResponse = mockResponseWithStatusCodeAndBody(
            HTTP_OK, getPublicationSampleFundingSourceWithoutContext(sourceUri1));

        when(mockUriRetriever.fetchResponse(sourceUri0, APPLICATION_JSON_LD.toString()))
            .thenReturn(Optional.of(firstResponse));
        when(mockUriRetriever.fetchResponse(sourceUri1, APPLICATION_JSON_LD.toString()))
            .thenReturn(Optional.of(secondResponse));

        assertHasExpectedFundings(
            sourceUri0,
            sourceUri1,
            fromPublication(mockUriRetriever, publication).asJsonNode());
    }

    @Test
    void shouldExpandMultipleFundingsWithTheSameSource() throws JsonProcessingException {
        final var publication = randomBookWithConfirmedPublisher();
        var source = randomUri();
        publication.setFundings(List.of(fundingWithSource(source), fundingWithSource(source)));
        final var sourceUri0 = publication.getFundings().get(0).getSource();
        final var sourceUri1 = publication.getFundings().get(1).getSource();
        final var mockUriRetriever = mock(UriRetriever.class);
        var firstResponse = mockResponseWithStatusCodeAndBody(
            HTTP_OK, getPublicationSampleFundingSourceWithoutContext(sourceUri0));
        var secondResponse = mockResponseWithStatusCodeAndBody(
            HTTP_OK, getPublicationSampleFundingSourceWithoutContext(sourceUri1));

        when(mockUriRetriever.fetchResponse(sourceUri0, APPLICATION_JSON_LD.toString()))
            .thenReturn(Optional.of(firstResponse));
        when(mockUriRetriever.fetchResponse(sourceUri1, APPLICATION_JSON_LD.toString()))
            .thenReturn(Optional.of(secondResponse));

        var expandedResource = fromPublication(mockUriRetriever, publication).asJsonNode();

        assertTrue(expandedResource.at(JsonPointer.compile("/fundings/1/source")).has("id"));
        assertTrue(expandedResource.at(JsonPointer.compile("/fundings/0/source")).has("id"));
    }

    private static Funding fundingWithSource(URI source) {
        return new FundingBuilder().withId(randomUri()).withSource(source).build();
    }

    @Test
    void shouldReturnIndexDocumentWithValidExpandedFundingSourceWhenFetchFundingReturnNotFound()
        throws JsonProcessingException {
        final var publication = randomBookWithConfirmedPublisher();
        final var mockUriRetriever = mock(UriRetriever.class);
        var httpResponse = mockResponseWithStatusCodeAndBody(HTTP_NOT_FOUND, randomString());
        when(mockUriRetriever.fetchResponse(any(), anyString())).thenReturn(Optional.of(httpResponse));
        var expandedResource = fromPublication(mockUriRetriever, publication).asJsonNode();

        assertTrue(expandedResource.at(JsonPointer.compile("/fundings/1/source")).has("id"));
        assertTrue(expandedResource.at(JsonPointer.compile("/fundings/0/source")).has("id"));
    }

    @Test
    void shouldNotCreateTopLevelOrgForBlankNodes() throws Exception {

        final Publication publication = randomBookWithConfirmedPublisher();
        final URI seriesUri = extractSeriesId(publication);
        final URI publisherUri = extractPublisherId(publication);
        final String publisherName = randomString();
        final String seriesName = randomString();

        final UriRetriever mockUriRetriever = mock(UriRetriever.class);
        addPublicationChannelPublisherToMockUriRetriever(mockUriRetriever, seriesUri, seriesName, publisherUri,
                                                         publisherName);

        ((Organization) publication.getEntityDescription().getContributors().get(0).getAffiliations().get(0))
            .setId(null);

        ObjectNode framedResultNode = fromPublication(mockUriRetriever, publication).asJsonNode();

        var affiliations = framedResultNode.findValues("affiliations").get(0);
        affiliations.forEach(
            aff -> {
                if (!aff.has("id")) {
                    assertThat(aff.has("topLevelOrganization"), is(equalTo(false)));
                }
            }
        );
    }

    @ParameterizedTest(name = "should return properly framed document with id based on Id-namespace and resource "
                              + "identifier. Instance type:{0}")
    @MethodSource("publicationInstanceProvider")
    void shouldReturnDocumentWithIdBasedOnIdNameSpaceAndResourceIdentifier(Class<?> publicationInstance)
        throws JsonProcessingException {

        var publication = PublicationGenerator.randomPublication(publicationInstance);
        var indexDocument = fromPublication(uriRetriever, publication);
        var json = (ObjectNode) objectMapper.readTree(indexDocument.toJsonString());
        var expectedUri =
            UriWrapper.fromUri(HOST_URI).addChild(publication.getIdentifier().toString()).getUri();
        var actualUri = URI.create(json.at(PublicationJsonPointers.ID_JSON_PTR).textValue());
        assertThat(actualUri, is(equalTo(expectedUri)));
    }

    @Test
    void shouldReturnIndexDocumentContainingConfirmedSeriesUriFromNsdPublicationChannels()
        throws JsonProcessingException {

        Publication publication = randomBookWithConfirmedPublisher();
        Book book =
            extractBook(publication);
        Series series = (Series) book.getSeries();
        URI expectedSeriesUri = series.getId();
        ExpandedResource actualDocument = fromPublication(uriRetriever, publication);
        assertThat(actualDocument.getPublicationContextUris(), hasItems(expectedSeriesUri));
    }

    @Test
    void shouldReturnIndexDocumentContainingReturnsJournalUriFromNsdPublicationChannels()
        throws JsonProcessingException {

        Publication publication = randomJournalArticleWithConfirmedJournal();
        Journal journal =
            (Journal) publication.getEntityDescription().getReference().getPublicationContext();
        URI expectedJournalUri = journal.getId();
        ExpandedResource actualDocument = fromPublication(uriRetriever, publication);
        assertThat(actualDocument.getPublicationContextUris(), contains(expectedJournalUri));
    }

    @Test
    void shouldReturnIndexDocumentWithConfirmedSeriesIdWhenBookIsPartOfSeriesFoundInNsd()
        throws JsonProcessingException {

        Publication publication = PublicationGenerator.randomPublication(BookMonograph.class);
        ExpandedResource actualDocument = fromPublication(uriRetriever, publication);
        Book book = extractBook(publication);
        Series confirmedSeries = (Series) book.getSeries();
        URI expectedSeriesId = confirmedSeries.getId();
        Publisher publisher = (Publisher) book.getPublisher();
        URI expectedPublisherId = publisher.getId();
        assertThat(actualDocument.getPublicationContextUris(),
                   containsInAnyOrder(expectedSeriesId, expectedPublisherId));
    }

    @Test
    void shouldReturnIndexDocumentWithConfirmedJournalIdWhenPublicationIsPublishedInConfirmedJournal()
        throws JsonProcessingException {

        Publication publication = PublicationGenerator.randomPublication(FeatureArticle.class);
        ExpandedResource actualDocument = fromPublication(uriRetriever, publication);
        Journal journal = extractJournal(publication);
        URI expectedJournalId = journal.getId();
        assertThat(actualDocument.getPublicationContextUris(), containsInAnyOrder(expectedJournalId));
    }

    @Test
    void shouldReturnExpandedResourceWithAnthologyPublicationChannelUrisWhenPublicationIsAcademicChapter()
        throws JsonProcessingException {
        var bookAnthology = PublicationGenerator.randomPublication(BookAnthology.class);
        var academicChapter = getAcademicChapterPartOfAnthology(bookAnthology);
        mockUriRetrieverPublicationResponse(bookAnthology);
        var expectedPublicationChannelIds = getPublicationContextUris(extractBook(bookAnthology));

        var expandedResource = fromPublication(uriRetriever, academicChapter);
        var expandedResourceJsonNode = expandedResource.asJsonNode();
        var actualPublicationChannelUris = extractActualPublicationChannelUris(expandedResourceJsonNode);
        assertThat(actualPublicationChannelUris,
                   containsInAnyOrder(expectedPublicationChannelIds.toArray()));
    }

    @Test
    void shouldReturnExpandedResourceWithAnthologyPublicationChannelLevelWhenPublicationIsAcademicChapter()
        throws IOException {
        var bookAnthology = PublicationGenerator.randomPublication(BookAnthology.class);
        var academicChapter = getAcademicChapterPartOfAnthology(bookAnthology);
        mockUriRetrieverResponses(bookAnthology);

        var expandedResource = fromPublication(uriRetriever, academicChapter);
        var expandedResourceJsonNode = expandedResource.asJsonNode();
        var actualSeriesLevel = expandedResourceJsonNode.at(SERIES_LEVEL_JSON_PTR).textValue();
        var actualPublisherLevel = expandedResourceJsonNode.at(PUBLISHER_LEVEL_JSON_PTR).textValue();
        assertThat(actualSeriesLevel, is(not(nullValue())));
        assertThat(actualPublisherLevel, is(not(nullValue())));
    }

    @Test
    void shouldNotFailWhenThereIsNoPublicationContext() throws JsonProcessingException {

        Publication publication = PublicationGenerator.randomPublication(BookMonograph.class);
        publication.getEntityDescription().getReference().setPublicationContext(null);
        assertThat(ExpandedResource.fromPublication(uriRetriever, publication), is(not(nullValue())));
    }

    @Test
    void shouldNotFailWhenThereIsNoPublicationInstance() throws JsonProcessingException {

        Publication publication = PublicationGenerator.randomPublication(BookMonograph.class);
        publication.getEntityDescription().getReference().setPublicationInstance(null);
        assertThat(ExpandedResource.fromPublication(uriRetriever, publication), is(not(nullValue())));
    }

    @Test
    void shouldNotFailWhenThereIsNoMainTitle() throws JsonProcessingException {

        Publication publication = PublicationGenerator.randomPublication(BookMonograph.class);
        publication.getEntityDescription().setMainTitle(null);
        assertThat(ExpandedResource.fromPublication(uriRetriever, publication), is(not(nullValue())));
    }

    @Test
    void shouldNotFailWhenInputContainsAffiliationsThatAreIncomplete() {
        var publication = createPublicationWithEmptyAffiliations();
        assertDoesNotThrow(() -> ExpandedResource.fromPublication(uriRetriever, publication));
    }

    @Test
    void shouldUseApiVersionWhenLookingUpOrganizations() throws JsonProcessingException {
        var publication = PublicationGenerator.randomPublication();
        var orgIds = getOrgIdsForContributorAffiliations(publication);
        ExpandedResource.fromPublication(uriRetriever, publication);
        orgIds.forEach(
            orgId -> verify(uriRetriever, times(1)).getRawContent(orgId, "application/ld+json; version=2023-05-26"));
    }

    @Test
    void shouldNotExpandDoiWhenIdUsedElsewhere() throws IOException {
        var bookAnthology = bookAnthologyWithDoiReferencedInAssociatedLink();
        mockUriRetrieverResponses(bookAnthology);

        var expandedResource = ExpandedResource.fromPublication(uriRetriever, bookAnthology);

        var actualDoi = expandedResource.getAllFields().get("doi");

        assertThat(actualDoi, allOf(Matchers.instanceOf(String.class),
                                    is((equalTo(bookAnthology.getDoi().toString())))));
    }

    @Test
    void shouldNotExpandHandleWhenIdUsedElsewhere() throws IOException {
        var bookAnthology = bookAnthologyWithHandleReferencedInAssociatedLink();
        mockUriRetrieverResponses(bookAnthology);

        var expandedResource = ExpandedResource.fromPublication(uriRetriever, bookAnthology);

        var actualHandle = expandedResource.getAllFields().get("handle");

        assertThat(actualHandle, allOf(Matchers.instanceOf(String.class),
                                       is((equalTo(bookAnthology.getHandle().toString())))));
    }

    @Test
    void shouldNotExpandLinkWhenIdUsedElsewhere() throws IOException {
        var bookAnthology = bookAnthologyWithLinkReferencedInAssociatedLink();
        mockUriRetrieverResponses(bookAnthology);

        var expandedResource = ExpandedResource.fromPublication(uriRetriever, bookAnthology);

        var actualLink = expandedResource.getAllFields().get("link");

        assertThat(actualLink, allOf(Matchers.instanceOf(String.class),
                                     is((equalTo(bookAnthology.getLink().toString())))));
    }

    private static JsonNode getTopLevel(ArrayNode topLevelNodes, String topLevelOrgId) {
        return StreamSupport.stream(topLevelNodes.spliterator(), false)
                   .filter(node -> node.at(JSON_PTR_ID).textValue().contains(topLevelOrgId))
                   .findFirst()
                   .orElse(null);
    }

    private static void mockOrganizationResponse(URI affiliationToBeExpanded, UriRetriever mockUriRetriever) {
        var mockedCristinResponse = stringFromResources(Path.of(CRISTIN_ORG_JSON)).replace(
            "__REPLACE_AFFILIATION_ID__", affiliationToBeExpanded.toString());
        mockGetRawContentResponse(mockUriRetriever, affiliationToBeExpanded, mockedCristinResponse);
    }

    private static void mockOrganizationResponseForTopLevelAffiliation(URI affiliationToBeExpanded,
                                                                       UriRetriever mockUriRetriever) {
        var mockedCristinResponse = stringFromResources(Path.of(TOP_LEVEL_CRISTIN_ORG_JSON)).replace(
            "__REPLACE_AFFILIATION_ID__", affiliationToBeExpanded.toString());
        mockGetRawContentResponse(mockUriRetriever, affiliationToBeExpanded, mockedCristinResponse);
    }

    private static void mockCristinOrganizationRawContentResponse(UriRetriever mockUriRetriever,
                                                                  Publication publication) {
        publication.getEntityDescription()
            .getContributors()
            .stream()
            .flatMap(contributor -> contributor.getAffiliations().stream())
            .filter(Organization.class::isInstance)
            .map(Organization.class::cast)
            .map(Organization::getId)
            .forEach(id ->
                         mockGetRawContentResponse(mockUriRetriever, id,
                                                   getCristinResponseWithCountryCodeForOrganization(id.toString(),
                                                                                                    COUNTRY_CODE_NO)));
    }

    private static void assertHasExpectedFundings(URI sourceUri0, URI sourceUri1, ObjectNode framedResultNode) {
        final var extractedSourceId = extractSourceId(framedResultNode);
        final var fundings = framedResultNode.at(PublicationJsonPointers.FUNDING_SOURCE_POINTER);
        fundings.forEach(ExpandedResourceTest::assertHasLabels);
        assertThat(extractedSourceId, hasItems(sourceUri0, sourceUri1));
    }

    private static void assertHasLabels(JsonNode funding) {
        assertThat(funding.get("labels"), is(not(nullValue())));
    }

    private static Set<URI> extractSourceId(ObjectNode framedResultNode) {
        return framedResultNode
                   .at(PublicationJsonPointers.FUNDING_SOURCE_POINTER)
                   .findValues("source").stream()
                   .flatMap(node -> node.findValues("id").stream())
                   .map(JsonNode::textValue)
                   .map(URI::create)
                   .collect(Collectors.toSet());
    }

    private static void addPublisherToMockUriRetriever(UriRetriever uriRetriever, URI publisherId) throws IOException {
        var publicationChannelSamplePublisher = getPublicationChannelSamplePublisher(publisherId, randomString());
        mockGetRawContentResponse(uriRetriever, publisherId, publicationChannelSamplePublisher);
    }

    private static void addSeriesToMockUriRetriever(UriRetriever uriRetriever, URI seriesId) throws IOException {

        var publicationChannelSampleSeries = getPublicationChannelSampleSeries(seriesId, randomString());
        mockGetRawContentResponse(uriRetriever, seriesId, publicationChannelSampleSeries);
    }

    private static void mockGetRawContentResponse(UriRetriever uriRetriever, URI uri, String response) {
        when(uriRetriever.getRawContent(eq(uri), any())).thenReturn(Optional.of(response));
    }

    private static List<URI> extractActualPublicationChannelUris(ObjectNode expandedResourceJsonNode) {
        var actualPublisherId = URI.create(expandedResourceJsonNode.at(PUBLISHER_ID_JSON_PTR).textValue());
        var actualSeriesId = URI.create(expandedResourceJsonNode.at(SERIES_ID_JSON_PTR).textValue());
        return List.of(actualPublisherId, actualSeriesId);
    }

    private static Publication getAcademicChapterPartOfAnthology(Publication bookAnthology) {
        var bookAnthologyUri = toPublicationId(bookAnthology.getIdentifier());
        var academicChapter = PublicationGenerator.randomPublication(AcademicChapter.class);
        var anthology = (Anthology) academicChapter.getEntityDescription().getReference().getPublicationContext();
        anthology.setId(bookAnthologyUri);
        return academicChapter;
    }

    private static Contributor createContributorsWithEmptyAffiliations(Contributor contributor) {
        return new Contributor.Builder()
                   .withIdentity(contributor.getIdentity())
                   .withAffiliations(List.of(new Organization()))
                   .withRole(contributor.getRole())
                   .withSequence(contributor.getSequence())
                   .withCorrespondingAuthor(contributor.isCorrespondingAuthor())
                   .build();
    }

    private static void addPublicationChannelPublisherToMockUriRetriever(
        UriRetriever mockUriRetriever, URI journalId, String journalName, URI publisherId, String publisherName)
        throws IOException {

        mockGetRawContentResponse(
            mockUriRetriever,
            journalId,
            getPublicationChannelSampleJournal(journalId, journalName)
        );
        mockGetRawContentResponse(
            mockUriRetriever,
            publisherId,
            getPublicationChannelSamplePublisher(publisherId, publisherName)
        );
    }

    private static String getCristinResponseWithCountryCodeForOrganization(String id, String countryCode) {
        return "{\n"
               + "  \"@context\": \"https://bibsysdev.github.io/src/organization-context.json\",\n"
               + "  \"type\": \"Organization\",\n"
               + "  \"id\": \"" + id + "\",\n"
               + "  \"labels\": {\n"
               + "    \"en\": \"Department of Something\",\n"
               + "    \"nb\": \"Institutt for Noe\"\n"
               + "  },\n"
               + "  \"country\": \"" + countryCode + "\"\n"
               + "}";
    }

    private static String getCristinResponseWithNestedPartOfsForOrganization(String id, String parentOrgId,
                                                                             String topOrgId) {
        return "{\n"
               + "  \"@context\" : \"https://bibsysdev.github.io/src/organization-context.json\",\n"
               + "  \"type\" : \"Organization\",\n"
               + "  \"id\": \"" + id + "\",\n"
               + "  \"labels\" : {\n"
               + "    \"en\" : \"Office of International Relations\",\n"
               + "    \"nb\" : \"Internasjonal seksjon\"\n"
               + "  },\n"
               + "  \"acronym\" : \"UTD-ST-INT\",\n"
               + "  \"country\" : \"NO\",\n"
               + "  \"partOf\" : [ {\n"
               + "    \"type\" : \"Organization\",\n"
               + "    \"id\": \"" + parentOrgId + "\",\n"
               + "    \"partOf\" : [ {\n"
               + "      \"type\" : \"Organization\",\n"
               + "      \"id\": \"" + topOrgId + "\"\n"
               + "    } ]\n"
               + "  } ]\n"
               + "}";
    }

    private static String getPublicationSampleFundingSource(URI sourceId) {
        return "{\n"
               + "  \"@context\": {\n"
               + "    \"@vocab\": \"https://nva.sikt.no/ontology/publication#\",\n"
               + "    \"id\": \"@id\",\n"
               + "    \"type\": \"@type\",\n"
               + "    \"name\": {\n"
               + "      \"@id\": \"label\",\n"
               + "      \"@container\": \"@language\"\n"
               + "    }\n"
               + "  },\n"
               + "  \"type\" : \"FundingSource\",\n"
               + "  \"id\" : \"" + sourceId + "\",\n"
               + "  \"identifier\" : \"NFR\",\n"
               + "  \"name\" : {\n"
               + "    \"en\" : \"Research Council of Norway (RCN)\",\n"
               + "    \"nb\" : \"Norges forskningsråd\"\n"
               + "  }\n"
               + "}";
    }

    private static String getPublicationSampleFundingSourceWithoutContext(URI sourceId) {
        return "{\n"
               + "  \"type\" : \"FundingSource\",\n"
               + "  \"id\" : \"" + sourceId + "\",\n"
               + "  \"identifier\" : \"NFR\",\n"
               + "  \"name\" : {\n"
               + "    \"en\" : \"Research Council of Norway (RCN)\",\n"
               + "    \"nb\" : \"Norges forskningsråd\"\n"
               + "  }\n"
               + "}";
    }

    private static Stream<Class<?>> publicationInstanceProvider() {
        return PublicationInstanceBuilder.listPublicationInstanceTypes().stream();
    }

    private static URI toPublicationId(SortableIdentifier identifier) {
        return UriWrapper.fromUri(ID_NAMESPACE)
                   .addChild(identifier.toString())
                   .getUri();
    }

    private static Set<URI> getOrgIdsForContributorAffiliations(Publication publication) {
        return publication
                   .getEntityDescription()
                   .getContributors()
                   .stream()
                   .map(Contributor::getAffiliations)
                   .map(ExpandedResourceTest::getOrgIds)
                   .flatMap(Set::stream).collect(Collectors.toSet());
    }

    private static Set<URI> getOrgIds(List<Corporation> organizations) {
        return organizations.stream()
                   .filter(Organization.class::isInstance)
                   .map(Organization.class::cast)
                   .map(Organization::getId).collect(Collectors.toSet());
    }

    private static JsonNode findDeepestNestedSubUnit(JsonNode jsonNode) {
        if (isNull(jsonNode) || isBlankJsonNode(jsonNode)) {
            return null;
        }
        while (hasPartHasContent(jsonNode)) {
            jsonNode = jsonNode.at(JSON_PTR_HAS_PART);
        }
        return jsonNode;
    }

    private static boolean hasPartHasContent(JsonNode jsonNode) {
        return !jsonNode.at(JSON_PTR_HAS_PART).isEmpty() || !isBlankJsonNode(jsonNode.at(JSON_PTR_HAS_PART));
    }

    private static boolean isBlankJsonNode(JsonNode jsonNode) {
        return jsonNode.isMissingNode();
    }

    private Publication bookAnthologyWithDoiReferencedInAssociatedLink() {
        var doi = randomDoi();
        return PublicationGenerator.randomPublication(BookAnthology.class)
                   .copy()
                   .withDoi(doi)
                   .withAssociatedArtifacts(List.of(new AssociatedLink(doi, null, null)))
                   .build();
    }

    private Publication bookAnthologyWithHandleReferencedInAssociatedLink() {
        var handle = randomUri();
        return PublicationGenerator.randomPublication(BookAnthology.class)
                   .copy()
                   .withHandle(handle)
                   .withAssociatedArtifacts(List.of(new AssociatedLink(handle, null, null)))
                   .build();
    }

    private Publication bookAnthologyWithLinkReferencedInAssociatedLink() {
        var link = randomUri();
        return PublicationGenerator.randomPublication(BookAnthology.class)
                   .copy()
                   .withLink(link)
                   .withAssociatedArtifacts(List.of(new AssociatedLink(link, null, null)))
                   .build();
    }

    private void mockUriRetrieverResponses(Publication bookAnthology) throws IOException {
        mockUriRetrieverPublicationChannelResponses(bookAnthology);
        mockUriRetrieverPublicationResponse(bookAnthology);
    }

    private void mockUriRetrieverPublicationChannelResponses(Publication bookAnthology) throws IOException {
        var seriesId = extractSeriesId(bookAnthology);
        var publisherId = extractPublisherId(bookAnthology);
        addSeriesToMockUriRetriever(uriRetriever, seriesId);
        addPublisherToMockUriRetriever(uriRetriever, publisherId);
    }

    private void mockUriRetrieverPublicationResponse(Publication publication) throws JsonProcessingException {
        var publicationId = toPublicationId(publication.getIdentifier());
        var publicationResponse = PublicationResponse.fromPublication(publication);
        publicationResponse.setId(publicationId);
        mockGetRawContentResponse(uriRetriever, publicationId, objectMapper.writeValueAsString(publicationResponse));
    }

    private List<URI> getPublicationContextUris(Book book) {
        var confirmedSeries = (Series) book.getSeries();
        var expectedSeriesId = confirmedSeries.getId();
        var publisher = (Publisher) book.getPublisher();
        var expectedPublisherId = publisher.getId();
        return List.of(expectedSeriesId, expectedPublisherId);
    }

    private Publication createPublicationWithEmptyAffiliations() {
        var publication = PublicationGenerator.randomPublication();
        publication.setStatus(PUBLISHED);
        var entityDescription = publication.getEntityDescription();
        var contributors = entityDescription.getContributors().stream()
                               .map(ExpandedResourceTest::createContributorsWithEmptyAffiliations)
                               .collect(Collectors.toList());
        entityDescription.setContributors(contributors);
        return publication;
    }

    private Journal extractJournal(Publication publication) {
        return (Journal) publication.getEntityDescription().getReference().getPublicationContext();
    }

    private URI extractPublisherId(Publication publication) {
        Book book = extractBook(publication);
        Publisher publisher = extractPublisher(book);
        return publisher.getId();
    }

    private Publisher extractPublisher(Book book) {
        return (Publisher) book.getPublisher();
    }

    private List<URI> extractAffiliationsUris(Publication publication) {
        return publication.getEntityDescription().getContributors()
                   .stream()
                   .flatMap(contributor -> contributor.getAffiliations().stream()
                                               .filter(Organization.class::isInstance)
                                               .map(Organization.class::cast)
                                               .map(Organization::getId))
                   .collect(Collectors.toList());
    }

    private URI extractSeriesId(Publication publication) {
        Book book = extractBook(publication);
        Series confirmedSeries = (Series) book.getSeries();
        return confirmedSeries.getId();
    }

    private Book extractBook(Publication publication) {
        return (Book) publication.getEntityDescription().getReference().getPublicationContext();
    }

    private Publication randomBookWithConfirmedPublisher() {
        return PublicationGenerator.randomPublication(BookMonograph.class);
    }

    private Publication randomJournalArticleWithConfirmedJournal() {
        return PublicationGenerator.randomPublication(FeatureArticle.class);
    }

    private Publication randomPublicationWithoutEntityDescription() {
        var publication = PublicationGenerator.randomPublication(BookMonograph.class);
        publication.setEntityDescription(null);
        return publication;
    }

    private Contributor randomContributor() {
        return new Contributor.Builder()
                   .withIdentity(new Identity.Builder().withName(randomString()).build())
                   .withRole(new RoleType(Role.ACTOR))
                   .withSequence(randomInteger(10000))
                   .withAffiliations(List.of(randomOrganization()))
                   .build();
    }

    private Publication randomBookWithManyContributors() {
        var publication = PublicationGenerator.randomPublication(BookMonograph.class);
        var contributions = IntStream
                                .rangeClosed(1, 10)
                                .mapToObj(i -> randomContributor())
                                .collect(Collectors.toList());
        publication.getEntityDescription().setContributors(contributions);
        return publication;
    }
}
