package no.sikt.nva.brage.migration.record;

public class UnknownCustomerException extends RuntimeException {

    public UnknownCustomerException(String customer) {
        super("Unknown customer: " + customer);
    }

}
