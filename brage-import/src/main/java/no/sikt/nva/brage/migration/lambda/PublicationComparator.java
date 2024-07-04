package no.sikt.nva.brage.migration.lambda;

import static java.util.Objects.isNull;
import java.util.List;
import java.util.stream.Stream;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Publication;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.event.Lecture;
import no.unit.nva.model.instancetypes.report.ConferenceReport;
import no.unit.nva.model.pages.Pages;
import org.apache.commons.text.similarity.LevenshteinDistance;

public final class PublicationComparator {

    public static final int MAX_ACCEPTABLE_TITLE_LEVENSHTEIN_DISTANCE = 10;
    public static final int MAX_ACCEPTABLE_CONTRIBUTOR_NAME_LEVENSHTEIN_DISTANCE = 5;
    public static final int ALLOWED_PUBLICATION_YEAR_DIFFERENCE = 2;
    private static final LevenshteinDistance LEVENSHTEIN_DISTANCE = LevenshteinDistance.getDefaultInstance();

    private PublicationComparator() {
    }

    public static boolean publicationsMatch(Publication existingPublication, Publication  incomingPublication) {
        var titlesMatch = titlesMatch(existingPublication, incomingPublication);
        var atLeastOneContributorMatch = contributorsMatch(existingPublication, incomingPublication);
        var publicationDatesAreCloseToEachOther = publicationsDateAreClose(existingPublication,
                                                                               incomingPublication);
        var publicationContextTypeMatches = publicationContextTypeMatches(existingPublication, incomingPublication);
        return Stream.of(titlesMatch, atLeastOneContributorMatch, publicationDatesAreCloseToEachOther,
                         publicationContextTypeMatches).allMatch(Boolean::valueOf);
    }

    private static boolean publicationContextTypeMatches(Publication existingPublication,
                                                         Publication incomingPublication) {
        if (typesAreMissing(incomingPublication)) {
            return true;
        }
        var existingPublicationContext = getPublicationContext(existingPublication);
        var incomingPublicationContext = getPublicationContext(incomingPublication);
        var existingPublicationInstance = getPublicationInstance(existingPublication);
        var incomingPublicationInstance = getPublicationInstance(incomingPublication);
        if (incomingPublicationInstance instanceof ConferenceReport) {
            return existingPublicationInstance instanceof Lecture
                || existingPublicationContext.equals(incomingPublicationContext);
        } else {
            return existingPublicationContext.equals(incomingPublicationContext);
        }
    }

    private static boolean typesAreMissing(Publication incomingPublication) {
        return isNull(incomingPublication.getEntityDescription().getReference().getPublicationContext())
               && isNull(incomingPublication.getEntityDescription().getReference().getPublicationInstance());
    }

    private static PublicationInstance<? extends Pages> getPublicationInstance(Publication publication) {
        return publication.getEntityDescription().getReference().getPublicationInstance();
    }

    private static Class<? extends PublicationContext> getPublicationContext(Publication incomingPublication) {
        return incomingPublication.getEntityDescription().getReference().getPublicationContext().getClass();
    }

    private static boolean publicationsDateAreClose(Publication existingPublication, Publication incomingPublication) {
        var existingPublicationDate = existingPublication.getEntityDescription().getPublicationDate();
        var incomingPublicationDate = incomingPublication.getEntityDescription().getPublicationDate();
        var difference =
            Math.abs(Integer.parseInt(existingPublicationDate.getYear())
                     - Integer.parseInt(incomingPublicationDate.getYear()));
        return difference <= ALLOWED_PUBLICATION_YEAR_DIFFERENCE;
    }

    private static boolean contributorsMatch(Publication existingPublication, Publication incomingPublication) {
        var existingContributors = existingPublication.getEntityDescription().getContributors();
        if (existingContributors.isEmpty()) {
            return true;
        }
        return atleasOneContributorMatch(incomingPublication, existingContributors);
    }

    private static boolean atleasOneContributorMatch(Publication incomingPublication,
                                                     List<Contributor> existingContributors) {
        var incomingContributors = incomingPublication.getEntityDescription().getContributors();
        return existingContributors.stream()
                   .flatMap(contributor -> containsContributor(incomingContributors, contributor))
                   .findAny().isPresent();
    }

    private static Stream<String> containsContributor(List<Contributor> incomingContributors,
                                                      Contributor contributor) {
        var name = contributor.getIdentity().getName();
        return incomingContributors.stream()
                   .map(Contributor::getIdentity)
                   .map(Identity::getName)
                   .filter(value -> levenshteinDistanceIsLessThan(
                       value,
                       name,
                       MAX_ACCEPTABLE_CONTRIBUTOR_NAME_LEVENSHTEIN_DISTANCE)
                   );
    }

    private static boolean levenshteinDistanceIsLessThan(String left, String right, int distance) {
        return LEVENSHTEIN_DISTANCE.apply(left, right) <= distance;
    }

    private static boolean titlesMatch(Publication existingPublication, Publication incomingPublication) {
        var existingPublicationTitle = existingPublication.getEntityDescription().getMainTitle();
        var incomingPublicationTitle = incomingPublication.getEntityDescription().getMainTitle();
        return levenshteinDistanceIsLessThan(existingPublicationTitle,
                                             incomingPublicationTitle,
                                             MAX_ACCEPTABLE_TITLE_LEVENSHTEIN_DISTANCE);
    }
}
