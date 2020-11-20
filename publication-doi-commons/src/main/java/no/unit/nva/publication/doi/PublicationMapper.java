package no.unit.nva.publication.doi;

import static java.util.Objects.nonNull;
import static java.util.function.Predicate.not;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent.DynamodbStreamRecord;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.StreamViewType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import no.unit.nva.publication.doi.dto.Contributor;
import no.unit.nva.publication.doi.dto.DoiRequest;
import no.unit.nva.publication.doi.dto.DoiRequestStatus;
import no.unit.nva.publication.doi.dto.Publication;
import no.unit.nva.publication.doi.dto.Publication.Builder;
import no.unit.nva.publication.doi.dto.PublicationDate;
import no.unit.nva.publication.doi.dto.PublicationMapping;
import no.unit.nva.publication.doi.dto.PublicationStatus;
import no.unit.nva.publication.doi.dto.PublicationType;
import no.unit.nva.publication.doi.dynamodb.dao.DynamodbStreamRecordImageDao;
import no.unit.nva.publication.doi.dynamodb.dao.DynamodbStreamRecordJsonPointers;
import no.unit.nva.publication.doi.dynamodb.dao.DynamodbStreamRecordJsonPointers.DynamodbImageType;
import nva.commons.utils.JsonUtils;

/**
 * {@link PublicationMapper} reads DAOs under {@link no.unit.nva.publication.doi.dynamodb.dao} related to streaming
 * DynamodbEvent's thats been published on Event bridge.
 *
 * <p>It maps these DAOs into {@link PublicationMapping} which optionally can contain either a `oldImage` or
 * `newImage` of a {@link Publication}.
 */
public class PublicationMapper {

    public static final String ERROR_NAMESPACE_MUST_CONTAIN_SUFFIX_SLASH = "Namespace must end with /";
    public static final String FORWARD_SLASH = "/";
    private static final String NAMESPACE_PUBLICATION = "publication";
    private static final ObjectMapper objectMapper = JsonUtils.objectMapper;
    protected String namespacePublication;

    /**
     * Construct a mapper to map between DAOs to DTOs.
     *
     * @param namespace Namespace to use for constructing ids from identifiers that are owned by Publication.
     */
    public PublicationMapper(String namespace) {
        if (namespace == null || !namespace.endsWith(FORWARD_SLASH)) {
            throw new IllegalArgumentException(ERROR_NAMESPACE_MUST_CONTAIN_SUFFIX_SLASH);
        }

        this.namespacePublication = namespace.toLowerCase(Locale.US) + NAMESPACE_PUBLICATION + FORWARD_SLASH;
    }

    /**
     * Map a DynamodbStreamRecord with oldImage and/or newImage to PublicationMapping. Publication is a wrapper object
     * containing mapped old and/or new Publication.
     *
     * @param streamRecord DynamodbStreamRecord
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
                fromDynamodbStreamRecordImage(dynamodb.getOldImage())
                    .ifPresent(publicationMappingBuilder::withOldPublication);
            }

            if (acceptStreamViewTypes(streamViewType,
                StreamViewType.NEW_AND_OLD_IMAGES, StreamViewType.NEW_IMAGE)) {
                fromDynamodbStreamRecordImage(dynamodb.getNewImage())
                    .ifPresent(publicationMappingBuilder::withNewPublication);
            }
        }
        return publicationMappingBuilder.build();
    }

    /**
     * Map to doi.{@link Publication} from {@link DynamodbStreamRecordImageDao}.
     *
     * @param dao {@link DynamodbStreamRecordImageDao}
     * @return Publication doi.Publication
     */
    public Publication fromDynamodbStreamRecordDao(DynamodbStreamRecordImageDao dao) {

        return Builder.newBuilder()
            .withId(transformIdentifierToId(namespacePublication, dao))
            .withInstitutionOwner(extractPublisherId(dao))
            .withMainTitle(dao.getMainTitle())
            .withType(extractPublicationInstanceType(dao))
            .withPublicationDate(extractPublicationDate(dao))
            .withDoi(extractDoiUrl(dao))
            .withDoiRequest(extractDoiRequest(dao))
            .withModifiedDate(extractModifiedDate(dao))
            .withStatus(extractPublicationStatus(dao))
            .withContributor(extractContributors(dao))
            .build();
    }

    private static URI transformIdentifierToId(String namespace, DynamodbStreamRecordImageDao streamRecord) {
        if (nonNull(namespace) && nonNull(streamRecord.getIdentifier())) {
            return URI.create(namespace + streamRecord.getIdentifier());
        }
        return null;
    }

