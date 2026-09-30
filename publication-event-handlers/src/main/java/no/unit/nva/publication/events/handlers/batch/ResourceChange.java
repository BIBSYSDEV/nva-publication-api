package no.unit.nva.publication.events.handlers.batch;

import java.util.List;

public record ResourceChange(String identifier, List<FieldChange> fieldChanges) {}
