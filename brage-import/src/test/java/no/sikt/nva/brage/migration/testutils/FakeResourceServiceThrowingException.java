package no.sikt.nva.brage.migration.testutils;

import static no.sikt.nva.brage.migration.lambda.BrageEntryEventConsumerTest.RESOURCE_EXCEPTION_MESSAGE;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import java.time.Clock;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.service.impl.ResourceService;

public class FakeResourceServiceThrowingException extends ResourceService {

    private int numberOfAttempts = 0;

    public FakeResourceServiceThrowingException(AmazonDynamoDB client) {
        super(client, Clock.systemDefaultZone());
    }

    @Override
    public Publication createPublicationFromImportedEntry(Publication publication) {
        numberOfAttempts++;
        throw new RuntimeException(RESOURCE_EXCEPTION_MESSAGE);
    }

    public int getNumberOfAttempts() {
        return numberOfAttempts;
    }
}
