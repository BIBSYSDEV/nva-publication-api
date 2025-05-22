package no.unit.nva.cristin.mapper.nva.exceptions;

    public final class UnconfirmedJournalException extends RuntimeException {

        private UnconfirmedJournalException() {
            super();
        }

        public static String name() {
            return UnconfirmedJournalException.class.getSimpleName();
        }
}
