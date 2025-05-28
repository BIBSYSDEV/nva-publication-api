package no.unit.nva.cristin.mapper;

import static java.util.Objects.nonNull;
import static no.unit.nva.cristin.mapper.nva.exceptions.ExceptionHandling.handlePublicationContextFailure;
import static nva.commons.core.attempt.Try.attempt;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import no.unit.nva.cristin.lambda.ErrorReport;
import no.unit.nva.cristin.mapper.channelregistry.ChannelRegistryMapper;
import no.unit.nva.cristin.mapper.nva.CristinMappingModule;
import no.unit.nva.cristin.mapper.nva.exceptions.UnconfirmedJournalException;
import no.unit.nva.model.contexttypes.Journal;
import no.unit.nva.model.contexttypes.Periodical;
import no.unit.nva.model.contexttypes.UnconfirmedJournal;
import no.unit.nva.model.exceptions.InvalidIssnException;
import software.amazon.awssdk.services.s3.S3Client;

public class PeriodicalBuilder extends CristinMappingModule {

    private final CristinObject cristinObject;

    public PeriodicalBuilder(CristinObject cristinObject, ChannelRegistryMapper channelRegistryMapper,
                             S3Client s3Client) {
        super(cristinObject, channelRegistryMapper, s3Client);
        this.cristinObject = cristinObject;
    }

    public Periodical buildPeriodicalForPublicationContext() {
        return Optional.ofNullable(cristinObject)
                   .map(CristinObject::getJournalPublication)
                   .map(CristinJournalPublication::getJournal)
                   .map(nsdCodeExists -> createJournal())
                   .orElseGet(this::createUnconfirmedJournal);
    }

    private Periodical createUnconfirmedJournal() {
        return attempt(this::createUnconfirmedJournalAndPersistErrorReport)
                   .orElseThrow(failure -> handlePublicationContextFailure(failure.getException()));
    }

    private UnconfirmedJournal createUnconfirmedJournalAndPersistErrorReport() throws InvalidIssnException {
        var title = extractPublisherTitle();
        ErrorReport.exceptionName(UnconfirmedJournalException.name())
            .withBody(title)
            .withCristinId(cristinObject.getId())
            .persist(s3Client);
        return new UnconfirmedJournal(title, extractIssn(), extractIssnOnline());
    }

    private Periodical createJournal() {
        var nsdCode = cristinObject.getJournalPublication().getJournal().getNsdCode();
        int publicationYear = extractYearReportedInNvi();
        var channelNames = nonNull(extractPublisherTitle()) ? List.of(extractPublisherTitle()) : List.<String>of();
        var journalUri =
            new PublishingChannelEntryResolver(nsdCode, publicationYear, channelNames,
                                               extractIssnList(),
                                               channelRegistryMapper, s3Client,
                                               cristinObject.getId()).createJournal();
        return nonNull(journalUri) ? new Journal(journalUri) : null;
    }

    private List<String> extractIssnList() {
        return Stream.of(extractIssn(), extractIssnOnline()).filter(Objects::nonNull).toList();
    }
}
