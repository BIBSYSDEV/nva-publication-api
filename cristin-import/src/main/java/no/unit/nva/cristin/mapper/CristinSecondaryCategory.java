package no.unit.nva.cristin.mapper;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import no.unit.nva.model.instancetypes.journal.JournalArticleContentType;
import nva.commons.core.SingletonCollector;

@SuppressWarnings("PMD.ExcessivePublicCount")
public enum CristinSecondaryCategory {
    ANTHOLOGY("ANTOLOGI", "ANTHOLOGY"),
    ARCHITECT_DESIGN("ARKITEKTTEGNING", "ARCHITECT_DESIGN"),
    MONOGRAPH("MONOGRAFI", "MONOGRAPH"),
    TEXTBOOK("LÆREBOK", "TEXTBOOK"),
    NON_FICTION_BOOK("FAGBOK", "NON_FICTION_BOOK"),
    ENCYCLOPEDIA("LEKSIKON", "ENCYCLOPEDIA"),
    POPULAR_BOOK("POPVIT_BOK", "POPULAR_BOOK"),
    ACADEMIC_COMMENTARY("KOMMENTARUTG", "ACADEMIC_COMMENTARY"),
    REFERENCE_MATERIAL("OPPSLAGSVERK", "REFERENCE_MATERIAL"),
    FEATURE_ARTICLE("KRONIKK", "FEATURE_ARTICLE"),
    FOREWORD("FORORD", "FOREWORD"),
    INTRODUCTION("INNLEDNING", "INTRODUCTION"),
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
    MAGISTER_THESIS("MAGISTERAVH", "MAGISTER_THESIS"),
    RESEARCH_REPORT("RAPPORT", "RESEARCH_REPORT"),
    DEGREE_LICENTIATE("LISENSIATAVH", "DEGREE_LICENTIATE"),
    DEGREE_PHD("DRGRADAVH", "DEGREE_PHD"),
    DEGREE_MASTER("MASTERGRADSOPPG", "DEGREE_MASTER"),
    EXHIBITION_CATALOG("UTSTILLINGSKAT", "EXHIBITION_CATALOG"),
    FILM_PRODUCTION("FILMPRODUKSJON", "FILM_PRODUCTION"),
    SECOND_DEGREE_THESIS("HOVEDFAGSOPPGAVE", "SECOND_DEGREE_THESIS"),
    MEDICAL_THESIS("FORSKERLINJEOPPG", "MEDICAL_THESIS"),
    CHAPTER_ACADEMIC("KAPITTEL", "CHAPTER_ACADEMIC"),
    CHAPTER("FAGLIG_KAPITTEL", "CHAPTER"),
    POPULAR_CHAPTER_ARTICLE("POPVIT_KAPITTEL", "POPULAR_CHAPTER_ARTICLE"),
    LEXICAL_IMPORT("LEKSIKAL_INNF", "LEXICAL_IMPORT"),
    CONFERENCE_LECTURE("VIT_FOREDRAG", "CONFERENCE_LECTURE"),
    CONFERENCE_POSTER("POSTER", "CONFERENCE_POSTER"),
    LECTURE("FOREDRAG_FAG", "LECTURE"),
    MUSEUM("MUSEUM"),
    MUSICAL_PERFORMANCE("MUSIKK_FRAMFORIN", "MUSICAL_PERFORMANCE"),
    MUSICAL_PIECE("MUSIKK_KOMP", "MUSICAL PIECE"),
    POPULAR_SCIENTIFIC_LECTURE("POPVIT_FOREDRAG", "POPULAR_SCIENTIFIC_LECTURE"),
    OTHER_PRESENTATION("ANNEN_PRESENTASJ", "OTHER_PRESENTATION"),
    INTERNET_EXHIBIT("UTST_WEB", "INTERNET_EXHIBIT"),
    PROGRAM_PARTICIPATION("PROGDELTAGELSE", "PROGRAM_PARTICIPATION"),
    PROGRAM_MANAGEMENT("PROGLEDELSE", "PROGRAM_MANAGEMENT"),
    INTERVIEW("INTERVJU", "INTERVIEW"),
    THEATRICAL_PRODUCTION("TEATERPRODUKSJON", "THEATRICAL_PRODUCTION"),
    VISUAL_ARTS("KUNST_OG_BILDE", "VISUAL_ARTS"),
    WRITTEN_INTERVIEW("INTERVJUSKRIFTL", "WRITTEN INTERVIEW"),
    ABSTRACT("SAMMENDRAG", "ABSTRACT"),
    BRIEFS("BRIEFS", "BRIEFS"),
    COMPENDIUM("KOMPENDIUM", "COMPENDIUM"),
    UNMAPPED;

