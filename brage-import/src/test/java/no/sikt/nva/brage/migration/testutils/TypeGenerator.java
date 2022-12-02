package no.sikt.nva.brage.migration.testutils;

import java.util.List;
import no.sikt.nva.brage.migration.record.Type;

public class TypeGenerator {

    public static Type generateChapter() {
        return new Type(List.of("Chapter"), "Chapter");
    }
}
