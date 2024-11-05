package no.unit.nva.publication.download;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.net.URI;
import java.time.Instant;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public record PresignedUriResponse(String fileIdentifier, URI id, Instant expires, URI shortenedVersion) {

}
