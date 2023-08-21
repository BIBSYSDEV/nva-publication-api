package no.unit.nva.cristin.mapper;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.net.URI;

@Data
@Builder(
    builderClassName = "CristinAssociatedUriBuilder",
    toBuilder = true,
    builderMethodName = "builder",
    buildMethodName = "build",
    setterPrefix = "with"
)
@AllArgsConstructor(access = AccessLevel.PUBLIC)
@JsonIgnoreProperties({"sprakkode"})
public class CristinAssociatedUri {

    private static final String ARCHIVE = "ARKIV";

    @JsonProperty("urltypekode")
    private String urlType;

    @JsonProperty("url")
    private URI url;


    @JsonIgnore
    public boolean isArchive() {
        return ARCHIVE.equalsIgnoreCase(getUrlType());
    }
}
