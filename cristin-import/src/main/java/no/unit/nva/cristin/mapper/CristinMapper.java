package no.unit.nva.cristin.mapper;

import static no.unit.nva.cristin.lambda.constants.HardcodedValues.HARDCODED_BOOK_ILLUSTRATED;
import static no.unit.nva.cristin.lambda.constants.HardcodedValues.HARDCODED_BOOK_LEVEL;
import static no.unit.nva.cristin.lambda.constants.HardcodedValues.HARDCODED_BOOK_PAGE;
import static no.unit.nva.cristin.lambda.constants.HardcodedValues.HARDCODED_BOOK_PEER_REVIEWED;
import static no.unit.nva.cristin.lambda.constants.HardcodedValues.HARDCODED_BOOK_TEXTBOOK_CONTENT;
import static no.unit.nva.cristin.lambda.constants.HardcodedValues.HARDCODED_BOOK_URI;
import static no.unit.nva.cristin.lambda.constants.HardcodedValues.HARDCODED_CHAPTER_ARTICLE_URI;
import static no.unit.nva.cristin.lambda.constants.HardcodedValues.HARDCODED_DEGREE_URL;
import static no.unit.nva.cristin.lambda.constants.HardcodedValues.HARDCODED_JOURNAL_LEVEL;
import static no.unit.nva.cristin.lambda.constants.HardcodedValues.HARDCODED_JOURNAL_NUMBER;
import static no.unit.nva.cristin.lambda.constants.HardcodedValues.HARDCODED_JOURNAL_PAGE;
import static no.unit.nva.cristin.lambda.constants.HardcodedValues.HARDCODED_JOURNAL_PEER_REVIEWED;
import static no.unit.nva.cristin.lambda.constants.HardcodedValues.HARDCODED_JOURNAL_URI;
import static no.unit.nva.cristin.lambda.constants.HardcodedValues.HARDCODED_NVA_CUSTOMER;
import static no.unit.nva.cristin.lambda.constants.HardcodedValues.HARDCODED_OPEN_JOURNAL_ACCESS;
import static no.unit.nva.cristin.lambda.constants.HardcodedValues.HARDCODED_REPORT_LEVEL;
import static no.unit.nva.cristin.lambda.constants.HardcodedValues.HARDCODED_REPORT_URL;
import static no.unit.nva.cristin.lambda.constants.HardcodedValues.HARDCODED_SAMPLE_DOI;
import static no.unit.nva.cristin.lambda.constants.MappingConstants.IGNORED_AND_POSSIBLY_EMPTY_PUBLICATION_FIELDS;
import static no.unit.nva.cristin.mapper.CristinMainCategory.isBook;
import static no.unit.nva.cristin.mapper.CristinMainCategory.isChapter;
import static no.unit.nva.cristin.mapper.CristinMainCategory.isJournal;
import static no.unit.nva.cristin.mapper.CristinMainCategory.isReport;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.isChapterArticle;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.isDegreePhd;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.isJournalArticle;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.isJournalReview;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.isMonograph;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.isResearchReport;
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
import no.unit.nva.model.ResearchProject;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.contexttypes.Chapter;
import no.unit.nva.model.contexttypes.Degree;
import no.unit.nva.model.contexttypes.Journal;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.contexttypes.Report;
import no.unit.nva.model.exceptions.InvalidIsbnException;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.book.BookAnthology;
import no.unit.nva.model.instancetypes.book.BookMonograph;
import no.unit.nva.model.instancetypes.chapter.ChapterArticle;
import no.unit.nva.model.instancetypes.degree.DegreePhd;
import no.unit.nva.model.instancetypes.journal.JournalArticle;
import no.unit.nva.model.instancetypes.journal.JournalReview;
import no.unit.nva.model.instancetypes.report.ReportResearch;
import no.unit.nva.model.pages.MonographPages;
import no.unit.nva.model.pages.Pages;
import no.unit.nva.model.pages.Range;
import nva.commons.core.attempt.Try;
import nva.commons.core.language.LanguageMapper;

@SuppressWarnings({"PMD.GodClass", "PMD.CouplingBetweenObjects"})
public class CristinMapper {

    public static final String ERROR_PARSING_SECONDARY_CATEGORY = "Error parsing secondary category";
    public static final String ERROR_PARSING_MAIN_CATEGORY = "Error parsing main category";
    public static final String ERROR_PARSING_MAIN_OR_SECONDARY_CATEGORIES = "Error parsing main or secondary "
                                                                            + "categories";

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
                                      .withProjects(extractProjects())
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

