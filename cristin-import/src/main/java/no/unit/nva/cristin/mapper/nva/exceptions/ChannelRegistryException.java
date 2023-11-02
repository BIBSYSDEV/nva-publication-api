package no.unit.nva.cristin.mapper.nva.exceptions;

public class ChannelRegistryException extends RuntimeException {

    private static final String MESSAGE_FORMAT = "Pid for NSD code %d not found";

    public ChannelRegistryException(int nsdCode) {
        super(String.format(MESSAGE_FORMAT, nsdCode));
    }
}
