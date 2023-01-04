package no.unit.nva.cristin.mapper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder(
    builderClassName = "CristinPublisherBuilder",
    toBuilder = true,
    builderMethodName = "builder",
    buildMethodName = "build",
    setterPrefix = "with"
)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@JsonIgnoreProperties({"forlagsnr"})
public class CristinPublisher {
    
    public static final String PUBLISHER_NAME = "forlagsnavn";
    public static final String NSD_CODE = "nsdkode";
    @JsonProperty(PUBLISHER_NAME)
    private String publisherName;
    @JsonProperty(NSD_CODE)
    private Integer nsdCode;

    public CristinPublisher() {

    }
}
