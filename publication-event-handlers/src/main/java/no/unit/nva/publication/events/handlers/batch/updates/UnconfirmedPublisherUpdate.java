package no.unit.nva.publication.events.handlers.batch.updates;

import java.net.URI;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.contexttypes.Publisher;
import no.unit.nva.model.contexttypes.UnconfirmedPublisher;
import no.unit.nva.publication.events.handlers.batch.ManualUpdateType;
import no.unit.nva.publication.events.handlers.batch.ManuallyUpdatePublicationsRequest;
import no.unit.nva.publication.events.handlers.batch.PublicationChannels;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.service.impl.ResourceService;

public final class UnconfirmedPublisherUpdate extends UnconfirmedChannelUpdate {

  public UnconfirmedPublisherUpdate(ResourceService resourceService) {
    super(resourceService, PublicationChannels.PUBLISHER_PATH);
  }

  @Override
  public ManualUpdateType type() {
    return ManualUpdateType.UNCONFIRMED_PUBLISHER;
  }

  @Override
  public boolean matches(Resource resource, ManuallyUpdatePublicationsRequest request) {
    return PublicationChannels.publishingHouseOf(resource, UnconfirmedPublisher.class)
        .map(UnconfirmedPublisher::getName)
        .filter(name -> request.comparator().matches(name, request.oldValue()))
        .isPresent();
  }

  @Override
  protected PublicationContext confirmedContext(Resource resource, URI channelUri) {
    return PublicationChannels.bookOf(resource)
        .orElseThrow()
        .copy()
        .withPublisher(new Publisher(channelUri))
        .build();
  }
}
