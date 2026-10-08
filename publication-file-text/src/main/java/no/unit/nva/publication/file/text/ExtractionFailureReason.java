package no.unit.nva.publication.file.text;

public enum ExtractionFailureReason {
  UNSUPPORTED_FORMAT,
  FILE_TOO_LARGE,
  PASSWORD_PROTECTED,
  IMAGE_ONLY_CONTENT,
  BLANK_CONTENT,
  PARSE_ERROR,
  TRUNCATED_CONTENT
}
