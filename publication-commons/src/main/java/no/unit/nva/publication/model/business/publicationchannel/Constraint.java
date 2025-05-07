package no.unit.nva.publication.model.business.publicationchannel;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName(Constraint.TYPE)
public record Constraint(ChannelPolicy publishingPolicy, ChannelPolicy editingPolicy, List<String> scope) {

    static final String TYPE = "Constraint";
}
