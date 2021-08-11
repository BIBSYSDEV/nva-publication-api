package no.unit.nva.cristin.lambda.dtos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import no.unit.nva.publication.s3imports.FileContentsEvent;

/**
 * Wrapper class for using generic in Lambda handler constructors.
 */

public class CristinObjectEvent extends FileContentsEvent<JsonNode> {

    @JsonCreator
    public CristinObjectEvent(@JsonProperty(FILE_URI) URI fileUri,
                              @JsonProperty(CONTENTS_FIELD) JsonNode contents) {

        super(fileUri, contents);
    }
}
