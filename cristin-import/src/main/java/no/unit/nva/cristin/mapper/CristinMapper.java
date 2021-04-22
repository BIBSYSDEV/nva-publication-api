package no.unit.nva.cristin.mapper;

import java.net.URI;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationDate;

public class CristinMapper {

    private final CristinObject cristinObject;

    public CristinMapper(CristinObject cristinObject) {
        this.cristinObject = cristinObject;
    }

    public Publication generatePublication() {
        return new Publication.Builder()
                   .withAdditionalIdentifiers(Set.of(extractIdentifier()))
                   .withEntityDescription(generateEntityDescription())
                   .withCreatedDate(extractEntryCreationDate())
                   .build();
    }

    private Instant extractEntryCreationDate() {
        return Optional.ofNullable(cristinObject.getEntryCreationDate())
                   .map(ld -> ld.atStartOfDay().toInstant(zoneOffset()))
                   .orElse(null);
    }

    private ZoneOffset zoneOffset() {
        return ZoneOffset.systemDefault().getRules().getOffset(Instant.now());
    }

    private EntityDescription generateEntityDescription() {
        return new EntityDescription.Builder()
                   .withLanguage(extractLanguage())
                   .withMainTitle(extractMainTitle())
                   .withDate(extractPublicationDate())
                   .build();
    }

    private PublicationDate extractPublicationDate() {
        return new PublicationDate.Builder().withYear(cristinObject.getPublicationYear()).build();
    }

    private String extractMainTitle() {
        return longestTitle()
                   .map(CristinTitle::getTitle)
                   .orElseThrow();
    }

    private Optional<CristinTitle> longestTitle() {
        return extractCristinTitles()
                   .max(CristinTitle::compareTo);
    }

    private Stream<CristinTitle> extractCristinTitles() {
        return Optional.ofNullable(cristinObject)
                   .map(CristinObject::getCristinTitles)
                   .stream()
                   .flatMap(Collection::stream);
    }

    private URI extractLanguage() {
        return longestTitle()
                   .map(CristinTitle::getLanguagecode)
                   .map(LanguageCodeMapper::parseLanguage)
                   .orElse(LanguageCodeMapper.ENGLISH_LANG_URI);
    }

    private AdditionalIdentifier extractIdentifier() {
        return new AdditionalIdentifier(CristinObject.IDENTIFIER_ORIGIN, cristinObject.getId());
    }
}
