package no.unit.nva.publication.messages;

import java.io.ByteArrayOutputStream;
import java.time.Clock;
import java.util.Map;
import no.unit.nva.model.Publication;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.MessageService;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.stubs.FakeContext;
import nva.commons.apigateway.exceptions.ApiGatewayException;

public class MessagesTest extends ResourcesLocalTest {
    
    protected ResourceService resourceService;
    protected MessageService messageService;
    protected ByteArrayOutputStream output;
    protected FakeContext context;
    
    public void setup() {
        super.init();
        this.resourceService = new ResourceService(client, Clock.systemDefaultZone());
        this.messageService = new MessageService(client);
        this.output = new ByteArrayOutputStream();
        this.context = new FakeContext();
    }
    
    protected Map<String, String> messagePathParameter(String messageIdentifier) {
        return Map.of(MessageApiConfig.MESSAGE_IDENTIFIER_PATH_PARAMETER, messageIdentifier);
    }
    
    protected Publication createPublication() throws ApiGatewayException {
        var publication = PublicationGenerator.randomPublication();
        return Resource.fromPublication(publication).persistNew(resourceService,
            UserInstance.fromPublication(publication));
    }
}
