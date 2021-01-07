package no.unit.nva.publication.create;

import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import no.unit.nva.publication.identifiers.SortableIdentifier;
import no.unit.nva.publication.storage.model.Resource;
import nva.commons.handlers.ApiGatewayHandler;
import nva.commons.handlers.RequestInfo;
import org.slf4j.LoggerFactory;

public class CreateResourceHandler extends ApiGatewayHandler<Void, Resource> {

    public CreateResourceHandler() {
        super(Void.class, LoggerFactory.getLogger(CreateResourceHandler.class));
    }

    @Override
    protected Resource processInput(Void input, RequestInfo requestInfo, Context context) {
        String feideId = requestInfo.getFeideId().orElseThrow();
        String customerId = requestInfo.getCustomerId().orElseThrow();
        logger.info("FeideId:" + feideId);
        logger.info("CustomerId:" + customerId);
        Resource resource = new Resource();
        resource.setIdentifier(SortableIdentifier.next());
        return resource;
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, Resource output) {
        return HttpURLConnection.HTTP_ACCEPTED;
    }
}