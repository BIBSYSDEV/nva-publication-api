package no.unit.nva.publication.ticket;

import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.external.services.UriRetriever;
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

import java.io.ByteArrayOutputStream;
import java.time.Clock;
import java.util.function.Consumer;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static nva.commons.core.attempt.Try.attempt;
import static org.mockito.Mockito.mock;

public abstract class TicketTestLocal extends ResourcesLocalTest {
    
    public static final FakeContext CONTEXT = new FakeContext();
    protected ResourceService resourceService;
    protected TicketService ticketService;
    protected ByteArrayOutputStream output;
    protected UriRetriever uriRetriever;

    public void init() {
        super.init();
        this.resourceService = new ResourceService(client, Clock.systemDefaultZone());
        this.uriRetriever = mock(UriRetriever.class);
        this.ticketService = new TicketService(client, SortableIdentifier::next, uriRetriever);
        mockUriRetriever(REGISTRATOR_PUBLISHES_METADATA_AND_FILES);
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

    protected void mockUriRetriever(PublicationWorkflow publicationWorkflow) {
        when(uriRetriever.getDto(any(), any()))
                .thenReturn(Optional.of(new WorkFlowDto(publicationWorkflow)));
    }
}
