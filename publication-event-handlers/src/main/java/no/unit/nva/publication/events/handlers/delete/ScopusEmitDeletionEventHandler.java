package no.unit.nva.publication.events.handlers.delete;

import static java.util.UUID.randomUUID;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import no.unit.nva.publication.events.bodies.ScopusDeletionEvent;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.Environment;
import nva.commons.core.paths.UnixPath;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;

public class ScopusEmitDeletionEventHandler implements RequestHandler<S3Event, Void> {

    public static final String S3_URI_TEMPLATE = "s3://%s/%s";
    public static final String SCOPUS_IDENTIFIER_DELIMITER = "DELETE-";
    public static final String EVENTS_BUCKET = new Environment().readEnv("EVENTS_BUCKET");
    public static final String EMITTED_EVENT_LOG_MESSAGE = "Emitted Event:{}";
    private static final int SINGLE_EXPECTED_FILE = 0;
    private static final Logger logger = LoggerFactory.getLogger(ScopusEmitDeletionEventHandler.class);
    private final S3Client s3Client;

    protected ScopusEmitDeletionEventHandler(S3Client s3Client) {
        super();
        this.s3Client = s3Client;
    }

    @Override
    public Void handleRequest(S3Event input, Context context) {
        attempt(() -> readFile(input))
            .map(this::toScopudIdentifierList)
            .stream().flatMap(List::stream)
            .forEach(this::emitDeletionEvent);

        return null;
    }

    private static String[] splitOnNewLine(String string) {
        return string.split("\n");
    }

    private void emitDeletionEvent(String item) {
        var event = new ScopusDeletionEvent(ScopusDeletionEvent.EVENT_TOPIC, item);
        var s3Driver = new S3Driver(s3Client, EVENTS_BUCKET);
        attempt(() -> s3Driver.insertFile(UnixPath.of(randomUUID().toString()), event.toJsonString()));
        logger.info(EMITTED_EVENT_LOG_MESSAGE, event.toJsonString());
    }

    private List<String> toScopudIdentifierList(String string) {
        return Arrays.stream(splitOnNewLine(string))
                   .map(this::extractScopusIdentifier)
                   .collect(Collectors.toList());
    }

    private String extractScopusIdentifier(String item) {
        return item.split(SCOPUS_IDENTIFIER_DELIMITER)[1];
    }

    private String readFile(S3Event event) {
        var s3Driver = new S3Driver(s3Client, extractBucketName(event));
        var fileUri = createS3BucketUri(event);
        return s3Driver.getFile(UriWrapper.fromUri(fileUri).toS3bucketPath());
    }

    private String extractBucketName(S3Event event) {
        return event.getRecords().get(SINGLE_EXPECTED_FILE).getS3().getBucket().getName();
    }

    private URI createS3BucketUri(S3Event s3Event) {
        return URI.create(String.format(S3_URI_TEMPLATE, extractBucketName(s3Event), extractFilename(s3Event)));
    }

    private String extractFilename(S3Event event) {
        return event.getRecords().get(SINGLE_EXPECTED_FILE).getS3().getObject().getKey();
    }
}
