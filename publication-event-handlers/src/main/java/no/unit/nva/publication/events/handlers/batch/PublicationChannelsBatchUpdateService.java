package no.unit.nva.publication.events.handlers.batch;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.events.handlers.batch.ChannelUpdateEvent.PublicationChannelSummary;
import no.unit.nva.publication.model.business.publicationchannel.ClaimedPublicationChannel;
import no.unit.nva.publication.model.business.publicationchannel.NonClaimedPublicationChannel;
import no.unit.nva.publication.model.business.publicationchannel.PublicationChannel;
import no.unit.nva.publication.service.impl.ResourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PublicationChannelsBatchUpdateService {

    private static final Logger logger = LoggerFactory.getLogger(PublicationChannelsBatchUpdateService.class);
    private static final String UPDATED_PUBLICATION_CHANNELS_MESSAGE = "Updated {} publications with channel {}";
    private static final String FOUND_PUBLICATIONS_WITH_CHANNEL_MESSAGE = "Found {} publications with channel {}";
    private static final String PUBLICATIONS_UPDATED_MESSAGE = "Publications updated: {}";
    private final ResourceService resourceService;

    public PublicationChannelsBatchUpdateService(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    public void updateChannels(ChannelUpdateEvent event) {
        var identifier = event.getChannelIdentifier();
        var publicationChannels = listAllPublicationChannelsWithIdentifier(identifier);

        logger.info(FOUND_PUBLICATIONS_WITH_CHANNEL_MESSAGE, publicationChannels.size(), identifier);

        var updatedChannels = publicationChannels.stream().map(channel -> update(channel, event)).toList();
        resourceService.batchUpdateChannels(updatedChannels);

        logger.info(UPDATED_PUBLICATION_CHANNELS_MESSAGE, updatedChannels.size(), identifier);

        logger.info(PUBLICATIONS_UPDATED_MESSAGE,
                    String.join(", ", updatedChannels.stream()
                        .map(PublicationChannel::getResourceIdentifier)
                        .map("%s, "::formatted)
                        .collect(Collectors.toSet())));
    }

    private static ClaimedPublicationChannel updateClaimedChannel(PublicationChannelSummary summary,
                                                                  ClaimedPublicationChannel claimed) {
        return claimed.update(summary.customerId(), summary.organizationId(), summary.constraint());
    }

    private static ClaimedPublicationChannel updateNonClaimedToClaimed(PublicationChannelSummary summary,
                                                                       NonClaimedPublicationChannel nonClaimed) {
        return nonClaimed.toClaimedChannel(summary.customerId(), summary.organizationId(), summary.constraint());
    }

    private ClaimedPublicationChannel updateToClaimed(PublicationChannel channel, PublicationChannelSummary summary) {
        return switch (channel) {
            case NonClaimedPublicationChannel nonClaimed -> updateNonClaimedToClaimed(summary, nonClaimed);
            case ClaimedPublicationChannel claimed -> updateClaimedChannel(summary, claimed);
        };
    }

    private PublicationChannel update(PublicationChannel channel, ChannelUpdateEvent event) {
        return switch (event.action()) {
            case ADDED, UPDATED -> updateToClaimed(channel, event.publicationChannelSummary());
            case REMOVED -> updateToNonClaimed(channel);
        };
    }

    private NonClaimedPublicationChannel updateToNonClaimed(PublicationChannel channel) {
        return switch (channel) {
            case NonClaimedPublicationChannel nonClaimed -> nonClaimed;
            case ClaimedPublicationChannel claimed -> claimed.toNonClaimedChannel();
        };
    }

    private ArrayList<PublicationChannel> listAllPublicationChannelsWithIdentifier(SortableIdentifier identifier) {
        Map<String, AttributeValue> startMarker = null;
        var publicationChannels = new ArrayList<PublicationChannel>();
        boolean isTruncated;
        do {
            var listingResult = resourceService.fetchAllPublicationChannelsByIdentifier(identifier, startMarker);
            publicationChannels.addAll(listingResult.getDatabaseEntries());
            startMarker = listingResult.getStartMarker();
            isTruncated = listingResult.isTruncated();
        } while (isTruncated);
        return publicationChannels;
    }
}
