package no.unit.nva.publication.events.handlers.batch.updates;

import no.unit.nva.publication.events.handlers.batch.ManuallyUpdatePublicationsRequest;
import no.unit.nva.publication.model.business.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class UpdateLog {

  private static final Logger logger = LoggerFactory.getLogger(UpdateLog.class);
  private static final String APPLIED_MESSAGE = "{} {}: {} changes, {}";
  private static final String PERSISTED = "persisted";
  private static final String NOT_PERSISTED = "not persisted";
  private static final String DRY_RUN = "dry run";

  private UpdateLog() {}

  static void logApplied(
      ManuallyUpdatePublicationsRequest request,
      Resource resource,
      int changeCount,
      boolean persisted) {
    logger.info(
        APPLIED_MESSAGE,
        request.type(),
        resource.getIdentifier(),
        changeCount,
        outcomeOf(request, persisted));
  }

  private static String outcomeOf(ManuallyUpdatePublicationsRequest request, boolean persisted) {
    if (request.isDryRun()) {
      return DRY_RUN;
    }
    return persisted ? PERSISTED : NOT_PERSISTED;
  }
}
