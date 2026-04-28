package no.unit.nva.model.associatedartifacts.file;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

public enum FileStatus {
  UPLOADED,
  PENDING_OPEN,
  PENDING_INTERNAL,
  OPEN,
  INTERNAL,
  HIDDEN,
  REJECTED;

  public static final String CANNOT_APPROVE_FILE_WITHOUT_LICENSE =
      "Cannot approve a file without a license: %s";

  private static final Map<FileStatus, Set<FileStatus>> ALLOWED_TRANSITIONS =
      Map.of(
          UPLOADED, Set.of(PENDING_OPEN, PENDING_INTERNAL, HIDDEN),
          PENDING_OPEN, Set.of(PENDING_OPEN, PENDING_INTERNAL, HIDDEN),
          PENDING_INTERNAL, Set.of(PENDING_OPEN, PENDING_INTERNAL, HIDDEN),
          OPEN, Set.of(PENDING_OPEN, PENDING_INTERNAL, HIDDEN, OPEN),
          INTERNAL, Set.of(PENDING_OPEN, PENDING_INTERNAL, HIDDEN, INTERNAL),
          HIDDEN, Set.of(PENDING_OPEN, PENDING_INTERNAL, HIDDEN),
          REJECTED, Set.of(PENDING_OPEN, PENDING_INTERNAL, HIDDEN, REJECTED));

  public static FileStatus from(File file) {
    return switch (file) {
      case UploadedFile f -> UPLOADED;
      case PendingOpenFile f -> PENDING_OPEN;
      case PendingInternalFile f -> PENDING_INTERNAL;
      case OpenFile f -> OPEN;
      case InternalFile f -> INTERNAL;
      case HiddenFile f -> HIDDEN;
      case RejectedFile f -> REJECTED;
      default -> throw new IllegalArgumentException("Unknown file type: " + file.getClass());
    };
  }

  public static FileStatus from(Class<? extends File> fileClass) {
    if (UploadedFile.class.equals(fileClass)) return UPLOADED;
    if (PendingOpenFile.class.equals(fileClass)) return PENDING_OPEN;
    if (PendingInternalFile.class.equals(fileClass)) return PENDING_INTERNAL;
    if (OpenFile.class.equals(fileClass)) return OPEN;
    if (InternalFile.class.equals(fileClass)) return INTERNAL;
    if (HiddenFile.class.equals(fileClass)) return HIDDEN;
    if (RejectedFile.class.equals(fileClass)) return REJECTED;
    throw new IllegalArgumentException("Unknown file type: " + fileClass);
  }

  public boolean canTransitionTo(FileStatus target) {
    return ALLOWED_TRANSITIONS.get(this).contains(target);
  }

  public boolean isPending() {
    return this == PENDING_OPEN || this == PENDING_INTERNAL;
  }

  public boolean isApproved() {
    return this == OPEN || this == INTERNAL;
  }

  public boolean isFinalized() {
    return this == OPEN || this == INTERNAL || this == HIDDEN;
  }

  public FileStatus approve() {
    return switch (this) {
      case PENDING_OPEN -> OPEN;
      case PENDING_INTERNAL -> INTERNAL;
      default -> throw new IllegalStateException("Cannot approve file with status: " + this);
    };
  }

  public FileStatus reject() {
    return switch (this) {
      case PENDING_OPEN, PENDING_INTERNAL -> REJECTED;
      default -> throw new IllegalStateException("Cannot reject file with status: " + this);
    };
  }

  public File toFile(File source) {
    return switch (this) {
      case UPLOADED -> source.copy().buildUploadedFile();
      case PENDING_OPEN -> source.copy().buildPendingOpenFile();
      case PENDING_INTERNAL -> source.copy().buildPendingInternalFile();
      case OPEN ->
          source
              .copy()
              .withPublishedDate(source.getPublishedDate().orElseGet(Instant::now))
              .buildOpenFile();
      case INTERNAL ->
          source
              .copy()
              .withPublishedDate(source.getPublishedDate().orElseGet(Instant::now))
              .buildInternalFile();
      case HIDDEN -> source.copy().buildHiddenFile();
      case REJECTED -> source.copy().buildRejectedFile();
    };
  }

  public File toFile(File.Builder builder) {
    return switch (this) {
      case UPLOADED -> builder.buildUploadedFile();
      case PENDING_OPEN -> builder.buildPendingOpenFile();
      case PENDING_INTERNAL -> builder.buildPendingInternalFile();
      case OPEN -> builder.buildOpenFile();
      case INTERNAL -> builder.buildInternalFile();
      case HIDDEN -> builder.buildHiddenFile();
      case REJECTED -> builder.buildRejectedFile();
    };
  }
}
