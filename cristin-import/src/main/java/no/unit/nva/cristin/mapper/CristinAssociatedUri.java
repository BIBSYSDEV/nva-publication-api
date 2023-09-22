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
import no.unit.nva.cristin.mapper.nva.exceptions.InvalidArchiveException;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;

import static nva.commons.core.attempt.Try.attempt;

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
        return attempt(() -> UriWrapper.fromUri(url).getUri())
            .orElseThrow(fail -> new InvalidArchiveException(fail.getException()));
    }


    @JsonIgnore
    public boolean isArchive() {
        return ARCHIVE.equalsIgnoreCase(getUrlType());
    }
}
