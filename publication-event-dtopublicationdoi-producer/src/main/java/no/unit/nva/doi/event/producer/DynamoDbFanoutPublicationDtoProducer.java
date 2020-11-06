package no.unit.nva.doi.event.producer;

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
    private static final Logger logger = LoggerFactory.getLogger(DynamoDbFanoutPublicationDtoProducer.class);
    public static final String CREATED = "Created";
    public static final String SKIPPED_CREATING = "Skipped creating";
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
        return fromDynamodbStreamRecords(input);
    }

    private PublicationHolder fromDynamodbStreamRecords(DynamodbEvent.DynamodbStreamRecord record) {
        var dto = mapToPublicationDto(record);
        logMappingResults(dto);
        return dto
            .map(publication -> new PublicationHolder(TYPE_DTO_DOI_PUBLICATION, publication))
            .orElse(NO_OUTPUT_NO_EVENT);
    }

    private void logMappingResults(Optional<Publication> dto) {
        logger.info("{} Publication DTO from DynamodbStreamRecord", dto.isPresent() ? CREATED : SKIPPED_CREATING);
    }

    private Optional<Publication> mapToPublicationDto(DynamodbEvent.DynamodbStreamRecord record) {
        return Optional.ofNullable(record)
            .map(publicationMapper::fromDynamodbStreamRecord)
            .filter(this::isEffectiveChange)
            .map(publicationMapping -> publicationMapping.getNewPublication().orElseThrow());
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
