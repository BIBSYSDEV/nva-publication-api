package no.unit.nva.expansion.rdf;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static nva.commons.apigateway.MediaTypes.APPLICATION_JSON_LD;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.contexttypes.Anthology;
import no.unit.nva.model.instancetypes.chapter.AcademicChapter;
import no.unit.nva.model.instancetypes.journal.AcademicArticle;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;
import no.unit.nva.publication.PublicationServiceConfig;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.uriretriever.FakeUriResponse;
import no.unit.nva.publication.uriretriever.FakeUriRetriever;
import nva.commons.apigateway.MediaType;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.paths.UriWrapper;
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
      "@context": "https://bibsys.github.io/nva-ontology/ontology/cristin-org-context.json",
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
      "@context": "https://bibsys.github.io/nva-ontology/ontology/cristin-org-context.json",
      "@id": "%s",
      "@type": "Organization"
    }
    """
        .formatted(orgUri);
  }

  private static String minimalPublicationJsonLd(URI publicationUri) {
    return """
    {
      "@context": "https://nva.sikt.no/ontology/publication",
      "@id": "%s",
      "@type": "Publication"
    }
    """
        .formatted(publicationUri);
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
