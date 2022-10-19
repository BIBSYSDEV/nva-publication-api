package no.unit.nva.cristin.mapper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class CristinSecondaryCategoryTest {
    
    @Test
    void toJournalArticleContentTypeThrowsExceptionWhenSecCatShouldNotBeMappedToJournalArticleContentType() {
        List<CristinSecondaryCategory> secondaryCategoriesNotMappingToJournalArticle =
            Stream.of(CristinSecondaryCategory.values())
                .filter(category -> !CristinSecondaryCategory.mapToJournalContentType.containsKey(category))
                .collect(Collectors.toList());
        for (CristinSecondaryCategory secondaryCategory : secondaryCategoriesNotMappingToJournalArticle) {
            Executable action = secondaryCategory::toJournalArticleContentType;
            IllegalStateException exception = assertThrows(IllegalStateException.class, action);
            assertThat(exception.getMessage(), containsString(secondaryCategory.toString()));
        }
    }
}