package no.sikt.nva.scopus.utils;

import no.unit.nva.model.Publication;
import no.unit.nva.publication.service.impl.ResourceService;

public class FakeResourceServiceThrowingException extends ResourceService {


    private int attemptsToSavePublication = 0;

    public FakeResourceServiceThrowingException() {
        super(null, null, null);
    }

    @Override
    public Publication createPublicationFromImportedEntry(Publication publication) {
        attemptsToSavePublication++;
        throw new RuntimeException("I am throwing exception");
    }

    public int getAttemptsToSavePublication() {
        return attemptsToSavePublication;
    }

}
