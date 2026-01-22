package no.sikt.nva.brage.migration.testutils;

import static no.sikt.nva.brage.migration.lambda.BrageEntryEventConsumerTest.RESOURCE_EXCEPTION_MESSAGE;
import static org.mockito.Mockito.mock;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import java.time.Clock;
import no.unit.nva.auth.uriretriever.UriRetriever;
import no.unit.nva.model.ImportSource;
import no.unit.nva.publication.external.services.ChannelClaimClient;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.utils.CustomerService;
import no.unit.nva.publication.service.FakeCristinUnitsUtil;
import no.unit.nva.publication.service.impl.ResourceService;

public class FakeResourceServiceThrowingException extends ResourceService {

    public static final String FAKE_TABLE_NAME = "fake-table-name";
    private int numberOfAttempts = 0;

    public FakeResourceServiceThrowingException(DynamoDbClient client) {
        super(client, FAKE_TABLE_NAME, Clock.systemDefaultZone(),
              mock(UriRetriever.class), mock(ChannelClaimClient.class), mock(CustomerService.class),
              new FakeCristinUnitsUtil());
    }

    @Override
    public Resource importResource(Resource resource, ImportSource importSource, UserInstance fileOwner) {
        numberOfAttempts++;
        throw new RuntimeException(RESOURCE_EXCEPTION_MESSAGE);
    }

    public int getNumberOfAttempts() {
        return numberOfAttempts;
    }
}
