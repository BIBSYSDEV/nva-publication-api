package no.unit.nva.publication.model.business.publicationstate;

import no.unit.nva.model.ImportSource;

@FunctionalInterface
public interface ImportSourceProvider {

  ImportSource importSource();
}
