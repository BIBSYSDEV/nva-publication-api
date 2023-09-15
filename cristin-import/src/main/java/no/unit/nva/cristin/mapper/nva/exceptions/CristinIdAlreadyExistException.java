package no.unit.nva.cristin.mapper.nva.exceptions;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CristinIdAlreadyExistException extends RuntimeException {

    private static final String MESSAGE_FORMAT =
        "NVA publications with identifier %s contains the cristin identifier %s";

    public CristinIdAlreadyExistException(String cristinId, Stream<String> nvaPublicationIdentifiers) {
        super(String.format(MESSAGE_FORMAT, nvaPublicationIdentifiers.collect(Collectors.joining()), cristinId));
    }
}
