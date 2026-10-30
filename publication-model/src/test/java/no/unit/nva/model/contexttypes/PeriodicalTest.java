package no.unit.nva.model.contexttypes;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import no.unit.nva.model.exceptions.InvalidIssnException;
import org.junit.jupiter.api.Test;

class PeriodicalTest {

  private static final String WITH_ID_METHOD_NAME = "withId";
  private static final String CONFIRM_METHOD_NAME = "confirm";

  @Test
  void withIdShouldKeepJournalType() {
    var newId = randomUri();

    var updated = new Journal(randomUri()).withId(newId);

    assertEquals(Journal.class, updated.getClass());
    assertEquals(newId, updated.getId());
  }

  @Test
  void withIdShouldKeepMediaContributionPeriodicalType() {
    var newId = randomUri();

    var updated = new MediaContributionPeriodical(randomUri()).withId(newId);

    assertEquals(MediaContributionPeriodical.class, updated.getClass());
    assertEquals(newId, updated.getId());
  }

  @Test
  void confirmShouldTurnUnconfirmedJournalIntoJournal() throws InvalidIssnException {
    var id = randomUri();

    var confirmed = new UnconfirmedJournal(randomString(), null, null).confirm(id);

    assertEquals(Journal.class, confirmed.getClass());
    assertEquals(id, confirmed.getId());
  }

  @Test
  void confirmShouldTurnUnconfirmedMediaContributionPeriodicalIntoMediaContributionPeriodical()
      throws InvalidIssnException {
    var id = randomUri();

    var confirmed =
        new UnconfirmedMediaContributionPeriodical(randomString(), null, null).confirm(id);

    assertEquals(MediaContributionPeriodical.class, confirmed.getClass());
    assertEquals(id, confirmed.getId());
  }

  @Test
  void everyRegisteredJournalSubtypeShouldOverrideWithIdToKeepItsType() {
    var journalSubtypes = registeredSubtypesOf(Journal.class);

    assertThat(journalSubtypes, hasItem(MediaContributionPeriodical.class));
    assertThat(
        journalSubtypes.stream().filter(type -> !declares(type, WITH_ID_METHOD_NAME)).toList(),
        is(empty()));
  }

  @Test
  void everyRegisteredUnconfirmedJournalSubtypeShouldOverrideConfirmToKeepItsType() {
    var unconfirmedJournalSubtypes = registeredSubtypesOf(UnconfirmedJournal.class);

    assertThat(unconfirmedJournalSubtypes, hasItem(UnconfirmedMediaContributionPeriodical.class));
    assertThat(
        unconfirmedJournalSubtypes.stream()
            .filter(type -> !declares(type, CONFIRM_METHOD_NAME))
            .toList(),
        is(empty()));
  }

  private static List<Class<?>> registeredSubtypesOf(Class<?> baseType) {
    return Arrays.stream(PublicationContext.class.getAnnotation(JsonSubTypes.class).value())
        .<Class<?>>map(JsonSubTypes.Type::value)
        .filter(baseType::isAssignableFrom)
        .filter(type -> !baseType.equals(type))
        .toList();
  }

  private static boolean declares(Class<?> type, String methodName) {
    return Arrays.stream(type.getDeclaredMethods())
        .filter(method -> methodName.equals(method.getName()))
        .anyMatch(method -> Arrays.equals(new Class<?>[] {URI.class}, method.getParameterTypes()));
  }
}
