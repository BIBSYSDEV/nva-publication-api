package no.unit.nva.model.testing.associatedartifacts;

import static no.unit.nva.testutils.RandomDataGenerator.randomInstant;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import java.util.UUID;
import no.unit.nva.model.Username;
import no.unit.nva.model.associatedartifacts.file.AdministrativeAgreement;
import no.unit.nva.model.associatedartifacts.file.PublisherVersion;
import no.unit.nva.model.associatedartifacts.file.UploadDetails;

public final class AdministrativeAgreementGenerator {

    private AdministrativeAgreementGenerator() {
        // NO-OP
    }

    public static AdministrativeAgreement random() {
        return new AdministrativeAgreement(UUID.randomUUID(), randomString(), randomString(),
                                           randomInteger().longValue(), randomUri(), true, PublisherVersion.ACCEPTED_VERSION, null,
                                           randomInserted());
    }

    private static UploadDetails randomInserted() {
        return new UploadDetails(randomUsername(), randomInstant());
    }

    private static Username randomUsername() {
        return new Username(randomString());
    }
}
