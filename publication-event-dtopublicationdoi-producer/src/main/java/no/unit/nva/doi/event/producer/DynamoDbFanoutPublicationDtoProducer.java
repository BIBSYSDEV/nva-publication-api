package no.unit.nva.doi.event.producer;

import static nva.commons.utils.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import java.util.Optional;
import no.unit.nva.events.handlers.EventHandler;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.publication.doi.PublicationMapper;
import no.unit.nva.publication.doi.dto.Publication;
import no.unit.nva.publication.doi.dto.PublicationHolder;
import no.unit.nva.publication.doi.dto.PublicationMapping;
import nva.commons.utils.JacocoGenerated;
import nva.commons.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Consumes DynamodbEvent's that's been published on EventBridge, and produces new PublicationCollection DTO with type
 * `doi.publication`.
 */
public class DynamoDbFanoutPublicationDtoProducer
    extends EventHandler<DynamodbEvent.DynamodbStreamRecord, PublicationHolder> {

    public static final String TYPE_DTO_DOI_PUBLICATION = "doi.publication";
    public static final PublicationHolder NO_OUTPUT_NO_EVENT = null;
    public static final String CREATED = "Created";
    public static final String SKIPPED_CREATING = "Skipped creating";
    private static final Logger logger = LoggerFactory.getLogger(DynamoDbFanoutPublicationDtoProducer.class);
    private final PublicationMapper publicationMapper;

    @JacocoGenerated
    public DynamoDbFanoutPublicationDtoProducer() {
        this(AppConfig.getNamespace());
    }

    public DynamoDbFanoutPublicationDtoProducer(String namespace) {
        super(DynamodbEvent.DynamodbStreamRecord.class);
        this.publicationMapper = new PublicationMapper(namespace);
    }

    @Override
    protected PublicationHolder processInput(DynamodbEvent.DynamodbStreamRecord input,
                                             AwsEventBridgeEvent<DynamodbEvent.DynamodbStreamRecord> event,
                                             Context context) {
        PublicationHolder result = fromDynamodbStreamRecords(input);
        //temporary logging until event consumers are built
        logResults(result);
        return result;
    }

    private void logResults(PublicationHolder result) {
        String jsonString = attempt(() -> JsonUtils.objectMapper.writeValueAsString(result)).orElse(fail -> null);
        logger.info("Output is: " + jsonString);
    }

    private PublicationHolder fromDynamodbStreamRecords(DynamodbEvent.DynamodbStreamRecord record) {
        var dto = mapToPublicationDto(record);
        logMappingResults(dto.orElse(null));

        return dto
            .map(publication -> new PublicationHolder(TYPE_DTO_DOI_PUBLICATION, publication))
            .orElse(NO_OUTPUT_NO_EVENT);

    }

    private void logMappingResults(Publication dto) {
        logger.info("{} Publication DTO from DynamodbStreamRecord", dto != null ? CREATED : SKIPPED_CREATING);
    }

    private Optional<Publication> mapToPublicationDto(DynamodbEvent.DynamodbStreamRecord record) {
        return Optional.ofNullable(record)
            .map(publicationMapper::fromDynamodbStreamRecord)
            .filter(this::shouldPropagateEvent)
            .map(publicationMapping -> publicationMapping.getNewPublication().orElseThrow());
    }

    private boolean shouldPropagateEvent(PublicationMapping publicationMapping) {
        boolean isChange = isEffectiveChange(publicationMapping);
        boolean publicationHasDoiRequest = publicationHasDoiRequest(publicationMapping);

        return isChange && publicationHasDoiRequest;
    }

    private boolean publicationHasDoiRequest(PublicationMapping publicationMapping) {
        return publicationMapping
            .getNewPublication()
            .map(Publication::getDoiRequest)
            .isPresent();
    }

    private boolean isEffectiveChange(PublicationMapping publicationMapping) {
        var newPublication = publicationMapping.getNewPublication().orElse(null);
        var oldPublication = publicationMapping.getOldPublication().orElse(null);

        if (newPublication != null) {
            return !newPublication.equals(oldPublication);
        }
        return false;
    }
}
