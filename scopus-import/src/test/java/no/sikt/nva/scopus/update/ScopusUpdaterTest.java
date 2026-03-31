package no.sikt.nva.scopus.update;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import no.sikt.nva.scopus.conversion.model.ImportCandidateSearchApiResponse;
import no.unit.nva.auth.uriretriever.RawContentRetriever;
import no.unit.nva.expansion.model.ExpandedImportCandidate;
import no.unit.nva.importcandidate.ImportCandidate;
import no.unit.nva.model.additionalidentifiers.ScopusIdentifier;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ScopusUpdaterTest extends ResourcesLocalTest {

  private ResourceService resourceService;
  private RawContentRetriever uriRetriever;
  private ScopusUpdater scopusUpdater;

  @BeforeEach
  void setUp() {
    super.init();
    resourceService = getResourceService(client);
    uriRetriever = mock(RawContentRetriever.class);
    scopusUpdater = new ScopusUpdater(resourceService, uriRetriever);
  }

  @Test
  void shouldUpdateAssociatedCustomersWhenUpdatingExistingImportCandidate() throws Exception {
    var oldCustomer = randomUri();
    var scopusIdentifier = ScopusIdentifier.fromValue(randomString());
    var persistedCandidate = createPersistedImportCandidate(scopusIdentifier, List.of(oldCustomer));

    var newCustomers = List.of(randomUri(), randomUri());
    var incomingCandidate = createPersistedImportCandidate(scopusIdentifier, newCustomers);

    mockSearchImportCandidateResponse(persistedCandidate);

    var result = scopusUpdater.updateImportCandidate(incomingCandidate);

    assertThat(result.getAssociatedCustomers(), containsInAnyOrder(newCustomers.toArray()));
  }

  private void mockSearchImportCandidateResponse(ImportCandidate persistedCandidate) {
    var expandedCandidate = new ExpandedImportCandidate();
    expandedCandidate.setIdentifier(uriWithIdentifier(persistedCandidate));

    var searchResponse =
        new ImportCandidateSearchApiResponse(List.of(expandedCandidate), 1).toString();

    when(uriRetriever.getRawContent(any(), any())).thenReturn(Optional.of(searchResponse));
  }

  private static URI uriWithIdentifier(ImportCandidate persistedCandidate) {
    return UriWrapper.fromUri(randomUri())
        .addChild(persistedCandidate.getIdentifier().toString())
        .getUri();
  }

  private ImportCandidate createPersistedImportCandidate(
      ScopusIdentifier scopusIdentifier, List<URI> newCustomers) {
    var importCandidate = createImportCandidates(scopusIdentifier, newCustomers);
    return resourceService.persistImportCandidate(importCandidate);
  }

  private static ImportCandidate createImportCandidates(
      ScopusIdentifier scopusIdentifier, List<URI> newCustomers) {
    return new ImportCandidate.Builder()
        .withAdditionalIdentifiers(Set.of(scopusIdentifier))
        .withAssociatedCustomers(newCustomers)
        .build();
  }
}
