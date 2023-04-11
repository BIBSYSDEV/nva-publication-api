package no.unit.nva.publication.ticket.model.identityservice;

import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.net.http.HttpResponse;
import no.unit.nva.commons.json.JsonUtils;
import nva.commons.core.attempt.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomerTransactionResult {

    private static final Logger logger = LoggerFactory.getLogger(CustomerTransactionResult.class);
    private final Try<CustomerDto> customer;
    private final URI customerId;

    public CustomerTransactionResult(HttpResponse<String> response, URI customerId) {
        this.customer = attempt(() -> JsonUtils.dtoObjectMapper.readValue(response.body(), CustomerDto.class));
        this.customerId = customerId;
    }

    public boolean isKnownThatCustomerAllowsPublishing() {
        return customer.map(CustomerDto::customerAllowsRegistratorsToPublishDataAndMetadata)
                   .orElse(fail -> returnFalseAndLogUnsuccessfulResponse(customerId));
    }

    private boolean returnFalseAndLogUnsuccessfulResponse(URI customerId) {
        logger.warn("Could not fetch publication workflow for customer: " + customerId);
        return false;
    }

}
