package no.unit.nva.publication;

import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import java.net.URI;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.storage.model.PublishingRequest;
import no.unit.nva.publication.storage.model.UserInstance;

public final class TestingUtils {

    private TestingUtils() {

    }

    public static URI randomOrgUnitId() {
        return URI.create(String.format("https://example.org/some/path/%s.%s.%s.%s",
                                        randomInteger(),
                                        randomInteger(),
                                        randomInteger(),
                                        randomInteger()));
    }

    public static UserInstance randomUserInstance() {
        return UserInstance.create(randomString(), randomUri());
    }

    public static Publication createPublicationForUser(UserInstance userInstance) {
        return PublicationGenerator.randomPublication()
            .copy()
            .withResourceOwner(new ResourceOwner(userInstance.getUserIdentifier(), randomOrgUnitId()))
            .withPublisher(createOrganization(userInstance.getOrganizationUri()))
            .build();
    }

    public static Organization createOrganization(URI orgUri) {
        return new Organization.Builder().withId(orgUri).build();
    }

    public static PublishingRequest createPublishingRequest(Publication publication) {
        return PublishingRequest.create(UserInstance.fromPublication(publication), publication.getIdentifier());
    }
}
