package no.unit.nva.publication.events.handlers.batch.updates;

import java.net.URI;
import java.util.Optional;
import no.unit.nva.model.contexttypes.Publisher;
import no.unit.nva.publication.events.handlers.batch.ManualUpdateType;
import no.unit.nva.publication.events.handlers.batch.ManuallyUpdatePublicationsRequest;
import no.unit.nva.publication.events.handlers.batch.PublicationChannels;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.service.impl.ResourceService;

public final class PublisherUpdate extends ResourceUpdate {

  public PublisherUpdate(ResourceService resourceService) {
    super(resourceService);
  }

  @Override
  public ManualUpdateType type() {
    return ManualUpdateType.PUBLISHER;
  }

  @Override
  public boolean matches(Resource resource, ManuallyUpdatePublicationsRequest request) {
    return currentPublisherId(resource).filter(uri -> uri.contains(request.oldValue())).isPresent();
  }

  @Override
  protected Resource update(Resource resource, ManuallyUpdatePublicationsRequest request) {
    var book = PublicationChannels.bookOf(resource).orElseThrow();
    var publisherUri =
        currentPublisherId(resource)
            .map(uri -> uri.replace(request.oldValue(), request.newValue()))
            .map(URI::create)
            .orElseThrow();

    PublicationChannels.setContextOf(
        resource, book.copy().withPublisher(new Publisher(publisherUri)).build());
    return resource;
  }

  private Optional<String> currentPublisherId(Resource resource) {
    return PublicationChannels.publishingHouseOf(resource, Publisher.class)
        .map(Publisher::getId)
        .map(URI::toString);
  }
}
