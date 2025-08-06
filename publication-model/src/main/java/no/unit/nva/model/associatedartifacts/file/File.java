package no.unit.nva.model.associatedartifacts.file;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.associatedartifacts.AssociatedArtifact;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactDto;
import no.unit.nva.model.associatedartifacts.NullRightsRetentionStrategy;
import no.unit.nva.model.associatedartifacts.RightsRetentionStrategy;
import no.unit.nva.model.associatedartifacts.RightsRetentionStrategyConfiguration;
import nva.commons.core.JacocoGenerated;

/**
 * An object that represents the description of a file.
 */
@SuppressWarnings("PMD.ExcessiveParameterList")
@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonSubTypes({@JsonSubTypes.Type(name = PendingOpenFile.TYPE, value = PendingOpenFile.class),
    @JsonSubTypes.Type(name = OpenFile.TYPE, value = OpenFile.class),
    @JsonSubTypes.Type(name = InternalFile.TYPE, value = InternalFile.class),
    @JsonSubTypes.Type(name = PendingInternalFile.TYPE, value = PendingInternalFile.class),
    @JsonSubTypes.Type(name = HiddenFile.TYPE, value = HiddenFile.class),
    @JsonSubTypes.Type(name = RejectedFile.TYPE, value = RejectedFile.class),
    @JsonSubTypes.Type(name = UploadedFile.TYPE, value = UploadedFile.class)})
public abstract class File implements JsonSerializable, AssociatedArtifact {

    public static final List<URI> DEPRECATED_RIGHTS_RESERVED_LICENSES = List.of(
        URI.create("https://rightsstatements.org/page/InC/1.0/"),
        URI.create("https://rightsstatements.org/page/InC/1.0"),
        URI.create("http://rightsstatements.org/vocab/InC/1.0/"),
        URI.create("http://rightsstatements.org/vocab/inc/1.0/"),
        URI.create("https://rightsstatements.org/vocab/InC/1.0/")
    );
    public static final String IDENTIFIER_FIELD = "identifier";
    public static final String NAME_FIELD = "name";
    public static final String MIME_TYPE_FIELD = "mimeType";
    public static final String SIZE_FIELD = "size";
    public static final String LICENSE_FIELD = "license";
    public static final String PUBLISHER_VERSION_FIELD = "publisherVersion";
    public static final String EMBARGO_DATE_FIELD = "embargoDate";
    public static final String RIGHTS_RETENTION_STRATEGY = "rightsRetentionStrategy";
    public static final String PUBLISHED_DATE_FIELD = "publishedDate";
    public static final String UPLOAD_DETAILS_FIELD = "uploadDetails";
    public static final String MISSING_LICENSE = "This file public and should therefore have a license";
    public static final String LEGAL_NOTE_FIELD = "legalNote";
    public static final Set<Class<? extends File>> APPROVED_FILE_TYPES = Set.of(OpenFile.class, InternalFile.class);
    public static final Set<Class<? extends File>> FINALIZED_FILE_TYPES = Set.of(OpenFile.class, InternalFile.class,
                                                                                HiddenFile.class);
    protected static final String RIGHTS_RESERVED_LICENSE = "https://nva.sikt.no/license/copyright-act/1.0";
    @JsonProperty(IDENTIFIER_FIELD)
    private final UUID identifier;
    @JsonProperty(NAME_FIELD)
    private final String name;
    @JsonProperty(MIME_TYPE_FIELD)
    private final String mimeType;
    @JsonProperty(SIZE_FIELD)
    private final Long size;
    @JsonProperty(LICENSE_FIELD)
    private final URI license;
    @JsonProperty(PUBLISHER_VERSION_FIELD)
    private final PublisherVersion publisherVersion;
    @JsonProperty(EMBARGO_DATE_FIELD)
    private final Instant embargoDate;
    @JsonProperty(LEGAL_NOTE_FIELD)
    private final String legalNote;
    @JsonProperty(PUBLISHED_DATE_FIELD)
    private final Instant publishedDate;
    @JsonProperty(UPLOAD_DETAILS_FIELD)
    private final UploadDetails uploadDetails;
    @JsonProperty(RIGHTS_RETENTION_STRATEGY)
    private RightsRetentionStrategy rightsRetentionStrategy;

    /**
     * Constructor for no.unit.nva.file.model.File objects. A file object is valid if it has a license or is explicitly
     * marked as an administrative agreement.
     *
     * @param identifier              A UUID that identifies the file in storage
     * @param name                    The original name of the file
     * @param mimeType                The mimetype of the file
     * @param size                    The size of the file
     * @param license                 The license for the file, may be null if and only if the file is an administrative
     *                                agreement
     * @param publisherVersion        True if the file owner has publisher authority
     * @param embargoDate             The date after which the file may be published
     * @param rightsRetentionStrategy The rights retention strategy for the file
     * @param uploadDetails           Information regarding who and when inserted the file into the system
     */

