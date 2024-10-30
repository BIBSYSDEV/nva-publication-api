package no.unit.nva.publication.download;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.net.URI;
import java.time.Instant;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public record PresignedUriResponse(URI signedUri, Instant expires, URI shortenedVersion) {

}
