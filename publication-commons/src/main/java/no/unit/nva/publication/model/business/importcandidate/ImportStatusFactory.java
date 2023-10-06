package no.unit.nva.publication.model.business.importcandidate;

import java.net.URI;
import java.time.Instant;
import no.unit.nva.model.Username;
import nva.commons.core.JacocoGenerated;

public final class ImportStatusFactory {

    @JacocoGenerated
    private ImportStatusFactory() {

    }

    public static ImportStatus createNotImported() {
        return ImportStatus.builder()
                   .withCandidateStatus(CandidateStatus.NOT_IMPORTED)
                   .withModifiedDate(Instant.now())
                   .build();
    }

    public static ImportStatus createImported(Username setBy, URI nvaPublicationUri) {
        return ImportStatus.builder()
                   .withCandidateStatus(CandidateStatus.IMPORTED)
                   .withSetBy(setBy)
                   .withNvaPublicationId(nvaPublicationUri)
                   .withModifiedDate(Instant.now())
                   .build();
    }

    public static ImportStatus createNotApplicable(Username setBy, String comment) {
        return ImportStatus.builder()
                   .withCandidateStatus(CandidateStatus.NOT_APPLICABLE)
                   .withSetBy(setBy)
                   .withModifiedDate(Instant.now())
                   .withComment(comment)
                   .build();
    }
}
