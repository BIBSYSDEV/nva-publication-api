package no.unit.nva.pubication.messages.list;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import java.net.URI;
import java.time.Clock;
import java.util.List;
import no.unit.nva.publication.service.impl.MessageService;
import no.unit.nva.publication.service.impl.ResourceMessages;
import no.unit.nva.publication.storage.model.UserInstance;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ListMessagesHandler extends ApiGatewayHandler<Void, ResourceMessages[]> {

    public static final Logger LOGGER = LoggerFactory.getLogger(ListMessagesHandler.class);
    private MessageService messageService;

    @JacocoGenerated
    public ListMessagesHandler() {
        this(new Environment(), defaultMessageService());
    }

    public ListMessagesHandler(Environment environment, MessageService messageService) {
        super(Void.class, environment, LOGGER);
        this.messageService = messageService;
    }

    @Override
    protected ResourceMessages[] processInput(Void input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        String feideId = requestInfo.getFeideId().orElse(null);
        URI customerId = requestInfo.getCustomerId().map(URI::create).orElse(null);
        UserInstance userInstance = new UserInstance(feideId, customerId);
        List<ResourceMessages> result = messageService.listMessagesForUser(userInstance);
        return convertListToArray(result);
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, ResourceMessages[] output) {
        return HttpURLConnection.HTTP_OK;
    }

    @JacocoGenerated
    private static MessageService defaultMessageService() {
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.defaultClient();
        return new MessageService(client, Clock.systemDefaultZone());
    }

    private ResourceMessages[] convertListToArray(List<ResourceMessages> result) {
        ResourceMessages[] resultArray = new ResourceMessages[result.size()];
        result.toArray(resultArray);
        return resultArray;
    }
}
