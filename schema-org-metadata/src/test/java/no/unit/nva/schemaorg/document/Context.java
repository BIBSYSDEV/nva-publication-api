package no.unit.nva.schemaorg.document;

import com.fasterxml.jackson.annotation.JsonProperty;
import nva.commons.core.JacocoGenerated;

import java.net.URI;
import java.util.Objects;

public class Context {
    public static final String VOCAB_FIELD = "@vocab";
    @JsonProperty(VOCAB_FIELD)
    private final URI vocab;

    public Context(@JsonProperty(VOCAB_FIELD) URI vocab) {
        this.vocab = vocab;
    }

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Context)) {
            return false;
        }
        Context context = (Context) o;
        return Objects.equals(vocab, context.vocab);
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(vocab);
    }
}
