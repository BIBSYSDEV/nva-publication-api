package no.unit.nva.cristin.mapper.nva.exceptions;

public class NoPublisherException extends RuntimeException {

    public static final String ERROR_MESSAGE = "Cristin entry without publisher. Id:";

    public NoPublisherException(Integer id) {
        super(ERROR_MESSAGE + id);

    }
}
