package no.sikt.nva.brage.migration.rollback;

public class NotFoundException extends RuntimeException {

  public NotFoundException(String message) {
    super(message);
  }
}
