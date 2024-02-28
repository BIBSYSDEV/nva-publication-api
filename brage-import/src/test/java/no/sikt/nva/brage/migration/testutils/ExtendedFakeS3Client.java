package no.sikt.nva.brage.migration.testutils;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import java.util.ArrayList;
import java.util.List;
import no.unit.nva.stubs.FakeS3Client;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CopyObjectResponse;
import software.amazon.awssdk.services.s3.model.CopyPartResult;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.UploadPartCopyRequest;
import software.amazon.awssdk.services.s3.model.UploadPartCopyResponse;

public class ExtendedFakeS3Client extends FakeS3Client {

    public static final long SOME_CONTENT_LENGTH = 2932645L;
    public static final String APPLICATION_PDF_MIMETYPE = "application/pdf";
    private final List<CopyObjectRequest> copyObjectRequestList;
    private final List<CompleteMultipartUploadResponse> multipartCopiedResults;

    public ExtendedFakeS3Client() {
        this.copyObjectRequestList = new ArrayList<>();
        this.multipartCopiedResults = new ArrayList<CompleteMultipartUploadResponse>();
    }

    @Override
    public CopyObjectResponse copyObject(CopyObjectRequest copyObjectRequest) {
        copyObjectRequestList.add(copyObjectRequest);
        return CopyObjectResponse.builder().build();
    }

    @Override
    public HeadObjectResponse headObject(HeadObjectRequest headObjectRequest) {
        return HeadObjectResponse.builder()
                   .contentLength(SOME_CONTENT_LENGTH)
                   .contentType(APPLICATION_PDF_MIMETYPE)
                   .build();
    }

    @Override
    public CompleteMultipartUploadResponse completeMultipartUpload(CompleteMultipartUploadRequest request) {
        var response = CompleteMultipartUploadResponse.builder()
                                                    .bucket(request.bucket())
                                                    .key(request.key())
                                                    .build();
        multipartCopiedResults.add(response);
        return response;
    }

    @Override
    public CreateMultipartUploadResponse createMultipartUpload(CreateMultipartUploadRequest request) {
        return CreateMultipartUploadResponse.builder().uploadId(randomString()).build();
    }

    @Override
    public UploadPartCopyResponse uploadPartCopy(UploadPartCopyRequest request) {
        return UploadPartCopyResponse.builder()
                   .copyPartResult(CopyPartResult.builder().eTag(randomString()).build()).build();
    }

    @Override
    public AbortMultipartUploadResponse abortMultipartUpload(AbortMultipartUploadRequest request) {
        return AbortMultipartUploadResponse.builder().build();
    }

    public List<CopyObjectRequest> getCopyObjectRequestList() {
        return copyObjectRequestList;
    }

    public List<CompleteMultipartUploadResponse> getMultipartCopiedResults() {
        return multipartCopiedResults;
    }
}
