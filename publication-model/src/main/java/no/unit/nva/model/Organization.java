package no.unit.nva.model;

import static java.util.Objects.isNull;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.net.URI;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;

@SuppressWarnings("PMD.AvoidUsingHardCodedIP")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class Organization extends Corporation {

    private static final String UIO_LEGACY_IDENTIFIER = "185.0.0.0";
    private static final String UIO_IDENTIFIER = "185.90.0.0";

    @JsonProperty("id")
    private URI id;

    public Organization() {
        super();
    }

    private Organization(Builder builder) {
        super();
        setId(builder.id);
    }

    public static Organization fromUri(URI uri) {
        return new Organization.Builder().withId(uri).build();
    }

    public URI getId() {
        return id;
    }

    public void setId(URI id) {
        this.id = replaceLegacyIdentifierIfNeeded(id);
    }

    private static URI replaceLegacyIdentifierIfNeeded(URI id) {
        if (isNull(id)) {
            return null;
        }
        var uriWrapper = UriWrapper.fromUri(id);
        return UIO_LEGACY_IDENTIFIER.equals(uriWrapper.getLastPathElement())
                   ? uriWrapper.replacePathElementByIndexFromEnd(0, UIO_IDENTIFIER).getUri()
                   : id;
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Organization that = (Organization) o;
        return Objects.equals(getId(), that.getId());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    public static final class Builder {

        private URI id;

        public Builder() {
        }

        public Builder withId(URI id) {
            this.id = id;
            return this;
        }

        public Organization build() {
            return new Organization(this);
        }
    }
}
