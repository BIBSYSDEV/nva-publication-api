package no.unit.nva.publication.model.business.publicationchannel;

import static no.unit.nva.publication.PublicationServiceConfig.API_HOST;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;
import no.unit.nva.model.contexttypes.Publisher;
import no.unit.nva.publication.external.services.ChannelClaimClient;
import no.unit.nva.publication.external.services.ChannelClaimDto;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.storage.PublicationChannelDao;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;

public final class PublicationChannelUtil {

    private static final String CUSTOMER = "customer";
    private static final String CHANNEL_CLAIM = "channel-claim";
    private static final String IDENTITY_SERVICE_EXCEPTION = "Something went wrong while retrieving channel claim!";

    @JacocoGenerated
    private PublicationChannelUtil() {
    }

    public static Optional<ChannelClaimDto> getChannelClaim(ChannelClaimClient channelClaimClient, URI channelClaimId) {
        try {
            return Optional.ofNullable(channelClaimClient.fetchChannelClaim(channelClaimId));
        } catch (NotFoundException exception) {
            return Optional.empty();
        } catch (Exception e) {
            throw new IllegalStateException(IDENTITY_SERVICE_EXCEPTION);
        }
    }

    public static PublicationChannelDao createPublicationChannelDao(ChannelClaimClient channelClaimClient,
                                                                    Resource resource,
                                                                    Publisher publisher) {
        var channelClaimId = toChannelClaimUri(publisher.getIdentifier());
        var channelType = ChannelType.fromChannelId(publisher.getId());
        return getChannelClaim(channelClaimClient, channelClaimId)
                   .map(channelClaimDto -> createClaimedChannelDao(resource, channelClaimDto, channelType))
                   .orElseGet(() -> createNonClaimedChannelDao(resource, channelClaimId, channelType));
    }

    public static URI toChannelClaimUri(UUID channelClaimIdentifier) {
        return UriWrapper.fromHost(API_HOST)
                   .addChild(CUSTOMER)
                   .addChild(CHANNEL_CLAIM)
                   .addChild(channelClaimIdentifier.toString())
                   .getUri();
    }

    private static PublicationChannelDao createNonClaimedChannelDao(Resource resource, URI channelClaimId,
                                                                    ChannelType channelType) {
        return NonClaimedPublicationChannel
                   .create(channelClaimId, resource.getIdentifier(), channelType)
                   .toDao();
    }

    private static PublicationChannelDao createClaimedChannelDao(Resource resource, ChannelClaimDto claim,
                                                                 ChannelType channelType) {
        return ClaimedPublicationChannel
                   .create(claim, resource.getIdentifier(), channelType)
                   .toDao();
    }

    public static Optional<UUID> getPublisherIdentifierWhenDegree(Resource resource) {
        return resource.getPublisherWhenDegree().map(Publisher::getIdentifier);
    }
}
