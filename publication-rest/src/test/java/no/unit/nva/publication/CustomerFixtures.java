package no.unit.nva.publication;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static nva.commons.core.attempt.Try.attempt;

import java.nio.file.Path;
import no.unit.nva.clients.CustomerDto;
import nva.commons.core.ioutils.IoUtils;

public final class CustomerFixtures {

  private CustomerFixtures() {}

  public static CustomerDto customerAcceptingFilesForAllTypes() {
    return customerFromResource(
        "customerWithAllTypesAllowingFileAndAllowingAutoApprovalOfPublishingRequests.json");
  }

  public static CustomerDto customerAcceptingFilesForAllTypesNotAllowingAutoPublishingFiles() {
    return customerFromResource(
        "customerWithAllTypesAllowingFileAndNotAllowingAutoApprovalOfPublishingRequests.json");
  }

  public static CustomerDto customerAcceptingFilesForAllTypesAndOverridableRrs() {
    return customerFromResource(
        "customerWithAllTypesAllowingFileAndAllowingAutoApprovalOfPublishingRequestsOverridableRrs.json");
  }

  public static CustomerDto customerAcceptingFilesForNoTypes() {
    return customerFromResource(
        "customerWithNoTypesAllowingFileAndAllowingAutoApprovalOfPublishingRequests.json");
  }

  private static CustomerDto customerFromResource(String resourceName) {
    var json = IoUtils.stringFromResources(Path.of(resourceName));
    return attempt(() -> dtoObjectMapper.readValue(json, CustomerDto.class)).orElseThrow();
  }
}
