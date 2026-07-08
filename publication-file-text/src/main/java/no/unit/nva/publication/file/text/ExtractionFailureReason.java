package no.unit.nva.publication.file.text;

public enum ExtractionFailureReason {
  UNSUPPORTED_FORMAT,
  PASSWORD_PROTECTED,
  BLANK_CONTENT,
  PARSE_ERROR,
  TRUNCATED_CONTENT
}
