package no.sikt.nva.scopus.conversion;

import static nva.commons.core.attempt.Try.attempt;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import no.scopus.generated.BibrecordTp;
import no.scopus.generated.CitationInfoTp;
import no.scopus.generated.CitationtypeAtt;
import no.scopus.generated.DocTp;
import no.scopus.generated.HeadTp;
import no.scopus.generated.IsbnTp;
import no.scopus.generated.IssnTp;
import no.scopus.generated.ItemTp;
import no.scopus.generated.MetaTp;
import no.scopus.generated.OrigItemTp;
import no.scopus.generated.PublisherTp;
import no.scopus.generated.SourceTp;
import no.scopus.generated.SourcetypeAtt;
import no.sikt.nva.scopus.ScopusConstants;
import no.sikt.nva.scopus.exception.UnsupportedSrcTypeException;
import no.unit.nva.model.contexttypes.Anthology;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.contexttypes.BookSeries;
import no.unit.nva.model.contexttypes.Journal;
import no.unit.nva.model.contexttypes.Periodical;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.contexttypes.Publisher;
import no.unit.nva.model.contexttypes.PublishingHouse;
import no.unit.nva.model.contexttypes.Report;
import no.unit.nva.model.contexttypes.Series;
import no.unit.nva.model.contexttypes.UnconfirmedJournal;
import no.unit.nva.model.contexttypes.UnconfirmedPublisher;
import no.unit.nva.model.contexttypes.UnconfirmedSeries;
import nva.commons.core.SingletonCollector;

@SuppressWarnings({"PMD.PrematureDeclaration", "PMD.UnusedLocalVariable", "PMD.GodClass"})
public class PublicationContextCreator {

    public static final String UNSUPPORTED_SOURCE_TYPE = "Unsupported source type, in %s";
    public static final String DASH = "-";
    public static final int START_YEAR_FOR_LEVEL_INFO = 2004;
    public static final String NOT_APPLICABLE = "NA";
    private final DocTp docTp;
    private final PublicationChannelConnection publicationChannelConnection;

    public PublicationContextCreator(DocTp docTp, PublicationChannelConnection publicationChannelConnection) {
        this.docTp = docTp;
        this.publicationChannelConnection = publicationChannelConnection;
    }

    public PublicationContext getPublicationContext() {
        if (isJournal()) {
            return createJournal();
        }
        if (isChapter()) {
            return createAnthology();
        }
        if (isBook()) {
            return createBook();
        }
        if (isBookSeries()) {
            return createBookInSeries();
        }
        if (isReport()) {
            return createReport();
        }
        if (isConferenceProceeding()) {
            if (hasIsbn() && hasNoIssn()) {
                return createAnthology();
            } else {
                return createJournal();
            }
        }
        throw new UnsupportedSrcTypeException(String.format(UNSUPPORTED_SOURCE_TYPE, docTp.getMeta().getEid()));
    }

    public Periodical createJournal() {
        return createConfirmedJournal().orElseGet(this::createUnconfirmedJournal);
    }

    public Book createBook() {
        BookSeries bookSeries = null;
        String seriesNumber = null;
        PublishingHouse publishingHouse = createPublisher();
        List<String> isbnList = findIsbn();
        return attempt(() -> new Book(bookSeries, seriesNumber, publishingHouse, isbnList, null)).orElseThrow();
    }

    public Book createBookInSeries() {
        BookSeries bookSeries = createBookSeries();
        String seriesNumber = null;
        PublishingHouse publishingHouse = createPublisher();
        List<String> isbnList = findIsbn();
        return attempt(() -> new Book(bookSeries, seriesNumber, publishingHouse, isbnList, null)).orElseThrow();
    }

    public Book createReport() {
        BookSeries bookSeries = null;
        String seriesTitle = null;
        String seriesNumber = null;
        PublishingHouse publishingHouse = createPublisher();
        List<String> isbnList = Collections.emptyList();
        return attempt(
            () -> new Report(bookSeries, seriesTitle, seriesNumber, publishingHouse, isbnList)).orElseThrow();
    }

