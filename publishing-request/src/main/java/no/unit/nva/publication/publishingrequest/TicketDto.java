package no.unit.nva.publication.publishingrequest;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TicketDto {
    
    @JsonProperty("type")
    public String getType() {
        return "TicketDto";
    }
    
    public void setType() {
        // NO-OP;
        
    }
}
