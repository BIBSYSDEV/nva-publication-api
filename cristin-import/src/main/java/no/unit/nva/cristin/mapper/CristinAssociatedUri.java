package no.unit.nva.cristin.mapper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;
import org.apache.commons.validator.routines.UrlValidator;

@Builder(
    builderClassName = "CristinAssociatedUriBuilder",
    toBuilder = true,
    builderMethodName = "builder",
    buildMethodName = "build",
    setterPrefix = "with"
)
@Getter
@Setter
@AllArgsConstructor(access = AccessLevel.PUBLIC)
@JsonIgnoreProperties({"sprakkode"})
public class CristinAssociatedUri {

    private static final String ARCHIVE = "ARKIV";

    @JsonProperty("urltypekode")
    private String urlType;

    @JsonProperty("url")
    private String url;

    @JacocoGenerated
    public CristinAssociatedUri() {

    }

    public URI toURI() {
        return UriWrapper.fromUri(url.trim()).getUri();
    }

    @JsonIgnore
    public boolean isValidUri() {
        return UrlValidator.getInstance().isValid(url.trim());
    }

    @JsonIgnore
    public boolean isArchive() {
        return ARCHIVE.equalsIgnoreCase(getUrlType());
    }
}
