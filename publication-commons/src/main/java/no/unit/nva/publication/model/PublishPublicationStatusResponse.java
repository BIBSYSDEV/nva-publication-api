package no.unit.nva.publication.model;

import java.util.Objects;
import nva.commons.core.JacocoGenerated;

public class PublishPublicationStatusResponse {
    
    private final String message;
    private final Integer statusCode;
    
    public PublishPublicationStatusResponse(String message, Integer statusCode) {
        this.message = message;
        this.statusCode = statusCode;
    }
    
    @JacocoGenerated
    public String getMessage() {
        return message;
    }
    
    @JacocoGenerated
    public Integer getStatusCode() {
        return statusCode;
    }
    
    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getMessage(), getStatusCode());
    }
    
    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PublishPublicationStatusResponse that = (PublishPublicationStatusResponse) o;
        return Objects.equals(getMessage(), that.getMessage())
               && Objects.equals(getStatusCode(), that.getStatusCode());
    }
}
