package no.unit.nva.publication.example;

import java.util.Objects;

public abstract class FileModel {

    private final String name;

    public String name() {
        return this.name;
    }

    public abstract String fileStatus();

    public FileModel(String name) {
        this.name = name;
    }

    public FileDto toDto() {
        return new FileDto(name(), fileStatus());
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        FileModel fileModel = (FileModel) o;
        return Objects.equals(name, fileModel.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }
}
