package no.unit.nva.publication.publishingrequest;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.publication.PublicationServiceConfig.API_HOST;
import static no.unit.nva.publication.PublicationServiceConfig.PUBLICATION_PATH;
import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomInstant;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.MessageDto;
import no.unit.nva.publication.model.business.MessageType;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.testing.TypeProvider;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class TicketDtoTest {
    
    public static Stream<Class<?>> ticketTypeProvider() {
        return TypeProvider.listSubTypes(TicketEntry.class);
    }
    
    @ParameterizedTest(name = "ticket type: {0}")
    @DisplayName("should convert Business Object to Data Transfer Object and back without information loss")
    @MethodSource("ticketTypeProvider")
    void shouldConvertDtoToBoAndBackWithoutInformationLoss(Class<? extends TicketEntry> ticketType) {
        var originalDto = createRandomDto(ticketType);
        var json = originalDto.toString();
        var parsedDto = TicketDto.fromJson(json);
        var bo = parsedDto.toTicket();
        var regeneratedDto = TicketDto.fromTicket(bo);
        assertThat(originalDto, doesNotHaveEmptyValues());
        assertThat(regeneratedDto, is(equalTo(originalDto)));
    }
    
    private TicketDto createRandomDto(Class<? extends TicketEntry> ticketType) {
        var publicationId = randomPublicationId();
        var ticketIdentifier = SortableIdentifier.next();
        return TicketDto.builder()
                   .withPublicationId(publicationId)
                   .withVersion(UUID.randomUUID())
                   .withModifiedDate(randomInstant())
                   .withIdentifier(ticketIdentifier)
                   .withCreatedDate(randomInstant())
                   .withId(createTicketId(publicationId, ticketIdentifier))
                   .withStatus(randomElement(TicketStatus.values()))
                   .withMessages(randomMessage())
                   .build(ticketType);
    }
    
    private static List<MessageDto> randomMessage() {
        var message = new MessageDto();
        message.setMessageId(randomUri());
        message.setMessageIdentifier(SortableIdentifier.next());
        message.setMessageType(randomElement(MessageType.values()).toString());
        message.setDate(randomInstant());
        message.setStatus(TicketStatus.UNREAD);
        message.setText(randomString());
        message.setSenderIdentifier(randomString());
        message.setRecipient(randomString());
        message.setOwnerIdentifier(randomString());
        assertThat(message, doesNotHaveEmptyValues());
        return List.of(message);
    }
    
    private URI createTicketId(URI publicationId, SortableIdentifier ticketIdentifier) {
        return UriWrapper.fromUri(publicationId)
                   .addChild(TicketUtils.TICKET_IDENTIFIER_PATH_PARAMETER)
                   .addChild(ticketIdentifier.toString())
                   .getUri();
    }
    
    private static URI randomPublicationId() {
        return UriWrapper.fromHost(API_HOST)
                   .addChild(PUBLICATION_PATH)
                   .addChild(SortableIdentifier.next().toString())
                   .getUri();
    }
}