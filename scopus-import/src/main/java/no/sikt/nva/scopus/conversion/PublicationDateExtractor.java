package no.sikt.nva.scopus.conversion;

import static nva.commons.core.attempt.Try.attempt;

import java.time.LocalDate;
import java.util.Optional;
import no.scopus.generated.DateSortTp;
import no.scopus.generated.DocTp;
import no.scopus.generated.MetaTp;
import no.scopus.generated.OpenAccessType;
import no.unit.nva.model.PublicationDate;

public final class PublicationDateExtractor {

  private PublicationDateExtractor() {}

  public static PublicationDate extractPublicationDate(DocTp docTp) {
    return getPublicationDateFromOaAccessEffectiveDate(docTp)
        .orElseGet(() -> PublicationDateExtractor.getPublicationDateFromDateSort(docTp));
  }

  private static Optional<PublicationDate> getPublicationDateFromOaAccessEffectiveDate(
      DocTp docTp) {
    return Optional.of(docTp.getMeta())
        .map(MetaTp::getOpenAccess)
        .map(OpenAccessType::getOaAccessEffectiveDate)
        .map(PublicationDateExtractor::toPublicationDate);
  }

  private static PublicationDate toPublicationDate(String value) {
    var localDate = attempt(() -> LocalDate.parse(value)).toOptional();
    return localDate.map(PublicationDateExtractor::toPublicationDate).orElse(null);
  }

  private static PublicationDate toPublicationDate(LocalDate date) {
    return new PublicationDate.Builder()
        .withYear(String.valueOf(date.getYear()))
        .withMonth(String.valueOf(date.getMonthValue()))
        .withDay(String.valueOf(date.getDayOfMonth()))
        .build();
  }

  private static PublicationDate getPublicationDateFromDateSort(DocTp docTp) {
    var dateSort = getDateSortTp(docTp);
    return new PublicationDate.Builder()
        .withDay(dateSort.getDay())
        .withMonth(dateSort.getMonth())
        .withYear(dateSort.getYear())
        .build();
  }

  /**
   * According to the "SciVerse SCOPUS CUSTOM DATA DOCUMENTATION" dateSort contains the publication
   * date if it exists, if not there are several rules to determine what's the second-best date is.
   * See "SciVerse SCOPUS CUSTOM DATA DOCUMENTATION" for details.
   */
  private static DateSortTp getDateSortTp(DocTp docTp) {
    return docTp.getItem().getItem().getProcessInfo().getDateSort();
  }
}
