package no.sikt.nva.brage.migration.merger.publicationcontextmerger;

import static no.unit.nva.model.testing.EntityDescriptionBuilder.randomIdentity;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.util.Set;
import no.sikt.nva.brage.migration.record.Record;
import no.unit.nva.model.Agent;
import no.unit.nva.model.contexttypes.Journal;
import no.unit.nva.model.contexttypes.NullPublisher;
import no.unit.nva.model.contexttypes.Publisher;
import no.unit.nva.model.contexttypes.ResearchData;
import no.unit.nva.model.contexttypes.UnconfirmedPublisher;
import org.junit.jupiter.api.Test;

class ResearchDataMergerTest {

  private static final String PUBLISHER_PROPERTY = "publisher";

  @Test
  void shouldKeepExistingPublisherWhenItIsNotAPublishingHouse() {
    var existingPublisher = randomIdentity();
    var merged = merge(existingPublisher, new Publisher(randomUri()), Set.of());

    assertThat(merged.publisher(), is(equalTo(existingPublisher)));
  }

  @Test
  void
      shouldUseBragePublisherWhenPublisherIsPrioritizedAndExistingPublisherIsNotAPublishingHouse() {
    var bragePublisher = new Publisher(randomUri());
    var merged = merge(randomIdentity(), bragePublisher, Set.of(PUBLISHER_PROPERTY));

    assertThat(merged.publisher(), is(equalTo(bragePublisher)));
  }

  @Test
  void shouldKeepConfirmedExistingPublisherWhenBrageHasUnconfirmedPublisher() {
    var existingPublisher = new Publisher(randomUri());
    var merged = merge(existingPublisher, new UnconfirmedPublisher(randomString()), Set.of());

    assertThat(merged.publisher(), is(equalTo(existingPublisher)));
  }

  @Test
  void shouldUseBragePublisherWhenExistingPublisherIsEffectivelyMissing() {
    var bragePublisher = new Publisher(randomUri());
    var merged = merge(new NullPublisher(), bragePublisher, Set.of());

    assertThat(merged.publisher(), is(equalTo(bragePublisher)));
  }

  @Test
  void shouldKeepExistingResearchDataWhenIncomingContextIsNotResearchData() {
    var existingResearchData = new ResearchData(randomIdentity());
    var record = new Record();
    record.setPrioritizedProperties(Set.of());

    var merged =
        new ResearchDataMerger(record).merge(existingResearchData, new Journal(randomUri()));

    assertThat(merged, is(equalTo(existingResearchData)));
  }

  private static ResearchData merge(
      Agent existingPublisher, Agent bragePublisher, Set<String> prioritizedProperties) {
    var record = new Record();
    record.setPrioritizedProperties(prioritizedProperties);
    return new ResearchDataMerger(record)
        .merge(new ResearchData(existingPublisher), new ResearchData(bragePublisher));
  }
}
