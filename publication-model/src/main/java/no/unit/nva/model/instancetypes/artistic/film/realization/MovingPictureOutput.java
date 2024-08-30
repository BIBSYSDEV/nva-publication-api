package no.unit.nva.model.instancetypes.artistic.film.realization;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import no.unit.nva.model.instancetypes.realization.WithSequence;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(name = "Broadcast", value = Broadcast.class),
    @JsonSubTypes.Type(name = "CinematicRelease", value = CinematicRelease.class),
    @JsonSubTypes.Type(name = "OtherRelease", value = OtherRelease.class)
})
@Schema(oneOf = {Broadcast.class, CinematicRelease.class, OtherRelease.class})
public interface MovingPictureOutput extends WithSequence {

}
