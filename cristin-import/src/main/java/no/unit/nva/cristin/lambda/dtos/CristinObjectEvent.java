package no.unit.nva.cristin.lambda.dtos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.unit.nva.cristin.mapper.CristinObject;

/**
 * Wrapper class for using in Lambda functions.
 */
public class CristinObjectEvent extends FileContentsEvent<CristinObject> {

    @JsonCreator
    public CristinObjectEvent(@JsonProperty(CONTENTS_FIELD) CristinObject contents,
                              @JsonProperty(PUBLICATIONS_OWNER_FIELD) String publicationsOwner) {

        super(contents, publicationsOwner);
    }
}
