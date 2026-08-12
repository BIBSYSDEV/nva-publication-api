package no.unit.nva.publication.events.handlers.batch;

import java.util.List;

record ManualUpdateResult(int matchedResources, List<ResourceChange> changes) {}
