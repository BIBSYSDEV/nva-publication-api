package no.unit.nva.cristin.mapper;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Arrays;
import java.util.List;
import nva.commons.core.SingletonCollector;

public enum CristinMainCategory {
  BOOK("BOK", "BOOK"),
  JOURNAL("TIDSSKRIFTPUBL", "JOURNAL"),
  REPORT("RAPPORT", "REPORT"),
  CHAPTER("BOKRAPPORTDEL", "CHAPTER"),
  EVENT("FOREDRAG", "EVENT"),
  EXHIBITION("UTSTILLING", "EXHIBITION"),
  MEDIA_CONTRIBUTION("MEDIEBIDRAG", "MEDIA_CONTRIBUTION"),
  ARTISTIC_PRODUCTION("KUNST_PRODUKSJON", "ARTISTIC_PRODUCTION"),
  INFORMATION_MATERIAL("INFORMASJONSMATR", "INFORMATION_MATERIAL"),
  UNMAPPED;

  public static final int DEFAULT_VALUE = 0;
  private final List<String> aliases;

  CristinMainCategory(String... mapping) {
    aliases = Arrays.asList(mapping);
  }

  @JsonCreator
  public static CristinMainCategory fromString(String category) {
    return Arrays.stream(values())
        .filter(item -> item.aliases.contains(category))
        .collect(SingletonCollector.collectOrElse(UNMAPPED));
  }

  public static boolean isBook(CristinObject cristinObject) {
    return BOOK == cristinObject.getMainCategory();
  }

  public static boolean isJournal(CristinObject cristinObject) {
    return JOURNAL == cristinObject.getMainCategory()
        && CristinSecondaryCategory.WRITTEN_INTERVIEW != cristinObject.getSecondaryCategory()
        && CristinSecondaryCategory.FEATURE_ARTICLE != cristinObject.getSecondaryCategory();
  }

  public static boolean isReport(CristinObject cristinObject) {
    return REPORT == cristinObject.getMainCategory();
  }

  public static boolean isChapter(CristinObject cristinObject) {
    return CHAPTER == cristinObject.getMainCategory();
  }

  public static boolean isEvent(CristinObject cristinObject) {
    return EVENT == cristinObject.getMainCategory();
  }

  public static boolean isArt(CristinObject cristinObject) {
    return ARTISTIC_PRODUCTION == cristinObject.getMainCategory();
  }

  public static boolean isInformationMaterial(CristinObject cristinObject) {
    return INFORMATION_MATERIAL == cristinObject.getMainCategory();
  }

  public static boolean isExhibition(CristinObject cristinObject) {
    return EXHIBITION == cristinObject.getMainCategory();
  }

  public static boolean isMediaContribution(CristinObject cristinObject) {
    return MEDIA_CONTRIBUTION == cristinObject.getMainCategory()
        || mainAndSecondaryCategoryIndicatesWrittenInterview(cristinObject)
        || mainAndSecondaryCategoryIndicatesFeatureArticle(cristinObject);
  }

  private static boolean mainAndSecondaryCategoryIndicatesWrittenInterview(
      CristinObject cristinObject) {
    return JOURNAL == cristinObject.getMainCategory()
        && CristinSecondaryCategory.WRITTEN_INTERVIEW == cristinObject.getSecondaryCategory();
  }

  private static boolean mainAndSecondaryCategoryIndicatesFeatureArticle(
      CristinObject cristinObject) {
    return JOURNAL == cristinObject.getMainCategory()
        && CristinSecondaryCategory.FEATURE_ARTICLE == cristinObject.getSecondaryCategory();
  }

  public boolean isUnknownCategory() {
    return UNMAPPED == this;
  }
}
