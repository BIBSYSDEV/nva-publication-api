package no.unit.nva.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class ResearchProject extends Project {

    private List<Approval> approvals;

    public ResearchProject() {
        super();
    }

    private ResearchProject(Builder builder) {
        super();
        setId(builder.id);
        setName(builder.name);
        setApprovals(builder.approvals);
    }

    public List<Approval> getApprovals() {
        return Objects.nonNull(approvals) ? approvals : Collections.emptyList();
    }

    public void setApprovals(List<Approval> approvals) {
        this.approvals = approvals;
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ResearchProject)) {
            return false;
        }
        ResearchProject researchProject = (ResearchProject) o;
        return Objects.equals(getId(), researchProject.getId())
               && Objects.equals(getName(), researchProject.getName())
               && Objects.equals(getApprovals(), researchProject.getApprovals());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getId(), getName(), getApprovals());
    }

    public static final class Builder {

        private URI id;
        private String name;
        private List<Approval> approvals;

        public Builder() {
        }

        public Builder withId(URI id) {
            this.id = id;
            return this;
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withApprovals(List<Approval> approvals) {
            this.approvals = approvals;
            return this;
        }

        public ResearchProject build() {
            return new ResearchProject(this);
        }
    }
}
