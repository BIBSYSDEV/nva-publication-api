package no.unit.nva.expansion.utils;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.expansion.ResourceExpansionService;
import no.unit.nva.expansion.model.ExpandedPerson;
import no.unit.nva.model.Username;
import no.unit.nva.publication.model.business.User;

public final class ExpansionUtil {

    private ExpansionUtil() {
    }

    public static ExpandedPerson expandPerson(Username username,
                                              ResourceExpansionService expansionService) {
        return Optional.ofNullable(username)
            .map(Username::getValue)
            .map(User::new)
            .map(expansionService::expandPerson)
            .orElse(null);
    }

    public static Set<ExpandedPerson> expandPersonViewedBy(Set<User> users,
                                                           ResourceExpansionService resourceExpansionService) {
        return users.stream()
            .map(resourceExpansionService::expandPerson)
            .collect(Collectors.toSet());
    }
}