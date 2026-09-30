package no.unit.nva.publication.events.handlers.batch;

public record FieldChange(String path, String oldValue, String newValue) {}
