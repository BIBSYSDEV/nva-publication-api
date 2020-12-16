package no.unit.nva.publication.delete;

import com.amazonaws.services.lambda.runtime.Context;
import nva.commons.exceptions.ApiGatewayException;
import nva.commons.handlers.ApiGatewayHandler;
import nva.commons.handlers.RequestInfo;
import nva.commons.utils.Environment;
import nva.commons.utils.JacocoGenerated;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeletePublicationHandler extends ApiGatewayHandler<Void,Void> {

    private static final Logger logger = LoggerFactory.getLogger(DeletePublicationHandler.class);

    @JacocoGenerated
    public DeletePublicationHandler() {
        super(Void.class, new Environment(), logger);
    }

    @Override
    protected Void processInput(Void input, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        return null;
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, Void output) {
        return HttpStatus.SC_ACCEPTED;
    }
}
