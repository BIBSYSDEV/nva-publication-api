package no.unit.nva.publication.services;

import static java.util.Objects.isNull;
import java.net.URI;
import no.unit.nva.publication.services.model.UriMap;
import no.unit.nva.publication.services.storage.UriMapDao;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadGatewayException;
import nva.commons.apigateway.exceptions.GatewayResponseSerializingException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;

public class UriResolverImpl implements UriResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(UriResolverImpl.class);
    public static final String COULD_NOT_RESOLVE_MESSAGE = "could not resolve %s";
    private static final String TABLE_NAME_ENVIRONMENT_VARIABLE = "SHORTENED_URI_TABLE_NAME";

    private final DynamoDbClient client;
    private final String tableName;

    @JacocoGenerated
    public static UriResolverImpl createDefault() {
        return new UriResolverImpl(
            DynamoDbClient.create(), new Environment().readEnv(TABLE_NAME_ENVIRONMENT_VARIABLE));
    }

    public UriResolverImpl(DynamoDbClient client, String tableName) {
        this.client = client;
        this.tableName = tableName;
    }


    @Override
    public URI resolve(URI alias) throws ApiGatewayException {
        var uriMap = findUriMapById(alias);
        return uriMap.longUri();
    }

    private static UriMap parseResultToUriMap(GetItemResponse getItemResponse) throws GatewayResponseSerializingException {
        try {
            return new UriMapDao(getItemResponse.item()).getUriMap();
        } catch (Exception e) {
            throw new GatewayResponseSerializingException(e);
        }
    }

    private UriMap findUriMapById(URI shortenedUri) throws ApiGatewayException {
        var getItemResponse = queryDatabase(shortenedUri);
        if (isNull(getItemResponse.item()) || getItemResponse.item().isEmpty()) {
            throw new NotFoundException(String.format(COULD_NOT_RESOLVE_MESSAGE, shortenedUri.toString()));
        }
        return parseResultToUriMap(getItemResponse);
    }

    private GetItemResponse queryDatabase(URI shortenedUri) throws ApiGatewayException {
        try {
            return client.getItem(createGetItemRequest(shortenedUri));
        } catch (Exception e) {
            LOGGER.error("DynamoDb exception: ", e);
            throw new BadGatewayException(String.format(COULD_NOT_RESOLVE_MESSAGE, shortenedUri));
        }
    }

    private GetItemRequest createGetItemRequest(URI shortenedUri) {
        return GetItemRequest.builder().tableName(tableName).key(UriMapDao.createKey(shortenedUri)).build();
    }
}
