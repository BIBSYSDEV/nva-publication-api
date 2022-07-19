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
        return CristinMainCategory.BOOK.equals(cristinObject.getMainCategory());
    }
    
    public static boolean isJournal(CristinObject cristinObject) {
        return CristinMainCategory.JOURNAL.equals(cristinObject.getMainCategory());
    }
    
    public static boolean isReport(CristinObject cristinObject) {
        return CristinMainCategory.REPORT.equals(cristinObject.getMainCategory());
    }
    
    public static boolean isChapter(CristinObject cristinObject) {
        return CristinMainCategory.CHAPTER.equals(cristinObject.getMainCategory());
    }
    
    public static boolean isEvent(CristinObject cristinObject) {
        return CristinMainCategory.EVENT.equals(cristinObject.getMainCategory());
    }
    
    public boolean isUnknownCategory() {
        return UNMAPPED.equals(this);
    }
}
