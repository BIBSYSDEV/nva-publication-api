package no.sikt.nva.brage.migration.testutils;

import java.util.ArrayList;
import java.util.List;
import no.unit.nva.stubs.FakeS3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CopyObjectResponse;

public class FakeS3cClientWithCopyObjectSupport extends FakeS3Client {

    private final List<CopyObjectRequest> copyObjectRequestList;

    public FakeS3cClientWithCopyObjectSupport() {
        this.copyObjectRequestList = new ArrayList<>();
    }

    @Override
    public CopyObjectResponse copyObject ( CopyObjectRequest copyObjectRequest) {
        copyObjectRequestList.add(copyObjectRequest);
        return CopyObjectResponse.builder().build();
    }

    public List<CopyObjectRequest> getCopyObjectRequestList() {
        return copyObjectRequestList;
    }
}