    private List<ResearchProject> extractProjects() {
        if (cristinObject.getPresentationalWork() == null) {
            return null;
        }
        return cristinObject.getPresentationalWork()
                .stream()
                .filter(CristinPresentationalWork::isProject)
                .map(CristinPresentationalWork::toNvaResearchProject)
                .collect(Collectors.toList());
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
                   .withNpiSubjectHeading(extractNpiSubjectHeading())
                   .withAbstract(extractAbstract())
                   .withTags(extractTags())
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
                   .withDoi(extractDoi())
                   .build();
    }

    private PublicationContext buildPublicationContext()
        throws InvalidIsbnException, MalformedURLException, InvalidIssnException {
        if (isBook(cristinObject)) {
            return buildBookForPublicationContext();
        }
        if (isJournal(cristinObject)) {
            return buildJournalForPublicationContext();
        }
        if (isReport(cristinObject)) {
            return buildPublicationContextWhenMainCategoryIsReport();
        }
        if (isChapter(cristinObject)) {
            return buildChapterForPublicationContext();
        }
        return null;
    }

    private Book buildBookForPublicationContext() throws MalformedURLException, InvalidIsbnException {
        List<String> isbnList = new ArrayList<>();
        isbnList.add(extractIsbn());
        return new Book.Builder()
                .withIsbnList(isbnList)
                .withPublisher(extractPublisherName())
                .withUrl(HARDCODED_BOOK_URI.toURL())
                .withLevel(HARDCODED_BOOK_LEVEL)
                .withOpenAccess(false)
                .build();
    }

    private Journal buildJournalForPublicationContext() throws MalformedURLException, InvalidIssnException {
        return new Journal.Builder()
                .withLevel(HARDCODED_JOURNAL_LEVEL)
                .withPeerReviewed(HARDCODED_JOURNAL_PEER_REVIEWED)
                .withOpenAccess(HARDCODED_OPEN_JOURNAL_ACCESS)
                .withUrl(HARDCODED_JOURNAL_URI.toURL())
                .withOnlineIssn(extractIssnOnline())
                .withPrintIssn(extractIssn())
                .withTitle(extractPublisherTitle())
                .build();
    }

    private PublicationContext buildPublicationContextWhenMainCategoryIsReport()
            throws MalformedURLException, InvalidIsbnException, InvalidIssnException {
        if (isDegreePhd(cristinObject)) {
            return new Degree.Builder()
                    .withUrl(HARDCODED_DEGREE_URL.toURL())
                    .withPublisher(extractPublisherName())
                    .build();
        }
        return new Report.Builder()
                .withLevel(HARDCODED_REPORT_LEVEL)
                .withPublisher(extractPublisherName())
                .withUrl(HARDCODED_REPORT_URL.toURL())
                .build();
    }

    private Chapter buildChapterForPublicationContext() {
        return new Chapter.Builder()
                .withLinkedContext(HARDCODED_CHAPTER_ARTICLE_URI)
                .build();
    }


    private PublicationInstance<? extends Pages> buildPublicationInstance() {
        if (isBook(cristinObject) && CristinSecondaryCategory.isAnthology(cristinObject)) {
            return createBookAnthology();
        } else if (isBook(cristinObject) && isMonograph(cristinObject)) {
            return createBookMonograph();
        } else if (isJournal(cristinObject) && isJournalArticle(cristinObject)) {
            return createJournalArticle();
        } else if (isJournal(cristinObject) && isJournalReview(cristinObject)) {
            return createJournalReview();
        } else if (isReport(cristinObject) && isResearchReport(cristinObject)) {
            return createReportResearch();
        } else if (isReport(cristinObject) && isDegreePhd(cristinObject)) {
            return createDegreePhd();
        } else if (isChapter(cristinObject) && isChapterArticle(cristinObject)) {
            return createChapterArticle();
        } else if (cristinObject.getMainCategory().isUnknownCategory()) {
            throw new UnsupportedOperationException(ERROR_PARSING_MAIN_CATEGORY);
        } else if (cristinObject.getSecondaryCategory().isUnknownCategory()) {
            throw new UnsupportedOperationException(ERROR_PARSING_SECONDARY_CATEGORY);
        }
        throw new RuntimeException(ERROR_PARSING_MAIN_OR_SECONDARY_CATEGORIES);
    }

    private MonographPages createMonographPages() {
        Range introductionRange = new Range.Builder()
                                      .withBegin(HARDCODED_BOOK_PAGE)
                                      .withEnd(HARDCODED_BOOK_PAGE)
                                      .build();
        return new MonographPages.Builder()
                   .withPages(extractNumberOfPages())
                   .withIllustrated(HARDCODED_BOOK_ILLUSTRATED)
                   .withIntroduction(introductionRange)
                   .build();
    }

