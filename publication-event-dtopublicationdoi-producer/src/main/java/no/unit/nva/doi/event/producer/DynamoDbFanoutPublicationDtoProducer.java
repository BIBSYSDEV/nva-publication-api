package no.unit.nva.doi.event.producer;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import java.util.List;
import java.util.stream.Collectors;
import no.unit.nva.events.handlers.EventHandler;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.publication.doi.PublicationMapper;
import no.unit.nva.publication.doi.dto.PublicationCollection;
import no.unit.nva.publication.doi.dto.PublicationMapping;
import nva.commons.utils.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Consumes DynamodbEvent's that's been published on EventBridge, and produces new PublicationCollection DTO with type
 * `doi.publication`.
 */
public class DynamoDbFanoutPublicationDtoProducer extends EventHandler<DynamodbEvent, PublicationCollection> {

    public static final String TYPE_DTO_DOI_PUBLICATION = "doi.publication";
    private static final Logger logger = LoggerFactory.getLogger(DynamoDbFanoutPublicationDtoProducer.class);
    private final PublicationMapper publicationMapper;

    @JacocoGenerated
    public DynamoDbFanoutPublicationDtoProducer() {
        this(AppConfig.getNamespace());
    }

    public DynamoDbFanoutPublicationDtoProducer(String namespace) {
        super(DynamodbEvent.class);
        this.publicationMapper = new PublicationMapper(namespace);
    }

    @Override
    protected PublicationCollection processInput(DynamodbEvent input,
                                                 AwsEventBridgeEvent<DynamodbEvent> event,
                                                 Context context) {
        return fromDynamodbStreamRecords(input.getRecords());
    }

    private PublicationCollection fromDynamodbStreamRecords(List<DynamodbEvent.DynamodbStreamRecord> records) {
        var dtos = records.stream()
            .parallel()
            .map(publicationMapper::fromDynamodbStreamRecord)
            .filter(this::isEffectiveChange)
            .map(publicationMapping -> publicationMapping.getNewPublication().orElseThrow())
            .collect(Collectors.toList());
        logger.info("From {} records we made {} Publication DTOs", records.size(), dtos.size());
        return new PublicationCollection(TYPE_DTO_DOI_PUBLICATION, dtos);
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
