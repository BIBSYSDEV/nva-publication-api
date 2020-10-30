package no.unit.nva.publication.doi;

import com.amazonaws.services.lambda.runtime.events.DynamodbEvent.DynamodbStreamRecord;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.StreamViewType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import no.unit.nva.publication.doi.dto.Publication;
import no.unit.nva.publication.doi.dto.Publication.Builder;
import no.unit.nva.publication.doi.dto.PublicationDate;
import no.unit.nva.publication.doi.dto.PublicationMapping;
import no.unit.nva.publication.doi.dto.PublicationType;
import no.unit.nva.publication.doi.dynamodb.dao.DynamodbStreamRecordImageDao;
import no.unit.nva.publication.doi.dynamodb.dao.DynamodbStreamRecordJsonPointers;
import no.unit.nva.publication.doi.dynamodb.dao.DynamodbStreamRecordJsonPointers.DynamodbImageType;
import nva.commons.utils.JsonUtils;

public class PublicationMapper {

    public static final String ERROR_NAMESPACE_MUST_CONTAIN_SUFFIX_SLASH = "Namespace must end with /";
    private static final String NAMESPACE_PUBLICATION = "publication";

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

            // do nothing for StreamViewType.KEYS_ONLY

            if (acceptStreamViewTypes(streamViewType,
                StreamViewType.NEW_AND_OLD_IMAGES, StreamViewType.OLD_IMAGE)) {
                Publication oldPublication = fromDynamodbStreamRecordImage(dynamodb.getOldImage());
                publicationMappingBuilder.withOldPublication(oldPublication);
            }

            if (acceptStreamViewTypes(streamViewType,
                StreamViewType.NEW_AND_OLD_IMAGES, StreamViewType.NEW_IMAGE)) {
                var newPublication = fromDynamodbStreamRecordImage(dynamodb.getNewImage());
                publicationMappingBuilder.withNewPublication(newPublication);
            }
        }

        return publicationMappingBuilder.build();
    }

    private boolean acceptStreamViewTypes(String streamViewType, StreamViewType... streamViewTypes) {
        return Arrays.stream(streamViewTypes)
            .map(StreamViewType::getValue)
            .filter(s -> s.equals(streamViewType))
            .findFirst()
            .isPresent();
    }

    private Publication fromDynamodbStreamRecordImage(Map<String,AttributeValue> image) {
        var jsonNode = objectMapper.convertValue(image, JsonNode.class);
        return fromDynamodbStreamRecordImage(jsonNode);
    }

    private Publication fromDynamodbStreamRecordImage(JsonNode jsonNode) {
        var jsonPointers = new DynamodbStreamRecordJsonPointers(DynamodbImageType.NONE);
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
            .withType(Optional.ofNullable(dao.getPublicationInstanceType()).map(PublicationType::findByName).orElse(null))
            .withPublicationDate(new PublicationDate(dao.getPublicationReleaseDate()))
            .withDoi(Optional.ofNullable(dao.getDoi()).map(URI::create).orElse(null))
            .withContributor(ContributorMapper.fromIdentityDaos(dao.getContributorIdentities()))
            .build();
    }
}
