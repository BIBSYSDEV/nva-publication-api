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
import nva.commons.apigateway.exceptions.BadGatewayException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.StringUtils;
import nva.commons.core.paths.UriWrapper;

public class PublishingService {

    private static final String API_HOST = new Environment().readEnv("API_HOST");
    protected static final String CUSTOMER = "customer";
    protected static final String CHANNEL_CLAIM = "channel-claim";
    protected static final String EVERYONE = "Everyone";
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

        publishPublication(userInstance, resource);

        if (!resource.getPendingFiles().isEmpty()) {
            publishResourceWithPendingFiles(userInstance, resource);
        }
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

    private void publishPublication(UserInstance userInstance, Resource resource)
        throws BadGatewayException, ForbiddenException {
        var publisher = getPublisher(resource);
        if (publisher.isEmpty()) {
            resource.publish(resourceService, userInstance);
            return;
        }

        var channelClaim = getChannelClaim(publisher.get());
        if (channelClaim.isEmpty()
            || EVERYONE.equals(channelClaim.get().channelClaim().constraint().publishingPolicy())) {
            resource.publish(resourceService, userInstance);
            return;
        }

        if (!userInstance.getTopLevelOrgCristinId().equals(channelClaim.get().claimedBy().organizationId())) {
            throw new ForbiddenException();
        }

        resource.publish(resourceService, userInstance);
    }

    private Resource getResource(SortableIdentifier resourceIdentifier) throws NotFoundException {
        return Resource.resourceQueryObject(resourceIdentifier)
                   .fetch(resourceService)
                   .orElseThrow(() -> new NotFoundException("Resource not found!"));
    }

    private void publishResourceWithPendingFiles(UserInstance userInstance, Resource resource)
        throws ApiGatewayException {
        var workflow = getCustomerWorkflow(userInstance);
        if (resource.isDegree()) {
            handleDegreeResource(userInstance, resource, workflow);
        } else {
            PublishingRequestCase.create(resource, userInstance, workflow).persistNewTicket(ticketService);
        }
    }

    private void handleDegreeResource(UserInstance userInstance, Resource resource, PublishingWorkflow workflow)
        throws ApiGatewayException {
        var publisher = getPublisher(resource);
        if (publisher.isPresent()) {
            var channelClaim = getChannelClaim(publisher.get());
            if (channelClaim.isPresent() && isClaimedByDifferentOrganization(channelClaim.get(), userInstance)) {
                var organizationId = getOrganizationId(channelClaim.get());
                FilesApprovalThesis.create(resource, userInstance, organizationId, workflow)
                    .persistNewTicket(ticketService);
                return;
            }
        }
        FilesApprovalThesis.createForUserInstitution(resource, userInstance, workflow)
            .persistNewTicket(ticketService);
    }

    private static URI getOrganizationId(ChannelClaimDto channelClaim) {
        return channelClaim.claimedBy().organizationId();
    }

    private Optional<ChannelClaimDto> getChannelClaim(Publisher publisher) throws BadGatewayException {
        try {
            return Optional.of(identityServiceClient.getChannelClaim(createChannelClaimUri(publisher)));
        } catch (NotFoundException exception) {
            return Optional.empty();
        } catch (Exception e) {
            throw new BadGatewayException("Could not fetch channel owner!");
        }
    }

    private boolean isClaimedByDifferentOrganization(ChannelClaimDto channelClaim, UserInstance userInstance) {
        return !channelClaim.claimedBy().id().equals(userInstance.getCustomerId());
    }

    private URI createChannelClaimUri(Publisher publisher) {
        var channelClaimIdentifier = UriWrapper.fromUri(publisher.getId())
                                         .replacePathElementByIndexFromEnd(0, StringUtils.EMPTY_STRING)
                                         .getLastPathElement();
        return UriWrapper.fromHost(API_HOST)
                   .addChild(CUSTOMER)
                   .addChild(CHANNEL_CLAIM)
                   .addChild(channelClaimIdentifier)
                   .getUri();
    }

    private PublishingWorkflow getCustomerWorkflow(UserInstance userInstance) throws NotFoundException {
        var customer = identityServiceClient.getCustomerById(userInstance.getCustomerId());
        return PublishingWorkflow.lookUp(customer.publicationWorkflow());
    }
}
