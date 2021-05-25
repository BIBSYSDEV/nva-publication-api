package no.unit.nva.cristin.mapper;

import static no.unit.nva.cristin.lambda.constants.MappingConstants.HARDCODED_SAMPLE_DOI;
import static no.unit.nva.cristin.lambda.constants.MappingConstants.IGNORED_PUBLICATION_FIELDS;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFields;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import java.net.MalformedURLException;
import java.net.URI;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Level;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.Publication.Builder;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.Reference;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.exceptions.InvalidIsbnException;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.book.BookAnthology;
import no.unit.nva.model.pages.MonographPages;
import no.unit.nva.model.pages.Pages;
import no.unit.nva.model.pages.Range;
import nva.commons.core.attempt.Try;

public class CristinMapper {

    public static final URI HARDCODED_NVA_CUSTOMER =
        URI.create("https://api.dev.nva.aws.unit.no/customer/f54c8aa9-073a-46a1-8f7c-dde66c853934");
    public static final String ERROR_PARSING_SECONDARY_CATEGORY = "Error parsing secondary category";
    public static final String ERROR_PARSING_MAIN_CATEGORY = "Error parsing main category";
    public static final String ERROR_PARSING_MAIN_OR_SECONDARY_CATEGORIES = "Error parsing main or secondary "
                                                                            + "categories";
    public static final String HARDCODED_BOOK_PUBLISHER = "SomePublisher";
    public static final Level SAMPLE_HARDCODED_LEVEL = Level.LEVEL_0;
    public static final String HARDCODED_PAGE = "1";
    public static final String HARDCODED_SERIES_NUMBER = "1";
    public static final String HARDCODED_SERIES_TITLE = "hardcoded series title";
    public static final URI HARDCODED_URI = URI.create("https://www.example.com/");

    private final CristinObject cristinObject;

    public CristinMapper(CristinObject cristinObject) {
        this.cristinObject = cristinObject;
    }

    public Publication generatePublication() {
        Publication publication = new Builder()
                                      .withAdditionalIdentifiers(Set.of(extractIdentifier()))
                                      .withEntityDescription(generateEntityDescription())
                                      .withCreatedDate(extractEntryCreationDate())
                                      .withPublisher(extractOrganization())
                                      .withOwner(cristinObject.getPublicationOwner())
                                      .withStatus(PublicationStatus.DRAFT)
                                      .withLink(HARDCODED_SAMPLE_DOI)
                                      .build();
        assertPublicationDoesNotHaveEmptyFields(publication);
        return publication;
    }

    private void assertPublicationDoesNotHaveEmptyFields(Publication publication) {
        try {
            assertThat(publication, doesNotHaveEmptyValuesIgnoringFields(IGNORED_PUBLICATION_FIELDS));
        } catch (Error error) {
            String message = error.getMessage();
            throw new MissingFieldsException(message);
        }
    }

    private Organization extractOrganization() {
        return new Organization.Builder().withId(HARDCODED_NVA_CUSTOMER).build();
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
                   .withContributors(extractContributors())
                   .build();
    }

    private List<Contributor> extractContributors() {
        return cristinObject.getContributors()
                   .stream()
                   .map(attempt(CristinContributor::toNvaContributor))
                   .map(Try::orElseThrow)
                   .collect(Collectors.toList());
    }

    private Reference buildReference() {
        PublicationInstance<? extends Pages> publicationInstance = buildPublicationInstance();
        PublicationContext publicationContext = attempt(this::buildPublicationContext).orElseThrow();
        return new Reference.Builder()
                   .withPublicationInstance(publicationInstance)
                   .withPublishingContext(publicationContext)
                   .build();
    }

    private PublicationContext buildPublicationContext() throws InvalidIsbnException, MalformedURLException {
        if (isBook()) {
            return new Book.Builder()
                       .withIsbnList(Collections.emptyList())
                       .withPublisher(HARDCODED_BOOK_PUBLISHER)
                       .withSeriesNumber(HARDCODED_SERIES_NUMBER)
                       .withSeriesTitle(HARDCODED_SERIES_TITLE)
                       .withUrl(HARDCODED_URI.toURL())
                       .withLevel(SAMPLE_HARDCODED_LEVEL)
                       .withOpenAccess(false)
                       .build();
        }
        return null;
    }

    private PublicationInstance<? extends Pages> buildPublicationInstance() {
        if (isBook() && isAnthology()) {
            return createBookAnthology();
        } else if (cristinObject.getMainCategory().isUnknownCategory()) {
            throw new UnsupportedOperationException(ERROR_PARSING_MAIN_CATEGORY);
        } else if (cristinObject.getSecondaryCategory().isUnknownCategory()) {
            throw new UnsupportedOperationException(ERROR_PARSING_SECONDARY_CATEGORY);
        }
        throw new RuntimeException(ERROR_PARSING_MAIN_OR_SECONDARY_CATEGORIES);
    }

    private BookAnthology createBookAnthology() {
        Range introductionRange = new Range.Builder().withBegin(HARDCODED_PAGE).withEnd(HARDCODED_PAGE).build();
        MonographPages pages = new MonographPages.Builder()
                                   .withPages(HARDCODED_PAGE)
                                   .withIllustrated(false)
                                   .withIntroduction(introductionRange)
                                   .build();
        return new BookAnthology.Builder()
                   .withPeerReviewed(false)
                   .withPages(pages)
                   .withTextbookContent(false)
                   .build();
    }

    private boolean isAnthology() {
        return CristinSecondaryCategory.ANTHOLOGY.equals(cristinObject.getSecondaryCategory());
    }

    private boolean isBook() {
        return CristinMainCategory.BOOK.equals(cristinObject.getMainCategory());
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
        return new AdditionalIdentifier(CristinObject.IDENTIFIER_ORIGIN, cristinObject.getId().toString());
    }
}