    public static final int DEFAULT_VALUE = 0;
    public static final Map<CristinSecondaryCategory, JournalArticleContentType> mapToJournalContentType =
        createMapToJournalContentType();
    private static final String CONVERSION_ERROR_MESSAGE = "Secondary category %s cannot be transformed to %s";
    private final List<String> aliases;

    CristinSecondaryCategory(String... aliases) {
        this.aliases = Arrays.asList(aliases);
    }

    @JsonCreator
    public static CristinSecondaryCategory fromString(String category) {
        return Arrays.stream(values())
                   .filter(enumValue -> enumValue.aliases.contains(category))
                   .collect(SingletonCollector.collectOrElse(UNMAPPED));
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
               || CristinSecondaryCategory.REFERENCE_MATERIAL.equals(cristinObject.getSecondaryCategory())
               || CristinSecondaryCategory.EXHIBITION_CATALOG.equals(cristinObject.getSecondaryCategory())
               || CristinSecondaryCategory.ACADEMIC_COMMENTARY.equals(cristinObject.getSecondaryCategory());
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

    public static boolean isAbstract(CristinObject cristinObject) {
        return CristinSecondaryCategory.ABSTRACT.equals(cristinObject.getSecondaryCategory());
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

    public static boolean isDegreePhd(CristinObject cristinObject) {
        return CristinSecondaryCategory.DEGREE_PHD.equals(cristinObject.getSecondaryCategory())
               || CristinSecondaryCategory.MAGISTER_THESIS.equals(cristinObject.getSecondaryCategory());
    }

    public static boolean isDegreeMaster(CristinObject cristinObject) {
        return CristinSecondaryCategory.DEGREE_MASTER.equals(cristinObject.getSecondaryCategory())
               || CristinSecondaryCategory.SECOND_DEGREE_THESIS.equals(cristinObject.getSecondaryCategory())
               || CristinSecondaryCategory.MEDICAL_THESIS.equals(cristinObject.getSecondaryCategory());
    }

    public static boolean isDegreeLicentiate(CristinObject cristinObject) {
        return CristinSecondaryCategory.DEGREE_LICENTIATE.equals(cristinObject.getSecondaryCategory());
    }

    public static boolean isReportWorkingPaper(CristinObject cristinObject) {
        return CristinSecondaryCategory.COMPENDIUM.equals(cristinObject.getSecondaryCategory());
    }

    public static boolean isBriefs(CristinObject cristinObject) {
        return CristinSecondaryCategory.BRIEFS.equals(cristinObject.getSecondaryCategory());
    }

    public static boolean isInterview(CristinObject cristinObject) {
        return CristinSecondaryCategory.INTERVIEW.equals(cristinObject.getSecondaryCategory())
               || CristinSecondaryCategory.WRITTEN_INTERVIEW.equals(cristinObject.getSecondaryCategory());
    }

    public static boolean isMuseum(CristinObject cristinObject) {
        return CristinSecondaryCategory.MUSEUM.equals(cristinObject.getSecondaryCategory());
    }

    public static boolean isProgramParticipation(CristinObject cristinObject) {
        return CristinSecondaryCategory.PROGRAM_PARTICIPATION.equals(cristinObject.getSecondaryCategory())
               || CristinSecondaryCategory.PROGRAM_MANAGEMENT.equals(cristinObject.getSecondaryCategory());
    }

    public static boolean isMediaFeatureArticle(CristinObject cristinObject) {
        return CristinSecondaryCategory.FEATURE_ARTICLE.equals(cristinObject.getSecondaryCategory());
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

    @JsonValue
    public String getValue() {
        if (Objects.nonNull(aliases) && !aliases.isEmpty()) {
            return aliases.get(DEFAULT_VALUE);
        }
        return this.name();
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

    private static Map<CristinSecondaryCategory, JournalArticleContentType> createMapToJournalContentType() {
        return Map.of(JOURNAL_ARTICLE, JournalArticleContentType.PROFESSIONAL_ARTICLE, POPULAR_ARTICLE,
                      JournalArticleContentType.POPULAR_SCIENCE_ARTICLE, ARTICLE,
                      JournalArticleContentType.ACADEMIC_ARTICLE, ACADEMIC_REVIEW,
                      JournalArticleContentType.ACADEMIC_LITERATURE_REVIEW, SHORT_COMMUNICATION,
                      JournalArticleContentType.ACADEMIC_ARTICLE);
    }

    private static String conversionError(CristinSecondaryCategory category, Class<?> publicatoinInstanceClass) {
        return String.format(CONVERSION_ERROR_MESSAGE, category, publicatoinInstanceClass.getSimpleName());
    }
}