    protected File(@JsonProperty(IDENTIFIER_FIELD) UUID identifier, @JsonProperty(NAME_FIELD) String name,
                   @JsonProperty(MIME_TYPE_FIELD) String mimeType, @JsonProperty(SIZE_FIELD) Long size,
                   @JsonProperty(LICENSE_FIELD) URI license,
                   @JsonProperty(PUBLISHER_VERSION_FIELD) PublisherVersion publisherVersion,
                   @JsonProperty(EMBARGO_DATE_FIELD) Instant embargoDate,
                   @JsonProperty(RIGHTS_RETENTION_STRATEGY) RightsRetentionStrategy rightsRetentionStrategy,
                   @JsonProperty(LEGAL_NOTE_FIELD) String legalNote,
                   @JsonProperty(PUBLISHED_DATE_FIELD) Instant publishedDate,
                   @JsonProperty(UPLOAD_DETAILS_FIELD) UploadDetails uploadDetails) {

        this.identifier = identifier;
        this.name = name;
        this.mimeType = mimeType;
        this.size = size;
        this.license = migrateRightsReservedLicense(license);
        this.publisherVersion = publisherVersion;
        this.embargoDate = embargoDate;
        this.rightsRetentionStrategy = assignDefaultStrategyIfNull(rightsRetentionStrategy);
        this.legalNote = legalNote;
        this.publishedDate = publishedDate;
        this.uploadDetails = uploadDetails;
    }

