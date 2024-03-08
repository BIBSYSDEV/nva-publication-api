package no.unit.nva.publication.rightsretention;

import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static nva.commons.apigateway.AccessRight.MANAGE_DOI;
import static nva.commons.apigateway.AccessRight.MANAGE_PUBLISHING_REQUESTS;
import static nva.commons.apigateway.AccessRight.SUPPORT;
import static org.junit.jupiter.api.Assertions.*;
import java.util.stream.Stream;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.instancetypes.journal.AcademicArticle;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.GeneralSupportRequest;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UnpublishRequest;
import nva.commons.apigateway.AccessRight;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class RightsRetentionsApplierTest {

    @Test
    @ParameterizedTest
    @MethodSource("ticketTypeAndAccessRightProvider")

    public void shouldForceNullRightsRetentionIfNotAcademicArticle(PublicationStatus publicationStatus,
                                                                   Class<? extends TicketEntry> ticketType,
                                                                   AccessRight accessRight) {
        RightsRetentionsApplier applier = new RightsRetentionsApplier();
        assertNotNull(applier);
    }


    public static Stream<Arguments> publicationTypeAndForceNull() {
        return Stream.of(Arguments.of(AcademicArticle, DoiRequest.class, MANAGE_DOI),
                         Arguments.of(PublicationStatus.DRAFT, PublishingRequestCase.class,
                                      MANAGE_PUBLISHING_REQUESTS),
                         Arguments.of(PUBLISHED, UnpublishRequest.class, MANAGE_PUBLISHING_REQUESTS),
                         Arguments.of(PUBLISHED, GeneralSupportRequest.class, SUPPORT));
    }

}