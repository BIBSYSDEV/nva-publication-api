package no.unit.nva.publication.doi;

import com.amazonaws.services.lambda.runtime.events.DynamodbEvent.DynamodbStreamRecord;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.Locale;
import java.util.Map;
import no.unit.nva.publication.doi.dto.Publication;
import no.unit.nva.publication.doi.dto.Publication.Builder;
import no.unit.nva.publication.doi.dto.PublicationDate;
import no.unit.nva.publication.doi.dto.PublicationMapping;
import no.unit.nva.publication.doi.dto.PublicationType;
import no.unit.nva.publication.doi.dynamodb.dao.DynamodbStreamRecordImageDao;
import no.unit.nva.publication.doi.dynamodb.dao.DynamodbStreamRecordJsonPointers;
import nva.commons.utils.JsonUtils;

public class PublicationMapper {

    public static final String ERROR_NAMESPACE_MUST_CONTAIN_SUFFIX_SLASH = "Namespace must end with /";
    private static final String NAMESPACE_PUBLICATION = "publication";
    public static final String EMPTY_JSON_POINTER_BASE = "";

    protected String namespacePublication;
    private static final ObjectMapper objectMapper = JsonUtils.objectMapper;

    /**
     * Construct a mapper to map between DAOs to DTOs.
     *
     * @param namespace Namespace to use for constructing ids from identifiers that are owned by Publication.
     */
    public PublicationMapper(String namespace) {
        if (namespace == null || !namespace.endsWith("/")) {
            throw new IllegalArgumentException(ERROR_NAMESPACE_MUST_CONTAIN_SUFFIX_SLASH);
        }

        var ns = namespace.toLowerCase(Locale.US);
        this.namespacePublication = ns + NAMESPACE_PUBLICATION;
    }

    private static URI transformIdentifierToId(String namespace, String identifier) {
        return URI.create(namespace + identifier);
    }

    /**
     * Map a DynamodbStreamRecord with oldImage and/or newImage to PublicationMapping. Publication is a wrapper object
     * containing mapped old and/or new Publication.
     *
     * @param streamRecord  DynamodbStreamRecord
     * @return PublicationMapping
     */
    public PublicationMapping fromDynamodbStreamRecord(DynamodbStreamRecord streamRecord) {
        var publicationMappingBuilder = PublicationMapping.Builder.newBuilder();
        var dynamodb = streamRecord.getDynamodb();

        if (dynamodb != null) {
            var streamViewType = dynamodb.getStreamViewType();

            if (streamViewType.contains("OLD")) {
                Publication oldPublication = fromDynamodbStreamRecordImage(dynamodb.getOldImage());
                publicationMappingBuilder.withOldPublication(oldPublication);
            }

            if (streamViewType.contains("NEW")) {
                var newPublication = fromDynamodbStreamRecordImage(dynamodb.getNewImage());
                publicationMappingBuilder.withNewPublication(newPublication);
            }
        }

        return publicationMappingBuilder.build();
    }

    private Publication fromDynamodbStreamRecordImage(Map<String,AttributeValue> image) {
        var jsonNode = objectMapper.convertValue(image, JsonNode.class);
        return fromDynamodbStreamRecordImage(jsonNode);
    }

    private Publication fromDynamodbStreamRecordImage(JsonNode jsonNode) {
        var jsonPointers = new DynamodbStreamRecordJsonPointers(EMPTY_JSON_POINTER_BASE);
        var dynamodbStreamRecordImageDao =
            new DynamodbStreamRecordImageDao.Builder(jsonPointers)
                .withDynamodbStreamRecordImage(jsonNode)
                .build();
        return fromDynamodbStreamRecordDao(dynamodbStreamRecordImageDao);
    }

    /**
     * Map to doi.{@link Publication} from {@link DynamodbStreamRecordImageDao}.
     *
     * @param dao {@link DynamodbStreamRecordImageDao}
     * @return Publication doi.Publication
     */
    public Publication fromDynamodbStreamRecordDao(DynamodbStreamRecordImageDao dao) {
        return Builder.newBuilder()
            .withId(transformIdentifierToId(namespacePublication, dao.getIdentifier()))
            .withInstitutionOwner(URI.create(dao.getPublisherId()))
            .withMainTitle(dao.getMainTitle())
            .withType(PublicationType.findByName(dao.getPublicationInstanceType()))
            .withPublicationDate(new PublicationDate(dao.getPublicationReleaseDate()))
            .withDoi(URI.create(dao.getDoi()))
            .withContributor(ContributorMapper.fromIdentityDaos(dao.getContributorIdentities()))
            .build();
    }
}
