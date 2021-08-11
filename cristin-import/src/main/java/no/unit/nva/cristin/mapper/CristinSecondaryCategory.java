package no.unit.nva.cristin.mapper;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import nva.commons.core.SingletonCollector;

public enum CristinSecondaryCategory {
    ANTHOLOGY("ANTOLOGI", "ANTHOLOGY"),
    MONOGRAPH("MONOGRAFI", "MONOGRAPH"),
    JOURNAL_ARTICLE("ARTIKKEL_FAG", "JOURNAL_ARTICLE"),
    JOURNAL_REVIEW("BOKANMELDELSE", "JOURNAL_REVIEW"),
    POPULAR_ARTICLE("ARTIKKEL_POP", "POPULAR_ARTICLE"),
    ARTICLE("ARTIKKEL", "ARTICLE"),
    ACADEMIC_REVIEW("OVERSIKTSART", "ACADEMIC_REVIEW"),
    RESEARCH_REPORT("RAPPORT", "RESEARCH_REPORT"),
    DEGREE_PHD("DRGRADAVH", "DEGREE_PHD"),
    CHAPTER_ARTICLE("KAPITTEL", "CHAPTER_ARTICLE"),
    CHAPTER("FAGLIG_KAPITTEL", "CHAPTER"),
    POPULAR_CHAPTER_ARTICLE("POPVIT_KAPITTEL", "POPULAR_CHAPTER_ARTICLE"),
    UNMAPPED;

    public static final int DEFAULT_VALUE = 0;
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
        return CristinSecondaryCategory.MONOGRAPH.equals(cristinObject.getSecondaryCategory());
    }

    public static boolean isJournalArticle(CristinObject cristinObject) {
        return CristinSecondaryCategory.JOURNAL_ARTICLE.equals(cristinObject.getSecondaryCategory())
                || CristinSecondaryCategory.POPULAR_ARTICLE.equals(cristinObject.getSecondaryCategory())
                || CristinSecondaryCategory.ARTICLE.equals(cristinObject.getSecondaryCategory())
                || CristinSecondaryCategory.ACADEMIC_REVIEW.equals(cristinObject.getSecondaryCategory());
    }

    public static boolean isJournalReview(CristinObject cristinObject) {
        return CristinSecondaryCategory.JOURNAL_REVIEW.equals(cristinObject.getSecondaryCategory());
    }

    public static boolean isResearchReport(CristinObject cristinObject) {
        return CristinSecondaryCategory.RESEARCH_REPORT.equals(cristinObject.getSecondaryCategory());
    }

    public static  boolean isDegreePhd(CristinObject cristinObject) {
        return CristinSecondaryCategory.DEGREE_PHD.equals(cristinObject.getSecondaryCategory());
    }

    public static boolean isChapterArticle(CristinObject cristinObject) {
        return CristinSecondaryCategory.CHAPTER_ARTICLE.equals(cristinObject.getSecondaryCategory())
                || CristinSecondaryCategory.CHAPTER.equals(cristinObject.getSecondaryCategory())
                || CristinSecondaryCategory.POPULAR_CHAPTER_ARTICLE.equals(cristinObject.getSecondaryCategory());
    }

    public boolean isUnknownCategory() {
        return UNMAPPED.equals(this);
    }

}
