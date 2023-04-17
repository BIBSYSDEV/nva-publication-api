package no.unit.nva.publication.ticket;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static nva.commons.core.attempt.Try.attempt;
import java.io.ByteArrayOutputStream;
import java.time.Clock;
import java.util.function.Consumer;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.RandomDataGenerator;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.NotFoundException;

public abstract class TicketTestLocal extends ResourcesLocalTest {
    
    public static final FakeContext CONTEXT = new FakeContext();
    protected ResourceService resourceService;
    protected TicketService ticketService;
    protected ByteArrayOutputStream output;

    
    public void init() {
        super.init();
        this.resourceService = new ResourceService(client, Clock.systemDefaultZone());
        this.ticketService = new TicketService(client);
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

    protected Publication createPersistAndPublishPublicationWithDoi() throws NotFoundException, BadRequestException {
        return createAndPersistPublicationWithDoiAndThenActOnIt(this::publish);
    }
    
    protected TicketEntry createPersistedTicket(Publication publication)
        throws ApiGatewayException {
        var doiTicket = DoiRequest.fromPublication(publication);
        return ticketService.createTicket(doiTicket);
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
        return randomPublication().copy().withDoi(null).build();
    }
    
    private Publication createAndPersistPublicationAndThenActOnIt(Consumer<Publication> action)
        throws ApiGatewayException {
        var publication = randomPublicationWithoutDoi();
        var userInstance = UserInstance.fromPublication(publication);
        var storedResult = Resource.fromPublication(publication).persistNew(resourceService, userInstance);
        action.accept(storedResult);
        return resourceService.getPublication(storedResult);
    }

    private Publication createAndPersistPublicationWithDoiAndThenActOnIt(Consumer<Publication> action)
        throws NotFoundException, BadRequestException {
        var publication = randomPublication();
        publication.setDoi(RandomDataGenerator.randomDoi());
        var userInstance = UserInstance.fromPublication(publication);
        var storedResult = Resource.fromPublication(publication).persistNew(resourceService, userInstance);
        action.accept(storedResult);
        return resourceService.getPublication(storedResult);
    }
}
