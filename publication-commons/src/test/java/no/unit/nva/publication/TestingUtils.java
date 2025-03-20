package no.unit.nva.publication;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import java.net.URI;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
import no.unit.nva.publication.model.business.GeneralSupportRequest;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UnpublishRequest;
import no.unit.nva.publication.model.business.UserInstance;

public final class TestingUtils extends TestDataSource {
    
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
        var publication = randomPublication().copy().withDoi(null).build();
        publication.getEntityDescription().setPublicationDate(new PublicationDate.Builder().withYear("2020").build());
        return publication;
    }
    
    public static Publication createUnpersistedPublication(UserInstance userInstance) {
        return randomPublicationWithoutDoi().copy()
                   .withResourceOwner(new ResourceOwner(new Username(userInstance.getUsername()), randomOrgUnitId()))
                   .withPublisher(createOrganization(userInstance.getCustomerId()))
                   .build();
    }
    
    public static Organization createOrganization(URI orgUri) {
        return new Organization.Builder().withId(orgUri).build();
    }
    
    public static GeneralSupportRequest createGeneralSupportRequest(Publication publication) {
        return GeneralSupportRequest.create(Resource.fromPublication(publication), UserInstance.fromPublication(publication));
    }

    public static UnpublishRequest createUnpublishRequest(Publication publication) {
        return (UnpublishRequest) TicketEntry.requestNewTicket(publication, UnpublishRequest.class)
                                      .withOwner(UserInstance.fromPublication(publication).getUsername());
    }
}
