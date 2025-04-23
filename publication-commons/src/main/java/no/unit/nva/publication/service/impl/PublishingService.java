package no.unit.nva.publication.service.impl;

import java.net.URI;
import java.util.Optional;
import no.unit.nva.clients.ChannelClaimDto;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.model.Reference;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.contexttypes.Degree;
import no.unit.nva.model.contexttypes.Publisher;
import no.unit.nva.publication.model.business.FilesApprovalThesis;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.PublishingWorkflow;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissions.publication.PublicationPermissions;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.StringUtils;
import nva.commons.core.paths.UriWrapper;

public class PublishingService {

    private final ResourceService resourceService;
    private final TicketService ticketService;
    private final IdentityServiceClient identityServiceClient;

    public PublishingService(ResourceService resourceService, TicketService ticketService,
                             IdentityServiceClient identityServiceClient) {
        this.resourceService = resourceService;
        this.ticketService = ticketService;
        this.identityServiceClient = identityServiceClient;
    }

    @JacocoGenerated
    public static PublishingService defaultService() {
        return new PublishingService(ResourceService.defaultService(), TicketService.defaultService(),
                                     IdentityServiceClient.prepare());
    }

    public void publishResource(SortableIdentifier resourceIdentifier, UserInstance userInstance)
        throws ApiGatewayException {
        var resource = getResource(resourceIdentifier);

        validatePermissions(resource, userInstance);

        resource.publish(userInstance);
        if (resource.getPendingFiles().isEmpty()) {
            resourceService.updateResource(resource, userInstance);
        } else {
            publishResourceWithPendingFiles(userInstance, resource);
        }
    }

    private Resource getResource(SortableIdentifier resourceIdentifier) throws NotFoundException {
        return Resource.resourceQueryObject(resourceIdentifier)
                   .fetch(resourceService)
                   .orElseThrow(() -> new NotFoundException("Resource not found!"));
    }

    private void publishResourceWithPendingFiles(UserInstance userInstance, Resource resource) throws ApiGatewayException {
        var workflow = getCustomerWorkflow(userInstance);

        if (resource.isDegree()) {
            handleDegreeResource(userInstance, resource, workflow);
        } else {
            handleNonDegreeResource(userInstance, resource, workflow);
        }
        resourceService.updateResource(resource, userInstance);
    }

    private void handleDegreeResource(UserInstance userInstance, Resource resource, PublishingWorkflow workflow) throws ApiGatewayException {
        var publisher = getPublisher(resource);
        if (publisher.isPresent()) {
            var channelClaim = getChannelClaim(publisher.get());
            if (isClaimedByDifferentOrganization(channelClaim, userInstance)) {
                createFilesApprovalThesisForDifferentOrganization(resource, userInstance, channelClaim, workflow);
            } else {
                createFilesApprovalThesisForUserInstitution(resource, userInstance, workflow);
            }
        }
    }

    private ChannelClaimDto getChannelClaim(Publisher publisher) throws ApiGatewayException {
        return identityServiceClient.getChannelClaim(createChannelClaimUri(publisher));
    }

    private boolean isClaimedByDifferentOrganization(ChannelClaimDto channelClaim, UserInstance userInstance) {
        return !channelClaim.claimedBy().id().equals(userInstance.getCustomerId());
    }

    private void createFilesApprovalThesisForDifferentOrganization(Resource resource, UserInstance userInstance,
                                                                   ChannelClaimDto channelClaim, PublishingWorkflow workflow)
        throws ApiGatewayException {
        FilesApprovalThesis.create(resource, userInstance, channelClaim.claimedBy().organizationId(), workflow)
            .persistNewTicket(ticketService);
    }

    private void createFilesApprovalThesisForUserInstitution(Resource resource, UserInstance userInstance, PublishingWorkflow workflow)
        throws ApiGatewayException {
        FilesApprovalThesis.createForUserInstitution(resource, userInstance, workflow)
            .persistNewTicket(ticketService);
    }

    private void handleNonDegreeResource(UserInstance userInstance, Resource resource, PublishingWorkflow workflow)
        throws ApiGatewayException {
        PublishingRequestCase.create(resource, userInstance, workflow)
            .persistNewTicket(ticketService);
    }

    private URI createChannelClaimUri(Publisher publisher) {
        return UriWrapper.fromUri(publisher.getId())
                   .replacePathElementByIndexFromEnd(0, StringUtils.EMPTY_STRING)
                   .getUri();
    }

    private static Optional<Publisher> getPublisher(Resource resource) {
        return Optional.ofNullable(resource.getEntityDescription())
                   .map(EntityDescription::getReference)
                   .map(Reference::getPublicationContext)
                   .filter(Degree.class::isInstance)
                   .map(Degree.class::cast)
                   .map(Book::getPublisher)
                   .filter(Publisher.class::isInstance)
                   .map(Publisher.class::cast);
    }

    private static void validatePermissions(Resource resource, UserInstance userInstance) throws ForbiddenException {
        var permissionStrategy = PublicationPermissions.create(resource.toPublication(), userInstance);
        if (!permissionStrategy.allowsAction(PublicationOperation.UPDATE)) {
            throw new ForbiddenException();
        }
    }

    private PublishingWorkflow getCustomerWorkflow(UserInstance userInstance) throws NotFoundException {
        var customer = identityServiceClient.getCustomerById(userInstance.getCustomerId());
        return PublishingWorkflow.lookUp(customer.publicationWorkflow());
    }
}
