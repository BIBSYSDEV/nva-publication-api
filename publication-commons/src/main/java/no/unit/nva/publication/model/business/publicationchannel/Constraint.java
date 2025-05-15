package no.unit.nva.publication.model.business.publicationchannel;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;

@JsonTypeName(Constraint.TYPE)
public record Constraint(ChannelPolicy publishingPolicy, ChannelPolicy editingPolicy, List<String> scope) {

    static final String TYPE = "Constraint";
}
