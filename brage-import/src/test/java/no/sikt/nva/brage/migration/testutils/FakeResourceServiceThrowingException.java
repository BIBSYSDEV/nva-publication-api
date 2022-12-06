package no.sikt.nva.brage.migration.testutils;

import no.unit.nva.model.Publication;
import no.unit.nva.publication.service.impl.ResourceService;

public class FakeResourceServiceThrowingException extends ResourceService {


    private int attemtsToSavePublication = 0;

    public FakeResourceServiceThrowingException() {
        super(null, null, null);
    }

    @Override
    public Publication createPublicationFromImportedEntry(Publication publication) {
        attemtsToSavePublication++;
        throw new RuntimeException("I am throwing exception");
    }

    public int getAttemtsToSavePublication() {
        return attemtsToSavePublication;
    }

}
