package no.unit.nva.publication.model.business;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.Clock;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import org.junit.jupiter.api.Test;

class UserInstanceTest {
    
    @Test
    void shouldReturnUserInstanceFromPublication() {
        Publication publication = PublicationGenerator.randomPublication();
        var userInstance = UserInstance.fromPublication(publication);
        assertThat(userInstance.getUserIdentifier(), is(equalTo(publication.getResourceOwner().getOwner())));
        assertThat(userInstance.getOrganizationUri(), is(equalTo(publication.getPublisher().getId())));
    }
    
    @Test
    void shouldReturnUserInstanceFromDoiRequest() {
        Publication publication = PublicationGenerator.randomPublication();
        var doiRequest = DoiRequest.fromPublication(publication, SortableIdentifier.next());
        var userInstance = UserInstance.fromDoiRequest(doiRequest);
        assertThat(userInstance.getUserIdentifier(), is(equalTo(publication.getResourceOwner().getOwner())));
        assertThat(userInstance.getOrganizationUri(), is(equalTo(publication.getPublisher().getId())));
    }
    
    @Test
    void shouldReturnUserInstanceFromMessage() {
        Publication publication = PublicationGenerator.randomPublication();
        
        var message = Message.create(UserInstance.fromPublication(publication), publication, randomString(),
            SortableIdentifier.next(), Clock.systemDefaultZone(), MessageType.SUPPORT);
        var userInstance = UserInstance.fromMessage(message);
        assertThat(userInstance.getUserIdentifier(), is(equalTo(publication.getResourceOwner().getOwner())));
        assertThat(userInstance.getOrganizationUri(), is(equalTo(publication.getPublisher().getId())));
    }
    
    @Test
    void shouldReturnUserInstanceFromRequestInfo() throws JsonProcessingException, UnauthorizedException {
        var customerId = randomUri();
        var username = randomString();
        var httpRequest = new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
                              .withCustomerId(customerId)
                              .withNvaUsername(username)
                              .build();
        var requestInfo = RequestInfo.fromRequest(httpRequest);
        var userInstance = UserInstance.fromRequestInfo(requestInfo);
        assertThat(userInstance.getUserIdentifier(), is(equalTo(username)));
        assertThat(userInstance.getOrganizationUri(), is(equalTo(customerId)));
    }
}