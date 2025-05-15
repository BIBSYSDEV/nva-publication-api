package no.unit.nva.publication.ticket;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonTypeName(UpdateTicketOwnershipRequest.TYPE)
public record UpdateTicketOwnershipRequest(URI ownerAffiliation, URI responsibilityArea) implements TicketRequest {

    public static final String TYPE = "UpdateTicketOwnershipRequest";
}
