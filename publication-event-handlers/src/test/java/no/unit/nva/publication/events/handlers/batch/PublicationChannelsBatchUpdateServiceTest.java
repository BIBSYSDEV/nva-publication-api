package no.unit.nva.publication.events.handlers.batch;

import static java.util.UUID.randomUUID;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.publication.events.handlers.batch.ChannelUpdateEvent.Action.ADDED;
import static no.unit.nva.publication.events.handlers.batch.ChannelUpdateEvent.Action.REMOVED;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.events.handlers.batch.ChannelUpdateEvent.Action;
import no.unit.nva.publication.events.handlers.batch.ChannelUpdateEvent.PublicationChannelSummary;
import no.unit.nva.publication.model.business.publicationchannel.ChannelPolicy;
import no.unit.nva.publication.model.business.publicationchannel.ChannelType;
import no.unit.nva.publication.model.business.publicationchannel.ClaimedPublicationChannel;
import no.unit.nva.publication.model.business.publicationchannel.Constraint;
import no.unit.nva.publication.model.business.publicationchannel.NonClaimedPublicationChannel;
import no.unit.nva.publication.model.business.publicationchannel.PublicationChannel;
import no.unit.nva.publication.service.PublicationChannelLocalTestUtil;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PublicationChannelsBatchUpdateServiceTest extends PublicationChannelLocalTestUtil {

    private PublicationChannelsBatchUpdateService service;
    private ResourceService resourceService;

    @BeforeEach
    void setUp() {
        super.init();
        resourceService = getResourceServiceBuilder().build();
        service = new PublicationChannelsBatchUpdateService(resourceService);
    }

    @Test
    void shouldUpdateNonClaimedChannelToClaimedWhenChannelIsBeingClaimed() {
        var identifier = randomUUID();
        var channel = NonClaimedPublicationChannel.create(toChannelClaimId(identifier), SortableIdentifier.next(),
                                                          ChannelType.PUBLISHER);
        super.persistPublicationChannel(channel);
        var event = createEvent(identifier, ADDED);

        service.updateChannels(event);

        var updatedChannel = getUpdatedChannels(identifier).getFirst();
        var expectedChannel = createExpectedClaimedChannel(channel, event, updatedChannel.getModifiedDate());

        assertEquals(expectedChannel, updatedChannel);
    }

    @Test
    void shouldUpdateClaimedChannelWhenChannelIsBeingClaimed() {
        var identifier = randomUUID();
        var channel = randomClaimedChannel(identifier);
        super.persistPublicationChannel(channel);
        var event = createEvent(identifier, ADDED);

        service.updateChannels(event);

        var updatedChannel = getUpdatedChannels(identifier).getFirst();
        var expectedChannel = createExpectedClaimedChannel(channel, event, updatedChannel.getModifiedDate());

        assertEquals(expectedChannel, updatedChannel);
    }

    @Test
    void shouldUpdateClaimedChannelToNonClaimedWhenChannelIsBeingUnclaimed() {
        var identifier = randomUUID();
        var channel = randomClaimedChannel(identifier);
        super.persistPublicationChannel(channel);
        var event = createEvent(identifier, REMOVED);

        service.updateChannels(event);

        var updatedChannel = getUpdatedChannels(identifier).getFirst();
        var expectedChannel = createExpectedNonClaimedChannel(channel, event, updatedChannel.getModifiedDate());

        assertEquals(expectedChannel, updatedChannel);
    }

    @Test
    void shouldUpdateMultipleChannels() {
        var identifier = randomUUID();
        var channel1 = randomClaimedChannel(identifier);
        super.persistPublicationChannel(channel1);
        var channel2 = randomClaimedChannel(identifier);
        super.persistPublicationChannel(channel2);

        var event = createEvent(identifier, ADDED);

        service.updateChannels(event);

        getUpdatedChannels(identifier).stream().map(ClaimedPublicationChannel.class::cast).forEach(channel -> {
            assertEquals(event.publicationChannelSummary().constraint(),channel.getConstraint());
            assertEquals(event.publicationChannelSummary().organizationId(),channel.getOrganizationId());
            assertEquals(event.publicationChannelSummary().customerId(),channel.getCustomerId());
        });
    }

    private static ClaimedPublicationChannel createExpectedClaimedChannel(PublicationChannel channel,
                                                                          ChannelUpdateEvent event, Instant modifiedDate) {
        return new ClaimedPublicationChannel(event.publicationChannelSummary().id(),
                                             event.publicationChannelSummary().customerId(),
                                             event.publicationChannelSummary().organizationId(),
                                        event.publicationChannelSummary().constraint(),
                                        channel.getChannelType(),
                                        channel.getIdentifier(),
                                        channel.getResourceIdentifier(),
                                        channel.getCreatedDate(),
                                        modifiedDate);
    }

    private static NonClaimedPublicationChannel createExpectedNonClaimedChannel(PublicationChannel channel,
                                                                          ChannelUpdateEvent event, Instant modifiedDate) {
        return new NonClaimedPublicationChannel(event.publicationChannelSummary().id(),
                                             channel.getChannelType(),
                                             channel.getIdentifier(),
                                             channel.getResourceIdentifier(),
                                             channel.getCreatedDate(),
                                             modifiedDate);
    }

    private static ClaimedPublicationChannel randomClaimedChannel(UUID identifier) {
        return new ClaimedPublicationChannel(toChannelClaimId(identifier),
                                             randomUri(),
                                             randomUri(),
                                             new Constraint(ChannelPolicy.EVERYONE, ChannelPolicy.OWNER_ONLY,
                                                            List.of(randomString())),
                                             ChannelType.SERIAL_PUBLICATION,
                                             new SortableIdentifier(identifier.toString()),
                                             SortableIdentifier.next(),
                                             Instant.now(),
                                             Instant.now());
    }

    private static ChannelUpdateEvent createEvent(UUID identifier, Action action) {
        return new ChannelUpdateEvent(action,
                                      new PublicationChannelSummary(toChannelClaimId(identifier), randomUri(),
                                                                    randomUri(), randomUri(),
                                                                    new Constraint(ChannelPolicy.EVERYONE,
                                                                                   ChannelPolicy.EVERYONE,
                                                                                   List.of(randomString()))));
    }

    private static URI toChannelClaimId(UUID channelClaimIdentifier) {
        return UriWrapper.fromHost(new Environment().readEnv("API_HOST"))
                   .addChild("customer")
                   .addChild("channel-claim")
                   .addChild(channelClaimIdentifier.toString())
                   .getUri();
    }

    private List<PublicationChannel> getUpdatedChannels(UUID identifier) {
        var sortableIdentifier = new SortableIdentifier(identifier.toString());
        return resourceService.fetchAllPublicationChannelsByIdentifier(sortableIdentifier, null).getDatabaseEntries();
    }
}

