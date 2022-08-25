package no.unit.nva.publication.publishingrequest;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static nva.commons.core.attempt.Try.attempt;
import java.io.ByteArrayOutputStream;
import java.time.Clock;
import java.util.function.Consumer;
import java.util.stream.Stream;
import no.unit.nva.model.Publication;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.publication.testing.TypeProvider;
import no.unit.nva.stubs.FakeContext;
import nva.commons.apigateway.exceptions.ApiGatewayException;

public abstract class TicketTest extends ResourcesLocalTest {
    
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
        this.ticketService = new TicketService(client, Clock.systemDefaultZone());
        this.output = new ByteArrayOutputStream();
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
        var publication = createAndPersistDraftPublication();
        var ticket = TicketEntry.requestNewTicket(publication, ticketType);
        return ticketService.createTicket(ticket, ticketType);
    }
    
    protected Publication createAndPersistPublicationAndMarkForDeletion()
        throws ApiGatewayException {
        return createAndPersistPublicationAndThenActOnIt(this::markForDeletion);
    }
    
    protected Publication nonPersistedPublication() {
        return randomPublication();
    }
    
    private void publish(Publication publication) {
        var userInstance = UserInstance.fromPublication(publication);
        attempt(() -> resourceService.publishPublication(userInstance, publication.getIdentifier()))
            .orElseThrow();
    }
    
    private void markForDeletion(Publication publication) {
        var userInstance = UserInstance.fromPublication(publication);
        attempt(() -> resourceService.markPublicationForDeletion(userInstance, publication.getIdentifier()))
            .orElseThrow();
    }
    
    private Publication createAndPersistPublicationAndThenActOnIt(Consumer<Publication> action)
        throws ApiGatewayException {
        var publication = PublicationGenerator.randomPublication().copy().withDoi(null).build();
        var userInstance = UserInstance.fromPublication(publication);
        var storedResult = resourceService.createPublication(userInstance, publication);
        action.accept(storedResult);
        return resourceService.getPublication(storedResult);
    }
}
