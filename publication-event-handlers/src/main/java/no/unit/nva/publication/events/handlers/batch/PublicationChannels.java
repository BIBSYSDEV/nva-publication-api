package no.unit.nva.publication.events.handlers.batch;

import static no.unit.nva.publication.PublicationServiceConfig.API_HOST;

import java.net.URI;
import java.util.Optional;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.contexttypes.PublishingHouse;
import no.unit.nva.publication.model.business.Resource;
import nva.commons.core.paths.UriWrapper;

public final class PublicationChannels {

  public static final String PUBLISHER_PATH = "publisher";
  public static final String SERIAL_PUBLICATION_PATH = "serial-publication";
  private static final String PUBLICATION_CHANNELS_V2_PATH = "publication-channels-v2";

  private PublicationChannels() {}

  public static PublicationContext contextOf(Resource resource) {
    return resource.getEntityDescription().getReference().getPublicationContext();
  }

  public static void setContextOf(Resource resource, PublicationContext context) {
    resource.getEntityDescription().getReference().setPublicationContext(context);
  }

  public static Optional<Book> bookOf(Resource resource) {
    return Optional.of(contextOf(resource)).filter(Book.class::isInstance).map(Book.class::cast);
  }

  public static <T extends PublishingHouse> Optional<T> publishingHouseOf(
      Resource resource, Class<T> type) {
    return bookOf(resource).map(Book::getPublisher).filter(type::isInstance).map(type::cast);
  }

  public static String publicationYearOf(Resource resource) {
    return resource.getEntityDescription().getPublicationDate().getYear();
  }

  public static boolean matches(String actual, String expected, Comparator comparator) {
    return switch (comparator) {
      case CONTAINS -> actual.contains(expected);
      case MATCHES -> actual.equals(expected);
    };
  }

  public static URI channelUri(String channelPath, String identifier, String year) {
    return UriWrapper.fromHost(API_HOST)
        .addChild(PUBLICATION_CHANNELS_V2_PATH, channelPath, identifier, year)
        .getUri();
  }
}
