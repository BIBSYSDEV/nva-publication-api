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
        return new ImportStatus(CandidateStatus.NOT_IMPORTED,
                                Instant.now(),
                                null,
                                null,
                                null);
    }

    public static ImportStatus createImported(Username setBy, URI nvaPublicationUri) {
        return new ImportStatus(CandidateStatus.IMPORTED,
                                Instant.now(), setBy,
                                nvaPublicationUri,
                                null);
    }

    public static ImportStatus createNotApplicable(Username setBy, String comment) {
        return new ImportStatus(CandidateStatus.NOT_APPLICABLE,
                                Instant.now(),
                                setBy,
                                null,
                                comment);
    }
}
