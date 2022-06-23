package no.unit.nva.publication.publishingrequest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class PublishingRequestUtilsTest {

    @Test
    void shouldReturnDefaultRequestService() {
        assertNotNull(PublishingRequestUtils.defaultRequestService());
    }

    @Test
    void shouldReturnPublishingRequestUtilsToKeepTestCoverageHappy() {
        assertNotNull(new PublishingRequestUtils());
    }

}
