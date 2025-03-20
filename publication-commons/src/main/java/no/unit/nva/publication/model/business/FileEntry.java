package no.unit.nva.publication.model.business;

import static java.util.Objects.nonNull;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.ImportSource;
import no.unit.nva.model.Publication;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.HiddenFile;
import no.unit.nva.model.associatedartifacts.file.InternalFile;
import no.unit.nva.model.associatedartifacts.file.OpenFile;
import no.unit.nva.model.associatedartifacts.file.PendingFile;
import no.unit.nva.publication.model.business.publicationstate.FileApprovedEvent;
import no.unit.nva.publication.model.business.publicationstate.FileDeletedEvent;
import no.unit.nva.publication.model.business.publicationstate.FileEvent;
import no.unit.nva.publication.model.business.publicationstate.FileHiddenEvent;
import no.unit.nva.publication.model.business.publicationstate.FileImportedEvent;
import no.unit.nva.publication.model.business.publicationstate.FileRejectedEvent;
import no.unit.nva.publication.model.business.publicationstate.FileRetractedEvent;
import no.unit.nva.publication.model.business.publicationstate.FileUploadedEvent;
import no.unit.nva.publication.model.storage.FileDao;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.core.JacocoGenerated;

@JsonTypeName(FileEntry.TYPE)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public final class FileEntry implements Entity, QueryObject<FileEntry> {

    public static final String TYPE = "File";
    public static final String DO_NOT_USE_THIS_METHOD = "Do not use this method";
    private final SortableIdentifier resourceIdentifier;
    private final User owner;
    private final URI ownerAffiliation;
    private final URI customerId;
    private final Instant createdDate;
    private Instant modifiedDate;
    private File file;
    private FileEvent fileEvent;

    /**
     * Constructor for FileEntry.
     * @param resourceIdentifier
     * @param createdDate
     * @param modifiedDate
     * @param owner
     * @param ownerAffiliation Top level cristin unit id
     * @param customerId
     * @param file
     * @param fileEvent
     */
    @JsonCreator
    private FileEntry(@JsonProperty("resourceIdentifier") SortableIdentifier resourceIdentifier,
                      @JsonProperty("createdDate") Instant createdDate,
                      @JsonProperty("modifiedDate") Instant modifiedDate, @JsonProperty("owner") User owner,
                      @JsonProperty("ownerAffiliation") URI ownerAffiliation,
                      @JsonProperty("customerId") URI customerId, @JsonProperty("file") File file,
                      @JsonProperty("fileEvent") FileEvent fileEvent) {
        this.resourceIdentifier = resourceIdentifier;
        this.createdDate = createdDate;
        this.modifiedDate = modifiedDate;
        this.owner = owner;
        this.ownerAffiliation = ownerAffiliation;
        this.customerId = customerId;
        this.file = file;
        this.fileEvent = fileEvent;
    }

    public static FileEntry create(File file, SortableIdentifier resourceIdentifier, UserInstance userInstance) {
        return new FileEntry(resourceIdentifier, Instant.now(), Instant.now(), userInstance.getUser(),
                             userInstance.getTopLevelOrgCristinId(), userInstance.getCustomerId(), file, null);
    }

    public static QueryObject<FileEntry> queryObject(UUID fileIdentifier, SortableIdentifier resourceIdentifier) {
        return new FileEntry(resourceIdentifier, null, null, null, null, null, File.builder()
                                                                                   .withIdentifier(UUID.fromString(
                                                                                       fileIdentifier.toString()))
                                                                                   .buildHiddenFile(), null);
    }

    public static QueryObject<FileEntry> queryObject(SortableIdentifier identifier) {
        var uuid = UUID.fromString(identifier.toString());
        var fileWithIdentifier = File.builder().withIdentifier(uuid).buildHiddenFile();
        return new FileEntry(null, null, null, null, null, null, fileWithIdentifier, null);
    }

    public static FileEntry fromDao(FileDao fileDao) {
        return (FileEntry) fileDao.getData();
    }

    public static FileEntry createFromImportSource(File file, SortableIdentifier identifier,
                                                   UserInstance userInstance, ImportSource importSource) {
        var fileEntry = create(file, identifier, userInstance);
        fileEntry.setFileEvent(FileImportedEvent.create(userInstance.getUser(), Instant.now(), importSource));
        return fileEntry;
    }

    public void persist(ResourceService resourceService) {
        var now = Instant.now();
        this.modifiedDate = now;
        this.setFileEvent(FileUploadedEvent.create(getOwner(), now));
        resourceService.persistFile(this);
    }

    public Optional<FileEntry> fetch(ResourceService resourceService) {
        return resourceService.fetchFile(this);
    }

    public SortableIdentifier getResourceIdentifier() {
        return resourceIdentifier;
    }

    @Override
    public SortableIdentifier getIdentifier() {
        return new SortableIdentifier(file.getIdentifier().toString());
    }

    @JacocoGenerated
    @JsonIgnore
    @Override
    public void setIdentifier(SortableIdentifier identifier) {
        throw new UnsupportedOperationException(DO_NOT_USE_THIS_METHOD);
    }

    @Override
    public Publication toPublication(ResourceService resourceService) {
        return attempt(() -> resourceService.getPublicationByIdentifier(getResourceIdentifier())).orElseThrow();
    }

    @JacocoGenerated
    @Override
    public String getType() {
        return TYPE;
    }

    @JacocoGenerated
    @Override
    public Instant getCreatedDate() {
        return createdDate;
    }

    @JacocoGenerated
    @JsonIgnore
    @Override
    public void setCreatedDate(Instant now) {
        throw new UnsupportedOperationException(DO_NOT_USE_THIS_METHOD);
    }

    @Override
    public Instant getModifiedDate() {
        return modifiedDate;
    }

    @JacocoGenerated
    @JsonIgnore
    @Override
    public void setModifiedDate(Instant now) {
        throw new UnsupportedOperationException(DO_NOT_USE_THIS_METHOD);
    }

    @Override
    public User getOwner() {
        return owner;
    }

    @Override
    public URI getCustomerId() {
        return customerId;
    }

    @Override
    public FileDao toDao() {
        return FileDao.fromFileEntry(this);
    }

    @JacocoGenerated
    @Override
    public String getStatusString() {
        return "NO_STATUS";
    }

    public File getFile() {
        return file;
    }

    public URI getOwnerAffiliation() {
        return ownerAffiliation;
    }

    public void softDelete(ResourceService resourceService, User user) {
        var fileEntry = this.softDelete(user);
        resourceService.updateFile(fileEntry);
    }

    public FileEntry softDelete(User user) {
        var now = Instant.now();
        this.setFileEvent(FileDeletedEvent.create(user, now));
        this.modifiedDate = now;
        return this;
    }

    public void delete(ResourceService resourceIdentifier) {
        resourceIdentifier.deleteFile(this);
    }

    public void update(File file, UserInstance userInstance, ResourceService resourceService) {
        update(file, userInstance);
        resourceService.updateFile(this);
    }

    public FileEntry update(File file, UserInstance userInstance) {
        if (!this.file.canBeConvertedTo(file)) {
            throw new IllegalStateException("%s cannot be updated to %s"
                                                .formatted(this.file.getClass().getSimpleName(),
                                                           file.getClass().getSimpleName()));
        }
        if (shouldRetractFile(file)) {
            this.setFileEvent(FileRetractedEvent.create(userInstance.getUser(), Instant.now()));
        }
        if (shouldHideFile(file)) {
            this.setFileEvent(FileHiddenEvent.create(userInstance.getUser(), Instant.now()));
        }
        if (!file.equals(this.file)) {
            this.file = this.file.copy()
                            .withPublisherVersion(file.getPublisherVersion())
                            .withLicense(file.getLicense())
                            .withEmbargoDate(file.getEmbargoDate().orElse(null))
                            .withLegalNote(file.getLegalNote())
                            .withRightsRetentionStrategy(file.getRightsRetentionStrategy())
                            .build(file.getClass());
            this.modifiedDate = Instant.now();
        }
        return this;
    }

    public void importNew(ResourceService resourceService,UserInstance userInstance, ImportSource importSource) {
        var now = Instant.now();
        this.modifiedDate = now;
        this.setFileEvent(FileImportedEvent.create(userInstance.getUser(), Instant.now(), importSource));
        resourceService.persistFile(this);
    }

    private boolean shouldHideFile(File file) {
        return !(this.file instanceof HiddenFile) && file instanceof HiddenFile;
    }

    private boolean shouldRetractFile(File file) {
        return
            (this.file instanceof OpenFile || this.file instanceof InternalFile || this.file instanceof HiddenFile) &&
            file instanceof PendingFile<?, ?>;
    }

    public void approve(ResourceService resourceService, User user) {
        if (file instanceof PendingFile<?,?> pendingFile) {
            this.file = pendingFile.approve();
            var now = Instant.now();
            this.modifiedDate = now;
            this.setFileEvent(FileApprovedEvent.create(user, now));
            resourceService.updateFile(this);
        }
    }

    public void reject(ResourceService resourceService, User user) {
        if (file instanceof PendingFile<?,?> pendingFile) {
            var now = Instant.now();
            this.setFileEvent(FileRejectedEvent.create(user, now, file.getArtifactType()));
            this.modifiedDate = now;
            this.file = pendingFile.reject();
            resourceService.updateFile(this);
        }
    }

    @JsonIgnore
    public boolean hasFileEvent() {
        return nonNull(getFileEvent());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getResourceIdentifier(), getOwner(), getOwnerAffiliation(), getCustomerId(),
                            getCreatedDate(), getModifiedDate(), getFile());
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof FileEntry fileEntry)) {
            return false;
        }
        return Objects.equals(getResourceIdentifier(), fileEntry.getResourceIdentifier()) &&
               Objects.equals(getOwner(), fileEntry.getOwner()) &&
               Objects.equals(getOwnerAffiliation(), fileEntry.getOwnerAffiliation()) &&
               Objects.equals(getCustomerId(), fileEntry.getCustomerId()) &&
               Objects.equals(getCreatedDate(), fileEntry.getCreatedDate()) &&
               Objects.equals(getModifiedDate(), fileEntry.getModifiedDate()) &&
               Objects.equals(getFile(), fileEntry.getFile());
    }

    public FileEvent getFileEvent() {
        return fileEvent;
    }

    // TODO: Make method private after we have crafted log entries for files
    public void setFileEvent(FileEvent fileEvent) {
        this.fileEvent = fileEvent;
    }
}
