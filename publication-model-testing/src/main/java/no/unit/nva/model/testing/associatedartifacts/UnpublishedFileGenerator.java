package no.unit.nva.model.testing.associatedartifacts;

import static no.unit.nva.testutils.RandomDataGenerator.randomInstant;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import java.util.UUID;
import no.unit.nva.model.Username;
import no.unit.nva.model.associatedartifacts.file.PublisherVersion;
import no.unit.nva.model.associatedartifacts.file.UnpublishedFile;
import no.unit.nva.model.associatedartifacts.file.UploadDetails;
import no.unit.nva.model.associatedartifacts.file.UserUploadDetails;
import no.unit.nva.model.testing.associatedartifacts.util.RightsRetentionStrategyGenerator;

public final class UnpublishedFileGenerator {

    private UnpublishedFileGenerator() {
        // NO-OP
    }

    public static UnpublishedFile random() {
        return new UnpublishedFile(UUID.randomUUID(), randomString(), randomString(), randomInteger().longValue(),
                                   randomUri(), false, PublisherVersion.ACCEPTED_VERSION, null,
                                   RightsRetentionStrategyGenerator.randomRightsRetentionStrategy(), randomString(),
                                   randomInserted());
    }

    private static UploadDetails randomInserted() {
        return new UserUploadDetails(randomUsername(), randomInstant());
    }

    private static Username randomUsername() {
        return new Username(randomString());
    }
}
