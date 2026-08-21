package no.unit.nva.publication.events.handlers.batch.updates;

import java.net.URI;
import java.util.Optional;
import no.unit.nva.model.contexttypes.Journal;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.contexttypes.UnconfirmedJournal;
import no.unit.nva.publication.events.handlers.batch.ManualUpdateType;
import no.unit.nva.publication.events.handlers.batch.ManuallyUpdatePublicationsRequest;
import no.unit.nva.publication.events.handlers.batch.PublicationChannels;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.service.impl.ResourceService;

public final class UnconfirmedJournalUpdate extends UnconfirmedChannelUpdate {

  public UnconfirmedJournalUpdate(ResourceService resourceService) {
    super(resourceService, PublicationChannels.SERIAL_PUBLICATION_PATH);
  }

  @Override
  public ManualUpdateType type() {
    return ManualUpdateType.UNCONFIRMED_JOURNAL;
  }

  @Override
  public boolean matches(Resource resource, ManuallyUpdatePublicationsRequest request) {
    return Optional.of(PublicationChannels.contextOf(resource))
        .filter(UnconfirmedJournal.class::isInstance)
        .map(UnconfirmedJournal.class::cast)
        .map(UnconfirmedJournal::getTitle)
        .filter(title -> request.comparator().matches(title, request.oldValue()))
        .isPresent();
  }

  @Override
  protected PublicationContext confirmedContext(Resource resource, URI channelUri) {
    return new Journal(channelUri);
  }
}
