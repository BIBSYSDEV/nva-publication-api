package no.unit.nva.cristin.mapper;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import no.unit.nva.model.instancetypes.book.BookMonographContentType;
import no.unit.nva.model.instancetypes.chapter.ChapterArticleContentType;
import no.unit.nva.model.instancetypes.journal.JournalArticleContentType;
import nva.commons.core.SingletonCollector;

public enum CristinSecondaryCategory {
    ANTHOLOGY("ANTOLOGI", "ANTHOLOGY"),
    MONOGRAPH("MONOGRAFI", "MONOGRAPH"),
    TEXTBOOK("LÆREBOK", "TEXTBOOK"),
    NON_FICTION_BOOK("FAGBOK", "NON_FICTION_BOOK"),
    ENCYCLOPEDIA("LEKSIKON", "ENCYCLOPEDIA"),
    POPULAR_BOOK("POPVIT_BOK", "POPULAR_BOOK"),
    REFERENCE_MATERIAL("OPPSLAGSVERK", "REFERENCE_MATERIAL"),
    FEATURE_ARTICLE("KRONIKK", "FEATURE_ARTICLE"),
    JOURNAL_LETTER("BREV_TIL_RED", "JOURNAL_LETTER"),
    READER_OPINION("LESERINNLEGG", "READER_OPINIION"),
    JOURNAL_REVIEW("BOKANMELDELSE", "JOURNAL_REVIEW"),
    JOURNAL_LEADER("LEDER", "JOURNAL_LEADER"),
    JOURNAL_CORRIGENDUM("ERRATA", "JOURNAL_CORRIGENDUM"),
    JOURNAL_ARTICLE("ARTIKKEL_FAG", "JOURNAL_ARTICLE"),
    POPULAR_ARTICLE("ARTIKKEL_POP", "POPULAR_ARTICLE"),
    ARTICLE("ARTIKKEL", "ARTICLE"),
    ACADEMIC_REVIEW("OVERSIKTSART", "ACADEMIC_REVIEW"),
    SHORT_COMMUNICATION("SHORTCOMM", "SHORT_COMMUNICATION"),
    RESEARCH_REPORT("RAPPORT", "RESEARCH_REPORT"),
    DEGREE_PHD("DRGRADAVH", "DEGREE_PHD"),
    DEGREE_MASTER("MASTERGRADSOPPG", "DEGREE_MASTER"),
    SECOND_DEGREE_THESIS("HOVEDFAGSOPPGAVE", "SECOND_DEGREE_THESIS"),
    MEDICAL_THESIS("FORSKERLINJEOPPG", "MEDICAL_THESIS"),
    CHAPTER_ACADEMIC("KAPITTEL", "CHAPTER_ACADEMIC"),
    CHAPTER("FAGLIG_KAPITTEL", "CHAPTER"),
    POPULAR_CHAPTER_ARTICLE("POPVIT_KAPITTEL", "POPULAR_CHAPTER_ARTICLE"),
    LEXICAL_IMPORT("LEKSIKAL_INNF", "LEXICAL_IMPORT"),
    CONFERENCE_LECTURE("VIT_FOREDRAG", "CONFERENCE_LECTURE"),
    CONFERENCE_POSTER("POSTER", "CONFERENCE_POSTER"),
    LECTURE("FOREDRAG_FAG", "LECTURE"),
    POPULAR_SCIENTIFIC_LECTURE("POPVIT_FOREDRAG", "POPULAR_SCIENTIFIC_LECTURE"),
    OTHER_PRESENTATION("ANNEN_PRESENTASJ", "OTHER_PRESENTATION"),
    INTERNET_EXHIBIT("UTST_WEB", "INTERNET_EXHIBIT"),
    UNMAPPED;

    public static final int DEFAULT_VALUE = 0;
    private final List<String> aliases;
    private static final String CONVERSION_ERROR_MESSAGE = "Secondary category %s cannot be transformed to %s";
    public static final Map<CristinSecondaryCategory, JournalArticleContentType> mapToJournalContentType =
            createMapToJournalContentType();
    public static final Map<CristinSecondaryCategory, BookMonographContentType> mapToBookMonographContentType =
            createMapToBookMonographContentType();

    CristinSecondaryCategory(String... aliases) {
        this.aliases = Arrays.asList(aliases);
    }

