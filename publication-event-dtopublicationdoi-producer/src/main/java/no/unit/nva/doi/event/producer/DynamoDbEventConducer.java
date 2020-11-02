package no.unit.nva.doi.event.producer;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import no.unit.nva.events.handlers.EventHandler;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.publication.doi.PublicationMapper;
import no.unit.nva.publication.doi.dto.Publication;
import no.unit.nva.publication.doi.dto.PublicationCollection;
import no.unit.nva.publication.doi.dto.PublicationMapping;
import nva.commons.utils.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Consumes DynamodbEvent's that's been published on EventBridge, and produces new PublicationCollection DTO with type
 * `doi.publication`.
 */
public class DynamoDbEventConducer extends EventHandler<DynamodbEvent, PublicationCollection> {

    private static final Logger logger = LoggerFactory.getLogger(DynamoDbEventConducer.class);
    public static final String TYPE_DTO_DOI_PUBLICATION = "doi.publication";
    private final PublicationMapper publicationMapper;

    @JacocoGenerated
    public DynamoDbEventConducer() {
        this(AppEnv.getNamespace());
    }

    public DynamoDbEventConducer(String namespace) {
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
        List<Publication> dtos = new ArrayList<>();
        for (DynamodbEvent.DynamodbStreamRecord record : records) {
            var publicationMapping = publicationMapper.fromDynamodbStreamRecord(record);
            if (isEffectiveChange(publicationMapping) && publicationMapping.getNewPublication().isPresent()) {
                dtos.add(publicationMapping.getNewPublication().get());
            }
        }
        logger.info("From {} records we made {} Publication DTOs", records.size(), dtos.size());
        return new PublicationCollection(TYPE_DTO_DOI_PUBLICATION, dtos);
    }

    private boolean isEffectiveChange(PublicationMapping publicationMapping) {
        Optional<Publication> newPublication = publicationMapping.getNewPublication();
        Optional<Publication> oldPublication = publicationMapping.getOldPublication();

        boolean isEffectiveChange = false;
        if (newPublication.isPresent()) {
            if (oldPublication.isPresent()) {
                isEffectiveChange = !newPublication.get().equals(oldPublication.get());
            } else {
                isEffectiveChange = true;
            }
        }
        return isEffectiveChange;
    }
}
