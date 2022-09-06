package no.unit.nva.publication.messages.model;

import static no.unit.nva.publication.PublicationServiceConfig.API_HOST;
import static no.unit.nva.publication.PublicationServiceConfig.PUBLICATION_PATH;
import static no.unit.nva.publication.messages.MessageApiConfig.TICKET_PATH;
import java.net.URI;
import no.unit.nva.publication.messages.MessageApiConfig;
import no.unit.nva.publication.model.business.Message;
import nva.commons.core.paths.UriWrapper;

public final class NewMessageDto {
    
    private NewMessageDto() {
    
    }
    
    public static URI constructMessageId(Message message) {
        return UriWrapper.fromHost(API_HOST)
                   .addChild(PUBLICATION_PATH)
                   .addChild(message.getResourceIdentifier().toString())
                   .addChild(TICKET_PATH)
                   .addChild(message.getTicketIdentifier().toString())
                   .addChild(MessageApiConfig.MESSAGE_PATH)
                   .addChild(message.getIdentifier().toString())
                   .getUri();
    }
}
