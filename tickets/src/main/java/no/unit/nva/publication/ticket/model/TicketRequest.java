package no.unit.nva.publication.ticket.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.unit.nva.model.Username;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.ticket.update.ViewStatus;

public class TicketRequest {

    public static final String ASSIGNEE_FIELD = "assignee";
    public static final String STATUS_FIELD = "status";
    public static final String VIEW_STATUS_FIELD = "viewStatus";
    @JsonProperty(STATUS_FIELD)
    private final TicketStatus status;
    @JsonProperty(ASSIGNEE_FIELD)
    private final Username assignee;
    @JsonProperty(VIEW_STATUS_FIELD)
    private final ViewStatus viewStatus;

    @JsonCreator
    public TicketRequest(@JsonProperty(STATUS_FIELD) TicketStatus status,
                         @JsonProperty(ASSIGNEE_FIELD) Username assignee,
                         @JsonProperty(VIEW_STATUS_FIELD) ViewStatus viewStatus) {
        this.status = status;
        this.assignee = assignee;
        this.viewStatus = viewStatus;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public Username getAssignee() {
        return assignee;
    }

    public ViewStatus getViewStatus() {
        return viewStatus;
    }
}
