package no.unit.nva.publication.ticket;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static nva.commons.core.attempt.Try.attempt;
import java.io.ByteArrayOutputStream;
import java.time.Clock;
import java.util.function.Consumer;
import java.util.stream.Stream;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.publication.testing.TypeProvider;
import no.unit.nva.stubs.FakeContext;
import nva.commons.apigateway.exceptions.ApiGatewayException;

public abstract class TicketTestLocal extends ResourcesLocalTest {
    
    public static final FakeContext CONTEXT = new FakeContext();
    protected ResourceService resourceService;
    protected TicketService ticketService;
    protected ByteArrayOutputStream output;
    
    public static Stream<Class<?>> ticketTypeProvider() {
        return TypeProvider.listSubTypes(TicketEntry.class);
    }
    
    public void init() {
        super.init();
        this.resourceService = new ResourceService(client, Clock.systemDefaultZone());
        this.ticketService = new TicketService(client);
        this.output = new ByteArrayOutputStream();
    }
    
    protected Publication createPublicationForTicket(Class<? extends TicketEntry> ticketType)
        throws ApiGatewayException {
        return DoiRequest.class.equals(ticketType)
                   ? createPersistAndPublishPublication()
                   : createAndPersistDraftPublication();
    }
    
    protected Publication createAndPersistPublication(PublicationStatus status) {
        var publication = randomPublicationWithoutDoi();
        publication.setStatus(status);
        return resourceService.createPublicationWithPredefinedCreationDate(publication);
    }
    
    protected Publication createAndPersistDraftPublication()
        throws ApiGatewayException {
        return createAndPersistPublicationAndThenActOnIt(publication -> {
        });
    }
    
    protected Publication createPersistAndPublishPublication()
        throws ApiGatewayException {
        return createAndPersistPublicationAndThenActOnIt(this::publish);
    }
    
    protected TicketEntry createPersistedTicket(Class<? extends TicketEntry> ticketType) throws ApiGatewayException {
        return createPersistedTicket(createAndPersistDraftPublication(), ticketType);
    }
    
    protected TicketEntry createPersistedTicket(Publication publication, Class<? extends TicketEntry> ticketType)
        throws ApiGatewayException {
        var ticket = TicketEntry.requestNewTicket(publication, ticketType);
        return ticketService.createTicket(ticket, ticketType);
    }

    protected TicketEntry persistTicket(Publication publication, Class<? extends TicketEntry> ticketType)
        throws ApiGatewayException {
        return TicketEntry.requestNewTicket(publication, ticketType).persistNewTicket(ticketService);
    }
    
    protected Publication nonPersistedPublication() {
        return randomPublication();
    }
    
    protected void publish(Publication publication) {
        var userInstance = UserInstance.fromPublication(publication);
        attempt(() -> resourceService.publishPublication(userInstance, publication.getIdentifier()))
            .orElseThrow();
    }
    
    private static Publication randomPublicationWithoutDoi() {
        return randomPreFilledPublicationBuilder().withDoi(null).build();
    }
    
    private Publication createAndPersistPublicationAndThenActOnIt(Consumer<Publication> action)
        throws ApiGatewayException {
        var publication = randomPublicationWithoutDoi();
        var userInstance = UserInstance.fromPublication(publication);
        var storedResult = resourceService.createPublication(userInstance, publication);
        action.accept(storedResult);
        return resourceService.getPublication(storedResult);
    }
}
