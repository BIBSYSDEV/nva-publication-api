package no.unit.nva.expansion.utils;

import static java.util.stream.StreamSupport.stream;
import static no.unit.nva.expansion.ExpansionConfig.objectMapper;
import static no.unit.nva.expansion.utils.PublicationJsonPointers.PUBLISHER_JSON_PTR;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static nva.commons.core.ioutils.IoUtils.stringToStream;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import no.unit.nva.expansion.ExpansionConfig;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Publication;
import no.unit.nva.model.contexttypes.ResearchData;
import no.unit.nva.model.instancetypes.journal.AcademicArticle;
import no.unit.nva.model.instancetypes.researchdata.SoftwareSourceCode;
import no.unit.nva.publication.uriretriever.FakeUriRetriever;
import org.junit.jupiter.api.Test;

/**
 * Covers the handling of contributor identities that occur more than once in the graph. The same
 * person is identified by the same URI, so Jena merges the occurrences into one node, which may
 * then carry several names. The chapter-in-anthology variant needs the parent publication from the
 * database and is covered in ExpandedResourceTest.
 */
class FramedJsonGeneratorTest {

  private static final Path FRAME = Path.of("frame.json");
  private static final JsonPointer CONTRIBUTORS_JSON_PTR =
      JsonPointer.compile("/entityDescription/contributors");
  private static final String ID_FIELD = "id";
  private static final String CONTEXT_FIELD = "@context";

  @Test
  void shouldChooseLongestNameForAllEntriesWhenSamePersonIsRegisteredTwiceWithDifferentNames()
      throws JsonProcessingException {
    var identityId = randomUri();
    var publication =
        publicationWithContributors(
            contributor(identityId, "Mai O Nes"), contributor(identityId, "Mai Olsen Nes"));

    var contributorNames = contributorNames(frame(publication));

    assertEquals(2, contributorNames.size(), "both contributor entries must be kept");
    contributorNames.forEach(name -> assertSingleName("Mai Olsen Nes", name));
  }

  @Test
  void shouldChooseLongestNameAndEmbedPublisherWhenPublisherIsSamePersonAsContributor()
      throws JsonProcessingException {
    var identityId = randomUri();
    var publisher = identity(identityId, "Mikkel Rev");
    var publication =
        researchDataPublication(publisher, contributor(identityId, "Mikkel Satosk Rev"));

    var framedResult = frame(publication);

    var contributorNames = contributorNames(framedResult);
    assertEquals(1, contributorNames.size());
    assertSingleName("Mikkel Satosk Rev", contributorNames.getFirst());
    assertPublisherEmbedded(framedResult, identityId, "Mikkel Satosk Rev");
  }

  @Test
  void shouldEmbedPersonPublisherWithNameWhenPublisherIsNotAContributor()
      throws JsonProcessingException {
    var publisher = identity(randomUri(), "Ante Fred");
    var publication = researchDataPublication(publisher, contributor(randomUri(), "Inga Fare"));

    var framedResult = frame(publication);

    assertPublisherEmbedded(framedResult, publisher.getId(), "Ante Fred");
  }

  @Test
  void shouldKeepLexicographicallyGreatestNameWhenMergedIdentityHasNamesOfEqualLength()
      throws JsonProcessingException {
    var identityId = randomUri();
    var publication =
        publicationWithContributors(
            contributor(identityId, "Kristian Sand"), contributor(identityId, "Kristian Sund"));

    var contributorNames = contributorNames(frame(publication));

    assertEquals(2, contributorNames.size());
    contributorNames.forEach(name -> assertSingleName("Kristian Sund", name));
  }

  @Test
  void shouldLeaveIdentityWithoutNameUntouched() throws JsonProcessingException {
    var identityId = randomUri();
    var publication = publicationWithContributors(contributor(identityId, null));

    var identity = frame(publication).at(CONTRIBUTORS_JSON_PTR).get(0).at("/identity");

    assertEquals(identityId.toString(), identity.at("/id").textValue());
    var name = identity.at("/name");
    assertTrue(name.isMissingNode() || name.isNull(), "unexpected name " + name);
  }

  @Test
  void shouldNotMergeNamesOfContributorsWithoutIdentityId() throws JsonProcessingException {
    var publication =
        publicationWithContributors(contributor(null, "Per Nille"), contributor(null, "Per Sille"));

    var contributorNames =
        contributorNames(frame(publication)).stream().map(JsonNode::textValue).sorted().toList();

    assertEquals(List.of("Per Nille", "Per Sille"), contributorNames);
  }

  private static void assertSingleName(String expectedName, JsonNode nameNode) {
    assertTrue(nameNode.isTextual(), "identity.name should be a single string but was " + nameNode);
    assertEquals(expectedName, nameNode.textValue());
  }

  private static void assertPublisherEmbedded(
      JsonNode framedResult, URI expectedId, String expectedName) {
    var publisherNode = framedResult.at(PUBLISHER_JSON_PTR);
    assertEquals(expectedId.toString(), publisherNode.at("/id").textValue());
    assertEquals(expectedName, publisherNode.at("/name").textValue());
    assertEquals("Identity", publisherNode.at("/type").textValue());
  }

  private static Publication publicationWithContributors(Contributor... contributors) {
    return withContributors(randomPublication(AcademicArticle.class), contributors);
  }

  private static Publication researchDataPublication(
      Identity publisher, Contributor... contributors) {
    var publication = withContributors(randomPublication(SoftwareSourceCode.class), contributors);
    publication
        .getEntityDescription()
        .getReference()
        .setPublicationContext(new ResearchData(publisher));
    return publication;
  }

  private static Publication withContributors(
      Publication publication, Contributor... contributors) {
    publication.getEntityDescription().setContributors(List.of(contributors));
    publication.setProjects(List.of());
    publication.setFundings(Set.of());
    return publication;
  }

  private static Identity identity(URI identityId, String name) {
    return new Identity.Builder().withId(identityId).withName(name).build();
  }

  private static Contributor contributor(URI identityId, String name) {
    return randomPublication(AcademicArticle.class)
        .getContributors()
        .getFirst()
        .copy()
        .withIdentity(identity(identityId, name))
        .withAffiliations(List.of())
        .build();
  }

  private static JsonNode frame(Publication publication) throws JsonProcessingException {
    var frame = SearchIndexFrame.getFrameWithContext(FRAME);
    var generator =
        new FramedJsonGenerator(
            List.of(toJsonLd(publication)), frame, FakeUriRetriever.newInstance());
    return objectMapper.readTree(generator.getFramedJson());
  }

  private static InputStream toJsonLd(Publication publication) throws JsonProcessingException {
    var json = (ObjectNode) objectMapper.readTree(objectMapper.writeValueAsString(publication));
    json.put(ID_FIELD, randomUri().toString());
    json.set(
        CONTEXT_FIELD,
        objectMapper.readTree(Publication.getJsonLdContext(ExpansionConfig.getApiHost())));
    return stringToStream(json.toString());
  }

  private static List<JsonNode> contributorNames(JsonNode framedResult) {
    return stream(framedResult.at(CONTRIBUTORS_JSON_PTR).spliterator(), false)
        .map(contributor -> contributor.at("/identity/name"))
        .toList();
  }
}
