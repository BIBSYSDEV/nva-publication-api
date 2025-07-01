package no.unit.nva.publication.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransactionFailedException extends RuntimeException {
    
    public static final String ERROR_MESSAGE = "Conflict: This error is thrown when the transaction could not be "
                                               + "completed. In most cases this is because uniqueness conditions did "
                                               + "not hold (Typically a duplicate DoiRequest or PublishingRequest)";
    private static final Logger logger = LoggerFactory.getLogger(TransactionFailedException.class);
    
    public TransactionFailedException(Exception exception) {
        super(ERROR_MESSAGE, exception);
        logger.error(exception.getMessage(), exception);
    }
}