    public Anthology createAnthology() {
        // TODO: We do not have access to partOf URI for chapter yet -> set a dummy URI
        return attempt(() -> new Anthology.Builder().withId(ScopusConstants.DUMMY_URI).build()).orElseThrow();
    }

    private boolean isJournal() {
        return Optional.ofNullable(docTp)
                   .map(DocTp::getMeta)
                   .map(MetaTp::getSrctype)
                   .map(srcType -> srcType.equals(SourcetypeAtt.J.value()))
                   .orElse(false);
    }

    private boolean isChapter() {
        return Optional.ofNullable(docTp)
                   .map(DocTp::getItem)
                   .map(ItemTp::getItem)
                   .map(OrigItemTp::getBibrecord)
                   .map(BibrecordTp::getHead)
                   .map(HeadTp::getCitationInfo)
                   .map(CitationInfoTp::getCitationType)
                   .orElse(Collections.emptyList())
                   .stream()
                   .anyMatch(citationType -> CitationtypeAtt.CH.equals(citationType.getCode()));
    }

    private boolean isBook() {
        return Optional.ofNullable(docTp)
                   .map(DocTp::getMeta)
                   .map(MetaTp::getSrctype)
                   .map(srcType -> srcType.equals(SourcetypeAtt.B.value()))
                   .orElse(false);
    }

    private boolean isBookSeries() {
        return Optional.ofNullable(docTp)
                   .map(DocTp::getMeta)
                   .map(MetaTp::getSrctype)
                   .map(srcType -> srcType.equals(SourcetypeAtt.K.value()))
                   .orElse(false);
    }

    private boolean isReport() {
        return Optional.ofNullable(docTp)
                   .map(DocTp::getMeta)
                   .map(MetaTp::getSrctype)
                   .map(srcType -> srcType.equals(SourcetypeAtt.R.value()))
                   .orElse(false);
    }

    private boolean isConferenceProceeding() {
        return Optional.ofNullable(docTp)
                   .map(DocTp::getMeta)
                   .map(MetaTp::getSrctype)
                   .map(srcType -> srcType.equals(SourcetypeAtt.P.value()))
                   .orElse(false);
    }

    private boolean hasIsbn() {
        return Optional.ofNullable(docTp)
                   .map(DocTp::getItem)
                   .map(ItemTp::getItem)
                   .map(OrigItemTp::getBibrecord)
                   .map(BibrecordTp::getHead)
                   .map(HeadTp::getSource)
                   .map(SourceTp::getIsbn)
                   .stream()
                   .noneMatch(List::isEmpty);
    }

    private boolean hasNoIssn() {
        return Optional.ofNullable(docTp)
                   .map(DocTp::getItem)
                   .map(ItemTp::getItem)
                   .map(OrigItemTp::getBibrecord)
                   .map(BibrecordTp::getHead)
                   .map(HeadTp::getSource)
                   .map(SourceTp::getIssn)
                   .stream()
                   .anyMatch(List::isEmpty);
    }

    private PublishingHouse createPublisher() {
        return fetchConfirmedPublisherFromPublicationChannels().orElseGet(this::createUnconfirmedPublisher);
    }

    private BookSeries createBookSeries() {
        return fetchConfirmedSeriesFromPublicationChannels().orElseGet(this::createUnconfirmedSeries);
    }

    private Optional<BookSeries> fetchConfirmedSeriesFromPublicationChannels() {
        var sourceTitle = findSourceTitle();
        var printIssn = findPrintIssn().orElse(null);
        var electronicIssn = findElectronicIssn().orElse(null);
        var publicationYear = findPublicationYear().orElseThrow();
        var seriesId = publicationChannelConnection.fetchSeries(printIssn, electronicIssn, sourceTitle,
                                                                publicationYear);
        return seriesId.map(Series::new);
    }

