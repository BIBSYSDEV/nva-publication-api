package no.unit.nva.publication.ticket;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.assertEquals;

import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.MessageStatus;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class MessageDtoTest {

  @ParameterizedTest
  @EnumSource(MessageStatus.class)
  void shouldHaveSameStatusAsMessage(MessageStatus status) {
    var message = Message.builder().withText(randomString()).withStatus(status).build();

    var messageDto = MessageDto.fromMessage(message);

    assertEquals(messageDto.getStatus(), message.getStatus());
  }
}
