package no.unit.nva.publication.utils;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import nva.commons.core.paths.UriWrapper;

/**
 * Class for encapsulating the logic of selecting the most appropriate affiliation for a resource owner.
 * Given a Set of OrgUnitIds (URIs), the class finds the most specific one per institution and
 * selects a random one.
 */
public class OrgUnitId implements Comparable<OrgUnitId> {

    public static final String UNIT_LEVEL_DELIMITER = "\\.";
    private final URI unitId;
    private final List<Integer> levels;

    public OrgUnitId(URI orgUnitId) {
        this.unitId = orgUnitId;
        var cristinIdString = new UriWrapper(orgUnitId).getPath().getFilename();
        levels = Arrays.stream(cristinIdString.split(UNIT_LEVEL_DELIMITER))
            .map(Integer::parseInt)
            .collect(Collectors.toList());
    }

    public static Optional<OrgUnitId> extractMostLikelyAffiliationForUser(List<OrgUnitId> orgUnitIds) {
        var candidates = findAffiliationCandidates(orgUnitIds);
        return selectMostPromisingCandidate(candidates);
    }

    @Override
    public int compareTo(OrgUnitId that) {
        return this.level() - that.level();
    }

    public URI getUnitId() {
        return unitId;
    }

    private static Stream<OrgUnitId> findAffiliationCandidates(List<OrgUnitId> orgUnitIds) {
        var groupedByInstitution = groupByFirstLevel(orgUnitIds);
        return selectMostSpecificUnitIdPerInstitution(groupedByInstitution.values());
    }

    private static Optional<OrgUnitId> selectMostPromisingCandidate(Stream<OrgUnitId> affiliationCandidates) {
        return affiliationCandidates.findAny();
    }

    private static Map<Integer, List<OrgUnitId>> groupByFirstLevel(List<OrgUnitId> orgUnitIds) {
        return orgUnitIds.stream()
            .filter(OrgUnitId::notEmpty)
            .collect(Collectors.groupingBy(OrgUnitId::firstLevel));
    }

    private static Stream<OrgUnitId> selectMostSpecificUnitIdPerInstitution(
        Collection<List<OrgUnitId>> groupedByInstitution) {
        return groupedByInstitution
            .stream()
            .map(OrgUnitId::findMostSpecificOrgUnitId)
            .filter(Optional::isPresent)
            .map(Optional::orElseThrow);
    }

    private static Optional<OrgUnitId> findMostSpecificOrgUnitId(List<OrgUnitId> list) {
        return list.stream().max(OrgUnitId::compareTo);
    }

    private int level() {
        int maxFoundLevel = 0;
        for (int currentLevel = 0; currentLevel < levels.size(); currentLevel++) {
            if (levels.get(currentLevel) > 0) {
                maxFoundLevel = currentLevel;
            }
        }
        return maxFoundLevel;
    }

    private boolean notEmpty() {
        return !levels.isEmpty();
    }

    private Integer firstLevel() {
        return levels.get(0);
    }
}
