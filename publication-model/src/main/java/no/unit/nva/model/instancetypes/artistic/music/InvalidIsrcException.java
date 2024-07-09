package no.unit.nva.model.instancetypes.artistic.music;

public class InvalidIsrcException extends Exception {

    public static final String ERROR_MESSAGE_TEMPLATE = "Invalid ISRC: %s";

    public InvalidIsrcException(String invalidIsrc) {
        super(formatErrorMessage(invalidIsrc));
    }

    public static String formatErrorMessage(String invalidIsrc) {
        return String.format(ERROR_MESSAGE_TEMPLATE, invalidIsrc);
    }
}
