package no.sikt.nva.scopus.conversion;

import static java.util.Collections.emptyList;
import jakarta.xml.bind.JAXBElement;
import java.util.Optional;
import java.util.stream.Stream;
import no.scopus.generated.CitationTypeTp;
import no.scopus.generated.CitationtypeAtt;
import no.scopus.generated.DocTp;
import no.scopus.generated.PagerangeTp;
import no.scopus.generated.SourceTp;
import no.scopus.generated.VolissTp;
import no.scopus.generated.VolisspagTp;
import no.sikt.nva.scopus.ScopusConstants;
import no.sikt.nva.scopus.exception.UnsupportedCitationTypeException;
import no.unit.nva.model.contexttypes.BookSeries;
import no.unit.nva.model.contexttypes.Chapter;
import no.unit.nva.model.contexttypes.Journal;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.contexttypes.UnconfirmedJournal;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.book.AcademicMonograph;
import no.unit.nva.model.instancetypes.chapter.AcademicChapter;
import no.unit.nva.model.instancetypes.journal.AcademicArticle;
import no.unit.nva.model.instancetypes.journal.AcademicLiteratureReview;
import no.unit.nva.model.instancetypes.journal.JournalCorrigendum;
import no.unit.nva.model.instancetypes.journal.JournalLeader;
import no.unit.nva.model.instancetypes.journal.JournalLetter;
import no.unit.nva.model.pages.Pages;
import no.unit.nva.model.pages.Range;

public class PublicationInstanceCreator {

    public static final String UNSUPPORTED_CITATION_TYPE_MESSAGE = "Unsupported citation type, cannot convert eid %s";

    private final DocTp docTp;
    private final PublicationContext publicationContext;

    public PublicationInstanceCreator(DocTp docTp, PublicationContext publicationContext) {
        this.docTp = docTp;
        this.publicationContext = publicationContext;
    }

    public PublicationInstance<? extends Pages> getPublicationInstance() {
        return getCitationTypeCode()
                   .flatMap(this::convertCitationTypeCodeToPublicationInstance)
                   .orElseThrow(this::getUnsupportedCitationTypeException);
    }

    private UnsupportedCitationTypeException getUnsupportedCitationTypeException() {
        return new UnsupportedCitationTypeException(
            String.format(UNSUPPORTED_CITATION_TYPE_MESSAGE, docTp.getMeta().getEid()));
    }

    /*
    See enum explanation in "SCOPUS CUSTOM DATA DOCUMENTATION", copy can be found at
    https://isikt.sharepoint.com/:b:/s/Dovre/EQGVGp2Xn-RDvDi8zg3XFlQB6vo95nGLbINztJcXjStG5w?e=O9wQwB
     */
    private Optional<PublicationInstance<? extends Pages>> convertCitationTypeCodeToPublicationInstance(
        CitationtypeAtt citationtypeAtt) {
        switch (citationtypeAtt) {
            case AR:
                return Optional.of(generateJournalArticle());
            case BK:
                return Optional.of(new AcademicMonograph(null));
            case CH:
                return Optional.of(generateChapterArticle());
            case CP:
                if (isJournal()) {
                    return Optional.of(generateJournalArticle());
                } else if (isChapter()) {
                    return Optional.of(generateChapterArticle());
                }
            case ED:
                return Optional.of(new JournalLeader(extractVolume().orElse(null),
                                                     extractIssue().orElse(null),
                                                     extractArticleNumber().orElse(null),
                                                     extractPages().orElse(null)));
            case ER:
                return Optional.of(new JournalCorrigendum(extractVolume().orElse(null),
                                                          extractIssue().orElse(null),
                                                          extractArticleNumber().orElse(null),
                                                          extractPages().orElse(null),
                                                          ScopusConstants.DUMMY_URI));
            case LE:
            case NO:
                return Optional.of(new JournalLetter(extractVolume().orElse(null),
                                                     extractIssue().orElse(null),
                                                     extractArticleNumber().orElse(null),
                                                     extractPages().orElse(null)));
            case RE:
            case SH:
                return Optional.of(new AcademicLiteratureReview(extractPages().orElse(null),
                                                                extractVolume().orElse(null),
                                                                extractIssue().orElse(null),
                                                                extractArticleNumber().orElse(null)));
            default:
                return Optional.empty();
        }
    }

    private AcademicArticle generateJournalArticle() {
        return new AcademicArticle(extractPages().orElse(null), extractVolume().orElse(null),
                                   extractIssue().orElse(null),
                                   extractArticleNumber().orElse(null));
    }

    private AcademicChapter generateChapterArticle() {
        return new AcademicChapter(extractPages().orElse(null));
    }

    private Optional<Range> extractPages() {
        return getVolisspagTpStream()
                   .filter(this::isPageRange)
                   .map(this::extractPageRange)
                   .findAny().orElse(Optional.empty());
    }

    private Stream<JAXBElement<?>> getVolisspagTpStream() {
        return Optional.ofNullable(getSourceTp().getVolisspag())
                   .map(VolisspagTp::getContent)
                   .orElse(emptyList())
                   .stream();
    }

    private Optional<String> extractVolume() {
        return getVolisspagTpStream()
                   .filter(this::isVolumeIssue)
                   .map(this::extractVolumeValue)
                   .findAny().orElse(Optional.empty());
    }

    private Optional<String> extractIssue() {
        return getVolisspagTpStream()
                   .filter(this::isVolumeIssue)
                   .map(this::extractIssueValue)
                   .findAny().orElse(Optional.empty());
    }

    private Optional<String> extractArticleNumber() {
        return Optional.ofNullable(getSourceTp().getArticleNumber());
    }

    private boolean isVolumeIssue(JAXBElement<?> content) {
        return content.getValue() instanceof VolissTp;
    }

    private Optional<String> extractVolumeValue(JAXBElement<?> content) {
        return Optional.ofNullable(((VolissTp) content.getValue()).getVolume());
    }

    private Optional<String> extractIssueValue(JAXBElement<?> content) {
        return Optional.ofNullable(((VolissTp) content.getValue()).getIssue());
    }

    private boolean isPageRange(JAXBElement<?> content) {
        return content.getValue() instanceof PagerangeTp;
    }

    private Optional<Range> extractPageRange(JAXBElement<?> content) {
        return Optional.of(new Range(((PagerangeTp) content.getValue()).getFirst(),
                                     ((PagerangeTp) content.getValue()).getLast()));
    }

    private SourceTp getSourceTp() {
        return docTp.getItem()
                   .getItem()
                   .getBibrecord()
                   .getHead()
                   .getSource();
    }

    private boolean isJournal() {
        return publicationContext instanceof Journal || publicationContext instanceof UnconfirmedJournal;
    }

    private boolean isChapter() {
        return publicationContext instanceof Chapter || publicationContext instanceof BookSeries;
    }

    private Optional<CitationtypeAtt> getCitationTypeCode() {
        return docTp.getItem()
                   .getItem()
                   .getBibrecord()
                   .getHead()
                   .getCitationInfo()
                   .getCitationType()
                   .stream()
                   .findFirst()
                   .map(CitationTypeTp::getCode);
    }
}
