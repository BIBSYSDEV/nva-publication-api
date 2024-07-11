package no.unit.nva.model;

import java.net.URI;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

class Project {

    private URI id;
    private String name;

    public Project() {
    }

    public URI getId() {
        return id;
    }

    public void setId(URI id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Project)) {
            return false;
        }
        Project project = (Project) o;
        return Objects.equals(getId(), project.getId())
               && Objects.equals(getName(), project.getName());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getId(), getName());
    }
}
