package no.unit.nva.publication.uploadfile;

import static java.net.HttpURLConnection.HTTP_OK;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ListPartsRequest;
import java.util.ArrayList;
import java.util.List;
import no.unit.nva.publication.uploadfile.restmodel.ListPartsElement;
import no.unit.nva.publication.uploadfile.restmodel.ListPartsRequestBody;
import no.unit.nva.publication.uploadfile.restmodel.ListPartsResponseBody;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class ListPartsHandler extends ApiGatewayHandler<ListPartsRequestBody, ListPartsResponseBody> {

    public static final String BUCKET_NAME = new Environment().readEnv("NVA_PERSISTED_STORAGE_BUCKET_NAME");
    private final AmazonS3 amazonS3;

    @JacocoGenerated
    public ListPartsHandler() {
        this(AmazonS3ClientBuilder.defaultClient());
    }

    public ListPartsHandler(AmazonS3 amazonS3) {
        super(ListPartsRequestBody.class);
        this.amazonS3 = amazonS3;
    }

    @Override
    protected void validateRequest(ListPartsRequestBody listPartsRequestBody, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        listPartsRequestBody.validate();
    }

    @Override
    protected ListPartsResponseBody processInput(ListPartsRequestBody input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        var listPartsRequest = toListPartsRequest(input);
        var listParts = listParts(listPartsRequest);
        return new ListPartsResponseBody(listParts);
    }

    @Override
    protected Integer getSuccessStatusCode(ListPartsRequestBody input, ListPartsResponseBody output) {
        return HTTP_OK;
    }

    private ListPartsRequest toListPartsRequest(ListPartsRequestBody input) {
        return new ListPartsRequest(BUCKET_NAME, input.key(), input.uploadId());
    }

    private List<ListPartsElement> listParts(ListPartsRequest listPartsRequest) {
        var listPartsElements = new ArrayList<ListPartsElement>();

        var partListing = amazonS3.listParts(listPartsRequest);
        boolean moreParts = true;
        while (moreParts) {
            partListing.getParts().stream().map(ListPartsElement::of).forEach(listPartsElements::add);
            if (partListing.isTruncated()) {
                var partNumberMarker = partListing.getNextPartNumberMarker();
                listPartsRequest.setPartNumberMarker(partNumberMarker);
                partListing = amazonS3.listParts(listPartsRequest);
            } else {
                moreParts = false;
            }
        }

        return listPartsElements;
    }
}
