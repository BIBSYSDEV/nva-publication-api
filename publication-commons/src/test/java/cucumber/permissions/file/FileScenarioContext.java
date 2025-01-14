package cucumber.permissions.file;

import no.unit.nva.model.FileOperation;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.publication.model.business.UserInstance;

public class FileScenarioContext {

    private File file;
    private UserInstance user;
    private FileOperation fileOperation;

    public void setFile(File file) {
        this.file = file;
    }

    public File getFile() {
        return file;
    }

    public void setUser(UserInstance user) {
        this.user = user;
    }

    public UserInstance getUser() {
        return user;
    }

    public void setFileOperation(FileOperation action) {
        this.fileOperation = action;
    }

    public FileOperation getFileOperation() {
        return fileOperation;
    }
}
