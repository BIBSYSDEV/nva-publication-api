package no.unit.nva.publication.events.handlers.batch.updates;

import java.net.URI;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.contexttypes.Series;
import no.unit.nva.model.contexttypes.UnconfirmedSeries;
import no.unit.nva.publication.events.handlers.batch.ManualUpdateType;
import no.unit.nva.publication.events.handlers.batch.ManuallyUpdatePublicationsRequest;
import no.unit.nva.publication.events.handlers.batch.PublicationChannels;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.service.impl.ResourceService;

public final class UnconfirmedSeriesUpdate extends UnconfirmedChannelUpdate {

  public UnconfirmedSeriesUpdate(ResourceService resourceService) {
    super(resourceService, PublicationChannels.SERIAL_PUBLICATION_PATH);
  }

  @Override
  public ManualUpdateType type() {
    return ManualUpdateType.UNCONFIRMED_SERIES;
  }

  @Override
  public boolean matches(Resource resource, ManuallyUpdatePublicationsRequest request) {
    return PublicationChannels.bookOf(resource)
        .map(Book::getSeries)
        .filter(UnconfirmedSeries.class::isInstance)
        .map(UnconfirmedSeries.class::cast)
        .map(UnconfirmedSeries::getTitle)
        .filter(title -> request.comparator().matches(title, request.oldValue()))
        .isPresent();
  }

  @Override
  protected PublicationContext confirmedContext(Resource resource, URI channelUri) {
    return PublicationChannels.bookOf(resource)
        .orElseThrow()
        .copy()
        .withSeries(new Series(channelUri))
        .build();
  }
}
