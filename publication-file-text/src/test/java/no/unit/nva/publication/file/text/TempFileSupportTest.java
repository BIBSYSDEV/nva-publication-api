package no.unit.nva.publication.file.text;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TempFileSupportTest {

  @TempDir Path tempDir;

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
