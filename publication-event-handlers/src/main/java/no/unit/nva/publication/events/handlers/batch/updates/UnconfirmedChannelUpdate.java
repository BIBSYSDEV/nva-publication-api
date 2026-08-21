package no.unit.nva.publication.events.handlers.batch.updates;

import java.net.URI;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.publication.events.handlers.batch.ManuallyUpdatePublicationsRequest;
import no.unit.nva.publication.events.handlers.batch.PublicationChannels;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.service.impl.ResourceService;

abstract class UnconfirmedChannelUpdate extends ResourceUpdate {

  private final String channelPath;

  protected UnconfirmedChannelUpdate(ResourceService resourceService, String channelPath) {
    super(resourceService);
    this.channelPath = channelPath;
  }

  @Override
  protected final Resource update(Resource resource, ManuallyUpdatePublicationsRequest request) {
    var channelUri =
        PublicationChannels.channelUri(
            channelPath, request.newValue(), PublicationChannels.publicationYearOf(resource));

    PublicationChannels.setContextOf(resource, confirmedContext(resource, channelUri));
    return resource;
  }

  protected abstract PublicationContext confirmedContext(Resource resource, URI channelUri);
}
