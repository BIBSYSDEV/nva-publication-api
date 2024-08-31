package no.unit.nva.model.instancetypes.artistic.architecture;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import no.unit.nva.model.instancetypes.artistic.architecture.realization.Award;
import no.unit.nva.model.instancetypes.artistic.architecture.realization.Competition;
import no.unit.nva.model.instancetypes.artistic.architecture.realization.Exhibition;
import no.unit.nva.model.instancetypes.artistic.architecture.realization.MentionInPublication;
import no.unit.nva.model.instancetypes.realization.WithSequence;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(name = "Competition", value = Competition.class),
    @JsonSubTypes.Type(name = "MentionInPublication", value = MentionInPublication.class),
    @JsonSubTypes.Type(name = "Award", value = Award.class),
    @JsonSubTypes.Type(name = "Exhibition", value = Exhibition.class)
})
@Schema(oneOf = {Competition.class, MentionInPublication.class, Award.class, Exhibition.class})
public interface ArchitectureOutput extends WithSequence {
}
