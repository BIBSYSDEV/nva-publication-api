package no.unit.nva.publication.download;

import static no.unit.nva.testutils.RandomDataGenerator.randomInstant;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.Map;
import no.unit.nva.publication.download.utils.UriShortenerLocalDynamoDb;
import no.unit.nva.publication.exception.TransactionFailedException;
import no.unit.nva.publication.services.UriResolver;
import no.unit.nva.publication.services.UriResolverImpl;
import no.unit.nva.publication.services.UriShortener;
import no.unit.nva.publication.services.UriShortenerImpl;
import no.unit.nva.publication.services.UriShortenerWriteClient;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadGatewayException;
import nva.commons.apigateway.exceptions.GatewayResponseSerializingException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;

class UriShortenerResolverTest extends UriShortenerLocalDynamoDb {
    private static final String UNPARSABLE_URI = "https://doi.org/10.1577/1548-8667(1998)010<0056:EOOAFI>2.0.CO;2";
    private static final String TABLE_NAME = "url_shortener";
    private static final String SHORTENED_URI_KEY = "shortenedUri";

    private UriResolver uriResolver;
    private UriShortener uriShortener;
    private String basePath;

    @BeforeEach
    void initialize() {
        super.init(TABLE_NAME);
        this.uriResolver = new UriResolverImpl(client, TABLE_NAME);
        this.uriShortener = new UriShortenerImpl(UriWrapper.fromUri(randomUri()),
                                                 new UriShortenerWriteClient(client, TABLE_NAME));
        this.basePath = randomString();
    }

    @Test
    void shouldThrowNotFoundExceptionWhenShortenedUriIsNotInDatabase() {
        assertThrows(NotFoundException.class, () -> uriResolver.resolve(randomUri()));
    }

    @Test
    void shouldThrowBadGatewayErrorWhenDynamoDbClientThrowsException() {
        var mockClient = mock(DynamoDbClient.class);
        when(mockClient.getItem(any(GetItemRequest.class))).thenThrow(DynamoDbException.class);
        uriResolver = new UriResolverImpl(mockClient, TABLE_NAME);
        assertThrows(BadGatewayException.class, () -> uriResolver.resolve(randomUri()));
    }

    @Test
    void shouldThrowInternalServerErrorWhenDtoCannotParseDynamoDbItem() {
        var mockClient = mock(DynamoDbClient.class);
        var unparsableGetItemResult = GetItemResponse.builder()
                                          .item(Map.of(SHORTENED_URI_KEY, AttributeValue.builder().s(UNPARSABLE_URI).build()))
                                          .build();
        when(mockClient.getItem(any(GetItemRequest.class))).thenReturn(unparsableGetItemResult);
        uriResolver = new UriResolverImpl(mockClient, TABLE_NAME);
        assertThrows(GatewayResponseSerializingException.class, () -> uriResolver.resolve(randomUri()));
    }

    @Test
    void shouldReturnLongUriWhenShortenedUriIsInDatabase() throws ApiGatewayException {
        var longUri = randomUri();
        var shortenedUri = uriShortener.shorten(longUri, basePath, randomInstant());
        var actualResult = uriResolver.resolve(shortenedUri);
        assertThat(actualResult, is(equalTo(longUri)));
    }

    @Test
    void shouldThrowTransactionFailedExceptionOnTransactionFail() {
        var longUri = randomUri();
        var mockClient = mock(DynamoDbClient.class);
        this.uriShortener = new UriShortenerImpl(UriWrapper.fromUri(randomUri()),
                                                 new UriShortenerWriteClient(mockClient, TABLE_NAME));
        when(mockClient.transactWriteItems(any(TransactWriteItemsRequest.class))).thenThrow(DynamoDbException.class);
        assertThrows(TransactionFailedException.class, () -> uriShortener.shorten(longUri, basePath, randomInstant()));
    }
}
