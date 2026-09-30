package no.unit.nva.model.associatedartifacts.file;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class FileStatusTest {

  @ParameterizedTest
  @EnumSource(FileStatus.class)
  void shouldAllowTransitionToSameStatus(FileStatus fileStatus) {
    assertThat(fileStatus.canTransitionTo(fileStatus), is(true));
  }
}
