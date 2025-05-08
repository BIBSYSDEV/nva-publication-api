package no.unit.nva.publication.events.handlers.batch;

import java.net.URI;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.publicationchannel.Constraint;
import nva.commons.core.paths.UriWrapper;

public record ChannelUpdateEvent(Action action, PublicationChannelSummary publicationChannelSummary) {

    public SortableIdentifier getChannelIdentifier() {
        var identifier = UriWrapper.fromUri(publicationChannelSummary().id()).getLastPathElement();
        return new SortableIdentifier(identifier);
    }

    public enum Action {
        ADDED, UPDATED, REMOVED
    }

    public record PublicationChannelSummary(URI id, URI channelId, URI customerId, URI organizationId,
                                            Constraint constraint) {

    }
}
