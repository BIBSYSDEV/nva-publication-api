package no.unit.nva.publication.storage.model.daos;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import com.fasterxml.jackson.databind.JsonNode;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.storage.model.DatabaseConstants;
import nva.commons.core.JsonUtils;
import org.junit.jupiter.api.Test;

public class UniquenessEntryTest {
    
    private static final String TYPE = "someType";
    private final SortableIdentifier sampleIdentifier = SortableIdentifier.next();
    
    @Test
    public void uniquenessEntryContainsPrimaryKeyOfTable() {
        UniquenessEntry uniquenessEntry = new UniquenessEntry(sampleIdentifier.toString()) {
            @Override
            protected String getType() {
                return TYPE;
            }
        };
        JsonNode json = JsonUtils.objectMapper.convertValue(uniquenessEntry, JsonNode.class);
        
        String expectedPartitionKey = TYPE + DatabaseConstants.KEY_FIELDS_DELIMITER + sampleIdentifier.toString();
        String expectedSortKey = expectedPartitionKey;
        String actualPartitionKey = json.get(DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME).textValue();
        String actualSortKey = json.get(DatabaseConstants.PRIMARY_KEY_SORT_KEY_NAME).textValue();
        
        assertThat(actualPartitionKey, is(equalTo(expectedPartitionKey)));
        assertThat(actualSortKey, is(equalTo(expectedSortKey)));
    }
}