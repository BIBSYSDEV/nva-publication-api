package no.unit.nva;

import com.fasterxml.jackson.databind.JsonNode;

public interface WithContext extends PublicationBase {

    JsonNode getContext();

    void setContext(JsonNode context);

}
