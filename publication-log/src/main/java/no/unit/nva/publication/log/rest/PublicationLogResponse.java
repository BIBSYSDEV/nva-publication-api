package no.unit.nva.publication.log.rest;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName(PublicationLogResponse.TYPE)
public record PublicationLogResponse(List<?> logEntries) {

    public static final String TYPE = "PublicationLog";
}
