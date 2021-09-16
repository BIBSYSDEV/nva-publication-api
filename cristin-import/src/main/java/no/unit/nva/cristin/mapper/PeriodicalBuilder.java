package no.unit.nva.cristin.mapper;

import static no.unit.nva.cristin.lambda.constants.MappingConstants.NVA_API_DOMAIN;
import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.util.Optional;
import no.unit.nva.cristin.lambda.constants.MappingConstants;
import no.unit.nva.model.contexttypes.Journal;
import no.unit.nva.model.contexttypes.Periodical;
import no.unit.nva.model.contexttypes.UnconfirmedJournal;
import no.unit.nva.publication.s3imports.UriWrapper;

public class PeriodicalBuilder extends CristinMappingModule {

    private final CristinObject cristinObject;

    public PeriodicalBuilder(CristinObject cristinObject) {
        super(cristinObject);
        this.cristinObject = cristinObject;
    }

    public Periodical buildPeriodicalForPublicationContext() {
        return Optional.ofNullable(cristinObject)
            .map(CristinObject::getJournalPublication)
            .map(CristinJournalPublication::getJournal)
            .map(CristinJournalPublicationJournal::getNsdCode)
            .map(nsdCodeExists -> createJournal())
            .orElseGet(this::createUnconfirmedJournal);
    }

    private Periodical createUnconfirmedJournal() {
        return attempt(() -> new UnconfirmedJournal(extractPublisherTitle(), extractIssn(), extractIssnOnline()))
            .orElseThrow();
    }

    private Periodical createJournal() {
        URI journalUri = new UriWrapper(NVA_API_DOMAIN).addChild(MappingConstants.NSD_PROXY_PATH)
            .addChild(MappingConstants.NSD_PROXY_PATH_JOURNAL)
            .addChild(cristinObject.getJournalPublication().getJournal().getNsdCode().toString())
            .addChild(cristinObject.getPublicationYear().toString())
            .getUri();
        return new Journal(journalUri.toString());
    }

    private String extractIssn() {
        return extractCristinJournalPublication().getJournal().getIssn();
    }

    private String extractIssnOnline() {
        return extractCristinJournalPublication().getJournal().getIssnOnline();
    }

    private String extractPublisherTitle() {
        return extractCristinJournalPublication().getJournal().getJournalTitle();
    }
}
