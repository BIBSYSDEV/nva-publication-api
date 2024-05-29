package no.unit.nva.expansion.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName(ExpandedJournal.TYPE)
public record ExpandedJournal(URI id, String name) {

    public static final String TYPE = "Journal";
}
