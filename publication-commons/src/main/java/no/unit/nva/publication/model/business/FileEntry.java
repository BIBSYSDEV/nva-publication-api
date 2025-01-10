package no.unit.nva.publication.model.business;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.time.Instant;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.associatedartifacts.file.File;
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
    private final File file;
    private final Instant createdDate;
    private final Instant modifiedDate;

    private FileEntry(SortableIdentifier resourceIdentifier, Instant createdDate, Instant modifiedDate, User owner,
                      URI ownerAffiliation, URI customerId, File file) {
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

    public SortableIdentifier getResourceIdentifier() {
        return resourceIdentifier;
    }

    @Override
    public SortableIdentifier getIdentifier() {
        return new SortableIdentifier(file.getIdentifier().toString());
    }

    @JacocoGenerated
    @Override
    public void setIdentifier(SortableIdentifier identifier) {
        throw new UnsupportedOperationException(DO_NOT_USE_THIS_METHOD);
    }

    //TODO: Implement once we implement database logic
    @JacocoGenerated
    @Override
    public Publication toPublication(ResourceService resourceService) {
        return null;
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
    @Override
    public void setCreatedDate(Instant now) {
        throw new UnsupportedOperationException(DO_NOT_USE_THIS_METHOD);
    }

    @Override
    public Instant getModifiedDate() {
        return modifiedDate;
    }

    @JacocoGenerated
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
}
