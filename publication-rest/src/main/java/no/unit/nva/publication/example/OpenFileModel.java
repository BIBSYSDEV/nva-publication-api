package no.unit.nva.publication.example;

public class OpenFileModel extends FileModel  {

    public static final String FILE_STATUS = "OpenFile";

    @Override
    public String fileStatus() {
        return FILE_STATUS;
    }

    public OpenFileModel(String name) {
        super(name);
    }
}
