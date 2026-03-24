package no.unit.nva.expansion.rdf;

import static no.unit.nva.expansion.ExpansionConfig.objectMapper;
import static no.unit.nva.publication.PublicationServiceConfig.PUBLICATION_HOST_URI;
import static nva.commons.apigateway.MediaTypes.APPLICATION_JSON_LD;
import static nva.commons.core.attempt.Try.attempt;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import no.unit.nva.auth.uriretriever.RawContentRetriever;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.expansion.model.nvi.NviCandidateResponse;
import no.unit.nva.expansion.model.nvi.ScientificIndex;
import no.unit.nva.expansion.utils.AffiliationQueries;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.Reference;
import no.unit.nva.model.contexttypes.Anthology;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.contexttypes.Journal;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.contexttypes.Publisher;
import no.unit.nva.model.contexttypes.Series;
import no.unit.nva.model.funding.ConfirmedFunding;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PublicationRdfExpansion {

  private static final Logger logger = LoggerFactory.getLogger(PublicationRdfExpansion.class);
  private static final String NVA = "https://nva.sikt.no/ontology/publication#";
  private static final String API_HOST = new Environment().readEnv("API_HOST");
  private static final String CONTENT_TYPE_JSON = "application/json";
  private static final String SCIENTIFIC_INDEX_PATH = "scientific-index";
  private static final String PUBLICATION_PATH = "publication";
  private static final String REPORT_STATUS_PATH = "report-status";
  private static final Property NVA_SCIENTIFIC_INDEX =
      ResourceFactory.createProperty(NVA, "scientificIndex");
  private static final Property NVA_YEAR = ResourceFactory.createProperty(NVA, "year");
  private static final Property NVA_STATUS = ResourceFactory.createProperty(NVA, "status");
  private static final Path FUNDING_QUERY_SPARQL = Path.of("funding_query.sparql");

  private final RawContentRetriever uriRetriever;
  private final ResourceService resourceService;

  public PublicationRdfExpansion(
      RawContentRetriever uriRetriever, ResourceService resourceService) {
    this.uriRetriever = uriRetriever;
    this.resourceService = resourceService;
  }

  public String toNTriples(SortableIdentifier identifier) {
    var resource = fetchResource(identifier);
    var writer = new StringWriter();
    RDFDataMgr.write(writer, buildCbd(resource), Lang.NTRIPLES);
    return writer.toString();
  }

  private Model buildCbd(Resource resource) {
    var publication = resource.toPublication();
    var model = loadPublication(publication);
    enrichAffiliations(model, publication);
    enrichPublicationChannels(model, publication);
    enrichNviStatus(model, publication);
    enrichRelatedPublications(model, resource, publication);
    applyConstructQueries(model, publication);
    return model;
  }

  private Model loadPublication(Publication publication) {
    var model = ModelFactory.createDefaultModel();
    loadJsonLd(model, publicationToJsonLd(publication));
    return model;
  }

  private void enrichAffiliations(Model model, Publication publication) {
    affiliationUris(publication)
        .distinct()
        .forEach(uri -> fetchJsonLd(uri).ifPresent(body -> loadJsonLd(model, body)));
  }

  private void enrichPublicationChannels(Model model, Publication publication) {
    publicationChannelUris(publication)
        .forEach(uri -> fetchJsonLd(uri).ifPresent(body -> loadJsonLd(model, body)));
  }

  private void enrichNviStatus(Model model, Publication publication) {
    var publicationUri = publicationUri(publication);
    fetchNviStatus(publicationUri)
        .filter(ScientificIndex::isReported)
        .ifPresent(nvi -> addNviTriples(model, publicationUri, nvi));
  }

  private void enrichRelatedPublications(Model model, Resource resource, Publication publication) {
    if (isAnthologySubPart(publication)) {
      extractAnthologyUri(publication)
          .flatMap(this::fetchJsonLd)
          .ifPresent(body -> loadJsonLd(model, body));
    }
    resource.getRelatedResources().forEach(id -> fetchPublication(model, id));
  }

  private void fetchPublication(Model model, SortableIdentifier id) {
    try {
      var jsonLd = publicationToJsonLd(resourceService.getPublicationByIdentifier(id));
      loadJsonLd(model, jsonLd);
    } catch (NotFoundException e) {
      logFailure(id, e);
      throw new RuntimeException(e);
    }
  }

  private static void logFailure(SortableIdentifier id, Exception exception) {
    logger.warn("Could not load related publication {}: {}", id, exception.getMessage());
  }

  private void applyConstructQueries(Model model, Publication publication) {
    runConstruct(model, AffiliationQueries.TOP_LEVEL_ORGANIZATION);
    runConstruct(model, AffiliationQueries.CONTRIBUTOR_ORGANIZATION);
    runConstruct(model, AffiliationQueries.CONTRIBUTOR_INSTITUTION);
    runConstruct(model, AffiliationQueries.HAS_PART);
    enrichFunding(model, publication);
  }

  private void enrichFunding(Model model, Publication publication) {
    fundingSourceUris(publication)
        .forEach(uri -> fetchJsonLd(uri).ifPresent(body -> loadJsonLd(model, body)));
    runConstruct(model, fundingQuery(publicationUri(publication)));
  }

  private void addNviTriples(Model model, URI publicationUri, ScientificIndex nvi) {
    var publication = model.createResource(publicationUri.toString());
    var nviNode = model.createResource();
    model.add(publication, NVA_SCIENTIFIC_INDEX, nviNode);
    model.add(nviNode, NVA_YEAR, model.createLiteral(nvi.year()));
    model.add(nviNode, NVA_STATUS, model.createLiteral(nvi.status()));
  }

  private void runConstruct(Model model, String query) {
    try (var exec = QueryExecutionFactory.create(QueryFactory.create(query), model)) {
      model.add(exec.execConstruct());
    }
  }

  private void loadJsonLd(Model model, String jsonLd) {
    try {
      RDFDataMgr.read(
          model, new ByteArrayInputStream(jsonLd.getBytes(StandardCharsets.UTF_8)), Lang.JSONLD);
    } catch (RuntimeException e) {
      logger.warn("Skipping unreadable JSON-LD: {}", e.getMessage());
      throw e;
    }
  }

  private Optional<String> fetchJsonLd(URI uri) {
    return attempt(() -> uriRetriever.fetchResponse(uri, APPLICATION_JSON_LD.toString()))
        .map(
            response ->
                response.filter(PublicationRdfExpansion::successFamily).map(HttpResponse::body))
        .orElse(
            failure -> {
              logger.warn("Could not fetch {}: {}", uri, failure.getException().getMessage());
              throw new RuntimeException("Could not fetch " + uri, failure.getException());
            });
  }

  private Optional<ScientificIndex> fetchNviStatus(URI publicationId) {
    var nviUri = nviCandidateUri(publicationId);
    return attempt(() -> uriRetriever.fetchResponse(nviUri, CONTENT_TYPE_JSON))
        .map(
            opt ->
                opt.filter(PublicationRdfExpansion::successFamily)
                    .map(HttpResponse::body)
                    .map(this::toNviCandidateResponse)
                    .map(NviCandidateResponse::toNviStatus))
        .orElse(
            failure -> {
              logger.warn(
                  "Could not fetch NVI status for {}: {}",
                  publicationId,
                  failure.getException().getMessage());
              throw new RuntimeException(
                  "Could not fetch NVI status for " + publicationId, failure.getException());
            });
  }

  private static boolean successFamily(HttpResponse<String> response) {
    return response.statusCode() / 100 == 2;
  }

  private NviCandidateResponse toNviCandidateResponse(String body) {
    return attempt(() -> JsonUtils.dtoObjectMapper.readValue(body, NviCandidateResponse.class))
        .orElseThrow();
  }

  private String publicationToJsonLd(Publication publication) {
    return attempt(
            () -> {
              var json =
                  (ObjectNode) objectMapper.readTree(objectMapper.writeValueAsString(publication));
              json.put("id", publicationUri(publication).toString());
              json.set(
                  "@context",
                  objectMapper.readTree(
                      Publication.getJsonLdContext(UriWrapper.fromHost(API_HOST).getUri())));
              return objectMapper.writeValueAsString(json);
            })
        .orElseThrow();
  }

  private Resource fetchResource(SortableIdentifier identifier) {
    return attempt(() -> resourceService.getResourceByIdentifier(identifier)).orElseThrow();
  }

  private URI publicationUri(Publication publication) {
    return UriWrapper.fromUri(PUBLICATION_HOST_URI)
        .addChild(publication.getIdentifier().toString())
        .getUri();
  }

  private URI nviCandidateUri(URI publicationId) {
    var encoded = URLEncoder.encode(publicationId.toString(), StandardCharsets.UTF_8);
    return URI.create(
        UriWrapper.fromHost(API_HOST)
            .addChild(SCIENTIFIC_INDEX_PATH)
            .addChild(PUBLICATION_PATH)
            .addChild(encoded)
            .addChild(REPORT_STATUS_PATH)
            .toString());
  }

  private static String fundingQuery(URI publicationUri) {
    return nva.commons.core.ioutils.IoUtils.stringFromResources(FUNDING_QUERY_SPARQL)
        .formatted(publicationUri.toString());
  }

  private static Stream<URI> affiliationUris(Publication publication) {
    return publication.getContributors().stream()
        .map(Contributor::affiliations)
        .flatMap(Collection::stream)
        .filter(Organization.class::isInstance)
        .map(Organization.class::cast)
        .map(Organization::getId)
        .filter(Objects::nonNull);
  }

  private static Stream<URI> publicationChannelUris(Publication publication) {
    return Optional.ofNullable(publication.getEntityDescription())
        .map(EntityDescription::getReference)
        .map(Reference::getPublicationContext)
        .map(PublicationRdfExpansion::channelUrisFrom)
        .orElse(Stream.empty());
  }

  private static Stream<URI> channelUrisFrom(PublicationContext context) {
    return switch (context) {
      case Journal journal -> Stream.ofNullable(journal.getId());
      case Book book ->
          Stream.of(
                  book.getPublisher() instanceof Publisher publisher ? publisher.getId() : null,
                  book.getSeries() instanceof Series series ? series.getId() : null)
              .filter(Objects::nonNull);
      default -> Stream.empty();
    };
  }

  private static Stream<URI> fundingSourceUris(Publication publication) {
    return Optional.ofNullable(publication.getFundings()).orElse(java.util.Set.of()).stream()
        .filter(ConfirmedFunding.class::isInstance)
        .map(ConfirmedFunding.class::cast)
        .map(ConfirmedFunding::getId)
        .filter(Objects::nonNull);
  }

  private static boolean isAnthologySubPart(Publication publication) {
    return Optional.ofNullable(publication.getEntityDescription())
        .map(EntityDescription::getReference)
        .map(Reference::getPublicationContext)
        .filter(Anthology.class::isInstance)
        .isPresent();
  }

  private static Optional<URI> extractAnthologyUri(Publication publication) {
    return Optional.ofNullable(publication.getEntityDescription())
        .map(EntityDescription::getReference)
        .map(Reference::getPublicationContext)
        .map(PublicationRdfExpansion::extractAnthology)
        .map(Anthology::getId);
  }

  private static Anthology extractAnthology(PublicationContext publicationContext) {
    return publicationContext instanceof Anthology anthology ? anthology : null;
  }
}
