package no.unit.nva.importcandidate;

import static java.util.Objects.nonNull;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("PMD.UnusedAssignment")
@JsonTypeInfo(use = Id.NAME, property = "type")
public record SourceOrganization(SourceOrganizationIdentifier identifier,
                                 Collection<String> names,
                                 String text,
                                 Country country,
                                 Address address) implements ScopusOrganization {

    public SourceOrganization {
        names = nonNull(names) ? List.copyOf(names) : Collections.emptyList();
    }
}
