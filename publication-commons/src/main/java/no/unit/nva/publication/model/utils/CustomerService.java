package no.unit.nva.publication.model.utils;

import static nva.commons.core.attempt.Try.attempt;
import no.unit.nva.auth.uriretriever.RawContentRetriever;
import no.unit.nva.commons.json.JsonUtils;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;

public class CustomerService {

    protected static final String ERROR_MESSAGE_FETCHING_CUSTOMERS = "Could not fetch customers";
    private static final String CONTENT_TYPE = "application/json";

    private final RawContentRetriever uriRetriever;

    public CustomerService(RawContentRetriever rawContentRetriever) {
        this.uriRetriever = rawContentRetriever;
    }

    public CustomerList fetchCustomers() {
        var uri = UriWrapper.fromHost(new Environment().readEnv("API_HOST")).addChild("customer").getUri();
        return uriRetriever.getRawContent(uri, CONTENT_TYPE)
                   .map(this::toCustomerResponse)
                   .map(CustomerListResponse::customers)
                   .map(CustomerList::fromCustomerResponse)
                   .orElseThrow(() -> new CustomerServiceException(ERROR_MESSAGE_FETCHING_CUSTOMERS));
    }

    private CustomerListResponse toCustomerResponse(String value) {
        return attempt(() -> JsonUtils.dtoObjectMapper.readValue(value, CustomerListResponse.class)).orElseThrow();
    }
}
