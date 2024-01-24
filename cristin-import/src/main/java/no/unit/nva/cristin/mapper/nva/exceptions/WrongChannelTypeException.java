package no.unit.nva.cristin.mapper.nva.exceptions;

import no.unit.nva.cristin.mapper.channelregistry.ChannelRegistryEntry;

public class WrongChannelTypeException extends RuntimeException {

    private static final String MESSAGE_FORMAT = "Channel for PID %s has wrong value: %s";

    public WrongChannelTypeException(ChannelRegistryEntry channelRegistryEntry) {
        super(String.format(MESSAGE_FORMAT, channelRegistryEntry.id(), channelRegistryEntry.type().getValue()));
    }
}
