package no.unit.nva.publication;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static nva.commons.core.attempt.Try.attempt;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.nio.file.Path;
import no.unit.nva.clients.CustomerDto;
import nva.commons.core.ioutils.IoUtils;

public final class CustomerFixtures {

  private static final String ID_FIELD = "id";

  private CustomerFixtures() {}

  public static CustomerDto customerAcceptingFilesForAllTypes(URI customerId) {
    return customerFromResource(
        "customerWithAllTypesAllowingFileAndAllowingAutoApprovalOfPublishingRequests.json",
        customerId);
  }

  public static CustomerDto customerAcceptingFilesForAllTypesNotAllowingAutoPublishingFiles(
      URI customerId) {
    return customerFromResource(
        "customerWithAllTypesAllowingFileAndNotAllowingAutoApprovalOfPublishingRequests.json",
        customerId);
  }

  public static CustomerDto customerAcceptingFilesForAllTypesAndOverridableRrs(URI customerId) {
    return customerFromResource(
        "customerWithAllTypesAllowingFileAndAllowingAutoApprovalOfPublishingRequestsOverridableRrs.json",
        customerId);
  }

  public static CustomerDto customerAcceptingFilesForNoTypes(URI customerId) {
    return customerFromResource(
        "customerWithNoTypesAllowingFileAndAllowingAutoApprovalOfPublishingRequests.json",
        customerId);
  }

  private static CustomerDto customerFromResource(String resourceName, URI customerId) {
    var json = IoUtils.stringFromResources(Path.of(resourceName));
    return attempt(() -> (ObjectNode) dtoObjectMapper.readTree(json))
        .map(customer -> customer.put(ID_FIELD, customerId.toString()))
        .map(customer -> dtoObjectMapper.treeToValue(customer, CustomerDto.class))
        .orElseThrow();
  }
}
