package no.unit.nva.publication.model.business;

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
import no.unit.nva.model.Publication;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.PendingFile;
import no.unit.nva.publication.model.storage.Dao;
import no.unit.nva.publication.model.storage.FileDao;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.core.JacocoGenerated;

@JsonTypeName(FileEntry.TYPE)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public final class FileEntry implements Entity {

    public static final String TYPE = "File";
    public static final String DO_NOT_USE_THIS_METHOD = "Do not use this method";
    private final SortableIdentifier resourceIdentifier;
    private final User owner;
    private final URI ownerAffiliation;
    private final URI customerId;
    private final Instant createdDate;
    private Instant modifiedDate;
    private File file;

    @JsonCreator
    private FileEntry(@JsonProperty("resourceIdentifier") SortableIdentifier resourceIdentifier,
                      @JsonProperty("createdDate") Instant createdDate,
                      @JsonProperty("modifiedDate") Instant modifiedDate, @JsonProperty("owner") User owner,
                      @JsonProperty("ownerAffiliation") URI ownerAffiliation,
                      @JsonProperty("customerId") URI customerId, @JsonProperty("file") File file) {
        this.resourceIdentifier = resourceIdentifier;
        this.createdDate = createdDate;
        this.modifiedDate = modifiedDate;
        this.owner = owner;
        this.ownerAffiliation = ownerAffiliation;
        this.customerId = customerId;
        this.file = file;
    }

    public static FileEntry create(File file, SortableIdentifier resourceIdentifier, UserInstance userInstance) {
        return new FileEntry(resourceIdentifier, Instant.now(), Instant.now(), userInstance.getUser(),
                             userInstance.getTopLevelOrgCristinId(), userInstance.getCustomerId(), file);
    }

    public static FileEntry queryObject(UUID fileIdentifier, SortableIdentifier resourceIdentifier) {
        return new FileEntry(resourceIdentifier, null, null, null, null, null, File.builder()
                                                                                   .withIdentifier(UUID.fromString(
                                                                                       fileIdentifier.toString()))
                                                                                   .buildHiddenFile());
    }

    public static FileEntry fromDao(FileDao fileDao) {
        return (FileEntry) fileDao.getData();
    }

    public void persist(ResourceService resourceService) {
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
    public Dao toDao() {
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

    public void delete(ResourceService resourceIdentifier) {
        resourceIdentifier.deleteFile(this);
    }

    public void update(File fileUpdate, ResourceService resourceService) {
        if (!file.canBeConvertedTo(fileUpdate)) {
            throw new IllegalStateException("%s can not be updated to %s"
                                                .formatted(file.getClass().getSimpleName(),
                                                           fileUpdate.getClass().getSimpleName()));
        }
        if (!fileUpdate.equals(this.file)) {
            this.file = file.copy()
                .withPublisherVersion(fileUpdate.getPublisherVersion())
                .withLicense(fileUpdate.getLicense())
                .withEmbargoDate(fileUpdate.getEmbargoDate().orElse(null))
                .withLegalNote(fileUpdate.getLegalNote())
                .build(fileUpdate.getClass());
            this.modifiedDate = Instant.now();
            resourceService.updateFile(this);
        }
    }

    public void approve(ResourceService resourceService) {
        if (file instanceof PendingFile<?,?> pendingFile) {
            this.file = pendingFile.approve();
            this.modifiedDate = Instant.now();
            resourceService.updateFile(this);
        }
    }

    public void reject(ResourceService resourceService) {
        if (file instanceof PendingFile<?,?> pendingFile) {
            this.file = pendingFile.reject();
            this.modifiedDate = Instant.now();
            resourceService.updateFile(this);
        }
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
}
