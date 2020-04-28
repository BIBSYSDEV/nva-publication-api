package no.unit.nva.publication.model;

import nva.commons.utils.JacocoGenerated;

import java.util.Objects;

public class PublishPublicationStatus {

    private final String message;
    private final Integer statusCode;

    public PublishPublicationStatus(String message, Integer statusCode) {
        this.message = message;
        this.statusCode = statusCode;
    }

    public String getMessage() {
        return message;
    }

    public Integer getStatusCode() {
        return statusCode;
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
        PublishPublicationStatus that = (PublishPublicationStatus) o;
        return Objects.equals(getMessage(), that.getMessage())
                && Objects.equals(getStatusCode(), that.getStatusCode());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getMessage(), getStatusCode());
    }
}
