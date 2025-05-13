package no.unit.nva.publication.events.handlers.batch;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import java.net.URI;
import java.util.Locale;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.publicationchannel.Constraint;
import nva.commons.core.paths.UriWrapper;

public record ChannelUpdateEvent(Action action,
                                 @JsonProperty("data") PublicationChannelSummary publicationChannelSummary)
    implements JsonSerializable {

    public SortableIdentifier getChannelIdentifier() {
        var identifier = UriWrapper.fromUri(publicationChannelSummary().id()).getLastPathElement().toLowerCase(Locale.ROOT);
        return new SortableIdentifier(identifier);
    }

    public enum Action {
        ADDED("Added"), REMOVED("Removed"), UPDATED("Updated");
        private final String value;

        Action(String value) {
            this.value = value;
        }

        @JsonValue
        public String getValue() {
            return value;
        }
    }

    public record PublicationChannelSummary(URI id, URI channelId, URI customerId, URI organizationId,
                                            Constraint constraint) {

    }
}
