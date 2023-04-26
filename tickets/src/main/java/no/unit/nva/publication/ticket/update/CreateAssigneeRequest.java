package no.unit.nva.publication.ticket.update;

import no.unit.nva.publication.model.business.User;

public class CreateAssigneeRequest {
    
    private User assignee;
    
    public User getAssignee() {
        return assignee;
    }
    
    public void setAssignee(User assignee) {
        this.assignee = assignee;
    }
}
