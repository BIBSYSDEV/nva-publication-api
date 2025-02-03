package cucumber.permissions.file;

import no.unit.nva.model.FileOperation;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;

public class FileScenarioContext {

    private FileEntry file;
    private UserInstance user;
    private FileOperation fileOperation;
    private Resource resource;

    public void setFile(FileEntry file) {
        this.file = file;
    }

    public FileEntry getFile() {
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

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }
}
