package no.unit.nva.publication.file.text;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TempFileSupportTest {

  private static final String SWEEP_TEST_PREFIX = "temp-file-support-sweep-test-";
  private static final String SWEEP_TEST_SUFFIX = ".sweep";
  private static final String UNRELATED_SUFFIX = ".keep";

  @TempDir Path tempDir;

  @Test
  void shouldDeleteStaleTempFilesMatchingPrefixAndSuffix() throws IOException {
    var staleFile = Files.createTempFile(SWEEP_TEST_PREFIX, SWEEP_TEST_SUFFIX);

    TempFileSupport.deleteStaleTempFiles(SWEEP_TEST_PREFIX, SWEEP_TEST_SUFFIX);

    assertThat(staleFile).doesNotExist();
  }

  @Test
  void shouldLeaveFilesNotMatchingBothPrefixAndSuffixInPlace() throws IOException {
    var unrelatedFile = Files.createTempFile(SWEEP_TEST_PREFIX, UNRELATED_SUFFIX);

    TempFileSupport.deleteStaleTempFiles(SWEEP_TEST_PREFIX, SWEEP_TEST_SUFFIX);

    assertThat(unrelatedFile).exists();
    Files.deleteIfExists(unrelatedFile);
  }

  @Test
  void shouldDeleteExistingFile() throws IOException {
    var file = Files.createTempFile(tempDir, "test-", ".bin");

    TempFileSupport.deleteTempFile(file);

    assertThat(file).doesNotExist();
  }

  @Test
  void shouldDoNothingWhenPathIsNull() {
    assertThatCode(() -> TempFileSupport.deleteTempFile(null)).doesNotThrowAnyException();
  }

  @Test
  void shouldDoNothingWhenFileDoesNotExist() {
    var nonExistent = tempDir.resolve("does-not-exist.bin");

    assertThatCode(() -> TempFileSupport.deleteTempFile(nonExistent)).doesNotThrowAnyException();
  }
}
