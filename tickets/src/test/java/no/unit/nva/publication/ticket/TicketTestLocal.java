package no.unit.nva.publication.ticket;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static nva.commons.core.attempt.Try.attempt;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.function.Consumer;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationDate;
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
        this.resourceService = getResourceServiceBuilder().build();
        this.ticketService = getTicketService();
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
    
    protected TicketEntry createPersistedDoiTicket(Publication publication)
        throws ApiGatewayException {
        return DoiRequest.fromPublication(publication)
                            .withOwnerAffiliation(publication.getResourceOwner().getOwnerAffiliation())
                            .persistNewTicket(ticketService);
    }

    protected TicketEntry createPersistedDoiRequestWithOwnerAffiliation(Publication publication, URI ownerAffiliation)
        throws ApiGatewayException {
        return DoiRequest.fromPublication(publication)
                   .withOwnerAffiliation(ownerAffiliation)
                   .persistNewTicket(ticketService);
    }
    
    protected Publication nonPersistedPublication() {
        return randomPublication();
    }
    
    protected void publish(Publication publication) {
        Resource.fromPublication(publication).publish(resourceService, UserInstance.fromPublication(publication));
    }
    
    public static Publication randomPublicationWithoutDoi() {
        return randomPublication().copy().withDoi(null).build();
    }
    
    private Publication createAndPersistPublicationAndThenActOnIt(Consumer<Publication> action)
        throws ApiGatewayException {
        var publication = randomPublicationWithoutDoi();
        publication.getEntityDescription().setPublicationDate(new PublicationDate.Builder().withYear("2020").build());
        var userInstance = UserInstance.fromPublication(publication);
        var storedResult = Resource.fromPublication(publication).persistNew(resourceService, userInstance);
        action.accept(storedResult);
        return resourceService.getPublication(storedResult);
    }

    private Publication createAndPersistPublicationWithDoiAndThenActOnIt(Consumer<Publication> action)
        throws NotFoundException, BadRequestException {
        var publication = randomPublication();
        publication.getEntityDescription().setPublicationDate(new PublicationDate.Builder().withYear("2020").build());
        publication.setDoi(RandomDataGenerator.randomDoi());
        var userInstance = UserInstance.fromPublication(publication);
        var storedResult = Resource.fromPublication(publication).persistNew(resourceService, userInstance);
        action.accept(storedResult);
        return resourceService.getPublication(storedResult);
    }
}
