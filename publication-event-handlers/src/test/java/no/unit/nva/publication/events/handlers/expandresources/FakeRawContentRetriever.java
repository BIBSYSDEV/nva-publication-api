package no.unit.nva.publication.events.handlers.expandresources;

import java.net.URI;
import java.nio.file.Path;
import java.util.Optional;
import no.unit.nva.publication.external.services.RawContentRetriever;
import nva.commons.core.ioutils.IoUtils;

public class FakeRawContentRetriever implements RawContentRetriever {

    private String response;

    public FakeRawContentRetriever() {
        this.response = mockIdentityServiceResponseForPublisherNotAllowingAutomaticPublishingRequestApproval();
    }

    public void setResponse(String response) {
        this.response = response;
    }

    private static String mockIdentityServiceResponseForPublisherNotAllowingAutomaticPublishingRequestApproval() {
        return IoUtils.stringFromResources(Path.of("publishingrequests", "customers",
                                                   "customer_forbidding_publishing.json"));
    }

    @Override
    public Optional<String> getRawContent(URI uri, String mediaType) {
        return Optional.of(response);
    }
}
