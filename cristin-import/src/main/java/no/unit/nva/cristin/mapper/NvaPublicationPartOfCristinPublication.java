package no.unit.nva.cristin.mapper;

import static no.unit.nva.cristin.CristinImportConfig.cristinEntryMapper;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.model.Publication;
import nva.commons.core.JacocoGenerated;

@Data
@Builder(
    builderClassName = "NvaPublicationPartOfCristinPublicationBuilder",
    toBuilder = true,
    builderMethodName = "builder",
    buildMethodName = "build",
    setterPrefix = "with"
)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class NvaPublicationPartOfCristinPublication implements JsonSerializable {

    @JsonProperty
    private String nvaPublicationIdentifier;

    @JsonProperty
    private Publication childPublication;

    @JsonProperty
    private NvaPublicationPartOf partOf;

    @JacocoGenerated
    public NvaPublicationPartOfCristinPublication() {
    }

    public static NvaPublicationPartOfCristinPublication fromJson(String json) {
        return attempt(
            () -> cristinEntryMapper.readValue(json, NvaPublicationPartOfCristinPublication.class))
                   .orElseThrow();
    }
}
