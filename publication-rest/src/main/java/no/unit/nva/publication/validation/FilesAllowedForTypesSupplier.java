package no.unit.nva.publication.validation;

import java.net.URI;
import java.util.Set;

public interface FilesAllowedForTypesSupplier {
    Set<String> get(URI customerUri);
}
