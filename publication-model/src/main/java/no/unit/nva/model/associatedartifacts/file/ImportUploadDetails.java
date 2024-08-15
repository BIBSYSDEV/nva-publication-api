package no.unit.nva.model.associatedartifacts.file;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.Instant;
import java.util.Arrays;
import no.unit.nva.commons.json.JsonSerializable;
import nva.commons.core.SingletonCollector;

@JsonTypeName(ImportUploadDetails.TYPE)
@JsonTypeInfo(use = Id.NAME, property = "type")
public record ImportUploadDetails(Source source, String archive, Instant uploadedDate)
    implements UploadDetails, JsonSerializable {

    public static final String TYPE = "ImportUploadDetails";

    public enum Source {
        BRAGE("Brage"), SCOPUS("Scopus");

        private final String value;

        Source(String value) {
            this.value = value;
        }

        @JsonCreator
        public static Source fromValue(String value) {
            return Arrays.stream(Source.values())
                       .filter(enumValue -> enumValue.getValue().equalsIgnoreCase(value))
                       .collect(SingletonCollector.tryCollect())
                       .orElseThrow(fail -> new IllegalArgumentException("Could not parse UploadDetails system!"));
        }

        @JsonValue
        public String getValue() {
            return value;
        }
    }
}
