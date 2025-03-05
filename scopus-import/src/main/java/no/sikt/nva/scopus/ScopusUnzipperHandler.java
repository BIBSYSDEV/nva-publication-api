package no.sikt.nva.scopus;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import nva.commons.core.JacocoGenerated;

public class ScopusUnzipperHandler implements RequestHandler<S3Event, Void> {

    private final ScopusUnzipper scopusUnzipper;

    @JacocoGenerated
    public ScopusUnzipperHandler() {
        this(ScopusUnzipper.create());
    }

    public ScopusUnzipperHandler(ScopusUnzipper scopusUnzipper) {
        this.scopusUnzipper = scopusUnzipper;
    }

    @Override
    public Void handleRequest(S3Event event, Context context) {

        scopusUnzipper.unzipAndEnqueue(getBucketName(event), getObjectKey(event));

        return null;
    }

    private static String getObjectKey(S3Event s3Event) {
        return s3Event.getRecords().getFirst().getS3().getObject().getKey();
    }

    private static String getBucketName(S3Event s3Event) {
        return s3Event.getRecords().getFirst().getS3().getBucket().getName();
    }
}
