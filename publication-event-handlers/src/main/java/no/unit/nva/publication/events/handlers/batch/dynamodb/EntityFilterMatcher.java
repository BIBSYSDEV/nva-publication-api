package no.unit.nva.publication.events.handlers.batch.dynamodb;

import no.unit.nva.publication.model.storage.Dao;

public interface EntityFilterMatcher {

  boolean matches(Dao dao, BatchFilter filter);
}
