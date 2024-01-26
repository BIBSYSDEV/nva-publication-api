package no.unit.nva.publication.ticket;

import static no.unit.nva.model.PublicationStatus.DRAFT;
import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.model.PublicationStatus.PUBLISHED_METADATA;
import java.util.stream.Stream;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.GeneralSupportRequest;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

public class TicketAndPublicationStatusProvider implements ArgumentsProvider {

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
        return Stream.of(Arguments.of(DoiRequest.class, PUBLISHED),
                         Arguments.of(DoiRequest.class, PUBLISHED_METADATA),
                         Arguments.of(PublishingRequestCase.class, DRAFT),
                         Arguments.of(GeneralSupportRequest.class, DRAFT));
    }
}
