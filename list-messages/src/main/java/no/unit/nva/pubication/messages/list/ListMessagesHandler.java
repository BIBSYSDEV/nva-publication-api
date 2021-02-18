package no.unit.nva.pubication.messages.list;

import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ListMessagesHandler extends ApiGatewayHandler<Void, Void> {

    public static final Logger LOGGER = LoggerFactory.getLogger(ListMessagesHandler.class);

    @JacocoGenerated
    public ListMessagesHandler() {
        this(new Environment());
    }

    public ListMessagesHandler(Environment environment) {
        super(Void.class, environment, LOGGER);
    }

    @Override
    protected Void processInput(Void input, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        return null;
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, Void output) {
        return HttpURLConnection.HTTP_OK;
    }
}