    @JsonCreator
    public static CristinSecondaryCategory fromString(String category) {
        return Arrays.stream(values())
                   .filter(enumValue -> enumValue.aliases.contains(category))
                   .collect(SingletonCollector.collectOrElse(UNMAPPED));
    }

    @JsonValue
    public String getValue() {
        if (Objects.nonNull(aliases) && !aliases.isEmpty()) {
            return aliases.get(DEFAULT_VALUE);
        }
        return this.name();
    }

    public static boolean isAnthology(CristinObject cristinObject) {
        return CristinSecondaryCategory.ANTHOLOGY.equals(cristinObject.getSecondaryCategory());
    }

    public static boolean isMonograph(CristinObject cristinObject) {
        return CristinSecondaryCategory.MONOGRAPH.equals(cristinObject.getSecondaryCategory())
                || CristinSecondaryCategory.TEXTBOOK.equals(cristinObject.getSecondaryCategory())
                || CristinSecondaryCategory.NON_FICTION_BOOK.equals(cristinObject.getSecondaryCategory())
                || CristinSecondaryCategory.ENCYCLOPEDIA.equals(cristinObject.getSecondaryCategory())
                || CristinSecondaryCategory.POPULAR_BOOK.equals(cristinObject.getSecondaryCategory())
                || CristinSecondaryCategory.REFERENCE_MATERIAL.equals(cristinObject.getSecondaryCategory());
    }

    public static boolean isFeatureArticle(CristinObject cristinObject) {
        return CristinSecondaryCategory.FEATURE_ARTICLE.equals(cristinObject.getSecondaryCategory());
    }

    public static boolean isJournalLetter(CristinObject cristinObject) {
        return CristinSecondaryCategory.JOURNAL_LETTER.equals(cristinObject.getSecondaryCategory())
                || CristinSecondaryCategory.READER_OPINION.equals(cristinObject.getSecondaryCategory());
    }

    public static boolean isJournalReview(CristinObject cristinObject) {
        return CristinSecondaryCategory.JOURNAL_REVIEW.equals(cristinObject.getSecondaryCategory());
    }

    public static boolean isJournalLeader(CristinObject cristinObject) {
        return CristinSecondaryCategory.JOURNAL_LEADER.equals(cristinObject.getSecondaryCategory());
    }

    public static boolean isJournalCorrigendum(CristinObject cristinObject) {
        return CristinSecondaryCategory.JOURNAL_CORRIGENDUM.equals(cristinObject.getSecondaryCategory());
    }

    public static boolean isJournalArticle(CristinObject cristinObject) {
        return CristinSecondaryCategory.JOURNAL_ARTICLE.equals(cristinObject.getSecondaryCategory())
                || CristinSecondaryCategory.POPULAR_ARTICLE.equals(cristinObject.getSecondaryCategory())
                || CristinSecondaryCategory.ARTICLE.equals(cristinObject.getSecondaryCategory())
                || CristinSecondaryCategory.ACADEMIC_REVIEW.equals(cristinObject.getSecondaryCategory())
                || CristinSecondaryCategory.SHORT_COMMUNICATION.equals(cristinObject.getSecondaryCategory());
    }

    public static boolean isResearchReport(CristinObject cristinObject) {
        return CristinSecondaryCategory.RESEARCH_REPORT.equals(cristinObject.getSecondaryCategory());
    }

    public static  boolean isDegreePhd(CristinObject cristinObject) {
        return CristinSecondaryCategory.DEGREE_PHD.equals(cristinObject.getSecondaryCategory());
    }

    public static  boolean isDegreeMaster(CristinObject cristinObject) {
        return CristinSecondaryCategory.DEGREE_MASTER.equals(cristinObject.getSecondaryCategory())
                || CristinSecondaryCategory.SECOND_DEGREE_THESIS.equals(cristinObject.getSecondaryCategory())
                || CristinSecondaryCategory.MEDICAL_THESIS.equals(cristinObject.getSecondaryCategory());
    }

    public static boolean isChapterArticle(CristinObject cristinObject) {
        return CristinSecondaryCategory.CHAPTER_ACADEMIC.equals(cristinObject.getSecondaryCategory())
                || CristinSecondaryCategory.CHAPTER.equals(cristinObject.getSecondaryCategory())
                || CristinSecondaryCategory.POPULAR_CHAPTER_ARTICLE.equals(cristinObject.getSecondaryCategory())
                || CristinSecondaryCategory.LEXICAL_IMPORT.equals(cristinObject.getSecondaryCategory());
    }

