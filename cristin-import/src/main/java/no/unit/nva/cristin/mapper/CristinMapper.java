package no.unit.nva.cristin.mapper;

import static no.unit.nva.cristin.lambda.constants.HardcodedValues.HARDCODED_ILLUSTRATED;
import static no.unit.nva.cristin.lambda.constants.HardcodedValues.HARDCODED_LEVEL;
import static no.unit.nva.cristin.lambda.constants.HardcodedValues.HARDCODED_NPI_SUBJECT;
import static no.unit.nva.cristin.lambda.constants.HardcodedValues.HARDCODED_NVA_CUSTOMER;
import static no.unit.nva.cristin.lambda.constants.HardcodedValues.HARDCODED_PAGE;
import static no.unit.nva.cristin.lambda.constants.HardcodedValues.HARDCODED_PEER_REVIEWED;
import static no.unit.nva.cristin.lambda.constants.HardcodedValues.HARDCODED_SAMPLE_DOI;
import static no.unit.nva.cristin.lambda.constants.HardcodedValues.HARDCODED_TEXTBOOK_CONTENT;
import static no.unit.nva.cristin.lambda.constants.HardcodedValues.HARDCODED_URI;
import static no.unit.nva.cristin.lambda.constants.MappingConstants.IGNORED_AND_POSSIBLY_EMPTY_PUBLICATION_FIELDS;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFields;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import java.net.MalformedURLException;
import java.net.URI;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
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
import no.unit.nva.model.instancetypes.book.BookMonograph;
import no.unit.nva.model.pages.MonographPages;
import no.unit.nva.model.pages.Pages;
import no.unit.nva.model.pages.Range;
import nva.commons.core.SingletonCollector;
import nva.commons.core.attempt.Try;
import nva.commons.core.language.LanguageMapper;


public class CristinMapper {

    public static final String ERROR_PARSING_SECONDARY_CATEGORY = "Error parsing secondary category";
    public static final String ERROR_PARSING_MAIN_CATEGORY = "Error parsing main category";
    public static final String ERROR_PARSING_MAIN_OR_SECONDARY_CATEGORIES = "Error parsing main or secondary "
                                                                            + "categories";
    public static final String ERROR_MISSING_NUMBER_OF_PAGES_IN_BOOK_REPORT = "Number of pages in a Cristin "
            + "entry of type Book report can not be null";

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
            assertThat(publication,
                    doesNotHaveEmptyValuesIgnoringFields(IGNORED_AND_POSSIBLY_EMPTY_PUBLICATION_FIELDS));
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
        return ZoneOffset.UTC.getRules().getOffset(Instant.now());
    }

    private EntityDescription generateEntityDescription() {
        return new EntityDescription.Builder()
                   .withLanguage(extractLanguage())
                   .withMainTitle(extractMainTitle())
                   .withDate(extractPublicationDate())
                   .withReference(buildReference())
                   .withContributors(extractContributors())
                   .withNpiSubjectHeading(HARDCODED_NPI_SUBJECT)
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
            List<String> isbnList = new ArrayList<>();
            isbnList.add(extractIsbn());
            return new Book.Builder()
                       .withIsbnList(isbnList)
                       .withPublisher(extractPublisherName())
                       .withUrl(HARDCODED_URI.toURL())
                       .withLevel(HARDCODED_LEVEL)
                       .withOpenAccess(false)
                       .build();
        }
        return null;
    }

    private PublicationInstance<? extends Pages> buildPublicationInstance() {
        if (isBook() && isAnthology()) {
            return createBookAnthology();
        } else if (isBook() && isMonograph()) {
            return createBookMonograph();
        } else if (cristinObject.getMainCategory().isUnknownCategory()) {
            throw new UnsupportedOperationException(ERROR_PARSING_MAIN_CATEGORY);
        } else if (cristinObject.getSecondaryCategory().isUnknownCategory()) {
            throw new UnsupportedOperationException(ERROR_PARSING_SECONDARY_CATEGORY);
        }
        throw new RuntimeException(ERROR_PARSING_MAIN_OR_SECONDARY_CATEGORIES);
    }

    private MonographPages createMonographPages() {
        Range introductionRange = new Range.Builder().withBegin(HARDCODED_PAGE).withEnd(HARDCODED_PAGE).build();
        return new MonographPages.Builder()
                .withPages(extractNumberOfPages())
                .withIllustrated(HARDCODED_ILLUSTRATED)
                .withIntroduction(introductionRange)
                .build();
    }

    private BookAnthology createBookAnthology() {
        return new BookAnthology.Builder()
                   .withPeerReviewed(HARDCODED_PEER_REVIEWED)
                   .withPages(createMonographPages())
                   .withTextbookContent(HARDCODED_TEXTBOOK_CONTENT)
                   .build();
    }

    private BookMonograph createBookMonograph() {
        return new BookMonograph.Builder()
                   .withPeerReviewed(HARDCODED_PEER_REVIEWED)
                   .withPages(createMonographPages())
                   .withTextbookContent(HARDCODED_TEXTBOOK_CONTENT)
                   .build();
    }

    private boolean isAnthology() {
        return CristinSecondaryCategory.ANTHOLOGY.equals(cristinObject.getSecondaryCategory());
    }

    private boolean isMonograph() {
        return CristinSecondaryCategory.MONOGRAPH.equals(cristinObject.getSecondaryCategory());
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

    private CristinBookReport extractCristinBookReport() {
        return Optional.ofNullable(cristinObject)
                   .map(CristinObject::getBookReports)
                .stream()
                .flatMap(Collection::stream)
                .collect(SingletonCollector.collectOrElse(null));
    }

    private String extractNumberOfPages() {
        String numberOfPages = extractCristinBookReport().getNumberOfPages();
        if (numberOfPages == null) {
            throw new MissingFieldsException(ERROR_MISSING_NUMBER_OF_PAGES_IN_BOOK_REPORT);
        }
        return numberOfPages;
    }

    private String extractPublisherName() {
        return extractCristinBookReport().getPublisherName();
    }

    private String extractIsbn() {
        return extractCristinBookReport().getIsbn();
    }

    private URI extractLanguage() {
        return extractCristinTitles()
                   .filter(CristinTitle::isMainTitle)
                   .findFirst()
                   .map(CristinTitle::getLanguagecode)
                   .map(LanguageMapper::toUri)
                   .orElse(LanguageMapper.LEXVO_URI_UNDEFINED);
    }

    private AdditionalIdentifier extractIdentifier() {
        return new AdditionalIdentifier(CristinObject.IDENTIFIER_ORIGIN, cristinObject.getId().toString());
    }
}
