package no.unit.nva.model.instancetypes.artistic.music;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(name = "AudioVisualPublication", value = AudioVisualPublication.class),
    @JsonSubTypes.Type(name = "Concert", value = Concert.class),
    @JsonSubTypes.Type(name = "MusicScore", value = MusicScore.class),
    @JsonSubTypes.Type(name = "OtherPerformance", value = OtherPerformance.class)
})
public interface MusicPerformanceManifestation {

}
