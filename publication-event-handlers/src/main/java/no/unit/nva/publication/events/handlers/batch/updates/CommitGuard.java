package no.unit.nva.publication.events.handlers.batch.updates;

import no.unit.nva.publication.events.handlers.batch.ManuallyUpdatePublicationsRequest;

final class CommitGuard {

  private static final String DRY_RUN_MESSAGE =
      "Refusing to commit %s update because dry run was requested";

  private CommitGuard() {}

  static void rejectDryRun(ManuallyUpdatePublicationsRequest request) {
    if (request.isDryRun()) {
      throw new IllegalStateException(DRY_RUN_MESSAGE.formatted(request.type()));
    }
  }
}
