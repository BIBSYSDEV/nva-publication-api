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
  public static final Map<CristinSecondaryCategory, JournalArticleContentType>
      mapToJournalContentType = createMapToJournalContentType();
  private static final String CONVERSION_ERROR_MESSAGE =
      "Secondary category %s cannot be transformed to %s";
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
    return ANTHOLOGY == cristinObject.getSecondaryCategory();
  }

  public static boolean isMonograph(CristinObject cristinObject) {
    return MONOGRAPH == cristinObject.getSecondaryCategory()
        || TEXTBOOK == cristinObject.getSecondaryCategory()
        || NON_FICTION_BOOK == cristinObject.getSecondaryCategory()
        || ENCYCLOPEDIA == cristinObject.getSecondaryCategory()
        || POPULAR_BOOK == cristinObject.getSecondaryCategory()
        || REFERENCE_MATERIAL == cristinObject.getSecondaryCategory()
        || EXHIBITION_CATALOG == cristinObject.getSecondaryCategory()
        || ACADEMIC_COMMENTARY == cristinObject.getSecondaryCategory();
  }

  public static boolean isJournalLetter(CristinObject cristinObject) {
    return JOURNAL_LETTER == cristinObject.getSecondaryCategory()
        || READER_OPINION == cristinObject.getSecondaryCategory();
  }

  public static boolean isJournalReview(CristinObject cristinObject) {
    return JOURNAL_REVIEW == cristinObject.getSecondaryCategory();
  }

  public static boolean isJournalLeader(CristinObject cristinObject) {
    return JOURNAL_LEADER == cristinObject.getSecondaryCategory();
  }

  public static boolean isAbstract(CristinObject cristinObject) {
    return ABSTRACT == cristinObject.getSecondaryCategory();
  }

  public static boolean isJournalCorrigendum(CristinObject cristinObject) {
    return JOURNAL_CORRIGENDUM == cristinObject.getSecondaryCategory();
  }

  public static boolean isJournalArticle(CristinObject cristinObject) {
    return JOURNAL_ARTICLE == cristinObject.getSecondaryCategory()
        || POPULAR_ARTICLE == cristinObject.getSecondaryCategory()
        || ARTICLE == cristinObject.getSecondaryCategory()
        || ACADEMIC_REVIEW == cristinObject.getSecondaryCategory()
        || SHORT_COMMUNICATION == cristinObject.getSecondaryCategory();
  }

  public static boolean isResearchReport(CristinObject cristinObject) {
    return RESEARCH_REPORT == cristinObject.getSecondaryCategory();
  }

  public static boolean isDegreePhd(CristinObject cristinObject) {
    return DEGREE_PHD == cristinObject.getSecondaryCategory()
        || MAGISTER_THESIS == cristinObject.getSecondaryCategory();
  }

  public static boolean isDegreeMaster(CristinObject cristinObject) {
    return DEGREE_MASTER == cristinObject.getSecondaryCategory()
        || SECOND_DEGREE_THESIS == cristinObject.getSecondaryCategory()
        || MEDICAL_THESIS == cristinObject.getSecondaryCategory();
  }

  public static boolean isDegreeLicentiate(CristinObject cristinObject) {
    return DEGREE_LICENTIATE == cristinObject.getSecondaryCategory();
  }

  public static boolean isReportWorkingPaper(CristinObject cristinObject) {
    return COMPENDIUM == cristinObject.getSecondaryCategory();
  }

  public static boolean isBriefs(CristinObject cristinObject) {
    return BRIEFS == cristinObject.getSecondaryCategory();
  }

  public static boolean isInterview(CristinObject cristinObject) {
    return INTERVIEW == cristinObject.getSecondaryCategory()
        || WRITTEN_INTERVIEW == cristinObject.getSecondaryCategory();
  }

  public static boolean isMuseum(CristinObject cristinObject) {
    return MUSEUM == cristinObject.getSecondaryCategory();
  }

  public static boolean isProgramParticipation(CristinObject cristinObject) {
    return PROGRAM_PARTICIPATION == cristinObject.getSecondaryCategory()
        || PROGRAM_MANAGEMENT == cristinObject.getSecondaryCategory();
  }

  public static boolean isMediaFeatureArticle(CristinObject cristinObject) {
    return FEATURE_ARTICLE == cristinObject.getSecondaryCategory();
  }

  public static boolean isConferenceLecture(CristinObject cristinObject) {
    return CONFERENCE_LECTURE == cristinObject.getSecondaryCategory();
  }

  public static boolean isConferencePoster(CristinObject cristinObject) {
    return CONFERENCE_POSTER == cristinObject.getSecondaryCategory();
  }

  public static boolean isLecture(CristinObject cristinObject) {
    return LECTURE == cristinObject.getSecondaryCategory()
        || POPULAR_SCIENTIFIC_LECTURE == cristinObject.getSecondaryCategory();
  }

  public static boolean isOtherPresentation(CristinObject cristinObject) {
    return OTHER_PRESENTATION == cristinObject.getSecondaryCategory()
        || INTERNET_EXHIBIT == cristinObject.getSecondaryCategory();
  }

  @JsonValue
  public String getValue() {
    if (Objects.nonNull(aliases) && !aliases.isEmpty()) {
      return aliases.get(DEFAULT_VALUE);
    }
    return this.name();
  }

  public boolean isUnknownCategory() {
    return UNMAPPED == this;
  }

  public JournalArticleContentType toJournalArticleContentType() {
    if (mapToJournalContentType.containsKey(this)) {
      return mapToJournalContentType.get(this);
    } else {
      throw new IllegalStateException(conversionError(this, JournalArticleContentType.class));
    }
  }

  private static Map<CristinSecondaryCategory, JournalArticleContentType>
      createMapToJournalContentType() {
    return Map.of(
        JOURNAL_ARTICLE,
        JournalArticleContentType.PROFESSIONAL_ARTICLE,
        POPULAR_ARTICLE,
        JournalArticleContentType.POPULAR_SCIENCE_ARTICLE,
        ARTICLE,
        JournalArticleContentType.ACADEMIC_ARTICLE,
        ACADEMIC_REVIEW,
        JournalArticleContentType.ACADEMIC_LITERATURE_REVIEW,
        SHORT_COMMUNICATION,
        JournalArticleContentType.ACADEMIC_ARTICLE);
  }

  private static String conversionError(
      CristinSecondaryCategory category, Class<?> publicatoinInstanceClass) {
    return String.format(
        CONVERSION_ERROR_MESSAGE, category, publicatoinInstanceClass.getSimpleName());
  }
}
