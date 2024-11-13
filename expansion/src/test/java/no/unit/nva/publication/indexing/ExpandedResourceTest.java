package no.unit.nva.publication.indexing;

import static java.util.Objects.isNull;
import static java.util.stream.StreamSupport.stream;
import static no.unit.nva.expansion.ExpansionConfig.objectMapper;
import static no.unit.nva.expansion.model.ExpandedResource.fromPublication;
import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.model.testing.PublicationGenerator.randomDoi;
import static no.unit.nva.model.testing.PublicationGenerator.randomOrganization;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.publication.uriretriever.FakeUriResponse.HARD_CODED_TOP_LEVEL_ORG_URI;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.apigateway.MediaTypes.APPLICATION_JSON_LD;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsIterableContaining.hasItems;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.google.common.net.MediaType;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import no.unit.nva.expansion.model.ExpandedResource;
import no.unit.nva.expansion.utils.PublicationJsonPointers;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.associatedartifacts.AssociatedLink;
import no.unit.nva.model.contexttypes.Anthology;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.contexttypes.Journal;
import no.unit.nva.model.contexttypes.Publisher;
import no.unit.nva.model.contexttypes.Series;
import no.unit.nva.model.funding.ConfirmedFunding;
import no.unit.nva.model.instancetypes.book.AcademicMonograph;
import no.unit.nva.model.instancetypes.book.BookAnthology;
import no.unit.nva.model.instancetypes.book.BookMonograph;
import no.unit.nva.model.instancetypes.book.Encyclopedia;
import no.unit.nva.model.instancetypes.book.ExhibitionCatalog;
import no.unit.nva.model.instancetypes.book.NonFictionMonograph;
import no.unit.nva.model.instancetypes.book.PopularScienceMonograph;
import no.unit.nva.model.instancetypes.book.Textbook;
import no.unit.nva.model.instancetypes.chapter.AcademicChapter;
import no.unit.nva.model.instancetypes.chapter.ChapterConferenceAbstract;
import no.unit.nva.model.instancetypes.chapter.ChapterInReport;
import no.unit.nva.model.instancetypes.chapter.EncyclopediaChapter;
import no.unit.nva.model.instancetypes.chapter.ExhibitionCatalogChapter;
import no.unit.nva.model.instancetypes.chapter.Introduction;
import no.unit.nva.model.instancetypes.chapter.NonFictionChapter;
import no.unit.nva.model.instancetypes.chapter.PopularScienceChapter;
import no.unit.nva.model.instancetypes.chapter.TextbookChapter;
import no.unit.nva.model.instancetypes.journal.AcademicArticle;
import no.unit.nva.model.instancetypes.journal.FeatureArticle;
import no.unit.nva.model.instancetypes.report.ConferenceReport;
import no.unit.nva.model.instancetypes.report.ReportBasic;
import no.unit.nva.model.instancetypes.report.ReportBookOfAbstract;
import no.unit.nva.model.instancetypes.report.ReportPolicy;
import no.unit.nva.model.instancetypes.report.ReportResearch;
import no.unit.nva.model.instancetypes.report.ReportWorkingPaper;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.model.testing.PublicationInstanceBuilder;
import no.unit.nva.publication.PublicationServiceConfig;
import no.unit.nva.publication.indexing.verification.FundingResult;
import no.unit.nva.publication.indexing.verification.PublisherResult;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.uriretriever.FakeUriResponse;
import no.unit.nva.publication.uriretriever.FakeUriRetriever;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.paths.UriWrapper;
import nva.commons.logutils.LogUtils;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ExpandedResourceTest extends ResourcesLocalTest {

    public static final String JSON_PTR_TOP_LEVEL_ORGS = "/topLevelOrganizations";
    public static final String JSON_CONTRIBUTOR_ORGANIZATIONS = "/contributorOrganizations";
    public static final String JSON_PTR_ID = "/id";
    public static final String JSON_PTR_HAS_PART = "/hasPart";
    private static final String SERIES_LEVEL_JSON_PTR =
        "/entityDescription/reference/publicationContext/entityDescription/reference/publicationContext"
        + "/series/scientificValue";
    private static final String PUBLISHER_LEVEL_JSON_PTR = "/entityDescription/reference/publicationContext"
                                                           + "/entityDescription/reference/publicationContext"
                                                           + "/publisher/scientificValue";

    private static final String PUBLISHER_ID_JSON_PTR =
        "/entityDescription/reference/publicationContext/entityDescription/reference/publicationContext"
        + "/publisher/id";
    private static final String SERIES_ID_JSON_PTR =
        "/entityDescription/reference/publicationContext/entityDescription/reference/publicationContext"
        + "/series/id";
    private static final URI HOST_URI = PublicationServiceConfig.PUBLICATION_HOST_URI;
    private FakeUriRetriever fakeUriRetriever;
    private ResourceService resourceService;

    @BeforeEach
    void setup() {
        super.init();
        this.fakeUriRetriever = FakeUriRetriever.newInstance();
        resourceService = getResourceServiceBuilder().build();
    }

    @Test
    void shouldReturnIndexDocumentWithValidReferenceData() throws Exception {

        final Publication publication = randomBookWithConfirmedPublisher();
        FakeUriResponse.setupFakeForType(publication, fakeUriRetriever, resourceService);
        final ExpandedResource indexDocument = fromPublication(fakeUriRetriever, resourceService, publication);
        final JsonNode framedResultNode = indexDocument.asJsonNode();

        var publisherResultString = framedResultNode.at(PublicationJsonPointers.PUBLISHER_JSON_PTR).toString();
        var publisher =
            attempt(() -> objectMapper.readValue(publisherResultString, PublisherResult.class)).orElseThrow();

        assertTrue(publisher.isNotEmpty());
    }

    @Test
    void shouldReturnIndexDocumentWithContributorsPreviewAndCount() throws Exception {
        var publication = randomPublication(AcademicArticle.class);

        var contributor1 = contributorWithSequence(1);
        var contributor2 = contributorWithSequence(2);
        var contributor3 = contributorWithSequence(3);
        var contributor4 = contributorWithSequence(4);

        var contributors = Arrays.asList(contributor1, contributor2, contributor3, contributor4);
        Collections.shuffle(contributors);
        var expectedContributors = List.of(contributor1, contributor2, contributor3, contributor4);

        publication.getEntityDescription().setContributors(contributors);

        FakeUriResponse.setupFakeForType(publication, fakeUriRetriever, resourceService);
        var indexDocument = fromPublication(fakeUriRetriever, resourceService, publication);
        var framedResultNode = indexDocument.asJsonNode();

        var contributorsPreviewNode = (ArrayNode) framedResultNode.at("/entityDescription")
                                                      .at("/contributorsPreview");

        var actualContributorsPreview = stream(contributorsPreviewNode.spliterator(), false)
                                            .map(node -> objectMapper.convertValue(node, Contributor.class))
                                            .toList();

        assertThat(actualContributorsPreview, is(equalTo(expectedContributors)));
    }

    @Test
    void shouldAllowAndLogMissingChannel() {
        final var logger = LogUtils.getTestingAppenderForRootLogger();
        var publication = randomPublication(AcademicArticle.class);
        FakeUriResponse.setupFakeForType(publication, fakeUriRetriever, resourceService);
        var channel = ((Journal)publication.getEntityDescription().getReference().getPublicationContext()).getId();
        fakeUriRetriever.registerResponse(channel, 404, APPLICATION_JSON_LD, "");
        assertDoesNotThrow(() -> fromPublication(fakeUriRetriever, resourceService, publication));
        assertThat(logger.getMessages(),
                   containsString("Request for publication channel <%s> returned 404".formatted(channel)));
    }

    @Test
    void shouldReturnIndexDocumentWithContributorsPreviewWithNoMoreThan10Contributors() throws Exception {
        var publication = randomPublication();

        var contributors = IntStream.range(0, 20)
                               .mapToObj(i -> contributorWithSequence(randomInteger()))
                               .toList();

        publication.getEntityDescription().setContributors(contributors);

        FakeUriResponse.setupFakeForType(publication, fakeUriRetriever, resourceService);
        var indexDocument = fromPublication(fakeUriRetriever, resourceService, publication);
        var framedResultNode = indexDocument.asJsonNode();

        var contributorsCountNode = (IntNode) framedResultNode.at("/entityDescription")
                                                  .at("/contributorsCount");

        var contributorsPreviewNode = (ArrayNode) framedResultNode.at("/entityDescription")
                                                      .at("/contributorsPreview");

        var actualContributorsPreview = stream(contributorsPreviewNode.spliterator(), false)
                                            .map(node -> objectMapper.convertValue(node, Contributor.class))
                                            .toList();

        assertThat(actualContributorsPreview.size(), is(equalTo(10)));
        assertThat(contributorsCountNode.intValue(), is(equalTo(20)));
    }

    @Test
    void shouldReturnIndexDocumentWithTopLevelOrganizationsWithTreeToRelevantAffiliation() throws Exception {
        final var publication = randomPublication(AcademicArticle.class);
        final var affiliationToBeExpanded = FakeUriResponse.HARD_CODED_LEVEL_3_ORG_URI;
        var contributorAffiliatedToTopLevel = publication.getEntityDescription().getContributors().getFirst().copy()
                                                  .withAffiliations(
                                                      List.of(Organization.fromUri(affiliationToBeExpanded))).build();

        publication.getEntityDescription().setContributors(List.of(contributorAffiliatedToTopLevel));
        FakeUriResponse.setupFakeForType(publication, fakeUriRetriever, resourceService);

        var framedResultNode = fromPublication(fakeUriRetriever, resourceService, publication).asJsonNode();
        var topLevelNodes = (ArrayNode) framedResultNode.at(JSON_PTR_TOP_LEVEL_ORGS);
        var topLevelForExpandedAffiliation = getTopLevel(topLevelNodes, HARD_CODED_TOP_LEVEL_ORG_URI.toString());

        assertThat(findDeepestNestedSubUnit(topLevelForExpandedAffiliation).at(JSON_PTR_ID).textValue(),
                   is(equalTo(affiliationToBeExpanded.toString())));
    }

    @Test
    void shouldReturnIndexDocumentWithContributorsOrganizations() throws Exception {
        var publication = randomPublication(AcademicArticle.class);
        var contributor1org = Organization.fromUri(FakeUriResponse.HARD_CODED_LEVEL_3_ORG_URI);
        var contributor1parentOrg = Organization.fromUri(FakeUriResponse.HARD_CODED_LEVEL_2_ORG_URI);
        var contributor2org = Organization.fromUri(FakeUriResponse.constructCristinOrgUri("123.1.2.0"));
        var topLevelOrg = Organization.fromUri(HARD_CODED_TOP_LEVEL_ORG_URI);

        var contributor1 = contributorWithOneAffiliation(contributor1org);
        var contributor2 = contributorWithOneAffiliation(contributor2org);
        publication.getEntityDescription().setContributors(List.of(contributor1, contributor2));
        FakeUriResponse.setupFakeForType(publication, fakeUriRetriever, resourceService);

        var framedResultNode = fromPublication(fakeUriRetriever, resourceService, publication).asJsonNode();
        var contributorOrganizationsNode = framedResultNode.at(JSON_CONTRIBUTOR_ORGANIZATIONS);
        var actualOrganizations = Lists.newArrayList(contributorOrganizationsNode.elements())
                                      .stream()
                                      .map(JsonNode::textValue)
                                      .toList();

        var expectedOrganizations = Stream.of(contributor1org,
                                              contributor1parentOrg,
                                              topLevelOrg,
                                              contributor2org).map(Organization::getId).map(URI::toString).toArray();

        assertThat(actualOrganizations, containsInAnyOrder(expectedOrganizations));
    }

    @Test
    void shouldReturnIndexDocumentWithTopLevelOrganizationWithoutHasPartsIfContributorAffiliatedWithTopLevel()
        throws Exception {
        final var publication = randomBookWithConfirmedPublisher();
        final var affiliationToBeExpanded = HARD_CODED_TOP_LEVEL_ORG_URI;
        var contributorAffiliatedToTopLevel = publication.getEntityDescription().getContributors().getFirst().copy()
                                                  .withAffiliations(
                                                      List.of(Organization.fromUri(affiliationToBeExpanded))).build();

        publication.getEntityDescription().setContributors(List.of(contributorAffiliatedToTopLevel));
        FakeUriResponse.setupFakeForType(publication, fakeUriRetriever, resourceService);

        var framedResultNode = fromPublication(fakeUriRetriever, resourceService, publication).asJsonNode();
        var topLevelNodes = (ArrayNode) framedResultNode.at(JSON_PTR_TOP_LEVEL_ORGS);
        var topLevelForExpandedAffiliation = getTopLevel(topLevelNodes, affiliationToBeExpanded.toString());

        var deepestNestedSubUnit = findDeepestNestedSubUnit(topLevelForExpandedAffiliation);
        assertThat(deepestNestedSubUnit.at(JSON_PTR_ID).textValue(), is(equalTo(affiliationToBeExpanded.toString())));
    }

    @Test
    void shouldReturnIndexDocumentWithSortedContributorsByTheirSequence() throws Exception {
        final var publication = randomBookWithManyContributors();
        FakeUriResponse.setupFakeForType(publication, fakeUriRetriever, resourceService);

        var framedResultNode = fromPublication(fakeUriRetriever, resourceService, publication).asJsonNode();
        var contributorsJson = framedResultNode.at("/entityDescription/contributors");

        List<Contributor> contributors = objectMapper.convertValue(contributorsJson,
                                                                   objectMapper.getTypeFactory()
                                                                       .constructCollectionType(List.class,
                                                                                                Contributor.class));
        var sortedContributors = contributors.stream().sorted(Comparator.comparing(Contributor::getSequence)).toList();

        assertThat(contributors, is(equalTo(sortedContributors)));
    }

    @Test
    void shouldReturnIndexDocumentWithIdWhenThereIsNoEntityDescription() throws Exception {
        final var publication = randomPublicationWithoutEntityDescription();
        FakeUriResponse.setupFakeForType(publication, fakeUriRetriever, resourceService);

        var framedResultNode = fromPublication(fakeUriRetriever, resourceService, publication).asJsonNode();
        var id = URI.create(framedResultNode.at(PublicationJsonPointers.ID_JSON_PTR).textValue());

        assertThat(id, is(not(nullValue())));
    }

    @Test
    @Deprecated
    void shouldReturnIndexDocumentWithValidFundingSource() throws Exception {

        final var publication = randomBookWithConfirmedPublisher();
        FakeUriResponse.setupFakeForType(publication, fakeUriRetriever, resourceService);

        final var framedResultNode = fromPublication(fakeUriRetriever, resourceService, publication).asJsonNode();
        final var extractedSourceId = extractSourceId(framedResultNode);

        // TODO: Better test here?
        assertThat(extractedSourceId.size(), is(equalTo(2)));
    }

    @Test
    void shouldReturnIndexDocumentWithValidFundingSourceInsertingContextInFundingSource() throws Exception {

        final var publication = randomBookWithConfirmedPublisher();
        final var sourceUri0 = publication.getFundings().get(0).getSource();
        final var sourceUri1 = publication.getFundings().get(1).getSource();
        FakeUriResponse.setupFakeForType(publication, fakeUriRetriever, resourceService);

        assertHasExpectedFundings(sourceUri0, sourceUri1, fromPublication(fakeUriRetriever, resourceService, publication).asJsonNode());
    }

    @Test
    void shouldExpandMultipleFundingsWithTheSameSource() throws JsonProcessingException {
        final var publication = randomBookWithConfirmedPublisher();
        FakeUriResponse.setupFakeForType(publication, fakeUriRetriever, resourceService);

        var expandedResource = fromPublication(fakeUriRetriever, resourceService, publication).asJsonNode();
        var type = new TypeReference<List<FundingResult>>() {
        };

        var string = expandedResource.at("/fundings").toString();
        var fundings = attempt(() -> objectMapper.readValue(string, type)).orElseThrow();
        fundings.forEach(funding -> assertTrue(funding.isNotEmpty()));
    }

    @Test
    void shouldReturnIndexDocumentWithValidExpandedFundingSourceWhenFetchFundingReturnNotFound()
        throws JsonProcessingException {
        final var publication = randomBookWithConfirmedPublisher();
        FakeUriResponse.setupFakeForType(publication, fakeUriRetriever, resourceService);
        int statusCode = 404;

        publication.getFundings().stream()
            .filter(ConfirmedFunding.class::isInstance)
            .map(ConfirmedFunding.class::cast)
            .map(ConfirmedFunding::getSource)
            .forEach(uri -> {
                fakeUriRetriever.registerResponse(uri, statusCode, MediaType.ANY_APPLICATION_TYPE, "");
            });
        var expandedResource = fromPublication(fakeUriRetriever, resourceService, publication).asJsonNode();

        assertTrue(expandedResource.at(JsonPointer.compile("/fundings/1/source")).has("id"));
        assertTrue(expandedResource.at(JsonPointer.compile("/fundings/0/source")).has("id"));
    }

    @Test
    void shouldNotCreateTopLevelOrgForBlankNodes() throws Exception {

        final Publication publication = randomBookWithConfirmedPublisher();

        ((Organization) publication.getEntityDescription()
                            .getContributors()
                            .getFirst()
                            .getAffiliations()
                            .getFirst()).setId(null);
        FakeUriResponse.setupFakeForType(publication, fakeUriRetriever, resourceService);

        ObjectNode framedResultNode = fromPublication(fakeUriRetriever, resourceService, publication).asJsonNode();

        var affiliations = framedResultNode.findValues("affiliations").getFirst();
        affiliations.forEach(aff -> {
            if (!aff.has("id")) {
                assertThat(aff.has("topLevelOrganization"), is(equalTo(false)));
            }
        });
    }

    @ParameterizedTest(name = "should return properly framed document with id based on Id-namespace and resource "
                              + "identifier. Instance type:{0}")
    @MethodSource("publicationInstanceProvider")
    void shouldReturnDocumentWithIdBasedOnIdNameSpaceAndResourceIdentifier(Class<?> publicationInstance)
        throws JsonProcessingException {

        var publication = PublicationGenerator.randomPublication(publicationInstance);
        FakeUriResponse.setupFakeForType(publication, fakeUriRetriever, resourceService);
        var indexDocument = fromPublication(fakeUriRetriever, resourceService, publication);
        var json = (ObjectNode) objectMapper.readTree(indexDocument.toJsonString());
        var expectedUri = UriWrapper.fromUri(HOST_URI).addChild(publication.getIdentifier().toString()).getUri();
        var actualUri = URI.create(json.at(PublicationJsonPointers.ID_JSON_PTR).textValue());
        assertThat(actualUri, is(equalTo(expectedUri)));
    }

    @Test
    void shouldReturnIndexDocumentContainingConfirmedSeriesUriFromNsdPublicationChannels()
        throws JsonProcessingException {

        Publication publication = randomBookWithConfirmedPublisher();
        Book book = extractBook(publication);
        Series series = (Series) book.getSeries();
        URI expectedSeriesUri = series.getId();
        FakeUriResponse.setupFakeForType(publication, fakeUriRetriever, resourceService);
        ExpandedResource actualDocument = fromPublication(fakeUriRetriever, resourceService, publication);
        assertThat(actualDocument.getPublicationContextUris(), hasItems(expectedSeriesUri));
    }

    @Test
    void shouldReturnIndexDocumentContainingReturnsJournalUriFromNsdPublicationChannels()
        throws JsonProcessingException {

        Publication publication = randomJournalArticleWithConfirmedJournal();
        Journal journal = (Journal) publication.getEntityDescription().getReference().getPublicationContext();
        URI expectedJournalUri = journal.getId();
        FakeUriResponse.setupFakeForType(publication, fakeUriRetriever, resourceService);
        ExpandedResource actualDocument = fromPublication(fakeUriRetriever, resourceService, publication);
        assertThat(actualDocument.getPublicationContextUris(), contains(expectedJournalUri));
    }

    @Test
    void shouldReturnIndexDocumentWithConfirmedSeriesIdWhenBookIsPartOfSeriesFoundInNsd()
        throws JsonProcessingException {

        Publication publication = PublicationGenerator.randomPublication(BookMonograph.class);
        FakeUriResponse.setupFakeForType(publication, fakeUriRetriever, resourceService);
        ExpandedResource actualDocument = fromPublication(fakeUriRetriever, resourceService, publication);
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
        FakeUriResponse.setupFakeForType(publication, fakeUriRetriever, resourceService);
        ExpandedResource actualDocument = fromPublication(fakeUriRetriever, resourceService, publication);
        Journal journal = extractJournal(publication);
        URI expectedJournalId = journal.getId();
        assertThat(actualDocument.getPublicationContextUris(), containsInAnyOrder(expectedJournalId));
    }

    @Test
    void shouldReturnExpandedResourceWithAnthologyPublicationChannelUrisWhenPublicationIsAcademicChapter()
        throws JsonProcessingException, NotFoundException {
        var publication = PublicationGenerator.randomPublication(AcademicChapter.class);
        FakeUriResponse.setupFakeForType(publication, fakeUriRetriever, resourceService);
        var parentUri = (Anthology) publication.getEntityDescription().getReference().getPublicationContext();
        var bookAnthology = resourceService.getPublicationByIdentifier(SortableIdentifier.fromUri(parentUri.getId()));
        var expectedPublicationChannelIds = getPublicationContextUris(extractBook(bookAnthology));
        var expandedResource = fromPublication(fakeUriRetriever, resourceService, publication);
        var expandedResourceJsonNode = expandedResource.asJsonNode();
        var actualPublicationChannelUris = extractActualPublicationChannelUris(expandedResourceJsonNode);
        assertThat(actualPublicationChannelUris, containsInAnyOrder(expectedPublicationChannelIds.toArray()));
    }

    @Test
    void shouldNotFailWhenThereIsNoPublicationContext() throws JsonProcessingException {

        Publication publication = PublicationGenerator.randomPublication(BookMonograph.class);
        var uriRetriever = FakeUriRetriever.newInstance();
        FakeUriResponse.setupFakeForType(publication, uriRetriever, resourceService);
        publication.getEntityDescription().getReference().setPublicationContext(null);
        assertThat(fromPublication(uriRetriever, resourceService, publication), is(not(nullValue())));
    }

    @Test
    void shouldNotFailWhenThereIsNoPublicationInstance() throws JsonProcessingException {

        Publication publication = PublicationGenerator.randomPublication(BookMonograph.class);
        var uriRetriever = FakeUriRetriever.newInstance();
        FakeUriResponse.setupFakeForType(publication, uriRetriever, resourceService);
        publication.getEntityDescription().getReference().setPublicationInstance(null);
        assertThat(fromPublication(uriRetriever, resourceService, publication), is(not(nullValue())));
    }

    @Test
    void shouldNotFailWhenThereIsNoMainTitle() throws JsonProcessingException {

        Publication publication = PublicationGenerator.randomPublication(BookMonograph.class);
        FakeUriResponse.setupFakeForType(publication, fakeUriRetriever, resourceService);
        publication.getEntityDescription().setMainTitle(null);
        assertThat(fromPublication(fakeUriRetriever, resourceService, publication), is(not(nullValue())));
    }

    @Test
    void shouldNotFailWhenInputContainsAffiliationsThatAreIncomplete() throws JsonProcessingException {
        var publication = createPublicationWithEmptyAffiliations();
        FakeUriResponse.setupFakeForType(publication, fakeUriRetriever, resourceService);
        assertDoesNotThrow(() -> fromPublication(fakeUriRetriever, resourceService, publication));
    }

    @Test
    void shouldNotExpandDoiWhenIdUsedElsewhere() throws IOException {
        var bookAnthology = bookAnthologyWithDoiReferencedInAssociatedLink();
        FakeUriResponse.setupFakeForType(bookAnthology, fakeUriRetriever, resourceService);
        var expandedResource = fromPublication(fakeUriRetriever, resourceService, bookAnthology);

        var actualDoi = expandedResource.getAllFields().get("doi");

        assertThat(actualDoi,
                   allOf(Matchers.instanceOf(String.class), is((equalTo(bookAnthology.getDoi().toString())))));
    }

    @Test
    void shouldNotExpandHandleWhenIdUsedElsewhere() throws IOException {
        var bookAnthology = bookAnthologyWithHandleReferencedInAssociatedLink();
        FakeUriResponse.setupFakeForType(bookAnthology, fakeUriRetriever, resourceService);
        var expandedResource = fromPublication(fakeUriRetriever, resourceService, bookAnthology);

        var actualHandle = expandedResource.getAllFields().get("handle");

        assertThat(actualHandle,
                   allOf(Matchers.instanceOf(String.class), is((equalTo(bookAnthology.getHandle().toString())))));
    }

    @Test
    void shouldNotExpandLinkWhenIdUsedElsewhere() throws IOException {
        var bookAnthology = bookAnthologyWithLinkReferencedInAssociatedLink();
        FakeUriResponse.setupFakeForType(bookAnthology, fakeUriRetriever, resourceService);
        var expandedResource = fromPublication(fakeUriRetriever, resourceService, bookAnthology);

        var actualLink = expandedResource.getAllFields().get("link");

        assertThat(actualLink,
                   allOf(Matchers.instanceOf(String.class), is((equalTo(bookAnthology.getLink().toString())))));
    }

    @Test
    void shouldReturnExpandedResourceWithAnthologyPublicationChannelLevelWhenPublicationIsAcademicChapter()
        throws IOException {
        var academicChapter = PublicationGenerator.randomPublication(AcademicChapter.class);
        FakeUriResponse.setupFakeForType(academicChapter, fakeUriRetriever, resourceService);
        var expandedResource = fromPublication(fakeUriRetriever, resourceService, academicChapter);
        var expandedResourceJsonNode = expandedResource.asJsonNode();
        var actualSeriesLevel = expandedResourceJsonNode.at(SERIES_LEVEL_JSON_PTR).textValue();
        var actualPublisherLevel = expandedResourceJsonNode.at(PUBLISHER_LEVEL_JSON_PTR).textValue();
        assertThat(actualSeriesLevel, is(not(nullValue())));
        assertThat(actualPublisherLevel, is(not(nullValue())));
    }

    @ParameterizedTest
    @MethodSource("validAnthologyContainersProvider")
    void shouldSetHasPartsRelationForBookAnthology(Class<?> publicationType) throws IOException {
        var bookAnthology = PublicationGenerator.randomPublication(publicationType);
        FakeUriResponse.setupFakeForType(bookAnthology, fakeUriRetriever, resourceService);
        var expandedResource = fromPublication(fakeUriRetriever, resourceService, bookAnthology).asJsonNode();

        var actualNode = expandedResource.get("joinField");
        var expectedNode = new ObjectNode(objectMapper.getNodeFactory());
        expectedNode.put("name", "hasParts");

        assertThat(actualNode, is(equalTo(expectedNode)));
    }

    @ParameterizedTest
    @MethodSource("validAnthologyMembersProvider")
    void shouldSetPartOfRelationForAnthologyMember(Class<?> publicationType) throws IOException, BadRequestException {
        var part = randomPersistedPublication(publicationType);
        var publicationContext = (Anthology) part.getEntityDescription().getReference().getPublicationContext();
        FakeUriResponse.setupFakeForType(part, fakeUriRetriever, resourceService);
        var expandedResource = fromPublication(fakeUriRetriever, resourceService, part).asJsonNode();
        var actualNode = expandedResource.get("joinField");
        var expectedNode = new ObjectNode(objectMapper.getNodeFactory());
        expectedNode.put("name", "partOf");
        expectedNode.put("parent", SortableIdentifier.fromUri(((Anthology) publicationContext).getId()).toString());

        assertThat(actualNode, is(equalTo(expectedNode)));
    }

    private Publication randomPersistedPublication(Class<?> publicationType) throws BadRequestException {
        var publication = PublicationGenerator.randomPublication(publicationType);
        return Resource.fromPublication(publication).persistNew(resourceService,
                                                              UserInstance.fromPublication(publication));
    }

    @Test
    void shouldUseApiVersionWhenLookingUpOrganizations() throws JsonProcessingException {
        var publication = PublicationGenerator.randomPublication();
        var uriRetriever = FakeUriRetriever.newInstance();
        FakeUriResponse.setupFakeForType(publication, uriRetriever, resourceService);
        var versionedType = MediaType.parse("application/ld+json; version=2023-05-26");
        publication.getEntityDescription().getContributors().stream()
            .map(Contributor::getAffiliations)
            .flatMap(i -> i.stream().map(Organization.class::cast).map(Organization::getId))
            .forEach(uri -> {
                var responseBody = """
                    {
                      "@context": "https://bibsysdev.github.io/src/organization-context.json",
                      "id": "%s",
                      "type": "Organization",
                      "labels": {
                        "en": "Happy duck"
                      }
                    }""".formatted(uri);
                uriRetriever.registerResponse(uri,
                                              200,
                                              versionedType,
                                              responseBody);
            });

        var actual = fromPublication(uriRetriever,resourceService, publication).toJsonString();
        assertThat(actual, containsString("Happy duck"));
    }

    @ParameterizedTest
    @MethodSource("validAnthologyMembersProvider")
    void shouldNotFailWhenAnthologyParentIsMissing(Class<?> publicationType)
        throws IOException {
        var academicChapter = PublicationGenerator.randomPublication(publicationType);
        var context = (Anthology) academicChapter.getEntityDescription().getReference().getPublicationContext();

        FakeUriResponse.setupFakeForType(academicChapter, fakeUriRetriever, resourceService);

        // Remove the publication context ID to simulate a publication that is missing the parent reference
        context.setId(null);

        var expandedResource = fromPublication(fakeUriRetriever, resourceService, academicChapter).asJsonNode();

        var actualNode = expandedResource.get("joinField");
        var expectedNode = new ObjectNode(objectMapper.getNodeFactory());
        expectedNode.put("name", "partOf");
        expectedNode.put("parent", "PARENT_IDENTIFIER_NOT_FOUND");

        assertThat(actualNode, is(equalTo(expectedNode)));
    }

    private static Contributor contributorWithOneAffiliation(Organization contributor1org) {
        return new Contributor.Builder().withIdentity(new Identity.Builder().withName(randomString()).build())
                   .withRole(new RoleType(Role.ACTOR))
                   .withSequence(randomInteger(10000))
                   .withAffiliations(List.of(contributor1org))
                   .build();
    }

    private static Contributor contributorWithSequence(int sequence) {
        return new Contributor.Builder().withIdentity(new Identity.Builder().withName(randomString()).build())
                   .withSequence(sequence)
                   .build();
    }

    private static Stream<Class<?>> validAnthologyContainersProvider() {
        return Stream.of(
            AcademicMonograph.class,
            NonFictionMonograph.class,
            PopularScienceMonograph.class,
            Textbook.class,
            Encyclopedia.class,
            ExhibitionCatalog.class,
            BookAnthology.class,
            ReportResearch.class,
            ReportPolicy.class,
            ReportWorkingPaper.class,
            ReportBookOfAbstract.class,
            ConferenceReport.class,
            ReportBasic.class);
    }

    private static Stream<Class<?>> validAnthologyMembersProvider() {
        return Stream.of(AcademicChapter.class,
                         EncyclopediaChapter.class,
                         ExhibitionCatalogChapter.class,
                         Introduction.class,
                         NonFictionChapter.class,
                         PopularScienceChapter.class,
                         TextbookChapter.class,
                         ChapterConferenceAbstract.class,
                         ChapterInReport.class);
    }

    private static JsonNode getTopLevel(ArrayNode topLevelNodes, String topLevelOrgId) {
        return stream(topLevelNodes.spliterator(), false)
                   .filter(node -> node.at(JSON_PTR_ID).textValue().contains(topLevelOrgId))
                   .findFirst()
                   .orElse(null);
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
        return framedResultNode.at(PublicationJsonPointers.FUNDING_SOURCE_POINTER)
                   .findValues("source")
                   .stream()
                   .flatMap(node -> node.findValues("id").stream())
                   .map(JsonNode::textValue)
                   .map(URI::create)
                   .collect(Collectors.toSet());
    }

    private static List<URI> extractActualPublicationChannelUris(ObjectNode expandedResourceJsonNode) {
        var actualPublisherId = URI.create(expandedResourceJsonNode.at(PUBLISHER_ID_JSON_PTR).textValue());
        var actualSeriesId = URI.create(expandedResourceJsonNode.at(SERIES_ID_JSON_PTR).textValue());
        return List.of(actualPublisherId, actualSeriesId);
    }

    private static Contributor createContributorsWithEmptyAffiliations(Contributor contributor) {
        return new Contributor.Builder().withIdentity(contributor.getIdentity())
                   .withAffiliations(List.of(new Organization()))
                   .withRole(contributor.getRole())
                   .withSequence(contributor.getSequence())
                   .withCorrespondingAuthor(contributor.isCorrespondingAuthor())
                   .build();
    }

    private static Stream<Class<?>> publicationInstanceProvider() {
        return PublicationInstanceBuilder.listPublicationInstanceTypes().stream();
    }

    private static JsonNode findDeepestNestedSubUnit(JsonNode jsonNode) {
        if (isNull(jsonNode) || isBlankJsonNode(jsonNode)) {
            return null;
        }
        while (hasPartHasContent(jsonNode)) {
            var hasPartArrayNode = (ArrayNode) jsonNode.at(JSON_PTR_HAS_PART);
            if (hasPartArrayNode.size() == 1) {
                jsonNode = hasPartArrayNode.get(0);
            } else {
                hasPartArrayNode.forEach(node -> findDeepestNestedSubUnit(node.at(JSON_PTR_HAS_PART)));
            }
        }
        return jsonNode;
    }

    private static boolean hasPartHasContent(JsonNode jsonNode) {
        var node = jsonNode.at(JSON_PTR_HAS_PART);
        return !node.isMissingNode() && node.isArray() && !node.isEmpty();
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

    private List<URI> getPublicationContextUris(Book book) {
        var confirmedSeries = (Series) book.getSeries();
        var expectedSeriesId = confirmedSeries.getId();
        var publisher = (Publisher) book.getPublisher();
        var expectedPublisherId = publisher.getId();
        return List.of(expectedSeriesId, expectedPublisherId);
    }

    private Publication createPublicationWithEmptyAffiliations() {
        var publication = PublicationGenerator.randomPublication(AcademicArticle.class);
        publication.setStatus(PUBLISHED);
        var entityDescription = publication.getEntityDescription();
        var contributors = entityDescription.getContributors()
                               .stream()
                               .map(ExpandedResourceTest::createContributorsWithEmptyAffiliations)
                               .toList();
        entityDescription.setContributors(contributors);
        return publication;
    }

    private Journal extractJournal(Publication publication) {
        return (Journal) publication.getEntityDescription().getReference().getPublicationContext();
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
        return new Contributor.Builder().withIdentity(new Identity.Builder().withName(randomString()).build())
                   .withRole(new RoleType(Role.ACTOR))
                   .withSequence(randomInteger(10000))
                   .withAffiliations(List.of(randomOrganization()))
                   .build();
    }

    private Publication randomBookWithManyContributors() {
        var publication = PublicationGenerator.randomPublication(BookMonograph.class);
        var contributions = IntStream.rangeClosed(1, 10)
                                .mapToObj(i -> randomContributor())
                                .toList();
        publication.getEntityDescription().setContributors(contributions);
        return publication;
    }
}
