package no.unit.nva.publication.model.business.importcandidate;

import java.net.URI;
import java.time.Instant;
import no.unit.nva.model.Username;
import nva.commons.core.JacocoGenerated;

public final class ImportStatusFactory {

    @JacocoGenerated
    private ImportStatusFactory() {

    }

    public static ImportStatusB createNotImported() {
        return new ImportStatusB(CandidateStatus.NOT_IMPORTED,
                                 Instant.now(),
                                 null,
                                 null,
                                 null);
    }

    public static ImportStatusB createImported(Username setBy, URI nvaPublicationUri) {
        return new ImportStatusB(CandidateStatus.IMPORTED,
                                 Instant.now(), setBy,
                                 nvaPublicationUri,
                                 null);
    }

    public static ImportStatusB createNotApplicable(Username setBy, String comment) {
        return new ImportStatusB(CandidateStatus.NOT_APPLICABLE,
                                 Instant.now(),
                                 setBy,
                                 null,
                                 comment);
    }
}
