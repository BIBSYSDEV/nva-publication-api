package no.unit.nva.doirequest.update;

import java.util.Optional;
import no.unit.nva.model.DoiRequestStatus;
import no.unit.nva.publication.exception.BadRequestException;
import nva.commons.core.JacocoGenerated;

public class ApiUpdateDoiRequest {
    
    public static final String NO_CHANGE_REQUESTED_ERROR = "You must request changes to do";
    private DoiRequestStatus doiRequestStatus;
    private String message;
    
    @JacocoGenerated
    public DoiRequestStatus getDoiRequestStatus() {
        return this.doiRequestStatus;
    }
    
    @JacocoGenerated
    public void setDoiRequestStatus(DoiRequestStatus status) {
        this.doiRequestStatus = status;
    }
    
    @JacocoGenerated
    public Optional<String> getMessage() {
        return Optional.ofNullable(message);
    }
    
    @JacocoGenerated
    public void setMessage(String message) {
        this.message = message;
    }
    
    public void validate() throws BadRequestException {
        if (doiRequestStatus == null) {
            throw noChangeRequested();
        }
    }
    
    private BadRequestException noChangeRequested() {
        return new BadRequestException(NO_CHANGE_REQUESTED_ERROR);
    }
}

