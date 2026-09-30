package no.unit.nva.publication.events.handlers.batch.updates;

import java.net.URI;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.contexttypes.Journal;
import no.unit.nva.model.contexttypes.Series;
import no.unit.nva.publication.events.handlers.batch.ManualUpdateType;
import no.unit.nva.publication.events.handlers.batch.ManuallyUpdatePublicationsRequest;
import no.unit.nva.publication.events.handlers.batch.PublicationChannels;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.service.impl.ResourceService;

public final class SerialPublicationUpdate extends ResourceUpdate {

  public SerialPublicationUpdate(ResourceService resourceService) {
    super(resourceService);
  }

  @Override
  public ManualUpdateType type() {
    return ManualUpdateType.SERIAL_PUBLICATION;
  }

  @Override
  public boolean matches(Resource resource, ManuallyUpdatePublicationsRequest request) {
    var context = PublicationChannels.contextOf(resource);
    if (context instanceof Book book && book.getSeries() instanceof Series series) {
      return series.getId().toString().contains(request.oldValue());
    }
    return context instanceof Journal journal
        && journal.getId().toString().contains(request.oldValue());
  }

  @Override
  protected Resource update(Resource resource, ManuallyUpdatePublicationsRequest request) {
    var context = PublicationChannels.contextOf(resource);

    if (context instanceof Book book && book.getSeries() instanceof Series series) {
      var newSeriesUri = replacedUri(series.getId(), request);
      PublicationChannels.setContextOf(
          resource, book.copy().withSeries(new Series(newSeriesUri)).build());
    } else if (context instanceof Journal journal) {
      PublicationChannels.setContextOf(
          resource, new Journal(replacedUri(journal.getId(), request)));
    }
    return resource;
  }

  private URI replacedUri(URI channelId, ManuallyUpdatePublicationsRequest request) {
    return URI.create(channelId.toString().replace(request.oldValue(), request.newValue()));
  }
}
