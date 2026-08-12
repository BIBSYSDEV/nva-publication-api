package no.unit.nva.publication.events.handlers.batch;

import java.net.URI;
import java.util.Optional;
import java.util.function.BiFunction;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.contexttypes.Journal;
import no.unit.nva.model.contexttypes.Publisher;
import no.unit.nva.model.contexttypes.PublishingHouse;
import no.unit.nva.model.contexttypes.Series;
import no.unit.nva.model.contexttypes.UnconfirmedJournal;
import no.unit.nva.model.contexttypes.UnconfirmedPublisher;
import no.unit.nva.model.contexttypes.UnconfirmedSeries;
import no.unit.nva.publication.model.business.Resource;

final class PublicationChannelUpdater {

  private static final String PUBLICATION_CHANNELS_V2_PATH_PARAM = "publication-channels-v2";
  private static final String PUBLISHER = "publisher";
  private static final String SERIAL_PUBLICATION = "serial-publication";
  private final ApiUriProvider uriProvider;

  private PublicationChannelUpdater(ApiUriProvider uriProvider) {
    this.uriProvider = uriProvider;
  }

  static PublicationChannelUpdater create(ApiUriProvider uriProvider) {
    return new PublicationChannelUpdater(uriProvider);
  }

  boolean hasPublisher(Resource resource, String publisher) {
    return getPublishingHouse(resource, Publisher.class)
        .map(Publisher::getId)
        .map(URI::toString)
        .filter(uri -> uri.contains(publisher))
        .isPresent();
  }

  Resource updatePublisher(Resource resource, ManuallyUpdatePublicationsRequest request) {
    var book = (Book) resource.getEntityDescription().getReference().getPublicationContext();
    var publisherUri =
        getPublishingHouse(resource, Publisher.class)
            .map(Publisher::getId)
            .map(URI::toString)
            .map(uri -> uri.replace(request.oldValue(), request.newValue()))
            .map(URI::create)
            .orElseThrow();

    resource
        .getEntityDescription()
        .getReference()
        .setPublicationContext(book.copy().withPublisher(new Publisher(publisherUri)).build());
    return resource;
  }

  boolean hasSerialPublication(Resource resource, String value) {
    var context = resource.getEntityDescription().getReference().getPublicationContext();
    if (context instanceof Book book && book.getSeries() instanceof Series series) {
      return series.getId().toString().contains(value);
    }
    return context instanceof Journal journal && journal.getId().toString().contains(value);
  }

  Resource updateSeriesOrJournal(Resource resource, ManuallyUpdatePublicationsRequest request) {
    var context = resource.getEntityDescription().getReference().getPublicationContext();
    var reference = resource.getEntityDescription().getReference();

    if (context instanceof Book book && book.getSeries() instanceof Series series) {
      var newSeriesUri =
          URI.create(series.getId().toString().replace(request.oldValue(), request.newValue()));
      reference.setPublicationContext(book.copy().withSeries(new Series(newSeriesUri)).build());
    } else if (context instanceof Journal journal) {
      var newJournalUri =
          URI.create(journal.getId().toString().replace(request.oldValue(), request.newValue()));
      reference.setPublicationContext(new Journal(newJournalUri));
    }
    return resource;
  }

  boolean hasUnconfirmedPublisher(Resource resource, String publisherName, Comparator comparator) {
    return getPublishingHouse(resource, UnconfirmedPublisher.class)
        .map(UnconfirmedPublisher::getName)
        .filter(value -> matches(value, publisherName, comparator))
        .isPresent();
  }

  Resource updateUnconfirmedPublisher(
      Resource resource, ManuallyUpdatePublicationsRequest request) {
    return updateUnconfirmedToConfirmed(
        resource, request, PUBLISHER, this::createBookWithPublisher);
  }

  boolean hasUnconfirmedSeries(Resource resource, String seriesTitle, Comparator comparator) {
    return Optional.of(resource.getEntityDescription().getReference().getPublicationContext())
        .filter(Book.class::isInstance)
        .map(Book.class::cast)
        .map(Book::getSeries)
        .filter(UnconfirmedSeries.class::isInstance)
        .map(UnconfirmedSeries.class::cast)
        .map(UnconfirmedSeries::getTitle)
        .filter(value -> matches(value, seriesTitle, comparator))
        .isPresent();
  }

  Resource updateUnconfirmedSeries(Resource resource, ManuallyUpdatePublicationsRequest request) {
    return updateUnconfirmedToConfirmed(
        resource, request, SERIAL_PUBLICATION, this::createBookWithSeries);
  }

  boolean hasUnconfirmedJournal(Resource resource, String journalTitle, Comparator comparator) {
    return Optional.of(resource.getEntityDescription().getReference().getPublicationContext())
        .filter(UnconfirmedJournal.class::isInstance)
        .map(UnconfirmedJournal.class::cast)
        .map(UnconfirmedJournal::getTitle)
        .filter(value -> matches(value, journalTitle, comparator))
        .isPresent();
  }

  Resource updateUnconfirmedJournal(Resource resource, ManuallyUpdatePublicationsRequest request) {
    var year = resource.getEntityDescription().getPublicationDate().getYear();
    var journalUri = buildPublicationChannelUri(SERIAL_PUBLICATION, year, request.newValue());
    resource.getEntityDescription().getReference().setPublicationContext(new Journal(journalUri));
    return resource;
  }

  private boolean matches(String actual, String expected, Comparator comparator) {
    return switch (comparator) {
      case CONTAINS -> actual.contains(expected);
      case MATCHES -> actual.equals(expected);
    };
  }

  private Resource updateUnconfirmedToConfirmed(
      Resource resource,
      ManuallyUpdatePublicationsRequest request,
      String channelType,
      BiFunction<Book, URI, Book> bookUpdater) {
    var book = (Book) resource.getEntityDescription().getReference().getPublicationContext();
    var year = resource.getEntityDescription().getPublicationDate().getYear();
    var channelUri = buildPublicationChannelUri(channelType, year, request.newValue());
    var newBook = bookUpdater.apply(book, channelUri);
    resource.getEntityDescription().getReference().setPublicationContext(newBook);
    return resource;
  }

  private Book createBookWithPublisher(Book book, URI publisherUri) {
    return book.copy().withPublisher(new Publisher(publisherUri)).build();
  }

  private Book createBookWithSeries(Book book, URI seriesUri) {
    return book.copy().withSeries(new Series(seriesUri)).build();
  }

  private <T extends PublishingHouse> Optional<T> getPublishingHouse(
      Resource resource, Class<T> type) {
    return Optional.of(resource.getEntityDescription().getReference().getPublicationContext())
        .filter(Book.class::isInstance)
        .map(Book.class::cast)
        .map(Book::getPublisher)
        .filter(type::isInstance)
        .map(type::cast);
  }

  private URI buildPublicationChannelUri(String type, String year, String pid) {
    return uriProvider.uriFrom(PUBLICATION_CHANNELS_V2_PATH_PARAM, type, pid, year);
  }
}
