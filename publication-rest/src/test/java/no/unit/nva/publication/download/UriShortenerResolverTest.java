package no.unit.nva.publication.download;

import static no.unit.nva.testutils.RandomDataGenerator.randomInstant;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AmazonDynamoDBException;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import no.unit.nva.publication.download.utils.UriShortenerLocalDynamoDb;
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

class UriShortenerResolverTest extends UriShortenerLocalDynamoDb {
    private static final String UNPARSABLE_URI = "https://doi.org/10.1577/1548-8667(1998)010<0056:EOOAFI>2.0.CO;2";
    private static final String TABLE_NAME = "url_shortener";
    private static final String SHORTENED_URI_KEY = "shortenedUri";

    private UriResolver uriResolver;
    private UriShortener uriShortener;

    @BeforeEach
    void initialize() {
        super.init(TABLE_NAME);
        this.uriResolver = new UriResolverImpl(client, TABLE_NAME);
        this.uriShortener = new UriShortenerImpl(UriWrapper.fromUri(randomUri()).getUri(),
                                                 new UriShortenerWriteClient(client, TABLE_NAME));
    }

    @Test
    void shouldThrowNotFoundExceptionWhenShortenedUriIsNotInDatabase() {
        assertThrows(NotFoundException.class, () -> uriResolver.resolve(randomUri()));
    }

    @Test
    void shouldThrowBadGatewayErrorWhenDynamoDbClientThrowsException() {
        var client = mock(AmazonDynamoDB.class);
        when(client.getItem(any())).thenThrow(AmazonDynamoDBException.class);
        uriResolver = new UriResolverImpl(client, TABLE_NAME);
        assertThrows(BadGatewayException.class, () -> uriResolver.resolve(randomUri()));
    }

    @Test
    void shouldThrowInternalServerErrorWhenDtoCannotParseDynamoDbItem() {
        var client = mock(AmazonDynamoDB.class);
        var unparsableGetItemResult = new GetItemResult();
        unparsableGetItemResult.addItemEntry(SHORTENED_URI_KEY, new AttributeValue(UNPARSABLE_URI));
        when(client.getItem(any())).thenReturn(unparsableGetItemResult);
        uriResolver = new UriResolverImpl(client, TABLE_NAME);
        assertThrows(GatewayResponseSerializingException.class, () -> uriResolver.resolve(randomUri()));
    }

    @Test
    void shouldReturnLongUriWhenShortenedUriIsInDatabase() throws ApiGatewayException {
        var longUri = randomUri();
        var shortenedUri = uriShortener.shorten(longUri, randomInstant());
        var actualResult = uriResolver.resolve(shortenedUri);
        assertThat(actualResult, is(equalTo(longUri)));
    }
}