    private BookAnthology createBookAnthology() {
        return new BookAnthology.Builder()
                   .withPeerReviewed(HARDCODED_BOOK_PEER_REVIEWED)
                   .withPages(createMonographPages())
                   .withTextbookContent(HARDCODED_BOOK_TEXTBOOK_CONTENT)
                   .build();
    }

    private BookMonograph createBookMonograph() {
        return new BookMonograph.Builder()
                   .withPeerReviewed(HARDCODED_BOOK_PEER_REVIEWED)
                   .withPages(createMonographPages())
                   .withTextbookContent(HARDCODED_BOOK_TEXTBOOK_CONTENT)
                   .build();
    }

    private PublicationInstance<? extends Pages> createJournalArticle() {
        Range numberOfPages = new Range(extractPagesBegin(), extractPagesEnd());
        return new JournalArticle.Builder()
                   .withArticleNumber(HARDCODED_JOURNAL_NUMBER)
                   .withIssue(HARDCODED_JOURNAL_PAGE)
                   .withPages(numberOfPages)
                   .withPeerReviewed(HARDCODED_JOURNAL_PEER_REVIEWED)
                   .withVolume(extractVolume())
                   .build();
    }

    private PublicationInstance<? extends Pages> createJournalReview() {
        Range numberOfPages = new Range(extractPagesBegin(), extractPagesEnd());
        return new JournalReview.Builder()
                .withArticleNumber(HARDCODED_JOURNAL_NUMBER)
                .withIssue(HARDCODED_JOURNAL_PAGE)
                .withPages(numberOfPages)
                .withVolume(extractVolume())
                .build();
    }

    private PublicationInstance<? extends Pages> createReportResearch() {
        return new ReportResearch.Builder().build();
    }

    private PublicationInstance<? extends Pages> createDegreePhd() {
        return new DegreePhd.Builder().build();
    }

    private PublicationInstance<? extends Pages> createChapterArticle() {
        return new ChapterArticle.Builder().build();
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

    private CristinBookOrReportMetadata extractCristinBookReport() {
        return Optional.ofNullable(cristinObject)
            .map(CristinObject::getBookOrReportMetadata)
            .orElse(null);
    }

    private String extractNumberOfPages() {
        return extractCristinBookReport().getNumberOfPages();
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

    private String extractNpiSubjectHeading() {
        if (!isBook(cristinObject)) {
            return null;
        }
        if (extractSubjectField() == null) {
            throw new MissingFieldsException(CristinBookOrReportMetadata.SUBJECT_FIELD_IS_A_REQUIRED_FIELD);
        }
        Integer code = extractSubjectField().getSubjectFieldCode();
        if (code == null) {
            throw new MissingFieldsException(CristinSubjectField.MISSING_SUBJECT_FIELD_CODE);
        }
        return String.valueOf(code);
    }

    private CristinSubjectField extractSubjectField() {
        return extractCristinBookReport().getSubjectField();
    }

    private CristinJournalPublication extractCristinJournalPublication() {
        return Optional.ofNullable(cristinObject)
                .map(CristinObject::getJournalPublication)
                .orElse(null);
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

    private String extractPagesBegin() {
        return extractCristinJournalPublication().getPagesBegin();
    }

    private String extractPagesEnd() {
        return extractCristinJournalPublication().getPagesEnd();
    }

    private String extractVolume() {
        return extractCristinJournalPublication().getVolume();
    }

    private URI extractDoi() {
        if (isJournal(cristinObject) && extractCristinJournalPublication().getDoi() != null) {
            return URI.create(extractCristinJournalPublication().getDoi());
        }
        return null;
    }

    private List<CristinTags> extractCristinTags() {
        return Optional.ofNullable(cristinObject)
                .map(CristinObject::getTags)
                .orElse(null);
    }

    private String extractAbstract() {
        return extractCristinTitles()
                .filter(CristinTitle::isMainTitle)
                .findFirst()
                .map(CristinTitle::getAbstractText)
                .orElse(null);
    }

    private List<String> extractTags() {
        if (extractCristinTags() == null) {
            return null;
        }
        List<String> listOfTags = new ArrayList<>();
        for (CristinTags cristinTags : extractCristinTags()) {
            if (cristinTags.getBokmal() != null) {
                listOfTags.add(cristinTags.getBokmal());
            }
            if (cristinTags.getEnglish() != null) {
                listOfTags.add(cristinTags.getEnglish());
            }
            if (cristinTags.getNynorsk() != null) {
                listOfTags.add(cristinTags.getNynorsk());
            }
        }
        return listOfTags;
    }

}
