package no.sikt.nva.brage.migration.mapper;

import static java.util.Objects.isNull;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import no.sikt.nva.brage.migration.record.Record;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.PublicationDate.Builder;
import no.unit.nva.model.Reference;
import no.unit.nva.model.Role;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.exceptions.InvalidIsbnException;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.model.exceptions.InvalidUnconfirmedSeriesException;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.language.LanguageMapper;
import nva.commons.core.paths.UriWrapper;
import org.joda.time.Instant;

public final class BrageNvaMapper {

    private BrageNvaMapper() {

    }

    public static Publication toNvaPublication(Record record)
        throws InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
        return new Publication.Builder()
                   .withDoi(extractDoi(record))
                   .withHandle(extractHandle(record))
                   .withEntityDescription(extractEntityDescription(record))
                   .withPublishedDate(extractPublishedDate(record))
                   .build();
    }

    private static java.time.Instant extractPublishedDate(Record record) {
        if (isNull(record.getPublishedDate())) {
            return null;
        }
        return Instant.parse(record.getPublishedDate().getNvaDate()).toDate().toInstant();
    }

    private static URI extractHandle(Record brageRecord) {
        return brageRecord.getId();
    }

    private static URI extractDoi(Record brageRecord) {
        return brageRecord.getDoi();
    }

    private static EntityDescription extractEntityDescription(Record brageRecord)
        throws InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
        return new EntityDescription.Builder()
                   .withLanguage(extractLanguage(brageRecord))
                   .withAbstract(extractAbstract(brageRecord))
                   .withDescription(extractDescription(brageRecord))
                   .withDate(extractDate(brageRecord))
                   .withContributors(extractContributors(brageRecord))
                   .withTags(extractTags(brageRecord))
                   .withReference(extractReference(brageRecord))
                   .build();
    }

    private static Reference extractReference(Record record)
        throws InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
        return new Reference.Builder()
                   .withPublishingContext(extractPublicationContext(record))
                   .build();
    }

    private static PublicationContext extractPublicationContext(Record record)
        throws InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
        return new PublicationContextMapper()
                   .buildPublicationContext(record);
    }

    private static List<String> extractTags(Record record) {
        return record.getEntityDescription().getTags();
    }

    private static List<Contributor> extractContributors(Record record) {
        if (isNull(record.getEntityDescription().getContributors())) {
            return null;
        }
        return record.getEntityDescription()
                   .getContributors()
                   .stream()
                   .filter(Objects::nonNull)
                   .map(BrageNvaMapper::mapContributorToNva)
                   .collect(Collectors.toList());
    }

    private static Contributor mapContributorToNva(no.sikt.nva.brage.migration.record.Contributor contributor) {
        return new Contributor.Builder()
                   .withRole(Role.lookup(contributor.getRole()))
                   .withIdentity(extractIdentity(contributor))
                   .build();
    }

    @JacocoGenerated
    private static Identity extractIdentity(no.sikt.nva.brage.migration.record.Contributor contributor) {
        if (isNull(contributor.getIdentity())) {
            return null;
        }
        var identity = contributor.getIdentity();
        return new Identity.Builder()
                   .withName(identity.getName())
                   .build();
    }

    private static PublicationDate extractDate(Record record) {
        if (isNull(record.getEntityDescription().getPublicationDate())) {
            return null;
        }
        var date = record.getEntityDescription().getPublicationDate().getNva();
        return new Builder()
                   .withYear(date.getYear())
                   .withMonth(date.getMonth())
                   .withDay(date.getDay())
                   .build();
    }

    private static URI extractLanguage(Record brageRecord) {
        if (isNull(brageRecord.getEntityDescription().getLanguage())) {
            return UriWrapper.fromUri(LanguageMapper.LEXVO_URI_UNDEFINED).getUri();
        }
        return brageRecord.getEntityDescription().getLanguage().getNva();
    }

    private static String extractDescription(Record brageRecord) {
        if (isNull(brageRecord.getEntityDescription().getDescriptions())) {
            return null;
        }
        return brageRecord.getEntityDescription().getDescriptions().get(0);
    }

    private static String extractAbstract(Record brageRecord) {
        if (isNull(brageRecord.getEntityDescription().getAbstracts())) {
            return null;
        }
        return brageRecord.getEntityDescription().getAbstracts().get(0);
    }
}
