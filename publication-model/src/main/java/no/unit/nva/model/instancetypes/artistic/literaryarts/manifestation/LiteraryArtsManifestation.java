package no.unit.nva.model.instancetypes.artistic.literaryarts.manifestation;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import no.unit.nva.model.PublicationDate;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(name = "LiteraryArtsAudioVisual", value = LiteraryArtsAudioVisual.class),
    @JsonSubTypes.Type(name = "LiteraryArtsMonograph", value = LiteraryArtsMonograph.class),
    @JsonSubTypes.Type(name = "LiteraryArtsPerformance", value = LiteraryArtsPerformance.class),
    @JsonSubTypes.Type(name = "LiteraryArtsWeb", value = LiteraryArtsWeb.class)
})
@Schema(oneOf = {LiteraryArtsAudioVisual.class, LiteraryArtsMonograph.class, LiteraryArtsPerformance.class,
    LiteraryArtsWeb.class})
public interface LiteraryArtsManifestation {
    PublicationDate getPublicationDate();
}
