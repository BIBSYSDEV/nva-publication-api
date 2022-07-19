package no.unit.nva.publication.model.storage;

import static no.unit.nva.publication.model.storage.UniquenessEntry.DESERIALIZATION_ERROR_MESSAGE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.stream.Stream;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.storage.model.DatabaseConstants;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class UniquenessEntryTest {
    
    public static final String SAMPLE_IDENTIFIER = SortableIdentifier.next().toString();
    
    @ParameterizedTest
    @MethodSource("uniquenessEntries")
    void getPrimaryPartitionKeyReturnsStringContainingOnlyTypeAndEntryIdentifier(UniquenessEntry entry) {
        String expectedKey = entry.getType() + DatabaseConstants.KEY_FIELDS_DELIMITER + SAMPLE_IDENTIFIER;
        assertThat(entry.getPrimaryKeyPartitionKey(), is(equalTo(expectedKey)));
    }
    
    @ParameterizedTest
    @MethodSource("uniquenessEntries")
    void partitionKeyAndSortKeyAreEqual(UniquenessEntry entry) {
        assertThat(entry.getPrimaryKeySortKey(), is(equalTo(entry.getPrimaryKeyPartitionKey())));
    }
    
    @ParameterizedTest(name = "UniquenessEntries should not be deserialized")
    @MethodSource("uniquenessEntries")
    void shouldNotBeDeserialized(UniquenessEntry entry) {
        var exception=assertThrows(UnsupportedOperationException.class, entry::getIdentifier);
        assertThat(exception.getMessage(),containsString(DESERIALIZATION_ERROR_MESSAGE));
    }
    
    private static Stream<UniquenessEntry> uniquenessEntries() {
        var identifierEntry = new IdentifierEntry(SAMPLE_IDENTIFIER);
        var uniqueDoiRequestEntry = new UniqueDoiRequestEntry(SAMPLE_IDENTIFIER);
        var uniquePublishingRequestEntry = new UniquePublishingRequestEntry(SAMPLE_IDENTIFIER);
        return Stream.of(identifierEntry, uniqueDoiRequestEntry,uniquePublishingRequestEntry);
    }
}