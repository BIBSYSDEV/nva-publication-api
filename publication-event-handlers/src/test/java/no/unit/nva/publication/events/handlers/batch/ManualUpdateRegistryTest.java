package no.unit.nva.publication.events.handlers.batch;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.Map;
import no.unit.nva.publication.service.impl.ResourceService;
import org.junit.jupiter.api.Test;

class ManualUpdateRegistryTest {

  @Test
  void everyUpdateTypeHasRegisteredUpdater() {
    assertThat(updaters().keySet(), containsInAnyOrder(ManualUpdateType.values()));
  }

  @Test
  void registeredUpdaterIsKeyedByItsOwnType() {
    updaters().forEach((type, updater) -> assertEquals(type, updater.type()));
  }

  private Map<ManualUpdateType, ManualUpdate> updaters() {
    return ManuallyUpdatePublicationUtil.updatersByType(mock(ResourceService.class));
  }
}
