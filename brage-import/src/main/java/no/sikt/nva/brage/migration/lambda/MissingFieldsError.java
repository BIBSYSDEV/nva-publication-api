package no.sikt.nva.brage.migration.lambda;

public final class MissingFieldsError {

    private MissingFieldsError() {
    }

    public static String name() {
        return MissingFieldsError.class.getSimpleName();
    }
}
