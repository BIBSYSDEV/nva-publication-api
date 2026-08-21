package no.unit.nva.publication.events.handlers.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class UpdateLog {

  private static final Logger logger = LoggerFactory.getLogger(UpdateLog.class);
  private static final String PLANNED_MESSAGE = "{} {}: {} changes, {}";
  private static final String PERSISTED = "persisted";
  private static final String NOT_PERSISTED = "not persisted";
  private static final String DRY_RUN = "dry run";

  private UpdateLog() {}

  static void logPlan(
      ManuallyUpdatePublicationsRequest request, UpdatePlan plan, boolean persisted) {
    logger.info(
        PLANNED_MESSAGE,
        request.type(),
        plan.resource().getIdentifier(),
        plan.fieldChanges().size(),
        outcomeOf(request, persisted));
  }

  private static String outcomeOf(ManuallyUpdatePublicationsRequest request, boolean persisted) {
    if (request.isDryRun()) {
      return DRY_RUN;
    }
    return persisted ? PERSISTED : NOT_PERSISTED;
  }
}
