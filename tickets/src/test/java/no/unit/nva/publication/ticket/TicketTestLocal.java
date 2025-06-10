package no.unit.nva.publication.ticket;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.List;
import java.util.function.Consumer;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserClientType;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.utils.CustomerList;
import no.unit.nva.publication.model.utils.CustomerService;
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
        this.resourceService = getResourceServiceBuilder().withCustomerService(customerService).build();
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
        var ownerAffiliation = publication.getResourceOwner().getOwnerAffiliation();
        var resource = Resource.fromPublication(publication);
        return DoiRequest.create(resource, userInstanceWithTopLevelCristinOrg(ownerAffiliation))
                            .persistNewTicket(ticketService);
    }

    protected TicketEntry createPersistedDoiRequestWithOwnerAffiliation(Publication publication, URI ownerAffiliation)
        throws ApiGatewayException {
        var userInstance = userInstanceWithTopLevelCristinOrg(ownerAffiliation);
        return DoiRequest.create(Resource.fromPublication(publication), userInstance).persistNewTicket(ticketService);
    }

    public static UserInstance userInstanceWithTopLevelCristinOrg(URI ownerAffiliation) {
        return new UserInstance(randomString(), randomUri(), ownerAffiliation, randomUri(), randomUri(),
                                List.of(), UserClientType.INTERNAL);
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
        return resourceService.getPublicationByIdentifier(storedResult.getIdentifier());
    }

    private Publication createAndPersistPublicationWithDoiAndThenActOnIt(Consumer<Publication> action)
        throws NotFoundException, BadRequestException {
        var publication = randomPublication();
        publication.getEntityDescription().setPublicationDate(new PublicationDate.Builder().withYear("2020").build());
        publication.setDoi(RandomDataGenerator.randomDoi());
        var userInstance = UserInstance.fromPublication(publication);
        var storedResult = Resource.fromPublication(publication).persistNew(resourceService, userInstance);
        action.accept(storedResult);
        return resourceService.getPublicationByIdentifier(storedResult.getIdentifier());
    }
}
