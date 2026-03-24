package no.unit.nva.expansion.rdf;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static nva.commons.apigateway.MediaTypes.APPLICATION_JSON_LD;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;
import no.unit.nva.auth.uriretriever.RawContentRetriever;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.contexttypes.Anthology;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.contexttypes.Publisher;
import no.unit.nva.model.contexttypes.Series;
import no.unit.nva.model.funding.FundingBuilder;
import no.unit.nva.model.instancetypes.book.BookMonograph;
import no.unit.nva.model.instancetypes.chapter.AcademicChapter;
import no.unit.nva.model.instancetypes.journal.AcademicArticle;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;
import no.unit.nva.publication.PublicationServiceConfig;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.ResourceRelationship;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.storage.ResourceRelationshipDao;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.uriretriever.FakeUriResponse;
import no.unit.nva.publication.uriretriever.FakeUriRetriever;
import nva.commons.apigateway.MediaType;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.paths.UriWrapper;
import nva.commons.logutils.LogUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.shacl.ShaclValidator;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.shacl.ValidationReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PublicationRdfExpansionTest extends ResourcesLocalTest {

  private static final String NVA_ONTOLOGY = "https://nva.sikt.no/ontology/publication#";
  private static final URI AFFILIATION_URI =
      URI.create("https://api.dev.nva.aws.unit.no/cristin/organization/20754.6.0.0");
  private static final URI TOP_LEVEL_ORG_URI =
      URI.create("https://api.dev.nva.aws.unit.no/cristin/organization/20754.0.0.0");

  private ResourceService resourceService;
  private FakeUriRetriever fakeUriRetriever;
  private PublicationRdfExpansion expansion;

  @BeforeEach
  void setUp() {
    super.init();
    resourceService = getResourceService(client);
    fakeUriRetriever = FakeUriRetriever.newInstance();
    expansion = new PublicationRdfExpansion(fakeUriRetriever, resourceService);
  }

  @Test
  void publicationCbdConformsToBaseShape() throws BadRequestException {
    var resource = persistedResource(randomPublication(AcademicArticle.class));
    registerNviNotFound(publicationUri(resource.toPublication()));

    var model = buildModel(resource);

    assertConformsTo(model, baseShape());
  }

  @Test
  void affiliatedPublicationHasResolvedOrganisationHierarchy() throws BadRequestException {
    var publication = publicationWithAffiliation(AFFILIATION_URI);
    var resource = persistedResource(publication);
    registerOrganisation(AFFILIATION_URI, organisationJsonLd(AFFILIATION_URI, TOP_LEVEL_ORG_URI));
    registerOrganisation(TOP_LEVEL_ORG_URI, topLevelOrganisationJsonLd(TOP_LEVEL_ORG_URI));
    registerNviNotFound(publicationUri(publication));

    var model = buildModel(resource);

    assertConformsTo(model, affiliatedPublicationShape());
  }

  @Test
  void nviReportedPublicationHasScientificIndexTriples() throws BadRequestException {
    var resource = persistedResource(randomPublication(AcademicArticle.class));
    registerNviReported(publicationUri(resource.toPublication()), "2024");

    var model = buildModel(resource);

    assertConformsTo(model, nviPublicationShape());
  }

  @Test
  void nviNonCandidatePublicationHasNoScientificIndexTriples() throws BadRequestException {
    var resource = persistedResource(randomPublication(AcademicArticle.class));
    registerNviNotFound(publicationUri(resource.toPublication()));

    var model = buildModel(resource);

    var pub = model.createResource(publicationUri(resource.toPublication()).toString());
    assertTrue(
        model
            .listObjectsOfProperty(pub, model.createProperty(NVA_ONTOLOGY + "scientificIndex"))
            .toList()
            .isEmpty(),
        "Non-candidate should have no scientificIndex triple");
  }

  @Test
  void academicChapterCbdIncludesParentAnthologyTriples() throws BadRequestException {
    var anthologyUri = URI.create("https://api.dev.nva.aws.unit.no/publication/anthology-id");
    var chapter = publicationWithAnthologyContext(anthologyUri);
    var resource = persistedResource(chapter);
    registerNviNotFound(publicationUri(chapter));
    registerPublicationJsonLd(anthologyUri, minimalPublicationJsonLd(anthologyUri));

    var model = buildModel(resource);

    assertTrue(
        model.containsResource(model.createResource(anthologyUri.toString())),
        "Anthology URI should be present in the CBD");
  }

  @Test
  void bookPublicationChannelsAreFetched() throws BadRequestException {
    var publisherUri =
        URI.create(
            "https://api.dev.nva.aws.unit.no/publication-channels-v2/publisher/test-pub/2024");
    var seriesUri =
        URI.create(
            "https://api.dev.nva.aws.unit.no/publication-channels-v2/serial-publication/test-series/2024");
    var publication = publicationWithBookContext(publisherUri, seriesUri);
    var resource = persistedResource(publication);
    fakeUriRetriever.registerResponse(
        publisherUri, SC_OK, APPLICATION_JSON_LD, minimalPublicationJsonLd(publisherUri));
    fakeUriRetriever.registerResponse(
        seriesUri, SC_OK, APPLICATION_JSON_LD, minimalPublicationJsonLd(seriesUri));
    registerNviNotFound(publicationUri(publication));

    var model = buildModel(resource);

    assertTrue(model.containsResource(model.createResource(publisherUri.toString())));
    assertTrue(model.containsResource(model.createResource(seriesUri.toString())));
  }

  @Test
  void relatedResourcesAreIncludedInCbd() throws BadRequestException {
    var childPublication = randomPublication(AcademicArticle.class);
    var childResource = persistedResource(childPublication);

    var parentPublication = randomPublication(AcademicArticle.class);
    var parentResource = persistedResource(parentPublication);
    insertRelation(parentResource.getIdentifier(), childResource.getIdentifier());

    registerNviNotFound(publicationUri(parentPublication));

    var model = buildModel(parentResource);

    var childUri =
        UriWrapper.fromUri(PublicationServiceConfig.PUBLICATION_HOST_URI)
            .addChild(childResource.getIdentifier().toString())
            .getUri();
    assertTrue(model.containsResource(model.createResource(childUri.toString())));
  }

  @Test
  void inaccessibleRelatedResourceThrows() throws BadRequestException {
    var parentPublication = randomPublication(AcademicArticle.class);
    var parentResource = persistedResource(parentPublication);
    var missingId = SortableIdentifier.next();
    insertRelation(parentResource.getIdentifier(), missingId);

    registerNviNotFound(publicationUri(parentPublication));

    var appender = LogUtils.getTestingAppenderForRootLogger();
    assertThrows(RuntimeException.class, () -> buildModel(parentResource));
    assertTrue(appender.getMessages().contains("Could not load related publication"));
    assertTrue(appender.getMessages().contains(missingId.toString()));
  }

  @Test
  void confirmedFundingSourceIsFetched() throws BadRequestException {
    var fundingId = URI.create("https://api.dev.nva.aws.unit.no/funding-source/test-funding-123");
    var publication = publicationWithConfirmedFunding(fundingId);
    var resource = persistedResource(publication);
    fakeUriRetriever.registerResponse(
        fundingId, SC_OK, APPLICATION_JSON_LD, minimalPublicationJsonLd(fundingId));
    registerNviNotFound(publicationUri(publication));

    var model = buildModel(resource);

    assertTrue(model.containsResource(model.createResource(fundingId.toString())));
  }

  @Test
  void nviCandidateNotReportedHasNoScientificIndexTriples() throws BadRequestException {
    var resource = persistedResource(randomPublication(AcademicArticle.class));
    registerNviWithStatus(publicationUri(resource.toPublication()), "2024", "Candidate");

    var model = buildModel(resource);

    var pub = model.createResource(publicationUri(resource.toPublication()).toString());
    assertTrue(
        model
            .listObjectsOfProperty(pub, model.createProperty(NVA_ONTOLOGY + "scientificIndex"))
            .toList()
            .isEmpty(),
        "Non-reported NVI candidate should have no scientificIndex triple");
  }

  @Test
  void malformedJsonLdBodyThrows() throws BadRequestException {
    var publication = publicationWithAffiliation(AFFILIATION_URI);
    var resource = persistedResource(publication);
    fakeUriRetriever.registerResponse(
        AFFILIATION_URI, SC_OK, APPLICATION_JSON_LD, "not valid json-ld {{{");
    registerNviNotFound(publicationUri(publication));

    var appender = LogUtils.getTestingAppenderForRootLogger();
    assertThrows(RuntimeException.class, () -> buildModel(resource));
    assertTrue(appender.getMessages().contains("Skipping unreadable JSON-LD"));
  }

  @Test
  void fetchJsonLdThrowsWhenRetrieverFails() throws BadRequestException {
    var publication = publicationWithAffiliation(AFFILIATION_URI);
    var resource = persistedResource(publication);
    var failingExpansion =
        new PublicationRdfExpansion(throwingRetrieverFor(AFFILIATION_URI), resourceService);

    var appender = LogUtils.getTestingAppenderForRootLogger();
    assertThrows(
        RuntimeException.class, () -> failingExpansion.toNTriples(resource.getIdentifier()));
    assertTrue(appender.getMessages().contains("Could not fetch"));
    assertTrue(appender.getMessages().contains(AFFILIATION_URI.toString()));
  }

  @Test
  void fetchNviStatusThrowsWhenRetrieverFails() throws BadRequestException {
    var resource = persistedResource(randomPublication(AcademicArticle.class));
    var publicationUri = publicationUri(resource.toPublication());
    var failingExpansion =
        new PublicationRdfExpansion(throwingRetrieverFor(nviUri(publicationUri)), resourceService);

    var appender = LogUtils.getTestingAppenderForRootLogger();
    assertThrows(
        RuntimeException.class, () -> failingExpansion.toNTriples(resource.getIdentifier()));
    assertTrue(appender.getMessages().contains("Could not fetch NVI status for"));
    assertTrue(appender.getMessages().contains(publicationUri.toString()));
  }

  // --- helpers ---

  private static URI publicationUri(Publication publication) {
    return UriWrapper.fromUri(PublicationServiceConfig.PUBLICATION_HOST_URI)
        .addChild(publication.getIdentifier().toString())
        .getUri();
  }

  private Model buildModel(Resource resource) {
    var nTriples = expansion.toNTriples(resource.getIdentifier());
    var model = ModelFactory.createDefaultModel();
    RDFDataMgr.read(
        model, new ByteArrayInputStream(nTriples.getBytes(StandardCharsets.UTF_8)), Lang.NTRIPLES);
    return model;
  }

  private Resource persistedResource(Publication publication) throws BadRequestException {
    var persisted =
        Resource.fromPublication(publication)
            .persistNew(resourceService, UserInstance.fromPublication(publication));
    return Resource.fromPublication(persisted);
  }

  private static Publication publicationWithAffiliation(URI affiliationUri) {
    var publication = randomPublication(AcademicArticle.class);
    var affiliation = new Organization.Builder().withId(affiliationUri).build();
    var contributor =
        new Contributor.Builder()
            .withRole(new RoleType(Role.CREATOR))
            .withAffiliations(java.util.List.of(affiliation))
            .build();
    publication.getEntityDescription().setContributors(java.util.List.of(contributor));
    return publication;
  }

  private static Publication publicationWithAnthologyContext(URI anthologyUri) {
    var publication = randomPublication(AcademicChapter.class);
    var anthology = new Anthology.Builder().withId(anthologyUri).build();
    publication.getEntityDescription().getReference().setPublicationContext(anthology);
    return publication;
  }

  private void registerOrganisation(URI uri, String jsonLd) {
    fakeUriRetriever.registerResponse(uri, SC_OK, APPLICATION_JSON_LD, jsonLd);
  }

  private void registerPublicationJsonLd(URI uri, String jsonLd) {
    fakeUriRetriever.registerResponse(uri, SC_OK, APPLICATION_JSON_LD, jsonLd);
  }

  private void registerNviNotFound(URI publicationUri) {
    fakeUriRetriever.registerResponse(
        nviUri(publicationUri), SC_NOT_FOUND, MediaType.JSON_UTF_8, "");
  }

  private void registerNviWithStatus(URI publicationUri, String year, String status) {
    var body =
        """
        {"reportStatus":{"status":"%s"},"period":"%s"}
        """
            .formatted(status, year);
    fakeUriRetriever.registerResponse(nviUri(publicationUri), SC_OK, MediaType.JSON_UTF_8, body);
  }

  private void registerNviReported(URI publicationUri, String year) {
    var body =
        """
        {"reportStatus":{"status":"Reported"},"period":"%s"}
        """
            .formatted(year);
    fakeUriRetriever.registerResponse(nviUri(publicationUri), SC_OK, MediaType.JSON_UTF_8, body);
  }

  private static URI nviUri(URI publicationUri) {
    var encoded = URLEncoder.encode(publicationUri.toString(), StandardCharsets.UTF_8);
    return URI.create(
        UriWrapper.fromHost(FakeUriResponse.API_HOST)
            .addChild("scientific-index")
            .addChild("publication")
            .addChild(encoded)
            .addChild("report-status")
            .toString());
  }

  private static String organisationJsonLd(URI orgUri, URI parentUri) {
    return """
    {
      "@context": {
        "nva": "https://nva.sikt.no/ontology/publication#",
        "Organization": "nva:Organization",
        "partOf": {"@id": "nva:partOf", "@type": "@id"}
      },
      "@id": "%s",
      "@type": "Organization",
      "partOf": [{"@id": "%s"}]
    }
    """
        .formatted(orgUri, parentUri);
  }

  private static String topLevelOrganisationJsonLd(URI orgUri) {
    return """
    {
      "@context": {
        "nva": "https://nva.sikt.no/ontology/publication#",
        "Organization": "nva:Organization",
        "partOf": {"@id": "nva:partOf", "@type": "@id"}
      },
      "@id": "%s",
      "@type": "Organization"
    }
    """
        .formatted(orgUri);
  }

  private static String minimalPublicationJsonLd(URI publicationUri) {
    return """
    {
      "@context": {"nva": "https://nva.sikt.no/ontology/publication#"},
      "@id": "%s",
      "@type": "nva:Publication"
    }
    """
        .formatted(publicationUri);
  }

  private static Publication publicationWithBookContext(URI publisherUri, URI seriesUri) {
    var publication = randomPublication(BookMonograph.class);
    var book =
        new Book.BookBuilder()
            .withPublisher(new Publisher(publisherUri))
            .withSeries(new Series(seriesUri))
            .build();
    publication.getEntityDescription().getReference().setPublicationContext(book);
    return publication;
  }

  private static Publication publicationWithConfirmedFunding(URI fundingId) {
    var publication = randomPublication(AcademicArticle.class);
    var funding =
        new FundingBuilder()
            .withId(fundingId)
            .withSource(URI.create("https://example.org/funding-source"))
            .withIdentifier("test-funding")
            .build();
    publication.setFundings(Set.of(funding));
    return publication;
  }

  private RawContentRetriever throwingRetrieverFor(URI targetUri) {
    return new RawContentRetriever() {
      @Override
      public Optional<String> getRawContent(URI uri, String mediaType) {
        if (uri.equals(targetUri)) throw new RuntimeException("Simulated retrieval failure");
        return fakeUriRetriever.getRawContent(uri, mediaType);
      }

      @Override
      public Optional<HttpResponse<String>> fetchResponse(URI uri, String mediaType) {
        if (uri.equals(targetUri)) throw new RuntimeException("Simulated retrieval failure");
        return fakeUriRetriever.fetchResponse(uri, mediaType);
      }
    };
  }

  private void insertRelation(SortableIdentifier parentId, SortableIdentifier childId) {
    var relationship = new ResourceRelationship(parentId, childId);
    var tableName = new nva.commons.core.Environment().readEnv("TABLE_NAME");
    client.putItem(
        new PutItemRequest(tableName, ResourceRelationshipDao.from(relationship).toDynamoFormat()));
  }

  // --- SHACL shapes (inline — each test asserts only what it claims) ---

  private static String baseShape() {
    return """
    @prefix sh:  <http://www.w3.org/ns/shacl#> .
    @prefix nva: <https://nva.sikt.no/ontology/publication#> .
    nva:BaseShape a sh:NodeShape ;
        sh:targetClass nva:Publication ;
        sh:property [ sh:path nva:identifier ; sh:minCount 1 ] .
    """;
  }

  private static String affiliatedPublicationShape() {
    return """
    @prefix sh:  <http://www.w3.org/ns/shacl#> .
    @prefix nva: <https://nva.sikt.no/ontology/publication#> .
    nva:AffiliationShape a sh:NodeShape ;
        sh:targetClass nva:Publication ;
        sh:property [
            sh:path nva:topLevelOrganization ;
            sh:minCount 1 ;
            sh:nodeKind sh:IRI
        ] ;
        sh:property [
            sh:path nva:contributorOrganization ;
            sh:minCount 1 ;
            sh:nodeKind sh:IRI
        ] .
    """;
  }

  private static String nviPublicationShape() {
    return """
    @prefix sh:  <http://www.w3.org/ns/shacl#> .
    @prefix nva: <https://nva.sikt.no/ontology/publication#> .
    @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
    nva:NviShape a sh:NodeShape ;
        sh:targetClass nva:Publication ;
        sh:property [ sh:path nva:scientificIndex ; sh:minCount 1 ] .
    nva:ScientificIndexShape a sh:NodeShape ;
        sh:targetClass nva:ScientificIndex ;
        sh:property [ sh:path nva:year   ; sh:minCount 1 ; sh:datatype xsd:string ] ;
        sh:property [ sh:path nva:status ; sh:minCount 1 ; sh:datatype xsd:string ] .
    """;
  }

  private static void assertConformsTo(Model data, String shapeTurtle) {
    var shapesModel = ModelFactory.createDefaultModel();
    RDFDataMgr.read(
        shapesModel,
        new ByteArrayInputStream(shapeTurtle.getBytes(StandardCharsets.UTF_8)),
        Lang.TURTLE);
    var shapes = Shapes.parse(shapesModel.getGraph());
    ValidationReport report = ShaclValidator.get().validate(shapes, data.getGraph());
    assertTrue(report.conforms(), "SHACL validation failed:\n" + reportText(report));
  }

  private static String reportText(ValidationReport report) {
    var sb = new StringBuilder();
    report.getEntries().forEach(e -> sb.append(e).append("\n"));
    return sb.toString();
  }
}
