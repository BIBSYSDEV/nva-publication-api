package no.unit.nva.cristin.mapper;

import static no.unit.nva.cristin.mapper.nva.exceptions.ExceptionHandling.handlePublicationContextFailure;
import static nva.commons.core.attempt.Try.attempt;
import java.util.List;
import java.util.Optional;
import no.unit.nva.cristin.mapper.channelregistry.ChannelRegistryMapper;
import no.unit.nva.cristin.mapper.nva.CristinMappingModule;
import no.unit.nva.model.contexttypes.MediaContributionPeriodical;
import no.unit.nva.model.contexttypes.Periodical;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.contexttypes.UnconfirmedMediaContributionPeriodical;
import software.amazon.awssdk.services.s3.S3Client;

public class MediaPeriodicalBuilder extends CristinMappingModule {

    public MediaPeriodicalBuilder(CristinObject cristinObject, ChannelRegistryMapper channelRegistryMapper,
                                  S3Client s3Client) {
        super(cristinObject, channelRegistryMapper, s3Client);
    }

    public PublicationContext buildMediaPeriodicalForPublicationContext() {
        return Optional.ofNullable(cristinObject)
                   .map(CristinObject::getJournalPublication)
                   .map(CristinJournalPublication::getJournal)
                   .map(CristinJournalPublicationJournal::getNsdCode)
                   .map(nsdCodeExists -> createMediaContributionPeriodical())
                   .orElseGet(this::createUnconfirmedMediaContributionPeriodical);
    }

    private Periodical createMediaContributionPeriodical() {
        Integer nsdCode = cristinObject.getJournalPublication().getJournal().getNsdCode();
        int publicationYear = extractYearReportedInNvi();
        var journalUri =
            new PublishingChannelEntryResolver(nsdCode, publicationYear, List.of(), channelRegistryMapper, s3Client, cristinObject.getId()).createJournal();
        return new MediaContributionPeriodical(journalUri);
    }

    private Periodical createUnconfirmedMediaContributionPeriodical() {
        return attempt(() -> new UnconfirmedMediaContributionPeriodical(extractPublisherTitle(),
                                                                        extractIssn(),
                                                                        extractIssnOnline()))
                   .orElseThrow(failure -> handlePublicationContextFailure(failure.getException()));
    }
}
