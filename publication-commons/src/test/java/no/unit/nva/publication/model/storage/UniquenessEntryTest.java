package no.unit.nva.publication.model.storage;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import java.util.stream.Stream;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.storage.model.DatabaseConstants;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class UniquenessEntryTest {
    
    public static final String SAMPLE_IDENTIFIER = SortableIdentifier.next().toString();
    
    @ParameterizedTest
    @MethodSource("uniquenessEntries")
    public void getPrimaryPartitionKeyReturnsStringContainingOnlyTypeAndEntryIdentifier(UniquenessEntry entry) {
        String expectedKey = entry.getType() + DatabaseConstants.KEY_FIELDS_DELIMITER + SAMPLE_IDENTIFIER;
        assertThat(entry.getPrimaryKeyPartitionKey(), is(equalTo(expectedKey)));
    }
    
    @ParameterizedTest
    @MethodSource("uniquenessEntries")
    public void partitionKeyAndSortKeyAreEqual(UniquenessEntry entry) {
        assertThat(entry.getPrimaryKeySortKey(), is(equalTo(entry.getPrimaryKeyPartitionKey())));
    }
    
    private static Stream<UniquenessEntry> uniquenessEntries() {
        IdentifierEntry identifierEntry = new IdentifierEntry(SAMPLE_IDENTIFIER);
        UniqueDoiRequestEntry uniqueDoiRequestEntry = new UniqueDoiRequestEntry(SAMPLE_IDENTIFIER);
        return Stream.of(identifierEntry, uniqueDoiRequestEntry);
    }
}