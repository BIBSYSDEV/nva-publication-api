package no.unit.nva.publication.services;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import java.net.URI;
import java.time.Instant;
import no.unit.nva.publication.services.model.UriMap;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;

public class UriShortenerImpl implements UriShortener {

    private static final String TABLE_NAME_ENVIRONMENT_VARIABLE = "SHORTENED_URI_TABLE_NAME";
    private final URI domain;
    private final UriShortenerWriteClient uriShortenerWriteClient;

    public UriShortenerImpl(URI domain, UriShortenerWriteClient uriShortenerWriteClient) {
        this.domain = domain;
        this.uriShortenerWriteClient = uriShortenerWriteClient;
    }

    @JacocoGenerated
    public static UriShortenerImpl createDefault() {
        return new UriShortenerImpl(UriWrapper.fromUri("https://" + new Environment().readEnv("API_HOST")).getUri(),
                                    new UriShortenerWriteClient(
            AmazonDynamoDBClientBuilder.defaultClient(), new Environment().readEnv(TABLE_NAME_ENVIRONMENT_VARIABLE)));
    }

    @Override
    public URI shorten(URI longUri, Instant expiration) {
        var uriMap = UriMap.create(longUri, expiration, domain);
        uriShortenerWriteClient.insertUriMap(uriMap);
        return uriMap.shortenedUri();
    }
}
