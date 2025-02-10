package no.unit.nva.publication.example;

public class ClosedFileModel extends FileModel {
    public static final String FILE_STATUS = "ClosedFile";

    @Override
    public String fileStatus() {
        return FILE_STATUS;
    }

    public ClosedFileModel(String name) {
        super(name);
    }
}