    private URI migrateRightsReservedLicense(URI license) {
        return nonNull(license) && DEPRECATED_RIGHTS_RESERVED_LICENSES.contains(license)
                   ? URI.create(RIGHTS_RESERVED_LICENSE)
                   : license;

    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Validate the file.
     */
    public void validate() {
        if (isNull(license)) {
            throw new MissingLicenseException(MISSING_LICENSE);
        }
    }

    public UploadDetails getUploadDetails() {
        return uploadDetails;
    }

    public Optional<Instant> getPublishedDate() {
        return Optional.ofNullable(publishedDate);
    }

    public UUID getIdentifier() {
        return identifier;
    }

    public String getName() {
        return name;
    }

    public String getMimeType() {
        return mimeType;
    }

    public Long getSize() {
        return size;
    }

    public String getLegalNote() {
        return legalNote;
    }

    public URI getLicense() {
        return license;
    }

    public boolean hasLicense() {
        return nonNull(license);
    }

    public PublisherVersion getPublisherVersion() {
        return publisherVersion;
    }

    public Optional<Instant> getEmbargoDate() {
        return Optional.ofNullable(embargoDate);
    }

    public RightsRetentionStrategy getRightsRetentionStrategy() {
        return rightsRetentionStrategy;
    }

    public void setRightsRetentionStrategy(RightsRetentionStrategy rightsRetentionStrategy) {
        this.rightsRetentionStrategy = rightsRetentionStrategy;
    }

    public boolean hasActiveEmbargo() {
        return getEmbargoDate().map(date -> !Instant.now().isAfter(date)).orElse(false);
    }

    public PendingOpenFile toPendingOpenFile() {
        return new PendingOpenFile(getIdentifier(), getName(), getMimeType(), getSize(), getLicense(),
                                   getPublisherVersion(), getEmbargoDate().orElse(null), getRightsRetentionStrategy(),
                                   getLegalNote(), getUploadDetails());
    }

    public OpenFile toOpenFile() {
        return new OpenFile(getIdentifier(), getName(), getMimeType(), getSize(), getLicense(), getPublisherVersion(),
                            getEmbargoDate().orElse(null), getRightsRetentionStrategy(), getLegalNote(), Instant.now(),
                            getUploadDetails());
    }

    public InternalFile toInternalFile() {
        return new InternalFile(getIdentifier(), getName(), getMimeType(), getSize(), getLicense(),
                                getPublisherVersion(), getEmbargoDate().orElse(null), getRightsRetentionStrategy(),
                                getLegalNote(), Instant.now(), getUploadDetails());
    }

    @JsonIgnore
    public abstract boolean isVisibleForNonOwner();
    @JsonIgnore
    public abstract boolean canBeConvertedTo(File file);

    public abstract Builder copy();

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getIdentifier(), getName(), getMimeType(), getSize(), getLicense(), getPublisherVersion(),
                            getEmbargoDate(), getRightsRetentionStrategy(), getLegalNote(), getPublishedDate(),
                            getUploadDetails());
    }

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof File file)) {
            return false;
        }
        return Objects.equals(this.getClass(), file.getClass()) &&
               Objects.equals(getIdentifier(), file.getIdentifier()) && Objects.equals(getName(), file.getName()) &&
               Objects.equals(getMimeType(), file.getMimeType()) && Objects.equals(getSize(), file.getSize()) &&
               Objects.equals(getLicense(), file.getLicense()) && getPublisherVersion() == file.getPublisherVersion() &&
               Objects.equals(getEmbargoDate(), file.getEmbargoDate()) &&
               Objects.equals(getRightsRetentionStrategy(), file.getRightsRetentionStrategy()) &&
               Objects.equals(getLegalNote(), file.getLegalNote()) &&
               Objects.equals(getPublishedDate(), file.getPublishedDate()) &&
               Objects.equals(getUploadDetails(), file.getUploadDetails());
    }

    @Override
    public String toString() {
        return toJsonString();
    }

    @Override
    public AssociatedArtifactDto toDto() {
        return FileDto.builder()
                   .withType(getArtifactType())
                   .withIdentifier(new SortableIdentifier(getIdentifier().toString()))
                   .withName(getName())
                   .withMimeType(getMimeType())
                   .withSize(getSize())
                   .withLicense(getLicense())
                   .withPublisherVersion(getPublisherVersion())
                   .withEmbargoDate(getEmbargoDate().orElse(null))
                   .withRightsRetentionStrategy(getRightsRetentionStrategy())
                   .withLegalNote(getLegalNote())
                   .withPublishedDate(getPublishedDate().orElse(null))
                   .withUploadDetails(getUploadDetails())
                   .build();
    }

    /**
     * Assigns a default RightsRetentionStrategy if the provided strategy is null. The default strategy is an instance
     * of NullRightsRetentionStrategy.
     *
     * @param strategy The RightsRetentionStrategy to be checked.
     * @return The provided strategy if it's not null, or a new NullRightsRetentionStrategy otherwise.
     */
    private RightsRetentionStrategy assignDefaultStrategyIfNull(RightsRetentionStrategy strategy) {
        return nonNull(strategy) ? strategy
                   : NullRightsRetentionStrategy.create(RightsRetentionStrategyConfiguration.UNKNOWN);
    }

    public static final class Builder {

        private UUID identifier;
        private String name;
        private String mimeType;
        private Long size;
        private URI license;
        private PublisherVersion publisherVersion;
        private Instant embargoDate;
        private RightsRetentionStrategy rightsRetentionStrategy;
        private String legalNote;
        private UploadDetails uploadDetails;

        private Builder() {
        }

        public Builder withIdentifier(UUID identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withMimeType(String mimeType) {
            this.mimeType = mimeType;
            return this;
        }

        public Builder withSize(Long size) {
            this.size = size;
            return this;
        }

        public Builder withLicense(URI license) {
            this.license = license;
            return this;
        }

        public Builder withUploadDetails(UploadDetails uploadDetails) {
            this.uploadDetails = uploadDetails;
            return this;
        }

        public Builder withPublisherVersion(PublisherVersion publisherVersion) {
            this.publisherVersion = publisherVersion;
            return this;
        }

        public Builder withEmbargoDate(Instant embargoDate) {
            this.embargoDate = embargoDate;
            return this;
        }

        public Builder withRightsRetentionStrategy(RightsRetentionStrategy rightsRetentionStrategy) {
            this.rightsRetentionStrategy = rightsRetentionStrategy;
            return this;
        }

        public Builder withLegalNote(String legalNote) {
            this.legalNote = legalNote;
            return this;
        }

        public File buildOpenFile() {
            return new OpenFile(identifier, name, mimeType, size, license, publisherVersion,
                                embargoDate, rightsRetentionStrategy, legalNote, Instant.now(), uploadDetails);
        }

        public File buildInternalFile() {
            return new InternalFile(identifier, name, mimeType, size, license,
                                    publisherVersion, embargoDate, rightsRetentionStrategy, legalNote, Instant.now(),
                                    uploadDetails);
        }

        public File buildPendingOpenFile() {
            return new PendingOpenFile(identifier, name, mimeType, size, license,
                                       publisherVersion, embargoDate, rightsRetentionStrategy, legalNote,
                                       uploadDetails);
        }

        public File buildPendingInternalFile() {
            return new PendingInternalFile(identifier, name, mimeType, size, license,
                                           publisherVersion, embargoDate, rightsRetentionStrategy, legalNote,
                                           uploadDetails);
        }

        public File buildRejectedFile() {
            return new RejectedFile(identifier, name, mimeType, size, license,
                                    publisherVersion, embargoDate, rightsRetentionStrategy, legalNote, uploadDetails);
        }

        public File buildHiddenFile() {
            return new HiddenFile(identifier, name, mimeType, size, license, publisherVersion,
                                  embargoDate, rightsRetentionStrategy, legalNote, uploadDetails);
        }

        public File buildUploadedFile() {
            return new UploadedFile(identifier, name, mimeType, size, rightsRetentionStrategy, uploadDetails);
        }

        public File build(Class<? extends File> clazz) {
            if (clazz.equals(RejectedFile.class)) {
                return buildRejectedFile();
            } else if (clazz.equals(OpenFile.class)) {
                return buildOpenFile();
            } else if (clazz.equals(PendingOpenFile.class)) {
                return buildPendingOpenFile();
            } else if (clazz.equals(PendingInternalFile.class)) {
                return buildPendingInternalFile();
            } else if (clazz.equals(InternalFile.class)) {
                return buildInternalFile();
            } else if (clazz.equals(HiddenFile.class)) {
                return buildHiddenFile();
            } else if (clazz.equals(UploadedFile.class)) {
                return buildUploadedFile();
            } else {
                throw new IllegalArgumentException("Invalid file type");
            }
        }
    }
}