package no.unit.nva.publication.file.upload;

import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.publication.file.upload.config.MultipartUploadConfig.BUCKET_NAME;
import com.amazonaws.services.lambda.runtime.Context;
import java.util.ArrayList;
import java.util.List;
import no.unit.nva.publication.file.upload.restmodel.ListPartsElement;
import no.unit.nva.publication.file.upload.restmodel.ListPartsRequestBody;
import no.unit.nva.publication.file.upload.restmodel.ListPartsResponseBody;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListPartsRequest;

public class ListPartsHandler extends ApiGatewayHandler<ListPartsRequestBody, ListPartsResponseBody> {

    private final S3Client s3Client;

    @JacocoGenerated
    public ListPartsHandler() {
        this(S3Client.create(), new Environment());
    }

    public ListPartsHandler(S3Client s3Client, Environment environment) {
        super(ListPartsRequestBody.class, environment);
        this.s3Client = s3Client;
    }

    @Override
    protected void validateRequest(ListPartsRequestBody listPartsRequestBody, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        listPartsRequestBody.validate();
    }

    @Override
    protected ListPartsResponseBody processInput(ListPartsRequestBody input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        var listPartsRequest = input.toListPartsRequest(BUCKET_NAME);
        var listParts = listParts(listPartsRequest);
        return new ListPartsResponseBody(listParts);
    }

    @Override
    protected Integer getSuccessStatusCode(ListPartsRequestBody input, ListPartsResponseBody output) {
        return HTTP_OK;
    }

    private List<ListPartsElement> listParts(ListPartsRequest listPartsRequest) {
        var listPartsElements = new ArrayList<ListPartsElement>();

        var partListing = s3Client.listParts(listPartsRequest);
        boolean moreParts = true;
        while (moreParts) {
            partListing.parts().stream().map(ListPartsElement::create).forEach(listPartsElements::add);
            if (partListing.isTruncated()) {
                var partNumberMarker = partListing.nextPartNumberMarker();
                listPartsRequest = listPartsRequest.toBuilder().partNumberMarker(partNumberMarker).build();
                partListing = s3Client.listParts(listPartsRequest);
            } else {
                moreParts = false;
            }
        }

        return listPartsElements;
    }
}
