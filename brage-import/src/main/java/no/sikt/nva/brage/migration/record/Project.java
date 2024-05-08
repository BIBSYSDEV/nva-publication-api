package no.sikt.nva.brage.migration.record;

import java.util.Map;
import java.util.Objects;
import no.unit.nva.model.funding.Funding;
import no.unit.nva.model.funding.FundingBuilder;

public record Project(String identifier, String name) {

    @Override
    public int hashCode() {
        return Objects.hash(identifier(), name());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Project project = (Project) o;
        return Objects.equals(identifier(), project.identifier()) && Objects.equals(name(), project.name());
    }

    public Funding toFunding() {
        return new FundingBuilder().withIdentifier(identifier).withLabels(Map.of("nb", name)).build();
    }
}
