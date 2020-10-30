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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


/**
 * This class should read events that gets invoked as a LambdaDestination based on published DynamodbEvent from Jan's code.
 */
@JacocoGenerated
public class DynamodDbEventConducer extends EventHandler<DynamodbEvent, PublicationCollection> {
    private final PublicationMapper publicationMapper;

    @JacocoGenerated
    public DynamodDbEventConducer() {
        super(DynamodbEvent.class);
        this.publicationMapper = defaultPublicationMapper();
    }



    private static PublicationMapper defaultPublicationMapper() {
        return new PublicationMapper("http://example.net/namespace");
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

        return new PublicationCollection(dtos);
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
