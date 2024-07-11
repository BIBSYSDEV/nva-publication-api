package no.unit.nva.model.instancetypes.media;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class MediaParticipationInRadioOrTv extends MediaBase {

    @JsonCreator
    public MediaParticipationInRadioOrTv() {
        super();
    }

}