    private List<Contributor> extractContributors(DynamodbStreamRecordImageDao dao) {
        if (nonNull(dao.getContributorIdentities())) {
            return ContributorMapper.fromIdentityDaos(dao.getContributorIdentities());
        } else {
            return Collections.emptyList();
        }
    }

    private PublicationStatus extractPublicationStatus(DynamodbStreamRecordImageDao dao) {
        return Optional.ofNullable(dao.getStatus())
            .map(PublicationStatus::lookup)
            .orElse(null);
    }

    private Instant extractModifiedDate(DynamodbStreamRecordImageDao dao) {
        return nonNull(dao.getModifiedDate()) ? Instant.parse(dao.getModifiedDate()) : null;
    }

    private PublicationDate extractPublicationDate(DynamodbStreamRecordImageDao dao) {
        return Optional.ofNullable(dao.getPublicationReleaseDate())
            .map(PublicationDate::new)
            .orElse(null);
    }

    private URI extractPublisherId(DynamodbStreamRecordImageDao dao) {
        return Optional.ofNullable(dao.getPublisherId())
            .map(URI::create)
            .orElse(null);
    }

    private DoiRequest extractDoiRequest(DynamodbStreamRecordImageDao dao) {
        JsonNode doiRequest = dao.getDoiRequest();
        if (nodeExists(doiRequest)) {
            return createNewDoiRequestObject(doiRequest);
        } else {
            return null;
        }
    }

    private DoiRequest createNewDoiRequestObject(JsonNode doiRequest) {
        var jsonPointers = new DynamodbStreamRecordJsonPointers(DynamodbImageType.NONE);
        var status = extractDoiRequestStatus(jsonPointers, doiRequest);
        var modifiedDate = extractDoiRequestModifiedDate(jsonPointers, doiRequest);

        return new DoiRequest.Builder()
            .withStatus(status)
            .withModifiedDate(modifiedDate)
            .build();
    }

    private boolean nodeExists(JsonNode doiRequest) {
        return nonNull(doiRequest) && !doiRequest.isMissingNode();
    }

    private Instant extractDoiRequestModifiedDate(DynamodbStreamRecordJsonPointers jsonPointers, JsonNode doiRequest) {
        return Optional.of(jsonPointers.getDoiRequestModifiedDateJsonPointer())
            .map(doiRequest::at)
            .map(JsonNode::textValue)
            .map(Instant::parse)
            .orElse(null);
    }

    private DoiRequestStatus extractDoiRequestStatus(DynamodbStreamRecordJsonPointers jsonPointers,
                                                     JsonNode doiRequest) {

        return Optional.ofNullable(jsonPointers.getDoiRequestStatusJsonPointer())
            .map(doiRequest::at)
            .map(JsonNode::textValue)
            .map(DoiRequestStatus::lookup)
            .orElse(null);
    }

    private boolean acceptStreamViewTypes(String streamViewType, StreamViewType... streamViewTypes) {
        return Arrays.stream(streamViewTypes)
            .map(StreamViewType::getValue)
            .anyMatch(s -> s.equals(streamViewType));
    }

    private Optional<Publication> fromDynamodbStreamRecordImage(Map<String, AttributeValue> image) {
        if (image == null || image.isEmpty()) {
            return Optional.empty();
        }

        var jsonNode = objectMapper.convertValue(image, JsonNode.class);
        return Optional.of(fromDynamodbStreamRecordImage(jsonNode));
    }

    private Publication fromDynamodbStreamRecordImage(JsonNode jsonNode) {
        var jsonPointers = new DynamodbStreamRecordJsonPointers(DynamodbImageType.NONE);
        var dynamodbStreamRecordImageDao =
            new DynamodbStreamRecordImageDao.Builder(jsonPointers)
                .withDynamodbStreamRecordImage(jsonNode)
                .build();
        return fromDynamodbStreamRecordDao(dynamodbStreamRecordImageDao);
    }

    private URI extractDoiUrl(DynamodbStreamRecordImageDao dao) {
        return Optional.ofNullable(dao.getDoi())
            .filter(not(String::isBlank))
            .map(URI::create)
            .orElse(null);
    }

    private PublicationType extractPublicationInstanceType(DynamodbStreamRecordImageDao dao) {
        return Optional.ofNullable(dao.getPublicationInstanceType())
            .filter(not(String::isBlank))
            .map(PublicationType::findByName)
            .orElse(null);
    }
}
