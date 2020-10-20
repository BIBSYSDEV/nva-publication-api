package no.unit.nva.publication.doi;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import no.unit.nva.publication.doi.dto.Contributor;
import no.unit.nva.publication.doi.dto.Publication;
import no.unit.nva.publication.doi.dto.Publication.Builder;
import no.unit.nva.publication.doi.dto.PublicationDate;
import no.unit.nva.publication.doi.dto.PublicationType;
import nva.commons.utils.JsonUtils;

public class PublicationMapper {

    public static final String DEFAULT_ROOT = "/dynamodb";
    public static final String PUBLICATION_TYPE = "Publication";
    protected static final ObjectMapper objectMapper = JsonUtils.objectMapper;

    private final String root;

    private static final JsonPointer CONTRIBUTOR_ARP_ID_JSON_POINTER = JsonPointer.compile("/m/identity/m/arpId/s");
    private static final JsonPointer CONTRIBUTOR_NAME_JSON_POINTER = JsonPointer.compile("/m/identity/m/name/s");

    private static final JsonPointer CONTRIBUTORS_LIST_POINTER = JsonPointer.compile(
        "/newImage/entityDescription/m/contributors/l");

    private static final JsonPointer PUBLICATION_IDENTIFIER_POINTER
        = JsonPointer.compile("/newImage/identifier/s");
    private static final JsonPointer PUBLICATION_TYPE_POINTER = JsonPointer.compile(
        "/newImage/entityDescription/m/reference/m/publicationInstance/m/type/s");
    private static final JsonPointer PUBLICATION_ENTITY_DESCRIPTION_POINTER = JsonPointer.compile(
        "/newImage/entityDescription/m");
    private static final JsonPointer DOI_POINTER = JsonPointer.compile(
        "/newImage/entityDescription/m/reference/m/doi/s");
    private static final JsonPointer MAIN_TITLE_POINTER = JsonPointer.compile(
        "/newImage/entityDescription/m/mainTitle/s");
    private static final JsonPointer TYPE_POINTER = JsonPointer.compile("/newImage/type/s");
    private static final JsonPointer INSTITUTION_OWNER_POINTER = JsonPointer.compile("/newImage/publisherId/s");

    public PublicationMapper() {
        this(DEFAULT_ROOT);
    }

    /**
     * Construct a PublicationMapper where json pointer for data lookups is prefixed with ROOT.
     * (because a Publication DTO payload can be wrapped under other json structures)
     * @param root root for Publication Dynamodb Event or Stream Record.
     */
    public PublicationMapper(String root) {
        this.root = root;
    }

    /**
     * Map to doi.Publication from a dynamo db stream record from nva_publication / nva_resources
     *
     * @param publicationIdPrefix                        prefix for a publication, from running environment
     *                                                   (https://nva.unit.no/publication)
     * @param json detail.dynamodb serialized as a string
     * @return Publication doi.Publication
     * @throws IOException on IO exception
     */
    public Publication fromDynamodbStreamRecord(String publicationIdPrefix,
                                                       String json)
        throws IOException {
        var record = JsonUtils.objectMapper.readTree(json);

        return parseDynamodbStreamRecord(publicationIdPrefix, record.at(root));
    }

    private Publication parseDynamodbStreamRecord(String publicationIdPrefix, JsonNode record) {
        var typeAttribute = textFromNode(record, TYPE_POINTER);
        if (typeAttribute == null || !typeAttribute.equals(PUBLICATION_TYPE)) {
            throw new IllegalArgumentException("Must be a dynamodb stream record of type Publication");
        }
        var publicationBuilder = Builder.newBuilder();
        publicationBuilder
            .withId(transformIdentifierToId(publicationIdPrefix, record))
            .withType(PublicationType.findByName(textFromNode(record, PUBLICATION_TYPE_POINTER)))
            .withPublicationDate(new PublicationDate(record.at(PUBLICATION_ENTITY_DESCRIPTION_POINTER)))
            .withMainTitle(textFromNode(record, MAIN_TITLE_POINTER))
            .withInstitutionOwner(URI.create(textFromNode(record, INSTITUTION_OWNER_POINTER)))
            .withContributor(extractContributors(record));
        extractDoiUrl(record).ifPresent(publicationBuilder::withDoi);
        return publicationBuilder.build();
    }

    private URI transformIdentifierToId(String publicationIdPrefix, JsonNode record) {
        return URI.create(publicationIdPrefix + textFromNode(record, PUBLICATION_IDENTIFIER_POINTER));
    }

    private List<Contributor> extractContributors(JsonNode record) {
        return toStream(record.at(CONTRIBUTORS_LIST_POINTER))
            .map(PublicationMapper::extractContributor)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private Optional<URI> extractDoiUrl(JsonNode record) {
        return Optional.ofNullable(textFromNode(record, DOI_POINTER))
            .map(URI::create);
    }

    private static Stream<JsonNode> toStream(JsonNode node) {
        return StreamSupport.stream(node.spliterator(), false);
    }

    private static Contributor extractContributor(JsonNode jsonNode) {
        var name = optionalTextFromNode(jsonNode, CONTRIBUTOR_NAME_JSON_POINTER);
        if (name.isEmpty()) {
            return null;
        }
        var arpId = optionalTextFromNode(jsonNode, CONTRIBUTOR_ARP_ID_JSON_POINTER);

        Contributor.Builder builder = new Contributor.Builder();
        builder.withName(name.get());
        arpId.ifPresent(id -> builder.withId(URI.create(id)));
        return builder.build();
    }

    private static String textFromNode(JsonNode jsonNode, JsonPointer jsonPointer) {
        JsonNode json = jsonNode.at(jsonPointer);
        return isPopulatedJsonPointer(json) ? json.asText() : null;
    }

    private static Optional<String> optionalTextFromNode(JsonNode jsonNode, JsonPointer jsonPointer) {
        return Optional.ofNullable(textFromNode(jsonNode, jsonPointer));
    }

    private static boolean isPopulatedJsonPointer(JsonNode json) {
        return !json.isNull() && !json.asText().isBlank();
    }
}
