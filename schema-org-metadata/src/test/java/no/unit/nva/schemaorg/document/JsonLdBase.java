package no.unit.nva.schemaorg.document;

import com.fasterxml.jackson.annotation.JsonProperty;
import nva.commons.core.JacocoGenerated;

import java.net.URI;
import java.util.Objects;

public class JsonLdBase {
    public static final String CONTEXT_FIELD = "@context";
    public static final String ID_FIELD = "@id";
    public static final String TYPE_FIELD = "@type";
    @JsonProperty(CONTEXT_FIELD)
    private final Context context;
    @JsonProperty(ID_FIELD)
    private final URI id;
    @JsonProperty(TYPE_FIELD)
    private final Object type;

    public JsonLdBase(@JsonProperty(CONTEXT_FIELD) URI context,
                      @JsonProperty(ID_FIELD) URI id,
                      @JsonProperty(TYPE_FIELD) Object type) {
        this.context = new Context(context);
        this.id = id;
        this.type = type;
    }

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof JsonLdBase)) {
            return false;
        }
        JsonLdBase that = (JsonLdBase) o;
        return Objects.equals(context, that.context)
                && Objects.equals(id, that.id)
                && Objects.equals(type, that.type);
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(context, id, type);
    }
}
