package no.unit.nva.publication.model.business.importcandidate;

import java.net.URI;
import java.time.Instant;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Username;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;

public final class ImportStatusFactory {

    private static final String PUBLICATION = "publication";
    private static final String API_HOST = new Environment().readEnv("API_HOST");

    @JacocoGenerated
    private ImportStatusFactory() {

    }

    public static ImportStatus createNotImported() {
        return ImportStatus.builder()
                   .withCandidateStatus(CandidateStatus.NOT_IMPORTED)
                   .withModifiedDate(Instant.now())
                   .build();
    }

    public static ImportStatus createImported(String username, SortableIdentifier publicationIdentifier) {
        return ImportStatus.builder()
                   .withCandidateStatus(CandidateStatus.IMPORTED)
                   .withSetBy(new Username(username))
                   .withNvaPublicationId(toPublicationUriIdentifier(publicationIdentifier))
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

    private static URI toPublicationUriIdentifier(SortableIdentifier identifier) {
        return UriWrapper.fromHost(API_HOST)
                   .addChild(PUBLICATION)
                   .addChild(identifier.toString())
                   .getUri();
    }
}
