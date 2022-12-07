package no.sikt.nva.brage.migration.testutils;

import java.util.ArrayList;
import java.util.List;
import no.unit.nva.stubs.FakeS3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CopyObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

public class FakeS3cClientWithCopyObjectSupport extends FakeS3Client {

    public static final long SOME_CONTENT_LENGTH = 2932645L;
    public static final String APPLICATION_PDF_MIMETYPE = "application/pdf";
    private final List<CopyObjectRequest> copyObjectRequestList;

    public FakeS3cClientWithCopyObjectSupport() {
        this.copyObjectRequestList = new ArrayList<>();
    }

    @Override
    public CopyObjectResponse copyObject(CopyObjectRequest copyObjectRequest) {
        copyObjectRequestList.add(copyObjectRequest);
        return CopyObjectResponse.builder().build();
    }

    public List<CopyObjectRequest> getCopyObjectRequestList() {
        return copyObjectRequestList;
    }

    @Override
    public HeadObjectResponse headObject(HeadObjectRequest headObjectRequest) {
        return HeadObjectResponse.builder()
                   .contentLength(SOME_CONTENT_LENGTH)
                   .contentType(APPLICATION_PDF_MIMETYPE)
                   .build();
    }
}
