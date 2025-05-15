package no.unit.nva.publication.service.impl;

import static no.unit.nva.publication.PublicationServiceConfig.DEFAULT_DYNAMODB_CLIENT;
import static no.unit.nva.publication.service.impl.ResourceService.DEFAULT_IDENTIFIER_SUPPLIER;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_TABLE_NAME;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import java.time.Clock;
import java.util.function.Supplier;
import no.unit.nva.auth.uriretriever.RawContentRetriever;
import no.unit.nva.auth.uriretriever.UriRetriever;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.external.services.ChannelClaimClient;

public final class ResourceServiceBuilder {

    private String tableName = RESOURCES_TABLE_NAME;
    private AmazonDynamoDB dynamoDbClient = DEFAULT_DYNAMODB_CLIENT;
    private Clock clock = Clock.systemDefaultZone();
    Supplier<SortableIdentifier> identifierSupplier = DEFAULT_IDENTIFIER_SUPPLIER;
    private RawContentRetriever uriRetriever = new UriRetriever();
    private ChannelClaimClient channelClaimClient;

    ResourceServiceBuilder() {
    }

    public ResourceServiceBuilder withDynamoDbClient(AmazonDynamoDB dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
        return this;
    }

    public ResourceServiceBuilder withTableName(String tableName) {
        this.tableName = tableName;
        return this;
    }

    public ResourceServiceBuilder withClock(Clock clock) {
        this.clock = clock;
        return this;
    }

    public ResourceServiceBuilder withIdentifierSupplier(Supplier<SortableIdentifier> identifierSupplier) {
        this.identifierSupplier = identifierSupplier;
        return this;
    }

    public ResourceServiceBuilder withUriRetriever(RawContentRetriever uriRetriever) {
        this.uriRetriever = uriRetriever;
        return this;
    }

    public ResourceServiceBuilder withChannelClaimClient(ChannelClaimClient channelClaimClient) {
        this.channelClaimClient = channelClaimClient;
        return this;
    }

    public ResourceService build() {
        return new ResourceService(dynamoDbClient, tableName, clock, identifierSupplier, uriRetriever, channelClaimClient);
    }

}
