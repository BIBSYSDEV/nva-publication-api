package no.unit.nva.publication.events.handlers.batch;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import no.unit.nva.auth.uriretriever.UriRetriever;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.SearchService;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ManuallyUpdatePublicationsHandler implements RequestStreamHandler {

  private static final Logger logger =
      LoggerFactory.getLogger(ManuallyUpdatePublicationsHandler.class);
  private static final String REPORT_LOG_MESSAGE = "Manual update report: {}";
  private final SearchService searchService;
  private final ResourceService resourceService;
  private final Environment environment;

  @JacocoGenerated
  public ManuallyUpdatePublicationsHandler() {
    this.resourceService = ResourceService.defaultService();
    this.searchService = SearchService.create(new UriRetriever(), resourceService);
    this.environment = new Environment();
  }

  public ManuallyUpdatePublicationsHandler(
      SearchService searchService, ResourceService resourceService, Environment environment) {
    this.searchService = searchService;
    this.resourceService = resourceService;
    this.environment = environment;
  }

  @Override
  public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context)
      throws IOException {
    var request = ManuallyUpdatePublicationsRequest.fromInputStream(inputStream);
    var searchResult = searchService.searchResourcesByParam(request.searchParams());
    var changes =
        ManuallyUpdatePublicationUtil.create(resourceService, environment)
            .update(searchResult.resources(), request);
    var report = ManuallyUpdatePublicationsReport.create(request, searchResult, changes);

    logger.info(REPORT_LOG_MESSAGE, report.toJsonString());
    JsonUtils.dtoObjectMapper.writeValue(outputStream, report);
  }
}
