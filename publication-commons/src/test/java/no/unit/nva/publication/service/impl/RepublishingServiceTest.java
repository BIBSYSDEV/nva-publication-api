package no.unit.nva.publication.service.impl;

import static no.unit.nva.model.PublicationStatus.UNPUBLISHED;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static org.junit.jupiter.api.Assertions.assertThrows;

import no.unit.nva.model.Publication;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.stubs.FakeIdentityServiceClient;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.apigateway.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RepublishingServiceTest extends ResourcesLocalTest {

  private ResourceService resourceService;
  private RepublishingService republishingService;

  @BeforeEach
  void setUp() {
    super.init();
    resourceService = getResourceService(client);
    republishingService = new RepublishingService(resourceService, new FakeIdentityServiceClient());
  }

  @Test
  void shouldThrowNotFoundExceptionWhenResourceDoesNotExist() {
    var publication = unpublishedPublication();

    assertThrows(
        NotFoundException.class,
        () ->
            republishingService.republish(
                Resource.fromPublication(publication), UserInstance.fromPublication(publication)));
  }

  @Test
  void shouldThrowForbiddenExceptionWhenUserIsNotAnEditor() throws Exception {
    var publication = unpublishedPublication();
    var owner = UserInstance.fromPublication(publication);
    var persistedPublication =
        Resource.fromPublication(publication).persistNew(resourceService, owner);

    assertThrows(
        ForbiddenException.class,
        () -> republishingService.republish(Resource.fromPublication(persistedPublication), owner));
  }

  private static Publication unpublishedPublication() {
    return randomPublication().copy().withStatus(UNPUBLISHED).build();
  }
}
