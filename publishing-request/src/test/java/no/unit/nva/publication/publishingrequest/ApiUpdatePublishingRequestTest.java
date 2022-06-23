package no.unit.nva.publication.publishingrequest;

import no.unit.nva.publication.exception.BadRequestException;
import no.unit.nva.publication.storage.model.PublishingRequestStatus;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ApiUpdatePublishingRequestTest {

    public static final String RANDOM_MESSAGE = "random message";

    @Test
    void shouldThrowBadRequestExceptionWhenNoPublishingStatusIsAssigned() throws BadRequestException {
        ApiUpdatePublishingRequest apiUpdatePublishingRequest = new ApiUpdatePublishingRequest();
        assertThrows(BadRequestException.class, () -> apiUpdatePublishingRequest.validate());
    }

    @Test
    void shouldApiUpdatePublishingRequest() throws BadRequestException {
        ApiUpdatePublishingRequest apiUpdatePublishingRequest = new ApiUpdatePublishingRequest();
        apiUpdatePublishingRequest.setPublishingRequestStatus(PublishingRequestStatus.PENDING);
        apiUpdatePublishingRequest.validate();
        assertThat(apiUpdatePublishingRequest.getPublishingRequestStatus(), equalTo(PublishingRequestStatus.PENDING));
        apiUpdatePublishingRequest.setMessage(RANDOM_MESSAGE);
        apiUpdatePublishingRequest.setPublishingRequestStatus(PublishingRequestStatus.APPROVED);
        apiUpdatePublishingRequest.validate();
        assertThat(apiUpdatePublishingRequest.getMessage().get(), equalTo(RANDOM_MESSAGE));
    }
}
