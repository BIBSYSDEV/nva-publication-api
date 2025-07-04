package cucumber.permissions.file;

import static cucumber.permissions.enums.FileEmbargoConfig.HAS_EMBARGO;
import static java.time.temporal.ChronoUnit.DAYS;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import cucumber.permissions.enums.FileEmbargoConfig;
import cucumber.permissions.enums.FileOwnerConfig;
import cucumber.permissions.publication.PublicationScenarioContext;
import java.time.Instant;
import java.util.Collections;
import no.unit.nva.model.FileOperation;
import no.unit.nva.model.Username;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.File.Builder;
import no.unit.nva.model.associatedartifacts.file.UploadedFile;
import no.unit.nva.model.associatedartifacts.file.UserUploadDetails;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissions.file.FilePermissions;

public final class FileScenarioContext {

    private final PublicationScenarioContext publicationScenarioContext;

    private FileOperation fileOperation;
    private Class<File> fileType;
    private FileOwnerConfig fileOwnerConfig = FileOwnerConfig.PUBLICATION_CREATOR;
    private FileEmbargoConfig fileEmbargoConfig = FileEmbargoConfig.HAS_NO_EMBARGO;

    public FileScenarioContext(PublicationScenarioContext publicationScenarioContext) {
        this.publicationScenarioContext = publicationScenarioContext;
    }

    public Class<File> getFileClassFromString() {
        return fileType;
    }

    public void setFileType(String fileType) throws ClassNotFoundException {
        this.fileType = getFileClassFromString(fileType);
    }

    public FileOwnerConfig getFileOwnerConfig() {
        return fileOwnerConfig;
    }

    public void setFileOwnerConfig(FileOwnerConfig fileOwner) {
        this.fileOwnerConfig = fileOwner;
    }

    public FileOperation getFileOperation() {
        return fileOperation;
    }

    public void setFileOperation(FileOperation action) {
        this.fileOperation = action;
    }

    public FileEmbargoConfig getFileEmbargoConfig() {
        return fileEmbargoConfig;
    }

    public void setFileEmbargoConfig(FileEmbargoConfig fileEmbargoConfig) {
        this.fileEmbargoConfig = fileEmbargoConfig;
    }

    @SuppressWarnings("unchecked")
    private Class<File> getFileClassFromString(String fileType) throws ClassNotFoundException {
        return (Class<File>) Class.forName(File.class.getPackageName() + "." + fileType);
    }

    private FileEntry createFileEntry(Resource resource, UserInstance userInstance) {
        var fileOwner = getFileOwner(resource, userInstance);
        var file = createFile(fileOwner);
        return FileEntry.create(file, resource.getIdentifier(), fileOwner);
    }

    private UserInstance getFileOwner(Resource resource, UserInstance userInstance) {
        return switch (getFileOwnerConfig()) {
            case FileOwnerConfig.USER -> userInstance;
            case FileOwnerConfig.PUBLICATION_CREATOR -> UserInstance.fromPublication(resource.toPublication());
            case FileOwnerConfig.SOMEONE_ELSE -> UserInstance.create(randomString(), randomUri(), randomUri(),
                                                                     Collections.emptyList(), randomUri());
            case FileOwnerConfig.CONTRIBUTOR_AT_CURATING_INSTITUTION -> throw new UnsupportedOperationException();
        };
    }

    private File createFile(UserInstance fileOwner) {
        var userUploadDetails = new UserUploadDetails(new Username(fileOwner.getUsername()), Instant.now());
        var file = File.builder().withUploadDetails(userUploadDetails);
        if (HAS_EMBARGO.equals(getFileEmbargoConfig())) {
            addEmbargo(file, getFileClassFromString());
        }
        return file.build(getFileClassFromString());
    }

    private void addEmbargo(Builder fileBuilder, Class<File> fileType) {
        if (UploadedFile.class.equals(fileType)) {
            throw new IllegalArgumentException("'UploadedFile.class' does not support embargo");
        }
        fileBuilder.withEmbargoDate(Instant.now().plus(100, DAYS));
    }

    public FilePermissions getFilePermissions() {
        var resource = publicationScenarioContext.createResource();
        var userInstance = publicationScenarioContext.getUserInstance();
        var fileEntry = createFileEntry(resource, userInstance);

        // Add file to resource?

        return new FilePermissions(fileEntry, userInstance, resource);
    }
}
