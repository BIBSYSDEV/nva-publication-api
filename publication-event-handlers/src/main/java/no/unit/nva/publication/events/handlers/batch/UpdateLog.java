package no.unit.nva.publication.events.handlers.batch;

import static nva.commons.core.attempt.Try.attempt;

import java.util.Collection;
import no.unit.nva.commons.json.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class UpdateLog {

  private static final Logger logger = LoggerFactory.getLogger(UpdateLog.class);
  private static final String PLANNED_MESSAGE = "{} {}: {} changes, {}";
  private static final String PAGE_CHANGES_MESSAGE = "Changes on page {}: {}";
  private static final String PERSISTED = "persisted";
  private static final String NOT_PERSISTED = "not persisted";
  private static final String DRY_RUN = "dry run";

  private UpdateLog() {}

  static void logChanges(int pageNumber, Collection<ResourceChange> changes) {
    if (!changes.isEmpty()) {
      logger.info(PAGE_CHANGES_MESSAGE, pageNumber, toJson(changes));
    }
  }

  static void logPlan(
      ManuallyUpdatePublicationsRequest request, UpdatePlan plan, boolean persisted) {
    logger.info(
        PLANNED_MESSAGE,
        request.type(),
        plan.resource().getIdentifier(),
        plan.fieldChanges().size(),
        outcomeOf(request, persisted));
  }

  private static String toJson(Collection<ResourceChange> changes) {
    return attempt(() -> JsonUtils.dtoObjectMapper.writeValueAsString(changes)).orElseThrow();
  }

  private static String outcomeOf(ManuallyUpdatePublicationsRequest request, boolean persisted) {
    if (request.isDryRun()) {
      return DRY_RUN;
    }
    return persisted ? PERSISTED : NOT_PERSISTED;
  }
}
