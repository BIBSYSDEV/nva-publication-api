package no.unit.nva.publication.approvalrequest.list;

import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class ListApprovalRequestHandler extends ApiGatewayHandler<String, String> {

    @SuppressWarnings("unused")
    @JacocoGenerated
    public ListApprovalRequestHandler() {
        this(new Environment());
    }

    public ListApprovalRequestHandler(Environment environment) {
        super(String.class, environment);
    }

    @Override
    protected String processInput(String input, RequestInfo requestInfo, Context context) {
        return null;
    }

    @Override
    protected Integer getSuccessStatusCode(String input, String output) {
        return HttpURLConnection.HTTP_OK;
    }

}
