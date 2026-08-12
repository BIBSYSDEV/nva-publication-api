package no.unit.nva.publication.events.handlers.batch;

import java.net.URI;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;

final class ApiUriProvider {

  private static final String API_HOST = "API_HOST";
  private final Environment environment;

  private ApiUriProvider(Environment environment) {
    this.environment = environment;
  }

  static ApiUriProvider create(Environment environment) {
    return new ApiUriProvider(environment);
  }

  URI uriFrom(String... pathSegments) {
    var builder = UriWrapper.fromHost(environment.readEnv(API_HOST));
    for (String segment : pathSegments) {
      builder = builder.addChild(segment);
    }
    return builder.getUri();
  }
}
