package no.unit.nva.model.associatedartifacts.file;

import java.time.Instant;
import java.util.UUID;
import no.unit.nva.model.associatedartifacts.RightsRetentionStrategy;

public class PendingOpenFile extends File {
    public static final String TYPE = "PendingOpenFile";

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
     * @param administrativeAgreement True if the file is an administrative agreement
     * @param publisherVersion        True if the file owner has publisher authority
     * @param embargoDate             The date after which the file may be published
     * @param rightsRetentionStrategy The rights retention strategy for the file
     * @param legalNote
     * @param publishedDate
     * @param uploadDetails           Information regarding who and when inserted the file into the system
     */
    protected PendingOpenFile(UUID identifier, String name, String mimeType, Long size, Object license,
                              boolean administrativeAgreement, PublisherVersion publisherVersion,
                              Instant embargoDate,
                              RightsRetentionStrategy rightsRetentionStrategy,
                              String legalNote, Instant publishedDate, UploadDetails uploadDetails) {
        super(identifier, name, mimeType, size, license, administrativeAgreement, publisherVersion, embargoDate,
              rightsRetentionStrategy, legalNote, publishedDate, uploadDetails);
    }

    @Override
    public boolean isVisibleForNonOwner() {
        return false;
    }

    @Override
    public Builder copy() {
        return builder()
                   .withIdentifier(this.getIdentifier())
                   .withName(this.getName())
                   .withMimeType(this.getMimeType())
                   .withSize(this.getSize())
                   .withLicense(this.getLicense())
                   .withAdministrativeAgreement(this.isAdministrativeAgreement())
                   .withPublisherVersion(this.getPublisherVersion())
                   .withEmbargoDate(this.getEmbargoDate().orElse(null))
                   .withRightsRetentionStrategy(this.getRightsRetentionStrategy())
                   .withLegalNote(this.getLegalNote())
                   .withUploadDetails(this.getUploadDetails());
    }
}
