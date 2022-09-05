package no.unit.nva.publication;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import java.net.URI;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.publication.model.business.GeneralSupportRequest;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;

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
    
    public static Publication randomPublicationWithoutDoi() {
        return randomPublication().copy().withDoi(null).build();
    }
    
    public static Publication createUnpersistedPublication(UserInstance userInstance) {
        return randomPublicationWithoutDoi()
                   .copy()
                   .withResourceOwner(new ResourceOwner(userInstance.getUserIdentifier(), randomOrgUnitId()))
                   .withPublisher(createOrganization(userInstance.getOrganizationUri()))
                   .build();
    }
    
    public static Organization createOrganization(URI orgUri) {
        return new Organization.Builder().withId(orgUri).build();
    }
    
    public static PublishingRequestCase createPublishingRequest(Publication publication) {
        return PublishingRequestCase.createOpeningCaseObject(UserInstance.fromPublication(publication),
            publication.getIdentifier());
    }
    
    public static GeneralSupportRequest createGeneralSupportRequest(Publication publication) {
        return (GeneralSupportRequest) TicketEntry.requestNewTicket(publication, GeneralSupportRequest.class);
    }
}
