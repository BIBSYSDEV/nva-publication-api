package no.unit.nva.publication.model.storage.importcandidate;

import static no.unit.nva.publication.model.storage.DataCompressor.compress;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Map;
import no.unit.nva.publication.model.storage.DataCompressor;

public interface DatabaseEntryWithData<I> {

    static <T> T fromAttributeValuesMap(Map<String, AttributeValue> map, Class<T> clazz) {
        return DataCompressor.decompress(map, clazz);
    }

    @JsonIgnore
    default Map<String, AttributeValue> toDynamoFormat() {
        return attempt(() -> compress(this)).orElseThrow();
    }

    @JsonIgnore
    I getData();
}
