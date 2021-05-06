package no.unit.nva.cristin.mapper;

import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import no.unit.nva.cristin.lambda.constants.MappingConstants;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.Reference;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.exceptions.InvalidIsbnException;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.book.BookAnthology;
import no.unit.nva.model.pages.Pages;

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
                   .withPublisher(extractOrganization())
                   .withOwner(cristinObject.getPublicationOwner())
                   .withStatus(PublicationStatus.DRAFT)
                   .build();
    }

    private Organization extractOrganization() {
        URI customerUri = URI.create("https://api.dev.nva.aws.unit.no/customer/f54c8aa9-073a-46a1-8f7c-dde66c853934");
        return new Organization.Builder().withId(customerUri).build();
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
                   .withReference(buildReference())
                   .build();
    }

    private Reference buildReference() {
        PublicationInstance<? extends Pages> publicationInstance = buildPublicationInstance();
        PublicationContext publicationContext = attempt(this::buildPublicationContext).orElseThrow();
        return new Reference.Builder()
                   .withPublicationInstance(publicationInstance)
                   .withPublishingContext(publicationContext)
                   .build();
    }

    private PublicationContext buildPublicationContext() throws InvalidIsbnException {
        if (isBook()) {
            return new Book.Builder()
                       .withIsbnList(Collections.emptyList())
                       .build();
        }
        return null;
    }

    private PublicationInstance<? extends Pages> buildPublicationInstance() {
        if (isBook() && isAnthology()) {
            return new BookAnthology.Builder().build();
        } else {
            return null;
        }
    }

    private boolean isAnthology() {
        return MappingConstants.SECONDARY_CATEGORY_ANTHOLOGY.equals(cristinObject.getSecondaryCategory());
    }

    private boolean isBook() {
        return MappingConstants.MAIN_CATEGORY_BOOK.equalsIgnoreCase(cristinObject.getMainCategory());
    }

    private PublicationDate extractPublicationDate() {
        return new PublicationDate.Builder().withYear(cristinObject.getPublicationYear()).build();
    }

    private String extractMainTitle() {
        return extractCristinTitles()
                   .filter(CristinTitle::isMainTitle)
                   .findFirst()
                   .map(CristinTitle::getTitle)
                   .orElseThrow();
    }

    private Stream<CristinTitle> extractCristinTitles() {
        return Optional.ofNullable(cristinObject)
                   .map(CristinObject::getCristinTitles)
                   .stream()
                   .flatMap(Collection::stream);
    }

    private URI extractLanguage() {
        return extractCristinTitles()
                   .filter(CristinTitle::isMainTitle)
                   .findFirst()
                   .map(CristinTitle::getLanguagecode)
                   .map(LanguageCodeMapper::parseLanguage)
                   .orElse(LanguageCodeMapper.ENGLISH_LANG_URI);
    }

    private AdditionalIdentifier extractIdentifier() {
        return new AdditionalIdentifier(CristinObject.IDENTIFIER_ORIGIN, cristinObject.getId());
    }
}
