package no.unit.nva.publication.service;

import static java.util.Collections.emptyList;
import static no.unit.nva.publication.model.business.PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_ONLY;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;

import java.util.List;
import java.util.UUID;
import no.unit.nva.clients.CustomerDto;
import no.unit.nva.clients.CustomerDto.RightsRetentionStrategy;
import no.unit.nva.publication.model.business.PublishingWorkflow;

/** Customers with only the fields publication-api reads set, for use with test fakes and mocks. */
public final class CustomerGenerator {

  private CustomerGenerator() {}

  public static CustomerDto customerWithWorkflow(PublishingWorkflow publishingWorkflow) {
    return customer(publishingWorkflow.getValue(), emptyList(), null);
  }

  public static CustomerDto customerWithRightsRetention(
      RightsRetentionStrategy rightsRetentionStrategy) {
    return customer(
        REGISTRATOR_PUBLISHES_METADATA_ONLY.getValue(), emptyList(), rightsRetentionStrategy);
  }

  public static CustomerDto customer(
      String publicationWorkflow,
      List<String> allowFileUploadForTypes,
      RightsRetentionStrategy rightsRetentionStrategy) {
    return new CustomerDto(
        randomUri(),
        UUID.randomUUID(),
        randomString(),
        randomString(),
        randomString(),
        randomUri(),
        publicationWorkflow,
        false,
        false,
        false,
        allowFileUploadForTypes,
        rightsRetentionStrategy,
        false,
        null);
  }
}