    public static boolean isConferenceLecture(CristinObject cristinObject) {
        return CristinSecondaryCategory.CONFERENCE_LECTURE.equals(cristinObject.getSecondaryCategory());
    }

    public static boolean isConferencePoster(CristinObject cristinObject) {
        return CristinSecondaryCategory.CONFERENCE_POSTER.equals(cristinObject.getSecondaryCategory());
    }

    public static boolean isLecture(CristinObject cristinObject) {
        return CristinSecondaryCategory.LECTURE.equals(cristinObject.getSecondaryCategory())
                || CristinSecondaryCategory.POPULAR_SCIENTIFIC_LECTURE.equals(cristinObject.getSecondaryCategory());
    }

    public static boolean isOtherPresentation(CristinObject cristinObject) {
        return CristinSecondaryCategory.OTHER_PRESENTATION.equals(cristinObject.getSecondaryCategory())
                || CristinSecondaryCategory.INTERNET_EXHIBIT.equals(cristinObject.getSecondaryCategory());
    }

    public boolean isUnknownCategory() {
        return UNMAPPED.equals(this);
    }

    public JournalArticleContentType toJournalArticleContentType() {
        if (mapToJournalContentType.containsKey(this)) {
            return mapToJournalContentType.get(this);
        } else {
            throw new IllegalStateException(conversionError(this, JournalArticleContentType.class));
        }
    }

    public BookMonographContentType toBookMonographContentType() {
        if (mapToBookMonographContentType.containsKey(this)) {
            return mapToBookMonographContentType.get(this);
        } else {
            throw new IllegalStateException(conversionError(this, BookMonographContentType.class));
        }
    }

    public ChapterArticleContentType toChapterArticleContentType() {
        if (createMapToChapterContentType().containsKey(this)) {
            return createMapToChapterContentType().get(this);
        } else {
            throw new IllegalStateException(conversionError(this, ChapterArticleContentType.class));
        }
    }

    private static Map<CristinSecondaryCategory, JournalArticleContentType> createMapToJournalContentType() {
        return Map.of(JOURNAL_ARTICLE, JournalArticleContentType.PROFESSIONAL_ARTICLE,
                POPULAR_ARTICLE, JournalArticleContentType.POPULAR_SCIENCE_ARTICLE,
                ARTICLE, JournalArticleContentType.ACADEMIC_ARTICLE,
                ACADEMIC_REVIEW, JournalArticleContentType.ACADEMIC_LITERATURE_REVIEW,
                SHORT_COMMUNICATION, JournalArticleContentType.ACADEMIC_ARTICLE);
    }

    private static Map<CristinSecondaryCategory, BookMonographContentType> createMapToBookMonographContentType() {
        return Map.of(MONOGRAPH, BookMonographContentType.ACADEMIC_MONOGRAPH,
                POPULAR_BOOK, BookMonographContentType.POPULAR_SCIENCE_MONOGRAPH,
                TEXTBOOK, BookMonographContentType.TEXTBOOK,
                ENCYCLOPEDIA, BookMonographContentType.ENCYCLOPEDIA,
                NON_FICTION_BOOK, BookMonographContentType.NON_FICTION_MONOGRAPH,
                REFERENCE_MATERIAL, BookMonographContentType.ENCYCLOPEDIA);
    }

    private static Map<CristinSecondaryCategory, ChapterArticleContentType> createMapToChapterContentType() {
        return Map.of(CHAPTER_ACADEMIC, ChapterArticleContentType.ACADEMIC_CHAPTER,
                POPULAR_CHAPTER_ARTICLE, ChapterArticleContentType.POPULAR_SCIENCE_CHAPTER,
                CHAPTER, ChapterArticleContentType.NON_FICTION_CHAPTER,
                LEXICAL_IMPORT, ChapterArticleContentType.ENCYCLOPEDIA_CHAPTER);
    }

    private static String conversionError(CristinSecondaryCategory category, Class<?> publicatoinInstanceClass) {
        return String.format(CONVERSION_ERROR_MESSAGE, category, publicatoinInstanceClass.getSimpleName());
    }

}
