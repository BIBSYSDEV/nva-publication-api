package no.unit.nva.publication.events.handlers.batch;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;
import no.unit.nva.auth.uriretriever.UriRetriever;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.SearchService;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ManuallyUpdatePublicationsHandler implements RequestStreamHandler {

  private static final Logger logger =
      LoggerFactory.getLogger(ManuallyUpdatePublicationsHandler.class);
  private static final String SUMMARY_LOG_MESSAGE = "Manual update summary: {}";
  private static final String PAGE_LOG_MESSAGE =
      "Page {}: {} hits fetched so far, {} resources changed, {} total hits";
  private static final String LIMIT_REACHED_LOG_MESSAGE =
      "Stopped after changing {} resources: run again to continue where this run left off";
  private final SearchService searchService;
  private final ResourceService resourceService;

  @JacocoGenerated
  public ManuallyUpdatePublicationsHandler() {
    this.resourceService = ResourceService.defaultService();
    this.searchService = SearchService.create(new UriRetriever(), resourceService);
  }

  public ManuallyUpdatePublicationsHandler(
      SearchService searchService, ResourceService resourceService) {
    this.searchService = searchService;
    this.resourceService = resourceService;
  }

  @Override
  public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context)
      throws IOException {
    var request = ManuallyUpdatePublicationsRequest.fromInputStream(inputStream);
    var progress = updateAllPages(request);

    logger.info(
        SUMMARY_LOG_MESSAGE,
        ManuallyUpdatePublicationsReport.summary(request, progress).toJsonString());
    JsonUtils.dtoObjectMapper.writeValue(
        outputStream, ManuallyUpdatePublicationsReport.create(request, progress));
  }

  private ManualUpdateProgress updateAllPages(ManuallyUpdatePublicationsRequest request) {
    var publicationUtil = ManuallyUpdatePublicationUtil.create(resourceService);
    var maxChanges = request.maxChanges();
    var progress = ManualUpdateProgress.empty();
    var pageUri =
        Optional.of(searchService.firstPageUri(request.searchParams(), request.searchPageSize()));

    while (pageUri.isPresent()) {
      var page = searchService.searchPage(pageUri.get());
      var result =
          publicationUtil.update(page.resources(), request, progress.remainingChanges(maxChanges));
      progress = progress.plus(page, result);
      logPage(progress);
      UpdateLog.logChanges(progress.pagesFetched(), result.changes());
      pageUri = progress.limitReached(maxChanges) ? Optional.empty() : page.nextPage();
    }

    if (progress.limitReached(maxChanges)) {
      logger.warn(LIMIT_REACHED_LOG_MESSAGE, maxChanges);
    }
    return progress;
  }

  private static void logPage(ManualUpdateProgress progress) {
    logger.info(
        PAGE_LOG_MESSAGE,
        progress.pagesFetched(),
        progress.hitsReturned(),
        progress.resourcesChanged(),
        progress.totalHits());
  }
}
