package no.unit.nva.publication.service;

import static java.util.Collections.emptySet;
import static no.unit.nva.publication.model.business.PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_ONLY;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import no.unit.nva.publication.commons.customer.Customer;
import no.unit.nva.publication.commons.customer.CustomerApiClient;
import no.unit.nva.publication.commons.customer.CustomerNotAvailableException;

/**
 * Returns a customer with the metadata-only publishing workflow for any customer ID, unless the ID
 * has been given its own customer or made unavailable.
 */
public class FakeCustomerApiClient implements CustomerApiClient {

  public static final Customer METADATA_ONLY_CUSTOMER =
      new Customer(emptySet(), REGISTRATOR_PUBLISHES_METADATA_ONLY.getValue(), null);

  private final Map<URI, Customer> customers = new HashMap<>();
  private final Set<URI> unavailableCustomers = new HashSet<>();

  public FakeCustomerApiClient withCustomer(URI customerId, Customer customer) {
    customers.put(customerId, customer);
    return this;
  }

  public FakeCustomerApiClient withUnavailableCustomer(URI customerId) {
    unavailableCustomers.add(customerId);
    return this;
  }

  @Override
  public Customer fetch(URI customerId) {
    if (unavailableCustomers.contains(customerId)) {
      throw new CustomerNotAvailableException(customerId);
    }
    return customers.getOrDefault(customerId, METADATA_ONLY_CUSTOMER);
  }
}
