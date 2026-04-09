package no.unit.nva.publication.exception;

public class CandidateAlreadyImportedException extends Exception {

  private static final String MESSAGE = "Can not update already imported candidate";

  public CandidateAlreadyImportedException() {
    super(MESSAGE);
  }
}
