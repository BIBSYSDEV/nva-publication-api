package no.sikt.nva.brage.migration.mapper;

import static java.util.Objects.isNull;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import no.sikt.nva.brage.migration.record.Language;
import no.sikt.nva.brage.migration.record.Record;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.PublicationDate.Builder;
import no.unit.nva.model.Reference;
import no.unit.nva.model.Role;
import no.unit.nva.model.exceptions.InvalidIsbnException;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.model.exceptions.InvalidUnconfirmedSeriesException;
import nva.commons.core.JacocoGenerated;
import org.apache.tika.langdetect.optimaize.OptimaizeLangDetector;
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
        return Optional.ofNullable(record.getPublishedDate())
            .map(date -> Instant.parse(record.getPublishedDate().getNvaDate()).toDate().toInstant())
            .orElse(null);
    }

    private static URI extractHandle(Record brageRecord) {
        return brageRecord.getId();
    }

    private static URI extractDoi(Record brageRecord) {
        return brageRecord.getDoi();
    }

    private static EntityDescription extractEntityDescription(Record record)
        throws InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
        return new EntityDescription.Builder()
                   .withLanguage(extractLanguage(record))
                   .withAbstract(extractAbstract(record))
                   .withDescription(extractDescription(record))
                   .withDate(extractDate(record))
                   .withContributors(extractContributors(record))
                   .withTags(extractTags(record))
                   .withReference(extractReference(record))
                   .withMainTitle(extractMainTitle(record))
                   .withAlternativeTitles(extractAlternativeTitles(record))
                   .build();
    }

    private static Map<String, String> extractAlternativeTitles(Record record) {
        return Optional.of(emptyIfNull(record.getEntityDescription().getAlternativeTitles()).stream()
                               .map(BrageNvaMapper::generateLanguageMap)
                               .flatMap(i -> i.entrySet().stream())
                               .collect(Collectors.toMap(Entry::getKey, Entry::getValue, (first, second) -> first)))
                   .orElse(null);
    }

    @JacocoGenerated
    private static List<String> emptyIfNull(List<String> values) {
        return isNull(values) ? Collections.emptyList() : values;
    }

    private static Map<String, String> generateLanguageMap(String title) {
        var detector = new OptimaizeLangDetector().loadModels();
        var language = detector.detect(title).getLanguage();
        return Map.of(language, title);
    }

    private static String extractMainTitle(Record record) {
        return Optional.ofNullable(record.getEntityDescription())
                   .map(no.sikt.nva.brage.migration.record.EntityDescription::getMainTitle)
                   .orElse(null);
    }

    private static Reference extractReference(Record record)
        throws InvalidIssnException, InvalidIsbnException, InvalidUnconfirmedSeriesException {
        return new Reference.Builder()
                   .withPublishingContext(PublicationContextMapper.buildPublicationContext(record))
                   .withPublicationInstance(PublicationInstanceMapper.buildPublicationInstance(record))
                   .build();
    }

    private static List<String> extractTags(Record record) {
        return record.getEntityDescription().getTags();
    }

    private static List<Contributor> extractContributors(Record record) {
        return Optional.ofNullable(getContributors(record))
                   .map(BrageNvaMapper::generateContributors)
                   .orElse(null);
    }

    private static List<no.sikt.nva.brage.migration.record.Contributor> getContributors(Record record) {
        return record.getEntityDescription().getContributors();
    }

    private static List<Contributor> generateContributors(
        List<no.sikt.nva.brage.migration.record.Contributor> contributors) {
        return contributors.stream()
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

    private static Identity extractIdentity(no.sikt.nva.brage.migration.record.Contributor contributor) {
        return Optional.ofNullable(contributor.getIdentity())
                   .map(BrageNvaMapper::generateIdentity)
                   .orElse(null);
    }

    private static Identity generateIdentity(no.sikt.nva.brage.migration.record.Identity identity) {
        return new Identity.Builder().withName(identity.getName())
                   .build();
    }

    private static PublicationDate extractDate(Record record) {
        return Optional.ofNullable(record.getEntityDescription().getPublicationDate())
                   .map(BrageNvaMapper::generatePublicationDate)
                   .orElse(null);
    }

    private static PublicationDate generatePublicationDate(
        no.sikt.nva.brage.migration.record.PublicationDate publicationDate) {
        return new Builder()
                   .withYear(publicationDate.getNva().getYear())
                   .withMonth(publicationDate.getNva().getMonth())
                   .withDay(publicationDate.getNva().getDay())
                   .build();
    }

    private static URI extractLanguage(Record brageRecord) {
        return Optional.ofNullable(brageRecord.getEntityDescription().getLanguage())
                   .map(BrageNvaMapper::generateLanguage)
                   .orElse(null);
    }

    private static URI generateLanguage(Language language) {
        return language.getNva();
    }

    private static String extractDescription(Record record) {
        return Optional.ofNullable(record.getEntityDescription().getDescriptions())
                   .map(BrageNvaMapper::generateDescription)
                   .orElse(null);
    }

    private static String generateDescription(List<String> descriptions) {
        return descriptions.get(0);
    }

    private static String generateAbstract(List<String> descriptions) {
        return descriptions.get(0);
    }

    private static String extractAbstract(Record record) {
        return Optional.ofNullable(record.getEntityDescription().getAbstracts())
                   .map(BrageNvaMapper::generateAbstract)
                   .orElse(null);
    }
}
