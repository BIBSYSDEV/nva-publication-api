package no.sikt.nva.brage.migration.testutils;

import no.unit.nva.stubs.FakeS3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CopyObjectResponse;

public class FakeS3ClientThrowingExceptionWhenCopying extends FakeS3Client {

    @Override
    public CopyObjectResponse copyObject(CopyObjectRequest copyObjectRequest) {
        throw new RuntimeException("I threw an exception");
    }
}