    private Optional<PublishingHouse> fetchConfirmedPublisherFromPublicationChannels() {
        var publisherName = findPublisherName();
        var publicationYear = findPublicationYear().orElse(null);
        return publisherName.map(name -> publicationChannelConnection.fetchPublisher(name, publicationYear))
                   .filter(Optional::isPresent)
                   .map(Optional::get)
                   .map(Publisher::new);
    }

    private UnconfirmedPublisher createUnconfirmedPublisher() {
        return new UnconfirmedPublisher(findPublisherName().orElse(null));
    }

    private UnconfirmedSeries createUnconfirmedSeries() {
        var title = findSourceTitle();
        var issn = findPrintIssn().orElse(null);
        var onlineIssn = findElectronicIssn().orElse(null);
        return attempt(() -> new UnconfirmedSeries(title, issn, onlineIssn)).orElseThrow();
    }

    private Optional<String> findPublisherName() {
        return docTp.getItem()
                   .getItem()
                   .getBibrecord()
                   .getHead()
                   .getSource()
                   .getPublisher()
                   .stream()
                   .map(PublisherTp::getPublishername)
                   .findFirst();
    }

    private Optional<Periodical> createConfirmedJournal() {
        return thereIsLevelInformationForPublication() ? fetchPeriodicalInfoFromPublicationChannels()
                   : Optional.empty();
    }

    private Optional<Periodical> fetchPeriodicalInfoFromPublicationChannels() {
        var sourceTitle = findSourceTitle();
        var printIssn = findPrintIssn().orElse(null);
        var electronicIssn = findElectronicIssn().orElse(null);
        var publicationYear = findPublicationYear().orElseThrow();
        var journalId = publicationChannelConnection.fetchJournal(printIssn, electronicIssn, sourceTitle,
                                                                  publicationYear);
        return journalId.map(Journal::new);
    }

    private boolean thereIsLevelInformationForPublication() {
        return findPublicationYear().map(year -> year >= START_YEAR_FOR_LEVEL_INFO).orElse(false);
    }

    private UnconfirmedJournal createUnconfirmedJournal() {
        var sourceTitle = findSourceTitle();
        var printIssn = findPrintIssn().orElse(null);
        var electronicIssn = findElectronicIssn().orElse(null);
        return attempt(() -> new UnconfirmedJournal(sourceTitle, printIssn, electronicIssn)).orElseThrow();
    }

    private Optional<Integer> findPublicationYear() {
        return Optional.ofNullable(docTp).map(DocTp::getMeta).map(MetaTp::getPubYear).map(Integer::parseInt);
    }

    private String findSourceTitle() {
        StringBuilder sourceTitle = new StringBuilder();
        getSource().getSourcetitle().getContent().forEach(sourceTitle::append);
        return sourceTitle.toString();
    }

    private Optional<String> findElectronicIssn() {
        return findIssn(getSource().getIssn(), ScopusConstants.ISSN_TYPE_ELECTRONIC);
    }

    private Optional<String> findPrintIssn() {
        return findIssn(getSource().getIssn(), ScopusConstants.ISSN_TYPE_PRINT);
    }

    private SourceTp getSource() {
        return docTp.getItem().getItem().getBibrecord().getHead().getSource();
    }

    private Optional<String> findIssn(List<IssnTp> issnTpList, String issnType) {
        return Optional.ofNullable(issnTpList.stream()
                                       .filter(issn -> issnType.equals(issn.getType()))
                                       .map(IssnTp::getContent)
                                       .filter(PublicationContextCreator::isApplicable)
                                       .map(this::addDashToIssn)
                                       .collect(SingletonCollector.collectOrElse(null)));
    }

    private static boolean isApplicable(String issn) {
        return !NOT_APPLICABLE.equals(issn);
    }

    private String addDashToIssn(String issn) {
        return issn.contains(DASH) ? issn : issn.substring(0, 4) + DASH + issn.substring(4);
    }

    private List<String> findIsbn() {
        return findIsbn(getSource().getIsbn());
    }

    private List<String> findIsbn(List<IsbnTp> isbnTpList) {
        return isbnTpList.stream().map(IsbnTp::getContent).collect(Collectors.toList());
    }
}
