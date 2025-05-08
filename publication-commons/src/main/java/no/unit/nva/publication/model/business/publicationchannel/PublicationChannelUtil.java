package no.unit.nva.publication.model.business.publicationchannel;

import static no.unit.nva.publication.PublicationServiceConfig.API_HOST;
import java.net.URI;
import java.util.Optional;
import no.unit.nva.clients.ChannelClaimDto;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.model.contexttypes.Publisher;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.storage.PublicationChannelDao;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;

public final class PublicationChannelUtil {

    public static final String CUSTOMER = "customer";
    public static final String CHANNEL_CLAIM = "channel-claim";
    private static final String IDENTITY_SERVICE_EXCEPTION = "Something went wrong while retrieving channel claim!";

    @JacocoGenerated
    public PublicationChannelUtil(){}

    public static Optional<ChannelClaimDto> getChannelClaim(IdentityServiceClient identityService, URI channelClaimId) {
        try {
            return Optional.ofNullable(identityService.getChannelClaim(channelClaimId));
        } catch (NotFoundException exception) {
            return Optional.empty();
        } catch (Exception e) {
            throw new IllegalStateException(IDENTITY_SERVICE_EXCEPTION);
        }
    }

    public static PublicationChannelDao createPublicationChannelDao(IdentityServiceClient identityService,
                                                                    Resource resource,
                                                                    Publisher publisher) {
        var channelClaimId = toChannelClaimUri(publisher);
        return getChannelClaim(identityService, channelClaimId)
                   .map(channelClaimDto -> createClaimedChannelDao(resource, channelClaimDto, channelClaimId))
                   .orElseGet(() -> createNonClaimedChannelDao(resource, publisher, channelClaimId));
    }

    private static URI toChannelClaimUri(Publisher publisher) {
        // The channel claim id will be available in channel claim response later, doing it manually until then
        return UriWrapper.fromHost(API_HOST)
                   .addChild(CUSTOMER)
                   .addChild(CHANNEL_CLAIM)
                   .addChild(publisher.getIdentifier().toString())
                   .getUri();
    }

    private static PublicationChannelDao createNonClaimedChannelDao(Resource resource, Publisher publisher,
                                                                    URI channelClaimId) {
        return NonClaimedPublicationChannel
                   .create(channelClaimId, resource.getIdentifier(), ChannelType.fromChannelId(publisher.getId()))
                   .toDao();
    }

    private static PublicationChannelDao createClaimedChannelDao(Resource resource, ChannelClaimDto claim,
                                                                 URI channelClaimId) {
        return ClaimedPublicationChannel
                   .create(channelClaimId, claim, resource.getIdentifier())
                   .toDao();
    }
}
