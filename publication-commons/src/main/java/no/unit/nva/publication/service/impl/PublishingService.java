package no.unit.nva.publication.service.impl;

import java.net.URI;
import java.util.Optional;
import no.unit.nva.clients.ChannelClaimDto;
import no.unit.nva.clients.ChannelClaimDto.ChannelClaim;
import no.unit.nva.clients.ChannelClaimDto.ChannelClaim.ChannelConstraint;
import no.unit.nva.clients.ChannelClaimDto.CustomerSummaryDto;
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
import nva.commons.core.paths.UriWrapper;

public class PublishingService {

    protected static final String CUSTOMER = "customer";
    protected static final String CHANNEL_CLAIM = "channel-claim";
    protected static final String EVERYONE = "Everyone";
    protected static final String NOT_FOUND_MESSAGE = "Resource with identifier %s not found!";
    private static final String API_HOST = new Environment().readEnv("API_HOST");
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

        publishResource(userInstance, resource);

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
        var permissionStrategy = PublicationPermissions.create(resource, userInstance);
        if (!permissionStrategy.allowsAction(PublicationOperation.UPDATE)) {
            throw new ForbiddenException();
        }
    }

    private static boolean everyoneCanPublish(ChannelClaimDto channelClaim) {
        return Optional.ofNullable(channelClaim)
                   .map(ChannelClaimDto::channelClaim)
                   .map(ChannelClaim::constraint)
                   .map(ChannelConstraint::publishingPolicy)
                   .map(EVERYONE::equals)
                   .orElse(false);
    }

    private static URI getOrganizationId(ChannelClaimDto channelClaim) {
        return channelClaim.claimedBy().organizationId();
    }

    private void publishResource(UserInstance userInstance, Resource resource)
        throws BadGatewayException, ForbiddenException {
        var publisher = getPublisher(resource);
        if (publisher.isEmpty()) {
            resource.publish(resourceService, userInstance);
        } else {
            var channelClaim = getChannelClaim(publisher.get());
            var instanceType = resource.getInstanceType().orElseThrow();
            if (channelClaim.isPresent() && channelClaim.get().channelClaim().constraint().scope().contains(instanceType)) {
                if (everyoneCanPublish(channelClaim.get()) || isClaimedByUserOrganization(channelClaim.get(), userInstance)) {
                    resource.publish(resourceService, userInstance);
                } else {
                    throw new ForbiddenException();
                }
            }
        }
    }

    private Resource getResource(SortableIdentifier resourceIdentifier) throws NotFoundException {
        return Resource.resourceQueryObject(resourceIdentifier)
                   .fetch(resourceService)
                   .orElseThrow(() -> new NotFoundException(NOT_FOUND_MESSAGE.formatted(resourceIdentifier)));
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
            if (channelClaim.isPresent() && !isClaimedByUserOrganization(channelClaim.get(), userInstance)) {
                var organizationId = getOrganizationId(channelClaim.get());
                FilesApprovalThesis.create(resource, userInstance, organizationId, workflow)
                    .persistNewTicket(ticketService);
                return;
            }
        }
        FilesApprovalThesis.createForUserInstitution(resource, userInstance, workflow).persistNewTicket(ticketService);
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

    private boolean isClaimedByUserOrganization(ChannelClaimDto channelClaim, UserInstance userInstance) {
        return Optional.ofNullable(channelClaim)
                   .map(ChannelClaimDto::claimedBy)
                   .map(CustomerSummaryDto::id)
                   .map(id -> userInstance.getCustomerId().equals(id))
                   .orElse(true);
    }

    private URI createChannelClaimUri(Publisher publisher) {
        return UriWrapper.fromHost(API_HOST)
                   .addChild(CUSTOMER)
                   .addChild(CHANNEL_CLAIM)
                   .addChild(publisher.getIdentifier().toString())
                   .getUri();
    }

    private PublishingWorkflow getCustomerWorkflow(UserInstance userInstance) throws NotFoundException {
        var customer = identityServiceClient.getCustomerById(userInstance.getCustomerId());
        return PublishingWorkflow.lookUp(customer.publicationWorkflow());
    }
}
