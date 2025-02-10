package no.unit.nva.publication.example;

import com.fasterxml.jackson.annotation.JsonProperty;
public record FileDto(String name, String fileStatus) {

    @JsonProperty("type")
    public String getType() {
        return fileStatus;
    }


    public FileModel toModel() {
        return switch (fileStatus()) {
            case OpenFileModel.FILE_STATUS -> new OpenFileModel(name());
            case ClosedFileModel.FILE_STATUS -> new ClosedFileModel(name());
            default -> throw new IllegalArgumentException("Unknown file class: " + fileStatus());
        };
    }
}
