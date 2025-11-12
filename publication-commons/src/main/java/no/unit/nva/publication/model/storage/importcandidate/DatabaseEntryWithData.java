package no.unit.nva.publication.model.storage.importcandidate;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Map;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.storage.DataCompressor;

public interface DatabaseEntryWithData<I> {

    static <T> T fromAttributeValuesMap(Map<String, AttributeValue> map, Class<T> clazz) {
        return DataCompressor.decompress(map, clazz);
    }

    @JsonIgnore
    I getData();

    @JsonIgnore
    SortableIdentifier getIdentifier();
}
