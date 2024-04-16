package no.unit.nva.cristin.mapper.nva.exceptions;

public final class ChannelRegistryException extends RuntimeException {

    private ChannelRegistryException() {}
    public static String name() {
        return ChannelRegistryException.class.getSimpleName();
    }
}
